import request from '@/utils/request'

export function getMySchedule() {
  return request({ url: '/teacher-portal/my-schedule', method: 'get' })
}

export function getMyEvaluation(params) {
  return request({ url: '/teacher-portal/my-evaluation', method: 'get', params })
}

export function getMyAvailableTime() {
  return request({ url: '/teacher-portal/my-available-time', method: 'get' })
}
