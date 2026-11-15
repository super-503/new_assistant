import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'

const backend = process.env.NEWS_BACKEND_URL ?? 'http://127.0.0.1:8091'

export default defineConfig({
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  plugins: [
    vue(),
    createSvgIconsPlugin({
      iconDirs: [fileURLToPath(new URL('./src/icons/svg', import.meta.url))],
      symbolId: 'icon-[dir]-[name]'
    }),
    AutoImport({ imports: ['vue', 'vue-router', 'pinia'], dts: 'auto-imports.d.ts' }),
    Components({
      dirs: ['src/components/common'],
      dts: 'components.d.ts',
      resolvers: [ElementPlusResolver()]
    })
  ],
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: { '/ai': backend, '/login': backend, '/logout': backend }
  }
})
