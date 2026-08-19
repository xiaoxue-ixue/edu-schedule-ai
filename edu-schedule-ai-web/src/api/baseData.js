import request from '@/utils/request'

/**
 * 通用 CRUD 工厂：根据资源路径生成 list/get/create/update/delete 方法
 * @param {string} basePath 资源路径，如 '/term'
 */
export function createCrud(basePath) {
  return {
    list(params) {
      return request({ url: basePath, method: 'get', params })
    },
    get(id) {
      return request({ url: `${basePath}/${id}`, method: 'get' })
    },
    create(data) {
      return request({ url: basePath, method: 'post', data })
    },
    update(id, data) {
      return request({ url: `${basePath}/${id}`, method: 'put', data })
    },
    remove(id) {
      return request({ url: `${basePath}/${id}`, method: 'delete' })
    },
    // ✅ 加别名：delete()，前端所有 .delete(id) 调用都能成功
    delete(id) { return this.remove(id) }
  }
}

// 基础数据
export const termApi = createCrud('/term')
export const campusApi = createCrud('/campus')
export const teacherApi = createCrud('/teacher')
export const clazzApi = createCrud('/clazz')
export const courseApi = createCrud('/course')
export const roomApi = createCrud('/room')
export const timeSlotApi = createCrud('/time-slot')

// 约束配置
export const teacherAvailableTimeApi = createCrud('/teacher-available-time')
export const roomAvailableTimeApi = createCrud('/room-available-time')
export const scheduleRuleApi = createCrud('/schedule-rule')

// 知识库
export const knowledgeApi = createCrud('/knowledge')

// 公告管理
export const announcementApi = createCrud('/announcement')

// 课表版本
export const scheduleVersionApi = createCrud('/schedule-version')
export function publishScheduleVersion(id) {
  return request({ url: `/schedule-version/${id}/publish`, method: 'post' })
}

// 教学评价
export const teachingEvaluationApi = createCrud('/teaching-evaluation')

// 运行日志
export const runLogApi = createCrud('/run-log')

// Function 工具
export const functionToolApi = createCrud('/function-tool')