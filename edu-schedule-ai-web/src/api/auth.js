import request from '@/utils/request'

export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}

export function register(data) {
  return request({ url: '/auth/register', method: 'post', data })
}

/** 获取图形验证码（公开接口，无需登录），返回 { captchaKey, imageBase64, expireSeconds } */
export function generateCaptcha() {
  return request({ url: '/captcha/generate', method: 'get' })
}

export function getInfo() {
  return request({ url: '/auth/info', method: 'get' })
}

export function changePassword(data) {
  return request({ url: '/auth/password', method: 'put', data })
}
