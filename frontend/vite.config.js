import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发代理：前端 5173 → 后端 8080。后端没有 CORS 配置，用代理把 /api 转发过去，
// 浏览器只与 5173 同源通信，彻底避开跨域，也无需改动后端。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
