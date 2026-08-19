import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  // 2026-08-15: 由 300000(5min) → 900000(15min)
  // 注：Agent 排课已改为后端 @Async 异步执行，前端 executeAgent 接口 100ms 内就会返回「已提交」，
  // 不会再触发此 timeout。这里调大纯粹作为其他长耗时接口的安全网（如冲突检测批量、课表版本生成等）
  timeout: 900000
})

// 请求拦截器：携带 token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('edu_token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 code != 200 / 401 / 403
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非统一返回结构（如文件流）直接返回
    if (res === null || typeof res !== 'object' || typeof res.code === 'undefined') {
      return res
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        handleUnauthorized()
      }
      // 403 已在上方 ElMessage 提示了 message，无需额外动作
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  (error) => {
    const status = error.response && error.response.status
    const bodyMsg = error.response?.data?.message
    if (status === 401) {
      handleUnauthorized()
    } else if (status === 403) {
      // 权限不足：不要清 token，不要跳登录，直接弹业务提示
      ElMessage.error(bodyMsg || error.message || '抱歉，您没有权限访问该资源')
    } else {
      ElMessage.error(bodyMsg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  localStorage.removeItem('edu_token')
  localStorage.removeItem('edu_user')
  const path = router.currentRoute.value.fullPath
  if (!path.startsWith('/login')) {
    router.push(`/login?redirect=${encodeURIComponent(path)}`)
  }
}

export default service