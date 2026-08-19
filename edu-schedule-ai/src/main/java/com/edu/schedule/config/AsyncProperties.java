package com.edu.schedule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 异步线程池配置参数（与 application.yml 的 edu.async.* 节点绑定）
 *
 * 两个独立的线程池：
 *   - schedule : 重型排课 Agent 任务（CPU + I/O 混合、30s~5min/次）
 *       · 并发不宜过大，否则 AI API / DB 连接同时打爆
 *       · 队列满后走 CallerRunsPolicy（HTTP 调用者线程自己跑，即背压）
 *   - sys      : 轻量系统任务（批量导入、通知、邮件等 IO 短任务）
 *       · 并发可稍大，队列更宽松
 *
 * 生产可在 application-prod.yml 覆盖：edu.async.schedule.core-pool-size=4 等
 */
@ConfigurationProperties(prefix = "edu.async")
public class AsyncProperties {

    private PoolProps schedule = new PoolProps(2, 4, 8, 120, "schedule-");
    private PoolProps sys      = new PoolProps(2, 10, 50, 60, "sys-");

    public static class PoolProps {
        private int corePoolSize;
        private int maxPoolSize;
        private int queueCapacity;
        private int keepAliveSeconds;
        private String threadNamePrefix;
        /** 优雅停机：等待已提交任务结束的最大秒数 */
        private int awaitTerminationSeconds = 60;

        public PoolProps() {}
        public PoolProps(int core, int max, int queue, int keepAlive, String prefix) {
            this.corePoolSize = core; this.maxPoolSize = max; this.queueCapacity = queue;
            this.keepAliveSeconds = keepAlive; this.threadNamePrefix = prefix;
        }

        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        public int getKeepAliveSeconds() { return keepAliveSeconds; }
        public void setKeepAliveSeconds(int keepAliveSeconds) { this.keepAliveSeconds = keepAliveSeconds; }
        public String getThreadNamePrefix() { return threadNamePrefix; }
        public void setThreadNamePrefix(String threadNamePrefix) { this.threadNamePrefix = threadNamePrefix; }
        public int getAwaitTerminationSeconds() { return awaitTerminationSeconds; }
        public void setAwaitTerminationSeconds(int awaitTerminationSeconds) { this.awaitTerminationSeconds = awaitTerminationSeconds; }
    }

    public PoolProps getSchedule() { return schedule; }
    public void setSchedule(PoolProps schedule) { this.schedule = schedule; }
    public PoolProps getSys() { return sys; }
    public void setSys(PoolProps sys) { this.sys = sys; }
}