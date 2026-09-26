import { defineConfig } from 'vitest/config'
import path from 'path'

export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      '@box/ui': path.resolve(__dirname, '../box-ui/src'),
    },
  },
  test: {
    environment: 'node',
    globals: true,
  },
})
