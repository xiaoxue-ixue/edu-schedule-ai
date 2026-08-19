import request from '@/utils/request'
import { createCrud, termApi as _termApi, announcementApi as _announcementApi, scheduleVersionApi as _scheduleVersionApi, publishScheduleVersion as _publish, teachingEvaluationApi as _teachingEvaluationApi, runLogApi as _runLogApi } from './baseData'

export const termApi = _termApi
export const announcementApi = _announcementApi
export const scheduleVersionApi = _scheduleVersionApi
export const publishScheduleVersion = _publish
export const teachingEvaluationApi = _teachingEvaluationApi
export const runLogApi = _runLogApi

// ========== 排课任务 ==========
export const scheduleTaskApi = createCrud('/schedule-task')

export function executeScheduleAgent(id) {
  return request({ url: `/schedule-task/${id}/agent`, method: 'post' })
}

// ========== 排课明细 ==========
export const scheduleDetailApi = createCrud('/schedule-detail')

// ========== 排课冲突检测 ==========
// ✅ GET /conflict-check 是查列表（分页）
// ✅ POST /conflict-check/check 是主动跑一次冲突检测
export function checkConflict(data) {
  return request({ url: `/conflict-check/check`, method: 'post', data })
}

export const conflictCheckApi = {
  list(params) {
    return request({ url: `/conflict-check`, method: 'get', params })
  },
  handle(id, handleMethod) {
    return request({ url: `/conflict-check/${id}/handle`, method: 'put', data: { handleMethod } })
  }
}

export function offlineScheduleVersion(id) {
  return request({ url: `/schedule-version/${id}/offline`, method: 'post' })
}

export function getScheduleVersionDetail(id) {
  return request({ url: `/schedule-version/${id}/detail`, method: 'get' })
}

// ========== 教师可用时间 ==========
export const teacherAvailableTimeApi = createCrud('/teacher-available-time')

// ========== 教室可用时间 ==========
export const roomAvailableTimeApi = createCrud('/room-available-time')

// ========== 排课规则 ==========
export const scheduleRuleApi = createCrud('/schedule-rule')

// ========== AI 教学质量分析 ==========
export const aiQualityAnalysisApi = {
  list(params) {
    return request({ url: '/ai-quality-analysis', method: 'get', params })
  },
  generate(data) {
    return request({ url: '/ai-quality-analysis/generate', method: 'post', data })
  },
  getById(id) {
    return request({ url: `/ai-quality-analysis/${id}`, method: 'get' })
  },
  remove(id) {
    return request({ url: `/ai-quality-analysis/${id}`, method: 'delete' })
  }
  
}

// 排课明细一键清空
export function clearScheduleDetail() {
  return request({ url: '/schedule-detail/clear', method: 'delete' })
}