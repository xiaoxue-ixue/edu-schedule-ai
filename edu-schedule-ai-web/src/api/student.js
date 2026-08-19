import request from '@/utils/request'

export function getStudentSchedule(params) {
  return request({ url: '/student/schedule', method: 'get', params })
}

export function getStudentAnnouncement(params) {
  return request({ url: '/student/announcement', method: 'get', params })
}

export function submitEvaluation(data) {
  return request({ url: '/student/evaluation', method: 'post', data })
}

export function getMyInfo() {
  return request({ url: '/student/my-info', method: 'get' })
}