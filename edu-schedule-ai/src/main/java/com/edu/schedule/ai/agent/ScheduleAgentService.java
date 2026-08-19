package com.edu.schedule.ai.agent;

import com.edu.schedule.ai.SpringAiModelFactory;
import com.edu.schedule.ai.log.AiCallLogService;
import com.edu.schedule.ai.tool.ScheduleToolSet;
import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.ScheduleConstants;
import com.edu.schedule.entity.AiCallLog;
import com.edu.schedule.entity.AiModelConfig;
import com.edu.schedule.entity.FunctionTool;
import com.edu.schedule.entity.PromptTemplate;
import com.edu.schedule.entity.ScheduleDetail;
import com.edu.schedule.entity.ScheduleTask;
import com.edu.schedule.mapper.AiModelConfigMapper;
import com.edu.schedule.mapper.AiToolCallLogMapper;
import com.edu.schedule.mapper.FunctionToolMapper;
import com.edu.schedule.mapper.PromptTemplateMapper;
import com.edu.schedule.mapper.ScheduleDetailMapper;
import com.edu.schedule.mapper.ScheduleTaskMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 排课 Agent（Tool-Use Agent / ReAct 范式的工程化实现）。
 *
 * 最终版能力：
 *  1. 429 限流自动重试（最多 4 轮，等待 65s 度过 TPM 窗口）
 *  2. 断点续排：已排课程保留，只排剩余的（不浪费 Token 重排）
 *  3. ★ 后端算差集：【待排课程白名单】直接注入 Prompt，不让 AI 自己做集合差（防止 AI 漏排）
 *  4. @Async 异步执行：Controller 立刻返回「运行中」，前端不超时
 */
@Service
public class ScheduleAgentService {

    @Autowired private SpringAiModelFactory modelFactory;
    @Autowired private PromptTemplateMapper promptTemplateMapper;
    @Autowired private ScheduleTaskMapper taskMapper;
    @Autowired private ScheduleDetailMapper detailMapper;
    @Autowired private AiToolCallLogMapper toolLogMapper;
    @Autowired private FunctionToolMapper functionToolMapper;
    @Autowired private AiModelConfigMapper aiModelConfigMapper;
    @Autowired private AiCallLogService callLogService;
    @Autowired private ScheduleToolSet toolSet;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final int MAX_ATTEMPTS = 4;
    private static final int RETRY_WAIT_SECONDS = 65;

    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个高效的高校智能排课 Agent。任务目标: {objective}\n\n" +
                    "工作流程:\n" +
                    "1. 先调用所有查询工具获取完整资源数据（queryCourses, queryTeachers, queryClasses, queryRooms, queryScheduleRules）\n" +
                    "2. 对 User Prompt 中提供的【待排课程白名单】中的每一门课程，严格按顺序处理：选教师→选班级→选教室→选时间→checkScheduleConflict→writeScheduleDetail\n" +
                    "3. 冲突检测通过(pass=true)才能写入，失败则换时间重试最多5次\n" +
                    "4. 白名单中所有课程都处理完毕（无论成功或失败）后，再返回完整总结\n\n" +
                    "关键约束:\n" +
                    "- 只能通过工具读写数据，不要编造任何ID\n" +
                    "- 每节课必须先做冲突检测再写入\n" +
                    "- 课程有自带的 teacherId，优先使用该教师；没有则从 queryTeachers 结果中选\n" +
                    "- writeScheduleDetail 的参数必须用查询工具返回的真实ID，不能自己造\n" +
                    "- ★ 绝对不可跳过白名单中的任何课程。在给出最终总结前，必须自己核对：白名单中的每个课程ID是否都至少调用过一次 checkScheduleConflict。\n" +
                    "- 如果 User Prompt 里还提供了【已排课程黑名单】，这些课程绝对不要再排，直接跳过。";

    // ==================== 同步入口（内部调用 / 调试） ====================

    public Map<String, Object> execute(Long taskId) {
        ScheduleTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("排课任务不存在");
        }
        callLogService.setTaskId(taskId);
        long totalStart = System.currentTimeMillis();

        AiCallLog callLog = new AiCallLog();
        callLog.setTaskId(taskId);
        List<String> attemptSummaries = new ArrayList<>();
        int totalRoundsAccum = 0;
        String lastError = null;

        int initialScheduled = detailMapper.selectListByTask(taskId).size();
        boolean isResumeFromFailure = initialScheduled > 0;

