<!--
  THESIS: 把 Web 端的烘焙台体验装进口袋，拒绝通用商城式卡片堆叠。
  OWN-WORLD: 瓷白、深烘棕、釉面绿与真实咖啡影像；无衬线负责操作，宋体只留给品牌停顿。
  STORY: 用户先看见咖啡与门店，再迅速完成选品、规格、结算、取餐与会员回馈。
  FIRST VIEWPORT: COZY 字标与搜索压在全幅门店影像上，首要点单动作和当前会员状态紧随其后。
  FORM: Web established-world extension / Operate；brief-pinned identity，未运行随机 seed。
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
  console.log('CozyCoffee App 启动')
  
  // 检查登录状态
  const token = uni.getStorageSync('token')
  if (token) {
    console.log('用户已登录')
  }
})

// 应用显示时触发（从后台进入前台）
onShow(() => {
  if (hiddenAt && Date.now() - hiddenAt >= 5 * 60 * 1000) {
    checkoutStore.invalidatePreview()
  }
  hiddenAt = null
  networkService.refresh()
  console.log('App 进入前台')
})

// 应用隐藏时触发（从前台进入后台）
onHide(() => {
  hiddenAt = Date.now()
  console.log('App 进入后台')
})
</script>

<style lang="scss">
/* 引入全局样式变量 */
@import './uni.scss';

/* ==================== 全局基础样式 ==================== */
page {
  background-color: $bg-color;
  font-family: $font-sans;
  font-size: $font-size-md;
  color: $text-primary;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
}

/* 去除默认边距 */
view, text, image, button, input, textarea, scroll-view {
  box-sizing: border-box;
}

image {
  display: block;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  line-height: inherit;
}

button::after {
  border: 0;
}

.cozy-display {
  font-family: $font-display;
  font-weight: 600;
  letter-spacing: -0.02em;
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
  padding: $spacing-md;
  margin: $spacing-sm;
}

/* ==================== 按钮样式 ==================== */
.btn-primary {
  min-height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $primary-color;
  color: $text-white;
  border: none;
  border-radius: $border-radius-md;
  padding: $spacing-sm $spacing-lg;
  font-size: $font-size-md;
  font-weight: 650;
  transition: background $cozy-duration $cozy-ease-out, transform $cozy-duration $cozy-ease-out;
}

.btn-primary:active {
  background: $primary-dark;
  transform: scale(0.985);
}

.btn-outline {
  min-height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: $primary-color;
  border: 1rpx solid $primary-color;
  border-radius: $border-radius-md;
  padding: $spacing-sm $spacing-lg;
  font-size: $font-size-md;
}

/* #ifdef H5 */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 1ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 1ms !important;
  }
}
/* #endif */

/* ==================== 安全区域适配 ==================== */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
