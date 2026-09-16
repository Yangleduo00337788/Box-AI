import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { TDesignResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'
import { TDesignChatResolver } from './src/resolvers/tdesignChatResolver'

export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/@vue-flow')) {
            return 'vue-flow'
          }
        },
      },
    },
  },
  plugins: [
    vue(),
    Components({
      resolvers: [
        TDesignChatResolver(),
        TDesignResolver({
          library: 'vue-next',
        }),
      ],
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      '@box/ui': path.resolve(__dirname, '../box-ui/src'),
      'tdesign-icons-vue-next': path.resolve(__dirname, 'node_modules/tdesign-icons-vue-next'),
    },
    dedupe: ['vue', 'tdesign-icons-vue-next'],
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/.well-known': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