        System.err.println("========================================");
        System.err.println("🚀 开始执行排课任务 #" + taskId + " 【" + task.getTaskName() + "】");
        if (isResumeFromFailure) {
            System.err.println("🔁 断点续排模式：数据库已存在 " + initialScheduled + " 条排课明细，保留 + 续排剩余课程");
        } else {
            System.err.println("🆕 全新排课模式：当前 0 条，从零开始排");
        }
        System.err.println("⏳ 最多尝试 " + MAX_ATTEMPTS + " 轮，遇到 429 限流自动等待 " + RETRY_WAIT_SECONDS + "s");
        System.err.println("========================================");

        taskMapper.updateResult(taskId, ScheduleConstants.TASK_RUNNING, null, 0, initialScheduled);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long attemptStart = System.currentTimeMillis();
            int attemptStartCount = detailMapper.selectListByTask(taskId).size();

            Set<Long> allCourseIds = getAllCourseIds(task);
            Set<Long> scheduledCourseIds = getScheduledCourseIds(taskId);
            Set<Long> todoCourseIds = new LinkedHashSet<>();
            for (Long id : allCourseIds) {
                if (!scheduledCourseIds.contains(id)) todoCourseIds.add(id);
            }

            System.err.println("\n🔄 ===== 第 " + attempt + "/" + MAX_ATTEMPTS + " 轮尝试开始 ====="
                    + " 已排=" + attemptStartCount
                    + " / 总课程=" + allCourseIds.size()
                    + " / 本轮待排=" + todoCourseIds.size()
                    + " 待排ID=" + todoCourseIds);

            if (todoCourseIds.isEmpty()) {
                System.err.println("🎊 所有 " + allCourseIds.size() + " 门课程均已排完！提前结束，不再重试");
                break;
            }

