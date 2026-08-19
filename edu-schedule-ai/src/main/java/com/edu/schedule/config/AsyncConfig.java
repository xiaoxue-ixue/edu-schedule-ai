package com.edu.schedule.config;

import com.edu.schedule.config.AsyncProperties.PoolProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自定义 @Async 线程池配置
 *
 * 【为什么替换 Spring 默认 SimpleAsyncTaskExecutor】
 *   1. SimpleAsyncTaskExecutor 每次调用都会 NEW THREAD，不池化、不复用；
 *      排课是重型任务，同时提交 10+ 任务就会创建 10+ 线程，加上每个线程持有 DB 连接、
 *      调用 AI API、写入排课明细，很快导致：线程数爆炸 → OOM → DB 连接池耗尽。
 *   2. 本配置提供两个有界线程池 + 有界队列 + CallerRunsPolicy（背压），彻底避免资源失控。
 *
 * 【使用方式】
 *   - 排课 Agent：          @Async("scheduleExecutor")        —— 重型
 *   - 系统批量/通知：       @Async("sysExecutor")             —— 轻量
 *   - 忘了写名字的 @Async： 会走 getAsyncExecutor() 默认 = scheduleExecutor
 *      （避免误落到 SimpleAsyncTaskExecutor）
 *
 * 【拒绝策略 CallerRunsPolicy】
 *   队列满后，不抛异常、不丢任务，而是让 HTTP 提交线程自己执行；
 *   效果 = 前端提交请求会变慢（阻塞等待任务完成），从而自然形成「背压」，
 *   管理员会感知到系统繁忙、不再疯狂点提交。
 *
 * 【注意】全项目只实现了这一个 AsyncConfigurer，避免 Spring 报错：
 *   "Only one AsyncConfigurer may exist"
 */
@Configuration
@EnableConfigurationProperties(AsyncProperties.class)
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    private final AsyncProperties props;

    public AsyncConfig(AsyncProperties props) {
        this.props = props;
    }

    /**
     * 排课专用线程池 —— 设为 @Primary，
     * 一旦未来有新的 Bean 注入 Executor 不指定名字时优先用它。
     */
    @Bean("scheduleExecutor")
    @Primary
    public ThreadPoolTaskExecutor scheduleExecutor() {
        return buildTaskExecutor("scheduleExecutor", props.getSchedule());
    }

    /** 轻量系统任务线程池（批量导入学生、系统通知、邮件等） */
    @Bean("sysExecutor")
    public ThreadPoolTaskExecutor sysExecutor() {
        return buildTaskExecutor("sysExecutor", props.getSys());
    }

    /**
     * 全局默认 @Async 执行器。
     * 这样即便以后在其他类里写 @Async 忘了显式指定名字，也会进入排课池，
     * 而不是 SimpleAsyncTaskExecutor（无界 newThread）。
     */
    @Override
    public Executor getAsyncExecutor() {
        return scheduleExecutor();
    }

    /**
     * @Async 方法的未捕获异常兜底处理器（即使 ScheduleAgentService.executeAsync 自己 try/catch 了，
     * 这里再做一层 SLF4J 日志，防止未来新增的异步方法漏处理导致异常悄悄消失）。
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                StringBuilder sb = new StringBuilder(256);
                sb.append("[@Async 未捕获异常] method=").append(method.getDeclaringClass().getSimpleName())
                  .append('#').append(method.getName())
                  .append(", args=");
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(params[i] == null ? "null" : params[i].toString());
                }
                log.error(sb.toString(), ex);
                // 额外打一段 stderr，方便管理员在 IDE / nohup 日志里立刻看到红标
                System.err.println("\n\n" + sb + "\n异常详情见 application log（默认 ERROR 级会落盘）\n");
                ex.printStackTrace(System.err);
                System.err.println();
            }
        };
    }

    // ----------------------------------------------------------------
    // 工具：统一构造 ThreadPoolTaskExecutor
    // ----------------------------------------------------------------
    private ThreadPoolTaskExecutor buildTaskExecutor(String beanName, PoolProps p) {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setBeanName(beanName);
        e.setCorePoolSize(p.getCorePoolSize());
        e.setMaxPoolSize(p.getMaxPoolSize());
        e.setQueueCapacity(p.getQueueCapacity());
        e.setKeepAliveSeconds(p.getKeepAliveSeconds());
        e.setThreadNamePrefix(p.getThreadNamePrefix());
        // 允许核心线程超时（即使 core 也可以被回收，避免常年占着 2 条空闲栈内存）
        e.setAllowCoreThreadTimeOut(true);
        // 优雅停机：Spring Boot shutdown 时等任务跑完（最多 awaitTerminationSeconds 秒）
        e.setWaitForTasksToCompleteOnShutdown(true);
        e.setAwaitTerminationSeconds(p.getAwaitTerminationSeconds());
        // 关键：队列/线程都用完时，用调用者线程自己跑（背压），不丢任务
        e.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        e.initialize();

        log.info("🧵 [{}] 线程池初始化完成: core={}, max={}, queue={}, keepAlive={}s, prefix={}, policy=CallerRunsPolicy",
                beanName, p.getCorePoolSize(), p.getMaxPoolSize(), p.getQueueCapacity(),
                p.getKeepAliveSeconds(), p.getThreadNamePrefix());
        return e;
    }
}