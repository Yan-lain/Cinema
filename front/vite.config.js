import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),// 根路径别名
    }
  },
  server: {
    host: '0.0.0.0',    // 允许外部访问
    port: 5173,         // 默认端口
    proxy: {            // API 代理配置
      '/api': {
        target: 'http://localhost:8080',// 后端服务器地址
        secure: false, // 不使用 HTTPS
        //rewrite: (path) => path.replace(/^\/api/, ''),// 重写路径，移除 /api 前缀
        changeOrigin: true // 改变源地址为后端服务器地址
      }
    }
  }
})