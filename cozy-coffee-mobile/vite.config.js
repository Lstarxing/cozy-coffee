import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [
        uni()
    ],
    // SCSS 全局变量注入
    css: {
        preprocessorOptions: {
            scss: {
                additionalData: `@import "@/uni.scss";`
            }
        }
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false
            }
        }
    }
})
