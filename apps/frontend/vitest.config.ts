import { defineConfig } from 'vitest/config'
import path from 'path'

export default defineConfig({
  test: {
    environment: 'jsdom',
    setupFiles: './vitest.setup.ts',
    globals: true,
    include: ['tests/**/*.{test,spec}.ts?(x)'],
    exclude: [
      'node_modules/**',
      'dist/**',
      'coverage/**',
      'src/**/__tests__/**',
    ],
    passWithNoTests: true,
    reporters: 'basic',
    watch: false,
    pool: 'threads',
    maxThreads: 2,
    minThreads: 1,
    testTimeout: 20000,
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
