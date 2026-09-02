<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/auth'
import { showSuccess, showError } from '../utils/message'

const router = useRouter()
const form = ref({ username: '', password: '', confirmPassword: '', nickname: '', phone: '' })
const loading = ref(false)

async function submit() {
  if (!form.value.username || !form.value.password) {
    showError('请输入用户名和密码')
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    showError('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await register({
      username: form.value.username,
      password: form.value.password,
      nickname: form.value.nickname || undefined,
      phone: form.value.phone || undefined
    })
    showSuccess('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // 拦截器已提示错误
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-card">
    <h1>CampusShare 注册</h1>
    <p class="auth-sub">创建一个账号，开始预约</p>
    <form @submit.prevent="submit">
      <label>用户名（3~50 字符）</label>
      <input v-model="form.username" autocomplete="username" />
      <label>密码（至少 6 位）</label>
      <input v-model="form.password" type="password" autocomplete="new-password" />
      <label>确认密码</label>
      <input v-model="form.confirmPassword" type="password" autocomplete="new-password" />
      <label>昵称（可选）</label>
      <input v-model="form.nickname" />
      <label>手机号（可选）</label>
      <input v-model="form.phone" />
      <button class="primary btn-block" type="submit" :disabled="loading">
        {{ loading ? '注册中...' : '注册' }}
      </button>
    </form>
    <p class="tip">已有账号？<router-link to="/login">去登录</router-link></p>
  </div>
</template>
