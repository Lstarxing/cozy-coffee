import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
// 引入全局样式
import 'element-plus/dist/index.css'
import '@/assets/styles/tokens.css'
import '@/assets/styles/style.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
