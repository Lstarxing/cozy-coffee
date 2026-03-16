/**
 * CozyCoffee 咖啡小程序 - 入口文件
 * 
 * 职责：
 * 1. 创建 Vue 应用实例
 * 2. 注册 Pinia 状态管理
 * 3. 挂载全局配置
 */
import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

// 创建应用实例的工厂函数 (SSR 兼容)
export function createApp() {
    // 创建 Vue 应用
    const app = createSSRApp(App)

    // 创建并注册 Pinia 状态管理
    const pinia = createPinia()
    app.use(pinia)

    return {
        app,
        pinia // 导出 pinia 实例，用于 SSR 场景
    }
}
