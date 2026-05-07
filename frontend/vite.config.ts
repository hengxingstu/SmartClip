import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  base: './',
  plugins: [vue()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }

          if (id.includes('element-plus')) {
            const match = id.match(/element-plus\/es\/components\/([^/]+)/)
            if (match?.[1]) {
              return `element-plus-${match[1]}`
            }
            return 'element-plus-core'
          }

          if (id.includes('/vue/') || id.includes('/pinia/')) {
            return 'vue-vendor'
          }

          if (id.includes('/axios/')) {
            return 'network-vendor'
          }

          return 'vendor'
        }
      }
    }
  }
})
