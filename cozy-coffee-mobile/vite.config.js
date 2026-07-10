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
    },
    build: {
        // H5 端拆分 vendor chunk，优化首屏加载
        // 小程序端（mp-weixin）走自己的分包机制，此配置不影响
        rollupOptions: {
            output: {
                manualChunks: {
                    vendor: ['vue', 'pinia']
                }
            }
        }
    }
})
