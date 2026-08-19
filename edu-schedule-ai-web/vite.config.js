import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// ============================================================
// 2026-08-19 加固 + 生产构建优化（d2 项）
// 目标：消除 "Some chunks are larger than 500 kB after minification" 警告
//       同时配合 Nginx 的 hash 文件名 + Cache-Control 做浏览器强缓存
// ============================================================
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'es2018',
    cssCodeSplit: true,
    sourcemap: false,
    reportCompressedSize: false,
    // 把阈值拉到 1200KB，手动拆包后剩余的大元素组件不会再报警
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        // --- 按依赖/功能拆 chunk（4 vendor + 3 app）---
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) {
              return 'vendor-vue'            // ~180KB：Vue3 全家桶，稳定不常更
            }
            if (id.includes('element-plus') || id.includes('@element-plus') || id.includes('dayjs')) {
              return 'vendor-element-plus'   // ~350KB：ElementPlus + 中文字体 dayjs locale
            }
            if (id.includes('echarts') || id.includes('axios')) {
              return 'vendor-libs'           // ~120KB：图表 / HTTP
            }
            return 'vendor-others'           // 其他依赖（pinia-plugin-persistedstate 等）
          }
          // --- 业务代码按类型拆：大体积的布局 / api 独立打包 ---
          if (id.includes('src/layout')) return 'app-layout'
          if (id.includes('src/api'))    return 'app-api'
        },
        // --- 输出文件名全部带 hash：Nginx 侧 "Cache-Control: immutable, max-age=31536000" 强缓存一年
        //     每次打包哈希自动变化 → 浏览器自动拉新版本，无需手动清缓存
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]'
      }
    }
  }
})
