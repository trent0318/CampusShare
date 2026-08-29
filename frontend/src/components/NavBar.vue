<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getUser, isAdmin, clearAuth } from '../utils/auth'

const router = useRouter()
const user = computed(() => getUser())
const admin = computed(() => isAdmin())

function logout() {
  clearAuth()
  router.push('/login')
}
</script>

<template>
  <header v-if="user" class="navbar">
    <span class="brand">CampusShare</span>
    <nav>
      <router-link to="/">首页</router-link>
      <router-link to="/resources">资源列表</router-link>
      <router-link to="/reservations">我的预约</router-link>
    </nav>
    <div class="right">
      <span>{{ user.nickname || user.username }}（{{ admin ? '管理员' : '用户' }}）</span>
      <button @click="logout">退出登录</button>
    </div>
  </header>
</template>
