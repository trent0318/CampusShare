<script setup>
import NavBar from './components/NavBar.vue'
import { messages } from './utils/message'
</script>

<template>
  <NavBar />
  <main class="container">
    <router-view />
  </main>

  <div class="toast-wrap">
    <div v-for="m in messages" :key="m.id" class="toast" :class="m.type">
      {{ m.text }}
    </div>
  </div>
</template>
