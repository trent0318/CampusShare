import { ref } from 'vue'

const TOKEN_KEY = 'campusshare_token'
const USER_KEY = 'campusshare_user'

// 用响应式 ref 保存登录态：localStorage 不是响应式的，直接读它会导致
// 登录后导航栏/首页不刷新。这里用 ref 作为单一数据源，组件读到的是响应式状态。
function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

const token = ref(localStorage.getItem(TOKEN_KEY) || '')
const user = ref(readUser())

export function getToken() {
  return token.value
}

export function setAuth(t, u) {
  token.value = t
  user.value = u
  localStorage.setItem(TOKEN_KEY, t)
  localStorage.setItem(USER_KEY, JSON.stringify(u))
}

export function clearAuth() {
  token.value = ''
  user.value = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getUser() {
  return user.value
}

export function isAdmin() {
  return !!user.value && user.value.role === 'ADMIN'
}
