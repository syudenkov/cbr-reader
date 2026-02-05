import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

// CBR Viewer Vite Configuration
// Production: Frontend is bundled with Spring Boot backend
// Development: Use vite dev server with proxy to backend
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'CBR/CBZ Viewer',
        short_name: 'Comic Viewer',
        description: 'Web-based comic book reader',
        theme_color: '#2196f3',
        background_color: '#0a0a0a',
        display: 'standalone',
        icons: [
          {
            src: '/icon-192.png',
            sizes: '192x192',
            type: 'image/png',
          },
          {
            src: '/icon-512.png',
            sizes: '512x512',
            type: 'image/png',
          },
        ],
      },
    }),
  ],
  build: {
    // Output to dist folder (copied to backend/src/main/resources/static by gradle)
    outDir: 'dist',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'redux-vendor': ['@reduxjs/toolkit', 'react-redux'],
          'mui-vendor': ['@mui/material', '@mui/icons-material'],
        },
      },
    },
  },
  server: {
    // Dev server config - only used during development
    port: 3344,
    proxy: {
      '/api': {
        target: 'http://localhost:4433',
        changeOrigin: true,
        cookieDomainRewrite: 'localhost',
        cookiePathRewrite: '/',
      },
    },
  },
});
