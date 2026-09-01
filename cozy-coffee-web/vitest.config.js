import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    clearMocks: true,
    include: ['src/**/*.test.{js,mjs}'],
    exclude: ['node_modules/**', 'scripts/**']
  }
})
