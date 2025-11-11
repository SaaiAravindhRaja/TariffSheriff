import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'framer-motion',
      'lucide-react',
      '@auth0/auth0-react',
      '@tanstack/react-query',
      'axios',
      '@radix-ui/react-dialog',
      '@radix-ui/react-dropdown-menu',
      '@radix-ui/react-select',
      '@radix-ui/react-tabs',
      '@radix-ui/react-switch',
      '@radix-ui/react-slot',
      'recharts',
      'class-variance-authority',
      'clsx',
      'tailwind-merge',
    ],
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 3000,
    cors: {
      origin: ['http://localhost:3000'],
      credentials: true,
    },
    fs: {
      strict: true,
      allow: [
        __dirname, // project root
        path.resolve(__dirname, 'public'),
      ],
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          ui: ['@radix-ui/react-dialog', '@radix-ui/react-dropdown-menu', '@radix-ui/react-select'],
          charts: ['recharts'],
          utils: ['axios', '@tanstack/react-query', 'date-fns'],
        },
      },
    },
    chunkSizeWarningLimit: 1000,
  },
})
