<script setup>
import { computed } from 'vue'
import { getUser } from '../utils/auth'

const user = computed(() => getUser())
</script>

<template>
  <div>
    <div class="hero">
      <h1>CampusShare</h1>
      <p class="subtitle">校园资源共享与预约平台</p>
      <p v-if="user" class="welcome">
        你好，{{ user.nickname || user.username }}（{{ user.role === 'ADMIN' ? '管理员' : '用户' }}）
      </p>
      <div class="hero-actions">
        <router-link to="/resources"><button class="primary">浏览资源</button></router-link>
        <router-link to="/reservations"><button>我的预约</button></router-link>
      </div>
    </div>

    <div class="feature-grid">
      <div class="feature">
        <h3>浏览资源</h3>
        <p>查看可预约的物品与场地</p>
      </div>
      <div class="feature">
        <h3>在线预约</h3>
        <p>选择时段，一键预约</p>
      </div>
      <div class="feature">
        <h3>按时签到</h3>
        <p>使用后签到、完成</p>
      </div>
    </div>
  </div>
</template>
