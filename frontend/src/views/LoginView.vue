<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '../api/auth'
import { setAuth } from '../utils/auth'
import { showSuccess, showError } from '../utils/message'

const router = useRouter()
const route = useRoute()
const form = ref({ username: '', password: '' })
const loading = ref(false)

async function submit() {
  if (!form.value.username || !form.value.password) {
    showError('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await login(form.value)
    setAuth(data.token, data.user)
    showSuccess('登录成功')
    router.push(route.query.redirect || '/')
  } catch (e) {
    // 拦截器已提示错误
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-card">
    <h1>CampusShare 登录</h1>
    <form @submit.prevent="submit">
      <label>用户名</label>
      <input v-model="form.username" autocomplete="username" />
      <label>密码</label>
      <input v-model="form.password" type="password" autocomplete="current-password" />
      <button class="primary" type="submit" :disabled="loading" style="margin-top:16px;width:100%">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>
    <p class="tip">没有账号？<router-link to="/register">去注册</router-link></p>
  </div>
</template>
