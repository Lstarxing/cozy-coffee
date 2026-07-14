<!--
  App.vue - 应用根组件
  
  职责：
  1. 定义应用生命周期钩子
  2. 引入全局样式
-->
<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useSessionStore } from '@/stores/session'
import { useCheckoutStore } from '@/stores/checkout'
import { SessionService } from '@/services/session/SessionService'
import { NetworkService } from '@/services/network/NetworkService'
import { Logger } from '@/services/logging/Logger'

const sessionStore = useSessionStore()
const checkoutStore = useCheckoutStore()
const sessionService = new SessionService({ sessionStore })
const networkService = new NetworkService(globalThis.uni)
let hiddenAt = null

// 应用启动时触发（全局只触发一次）
onLaunch(() => {
  sessionService.restore()
  networkService.start()
  sessionService.establishSilentSession().catch(error => {
    Logger.warn('Silent Session Skipped', { code: error.code })
  })
  console.log('🚀 Antigravity Coffee App 启动')
  
  // 检查登录状态
  const token = uni.getStorageSync('token')
  if (token) {
    console.log('✅ 用户已登录')
  }
})

// 应用显示时触发（从后台进入前台）
onShow(() => {
  if (hiddenAt && Date.now() - hiddenAt >= 5 * 60 * 1000) {
    checkoutStore.invalidatePreview()
  }
  hiddenAt = null
  networkService.refresh()
  console.log('📱 App 进入前台')
})

// 应用隐藏时触发（从前台进入后台）
onHide(() => {
  hiddenAt = Date.now()
  console.log('📱 App 进入后台')
})
</script>

<style lang="scss">
/* 引入全局样式变量 */
@import './uni.scss';

/* ==================== 全局基础样式 ==================== */
page {
  background-color: $bg-color;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 
               'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  font-size: $font-size-md;
  color: $text-primary;
  line-height: 1.5;
}

/* 去除默认边距 */
view, text, image {
  box-sizing: border-box;
}

/* ==================== 通用工具类 ==================== */
.flex {
  display: flex;
}

.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.flex-column {
  display: flex;
  flex-direction: column;
}

/* 文字省略 */
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ellipsis-2 {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* ==================== 卡片样式 ==================== */
.card {
  background: $bg-white;
  border-radius: $border-radius-md;
  box-shadow: $box-shadow;
  padding: $spacing-md;
  margin: $spacing-sm;
}

/* ==================== 按钮样式 ==================== */
.btn-primary {
  background: linear-gradient(135deg, $primary-color, $primary-dark);
  color: $text-white;
  border: none;
  border-radius: $border-radius-md;
  padding: $spacing-sm $spacing-lg;
  font-size: $font-size-md;
  font-weight: 500;
}

.btn-outline {
  background: transparent;
  color: $primary-color;
  border: 2rpx solid $primary-color;
  border-radius: $border-radius-md;
  padding: $spacing-sm $spacing-lg;
  font-size: $font-size-md;
}

/* ==================== 安全区域适配 ==================== */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
