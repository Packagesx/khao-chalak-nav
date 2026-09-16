import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    // PWA support (Milestone 0 addendum): makes the deployed web app
    // installable ("Add to Home Screen" on Android/iOS, or a desktop
    // install prompt) and enables an APK build later via PWABuilder/
    // Bubblewrap (Trusted Web Activity). This does NOT add offline
    // navigation/GPS caching yet (that's Milestone 11) — registerType
    // 'autoUpdate' just precaches the built app shell so the icon +
    // manifest are installable today.
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'apple-touch-icon.png'],
      manifest: {
        id: '/',
        name: 'Khao Chalak Outdoor Navigation',
        short_name: 'Khao Chalak',
        description:
          'GIS-first, offline-first outdoor navigation for Khao Chalak, Chonburi — trail running, hiking, and cycling.',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        background_color: '#0b0f14',
        theme_color: '#0b0f14',
        orientation: 'portrait',
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
          {
            src: '/icon-512-maskable.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        // Milestone 0: precache the app shell only. Real offline map/GPS
        // caching strategy is designed properly in Milestone 11.
        globPatterns: ['**/*.{js,css,html,svg,png,ico}'],
      },
    }),
  ],
})