            try {
                AiModelConfig config = resolveModel(task);
                ChatClient client = modelFactory.getChatClient(config.getModelCode());
                callLog.setModelCode(config.getModelCode());
                callLog.setModel(config.getModel());

                String systemPrompt = buildSystemPrompt(task);
                String userPrompt = buildUserPrompt(task, scheduledCourseIds, todoCourseIds, attempt);
                callLog.setPrompt(userPrompt);

                ToolCallback[] enabledTools = buildEnabledTools();
                if (enabledTools.length == 0) {
                    throw new BusinessException("没有启用的排课工具，请先在【Function工具】中启用工具");
                }

                System.err.println("🤖 Agent 模型: " + config.getModelCode() + " / " + config.getModel());
                System.err.println("⏳ Agent 开始自主调用工具排课（第" + attempt + "轮）...");

                ChatResponse response = client.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .toolCallbacks(enabledTools)
                        .call()
                        .chatResponse();

                String content = response.getResult().getOutput().getText();
                attemptSummaries.add("【第" + attempt + "轮成功】" + content);
                totalRoundsAccum = toolLogMapper.countByTaskId(taskId);
                int successCourses = detailMapper.selectListByTask(taskId).size();
                int writtenThisAttempt = successCourses - attemptStartCount;

                System.err.println("✅ 第 " + attempt + " 轮正常结束，本轮新写入 " + writtenThisAttempt
                        + " 条，累计 " + successCourses + "/" + allCourseIds.size() + " 条");
                fillUsage(callLog, response);
                runDiagnostics(taskId, successCourses, content);

                // 深度诊断：对比「白名单」和「实际处理数」，防止 AI 漏排
                Set<Long> stillTodo = new LinkedHashSet<>();
                for (Long id : todoCourseIds) {
                    if (!getScheduledCourseIds(taskId).contains(id)) stillTodo.add(id);
                }
                boolean allTodoProcessed = stillTodo.isEmpty();
                if (!allTodoProcessed) {
                    System.err.println("⚠️  后端校验发现：本轮白名单有 " + todoCourseIds.size()
                            + " 门，但还有 " + stillTodo.size() + " 门没排：" + stillTodo);
                    if (attempt < MAX_ATTEMPTS) {
                        System.err.println("  → 下一轮会把这 " + stillTodo.size() + " 门再次列入白名单，继续处理");
                    }
                }

                if (allTodoProcessed || successCourses >= allCourseIds.size()) {
                    String finalSummary = String.join("\n\n", attemptSummaries);
                    taskMapper.updateResult(taskId, ScheduleConstants.TASK_SUCCESS, finalSummary, totalRoundsAccum, successCourses);
                    callLog.setResponse(finalSummary);
                    callLog.setStatus("SUCCESS");
                    callLog.setDurationMs((int) (System.currentTimeMillis() - totalStart));
                    safeRecordCallLog(callLog);
                    System.err.println("========================================");
                    System.err.println("🎉 排课任务全部完成！共 " + attempt + " 轮，总耗时 "
                            + ((System.currentTimeMillis() - totalStart) / 1000) + " 秒，最终排课 " + successCourses + " 条");
                    System.err.println("========================================");
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("status", "SUCCESS");
                    result.put("result", finalSummary);
                    result.put("totalRounds", totalRoundsAccum);
                    result.put("successCourses", successCourses);
                    result.put("totalCourses", allCourseIds.size());
                    result.put("attempts", attempt);
                    return result;
                }

                if (attempt < MAX_ATTEMPTS) {
                    System.err.println("⏱️  白名单尚未全部处理完，等待 15s 后开始第 " + (attempt + 1) + " 轮续排...");
                    sleepSeconds(15);
                }
                continue;

            } catch (Exception e) {
                String detail = buildErrorMessage(e);
                int currentCount = detailMapper.selectListByTask(taskId).size();
                int writtenThisAttempt = currentCount - attemptStartCount;
                attemptSummaries.add("【第" + attempt + "轮失败】写了 " + writtenThisAttempt + " 条后终止：" + detail);
                lastError = detail;
                totalRoundsAccum = toolLogMapper.countByTaskId(taskId);

                System.err.println("❌ 第 " + attempt + " 轮异常！本轮写入 " + writtenThisAttempt
                        + " 条，累计 " + currentCount + " 条，错误: "
                        + detail.substring(0, Math.min(250, detail.length())));

                boolean isRateLimit = isRateLimitError(detail)
                        || isRateLimitError(e.getMessage() == null ? "" : e.getMessage());

                if (attempt < MAX_ATTEMPTS && (isRateLimit || writtenThisAttempt > 0)) {
                    if (isRateLimit) {
                        System.err.println("⏱️  检测到 429 TPM 限流，等待 " + RETRY_WAIT_SECONDS + " 秒后继续");
                    }
                    sleepSeconds(isRateLimit ? RETRY_WAIT_SECONDS : 15);
                    continue;
                }

                String finalSummary = String.join("\n\n", attemptSummaries);
                taskMapper.updateResult(taskId, ScheduleConstants.TASK_FAIL, finalSummary, totalRoundsAccum, currentCount);
                callLog.setResponse(finalSummary);
                callLog.setStatus("FAIL");
                callLog.setErrorMsg(detail);
                callLog.setDurationMs((int) (System.currentTimeMillis() - totalStart));
                safeRecordCallLog(callLog);

                System.err.println("========================================");
                System.err.println("💀 排课任务彻底失败：已尝试 " + attempt + " 轮，总耗时 "
                        + ((System.currentTimeMillis() - totalStart) / 1000) + " 秒");
                System.err.println("💾 已写入的 " + currentCount + " 条仍保留在数据库中。再次点【执行Agent排课】将自动续排剩下的");
                System.err.println("========================================");
                callLogService.clearContext();
                throw new BusinessException("Agent 执行失败（" + attempt + " 轮后放弃）: " + lastError);
            }
        }

        int finalCount = detailMapper.selectListByTask(taskId).size();
        String summary = String.join("\n\n", attemptSummaries);
        taskMapper.updateResult(taskId, ScheduleConstants.TASK_FAIL,
                summary + "\n\n注: 达到最大尝试轮次仍未全部处理完，剩余课程可再次执行任务续排",
                totalRoundsAccum, finalCount);
        callLog.setResponse(summary);
        callLog.setStatus("FAIL");
        callLog.setDurationMs((int) (System.currentTimeMillis() - totalStart));
        safeRecordCallLog(callLog);
        callLogService.clearContext();
        return Map.of("status", "FAIL", "result", summary, "successCourses", finalCount);
    }

    // ==================== 异步入口（Controller 调用此方法，立刻返回「运行中」） ====================

    @Async("scheduleExecutor")
    public void executeAsync(Long taskId) {
        String threadName = Thread.currentThread().getName();
        System.err.println("🧵 [AsyncAgent] 任务 #" + taskId + " 已提交到后台线程: " + threadName);
        try {
            execute(taskId);
            System.err.println("🧵 [AsyncAgent] 任务 #" + taskId + " 后台执行结束（状态已写回 schedule_task）");
        } catch (Throwable t) {
            System.err.println("🧵 [AsyncAgent] 任务 #" + taskId + " 后台异常: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            try {
                int currentCount = detailMapper.selectListByTask(taskId).size();
                String summary = "【后台异步未捕获异常】" + t.getClass().getSimpleName()
                        + ": " + (t.getMessage() == null ? "null" : t.getMessage());
                taskMapper.updateResult(taskId, ScheduleConstants.TASK_FAIL, summary, 0, currentCount);
            } catch (Exception ignore) {
            }
        }
    }

    // ==================== 辅助方法 ====================

    /** 从 edu_course 拿所有课程 ID（用于计算「白名单=总-已排」，杜绝 AI 自己做差集时漏掉）
     *  ★ 如果 task.getCourseScope() 以 "课程ID:" 开头，则只取指定的课程 ID，尊重用户选择的课程范围
     */
    private Set<Long> getAllCourseIds(ScheduleTask task) {
        try {
            List<Long> list;
            if (task.getCourseScope() != null && task.getCourseScope().startsWith("课程ID:")) {
                String ids = task.getCourseScope().substring("课程ID:".length());
                list = Arrays.stream(ids.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .map(Long::parseLong).collect(Collectors.toList());
            } else {
                list = jdbcTemplate.queryForList(
                        "SELECT id FROM edu_course ORDER BY id", Long.class);
            }
            return new LinkedHashSet<>(list);
        } catch (Exception e) {
            System.err.println("⚠️ 查询 edu_course 全部 ID 失败，兜底使用空集合: " + e.getMessage());
            return new LinkedHashSet<>();
        }
    }

    private Set<Long> getScheduledCourseIds(Long taskId) {
        List<ScheduleDetail> existing = detailMapper.selectListByTask(taskId);
        return existing.stream()
                .map(ScheduleDetail::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isRateLimitError(String msg) {
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("429") || lower.contains("rate limit")
                || lower.contains("tpm limit") || lower.contains("too many requests")
                || lower.contains("code\":50602") || lower.contains("50602");
    }

    private void sleepSeconds(int seconds) {
        try {
            for (int i = seconds; i > 0; i--) {
                if (i == seconds || i % 10 == 0 || i <= 3) {
                    System.err.println("   ⏳ 等待中... 还剩 " + i + " 秒");
                }
                Thread.sleep(1000L);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("⚠️ 等待被中断，立即开始下一轮");
        }
    }

    private void safeRecordCallLog(AiCallLog callLog) {
        try { callLogService.recordCall(callLog); }
        catch (Exception e) { System.err.println("⚠️ 记录 ai_call_log 失败: " + e.getMessage()); }
    }

    private void runDiagnostics(Long taskId, int successCourses, String agentReply) {
        System.err.println("========================================");
        System.err.println("🔍 诊断输出...");
        System.err.println("   schedule_detail 数据库实际条数(task_id=" + taskId + "): " + successCourses);
        if (agentReply != null) {
            System.err.println("   Agent 回复摘要: " + agentReply.substring(0, Math.min(250, agentReply.length())) + "...");
        }
        try {
            List<Map<String, Object>> toolStats = jdbcTemplate.queryForList(
                    "SELECT tool_name, success, COUNT(*) AS cnt FROM ai_tool_call_log " +
                            "WHERE task_id = ? GROUP BY tool_name, success ORDER BY tool_name, success",
                    taskId);
            System.err.println("📊 工具调用明细统计 (ai_tool_call_log):");
            for (Map<String, Object> s : toolStats) {
                System.err.println("   - " + s.get("tool_name")
                        + "  success=" + s.get("success")
                        + "  次数=" + s.get("cnt"));
            }
            int writeSuccess = 0, writeFail = 0;
            for (Map<String, Object> s : toolStats) {
                if ("writeScheduleDetail".equals(s.get("tool_name"))) {
                    if (Integer.valueOf(1).equals(s.get("success"))) {
                        writeSuccess = ((Number) s.get("cnt")).intValue();
                    } else {
                        writeFail = ((Number) s.get("cnt")).intValue();
                    }
                }
            }
            System.err.println("📊 writeScheduleDetail 诊断: 工具成功=" + writeSuccess
                    + " 次, 失败=" + writeFail + " 次, 数据库条数=" + successCourses);
            if (writeSuccess > 0 && successCourses == 0) {
                System.err.println("❌❌❌ 严重异常：工具写入成功 " + writeSuccess + " 次，但数据库 0 条！");
            } else if (writeSuccess != successCourses) {
                System.err.println("⚠️ 数量不一致：工具成功 " + writeSuccess + " 次，数据库 " + successCourses + " 条");
            } else if (writeSuccess > 0) {
                System.err.println("✅ 写入一致性通过：工具成功 " + writeSuccess + " 次 = 数据库 " + successCourses + " 条");
            }
        } catch (Exception diagEx) {
            System.err.println("⚠️ 诊断查询失败（不影响排课）: " + diagEx.getMessage());
        }
        System.err.println("========================================");
    }

    private String buildErrorMessage(Exception e) {
        Throwable root = rootCause(e);
        StringBuilder sb = new StringBuilder();
        if (root instanceof WebClientResponseException wx) {
            sb.append("HTTP ").append(wx.getStatusCode().value()).append(' ').append(wx.getStatusText());
            try {
                String body = wx.getResponseBodyAsString(java.nio.charset.StandardCharsets.UTF_8);
                if (body != null && !body.isBlank()) {
                    String trimmed = body.length() > 800 ? body.substring(0, 800) + "...(已截断)" : body;
                    sb.append(" 响应体: ").append(trimmed);
                }
            } catch (Exception ignore) {
            }
            sb.append(" | 根因: ").append(root.getMessage() == null ? "" : root.getMessage());
        } else {
            sb.append(root.getClass().getSimpleName()).append(": ");
            sb.append(root.getMessage() == null ? "null" : root.getMessage());
        }
        return sb.toString();
    }

    private AiModelConfig resolveModel(ScheduleTask task) {
        if (task.getModelConfigId() != null) {
            AiModelConfig c = aiModelConfigMapper.selectById(task.getModelConfigId());
            if (c != null && c.getEnabled() != null && c.getEnabled() == 1) return c;
        }
        return modelFactory.getDefaultConfig();
    }

    private String buildSystemPrompt(ScheduleTask task) {
        String template = DEFAULT_SYSTEM_PROMPT;
        String code = task.getPromptTemplateCode();
        if (code != null && !code.isBlank()) {
            PromptTemplate pt = promptTemplateMapper.selectByCode(code);
            if (pt != null && pt.getContent() != null) template = pt.getContent();
        }
        String objective = task.getObjective() == null ? "为本学期课程生成无冲突的排课方案" : task.getObjective();
        return template.replace("{objective}", objective);
    }

    /**
     * 最终版 User Prompt：
     *  - scheduledCourseIds：黑名单（已排，跳过）
     *  - todoCourseIds：白名单（必须处理，一门都不能漏）
     *  - 后端算好差集，直接把精确的白名单塞给 AI，不用它自己算集合
     */
    private String buildUserPrompt(ScheduleTask task, Set<Long> scheduledCourseIds, Set<Long> todoCourseIds, int attempt) {
        StringBuilder sb = new StringBuilder();
        sb.append("本次排课任务信息：\n");
        sb.append("- 任务ID: ").append(task.getId()).append("\n");
        sb.append("- 学期ID: ").append(task.getTermId()).append("\n");
        sb.append("- 任务目标: ").append(task.getObjective() == null || task.getObjective().isBlank() ? "为本学期课程排课" : task.getObjective()).append("\n");
        sb.append("- 课程范围: ").append(task.getCourseScope() == null || task.getCourseScope().isBlank() ? "全部课程" : task.getCourseScope()).append("\n");
        sb.append("- 排课策略: ").append(task.getRuleStrategy() == null || task.getRuleStrategy().isBlank() ? "AGENT" : task.getRuleStrategy()).append("\n");
        sb.append("- 当前尝试轮次: 第 ").append(attempt).append(" 轮\n\n");

        if (scheduledCourseIds != null && !scheduledCourseIds.isEmpty()) {
            sb.append("【⚠️ 黑名单：以下 ").append(scheduledCourseIds.size()).append(" 门课已成功排课，数据库已有写入，绝对不能重复排，直接跳过】\n");
            sb.append("已排课程ID: [").append(String.join(", ",
                    scheduledCourseIds.stream().map(String::valueOf).sorted().collect(Collectors.toList()))).append("]\n\n");
        }

        sb.append("============================================================\n");
        sb.append("🔴🔴🔴【最高优先级：待排课程白名单，共 ").append(todoCourseIds.size()).append(" 门，一门都不能漏！】🔴🔴🔴\n");
        sb.append("============================================================\n");
        sb.append("你本次只需要处理下面这 ").append(todoCourseIds.size()).append(" 门课程（按ID顺序）：\n");
        sb.append("【待排课程ID白名单】: [").append(String.join(", ",
                todoCourseIds.stream().map(String::valueOf).collect(Collectors.toList()))).append("]\n");
        sb.append("🔴 注意：白名单里的每一门，你必须至少调用一次 checkScheduleConflict（无论成功失败）。\n");
        sb.append("🔴 在你给最终总结之前，先数一遍：白名单上的ID个数 是否等于 你实际处理过的课程数。\n");
        sb.append("🔴 如果数量对不上，就立刻继续处理遗漏的课程，绝对不要提前总结！\n");
        sb.append("============================================================\n\n");

        sb.append("【你必须严格按以下步骤执行】：\n\n");
        sb.append("第 1 步：调用 queryCourses(taskId=").append(task.getId()).append(") 获取课程完整信息\n");
        sb.append("第 2 步：调用 queryTeachers 获取教师列表（含教师#1和教师#2的姓名）\n");
        sb.append("第 3 步：调用 queryClasses 获取班级列表（含每个班级的 student_count 学生人数）\n");
        sb.append("第 4 步：调用 queryRooms 获取教室列表（含每个教室的 capacity 容量）\n\n");

        sb.append("【排课核心逻辑】对【待排课程白名单】中的每一门课程依次执行：\n");
        sb.append("  a) 优先使用课程自带的 teacher_id（18号用教师2，21号和25号用教师1）；查 queryTeachers 确认教师存在\n");
        sb.append("  b) 选班级：选一个班级 student_count <= 教室 capacity 的组合\n");
        sb.append("  c) 选时间：星期几（1-5优先工作日）和节次（1-8节）\n");
        sb.append("  d) 调用 checkScheduleConflict 检测冲突\n");
        sb.append("  e) pass=true → 立刻 writeScheduleDetail 写入\n");
        sb.append("  f) pass=false → 换时间（或换教室/换班级）重试最多 5 次\n");
        sb.append("  g) 5 次都失败 → 说明真正冲突，记录失败原因，继续下一门白名单课程（绝对不能因为 18 号失败就停）\n\n");

        sb.append("【最终总结的格式要求】处理完白名单所有 ").append(todoCourseIds.size()).append(" 门课之后，返回如下结构：\n");
        sb.append("   本次新排成功：X 门（列出课程ID+课程名+分配到的时间）\n");
        sb.append("   冲突失败：Y 门（列出课程ID+课程名+失败原因）\n");
        sb.append("   跳过已排：").append(scheduledCourseIds.size()).append(" 门\n");
        sb.append("   合计：成功X + 失败Y + 跳过").append(scheduledCourseIds.size()).append(" = ").append(todoCourseIds.size() + scheduledCourseIds.size()).append(" 门\n");
        return sb.toString();
    }

    private ToolCallback[] buildEnabledTools() {
        ToolCallback[] all = ToolCallbacks.from(toolSet);
        Set<String> enabled = functionToolMapper.selectList(null).stream()
                .filter(t -> t.getEnabled() != null && t.getEnabled() == 1)
                .map(FunctionTool::getToolCode)
                .collect(Collectors.toSet());
        ToolCallback[] filtered = Arrays.stream(all)
                .filter(t -> enabled.contains(t.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
        if (filtered.length == 0) {
            System.err.println("⚠️  function_tool 全禁用，兜底启用全部 " + all.length + " 个核心工具");
            return all;
        }
        System.err.println("🔧 本次启用工具 (" + filtered.length + "/" + all.length + "): "
                + Arrays.stream(filtered).map(t -> t.getToolDefinition().name())
                .collect(Collectors.joining(", ")));
        return filtered;
    }

    private void fillUsage(AiCallLog callLog, ChatResponse response) {
        try {
            Usage usage = response.getMetadata().getUsage();
            if (usage != null) {
                callLog.setInputTokens(usage.getPromptTokens());
                callLog.setOutputTokens(usage.getCompletionTokens());
                callLog.setTotalTokens(usage.getTotalTokens());
            }
        } catch (Exception ignore) {
        }
    }

    private Throwable rootCause(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t;
    }
}