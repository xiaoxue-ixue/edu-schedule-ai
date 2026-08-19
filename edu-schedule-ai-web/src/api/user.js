import request from '@/utils/request'

export function listUsers(params) {
  return request({ url: '/users', method: 'get', params })
}

export function getUser(id) {
  return request({ url: `/users/${id}`, method: 'get' })
}

export function createUser(data) {
  return request({ url: '/users', method: 'post', data })
}

export function updateUser(id, data) {
  return request({ url: `/users/${id}`, method: 'put', data })
}

export function deleteUser(id) {
  return request({ url: `/users/${id}`, method: 'delete' })
}
export function batchCreateStudents(data) {
  return request({ url: '/users/batch-students', method: 'post', data })
}