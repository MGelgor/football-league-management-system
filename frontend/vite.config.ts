import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Varsayılan 8080; başka portta çalışan backend için: BACKEND_URL=http://localhost:8090 npm run dev
const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': BACKEND_URL,
      '/uploads': BACKEND_URL,
    },
  },
})
