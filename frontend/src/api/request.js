import axios from 'axios'
import { getToken, clearAuth } from '../utils/auth'
import { showError } from '../utils/message'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：自动注入 JWT
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：后端有两条错误通道
// 1) 业务/校验错误：HTTP 200 + body.code != 200
// 2) 未登录/越权：HTTP 401/403 + body.code（SecurityConfig.writeError 直接 setStatus）
request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      showError(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络连接失败'
    if (status === 401) {
      clearAuth()
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
    }
    showError(message)
    return Promise.reject(error)
  }
)

export default request
