<script setup>
import { computed } from 'vue'
import { getUser } from '../utils/auth'

const user = computed(() => getUser())
</script>

<template>
  <div class="card">
    <h1>CampusShare</h1>
    <p>校园资源共享与预约平台</p>
    <p v-if="user">
      当前登录：{{ user.nickname || user.username }}（{{ user.role === 'ADMIN' ? '管理员' : '用户' }}）
    </p>
    <div style="display:flex;gap:12px">
      <router-link to="/resources"><button class="primary">浏览资源</button></router-link>
      <router-link to="/reservations"><button>我的预约</button></router-link>
    </div>
  </div>
</template>
