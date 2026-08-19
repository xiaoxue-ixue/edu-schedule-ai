package com.edu.schedule.ai.log;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.entity.AiCallLog;
import com.edu.schedule.entity.AiToolCallLog;
import com.edu.schedule.mapper.AiCallLogMapper;
import com.edu.schedule.mapper.AiToolCallLogMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 调用与工具调用日志服务。
 * 通过 ThreadLocal 在一次 Agent 执行内传递 taskId / callLogId，
 * 供 @Tool 方法在执行时回写工具调用日志。
 */
@Service
public class AiCallLogService {

    @Autowired
    private AiCallLogMapper callLogMapper;

    @Autowired
    private AiToolCallLogMapper toolLogMapper;

    private static final ThreadLocal<Long> TASK_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> CALL_LOG_ID = new ThreadLocal<>();

    public void setTaskId(Long taskId) {
        TASK_ID.set(taskId);
    }

    public Long getTaskId() {
        return TASK_ID.get();
    }

    public void setCallLogId(Long callLogId) {
        CALL_LOG_ID.set(callLogId);
    }

    public Long getCallLogId() {
        return CALL_LOG_ID.get();
    }

    public void clearContext() {
        TASK_ID.remove();
        CALL_LOG_ID.remove();
    }

    public Long recordCall(AiCallLog log) {
        // 超长字段兜底截断，防止 MySQL DataTruncation 导致整条插入失败
        // TEXT 类型最多 65535 字节，这里保守留一些余量
        log.setPrompt(truncate(log.getPrompt(), 60000));
        log.setResponse(truncate(log.getResponse(), 60000));
        log.setErrorMsg(truncate(log.getErrorMsg(), 60000));
        callLogMapper.insert(log);
        CALL_LOG_ID.set(log.getId());
        return log.getId();
    }

    /**
     * 供 @Tool 方法在执行时记录一次工具调用
     */
    public void recordToolCall(String toolName, String args, String result, boolean success, long durationMs) {
        AiToolCallLog t = new AiToolCallLog();
        t.setCallLogId(CALL_LOG_ID.get());
        t.setTaskId(TASK_ID.get());
        t.setToolName(toolName);
        t.setToolArgs(truncate(args, 4000));
        t.setToolResult(truncate(result, 4000));
        t.setSuccess(success ? 1 : 0);
        t.setDurationMs((int) durationMs);
        toolLogMapper.insert(t);
    }

    public PageResult<AiCallLog> pageCallLogs(Integer page, Integer size, String keyword, Long taskId) {
        PageHelper.startPage(page, size);
        PageInfo<AiCallLog> info = new PageInfo<>(callLogMapper.selectList(keyword, taskId));
        return PageResult.of(page, size, info.getTotal(), info.getList());
    }

    public AiCallLog getCallLog(Long id) {
        return callLogMapper.selectById(id);
    }

    public List<AiToolCallLog> listToolLogsByCallLog(Long callLogId) {
        return toolLogMapper.selectByCallLogId(callLogId);
    }

    public List<AiToolCallLog> listToolLogsByTask(Long taskId) {
        return toolLogMapper.selectByTaskId(taskId);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
