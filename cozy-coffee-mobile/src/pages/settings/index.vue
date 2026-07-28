<template>
  <view class="settings-page">
    <view class="settings-intro">
      <text class="intro-kicker">COZY SETTINGS</text>
      <text class="intro-title cozy-display">账户设置</text>
      <text class="intro-copy">管理登录方式、必要提醒与本机数据。</text>
    </view>

    <view class="settings-section">
      <view class="section-heading">
        <text class="section-title">账号与安全</text>
        <text class="section-description">登录信息与密码维护</text>
      </view>
      <view class="settings-item" @click="goToPage('/pages/login/index')">
        <view class="item-copy">
          <text class="item-label">绑定手机</text>
          <text class="item-description">用于登录与接收必要的订单提醒</text>
        </view>
        <view class="item-right">
          <text class="item-value">{{ userPhone || '未绑定' }}</text>
          <text class="item-arrow">›</text>
        </view>
      </view>
      <view class="settings-item" @click="goToPage('/pages/login/reset')">
        <view class="item-copy">
          <text class="item-label">修改密码</text>
          <text class="item-description">开发环境账号可在此重置密码</text>
        </view>
        <view class="item-right">
          <text class="item-arrow">›</text>
        </view>
      </view>
    </view>

    <view class="settings-section">
      <view class="section-heading">
        <text class="section-title">消息提醒</text>
        <text class="section-description">仅保留与当前体验相关的通知</text>
      </view>
      <view class="settings-item">
        <view class="item-copy">
          <text class="item-label">订单状态</text>
          <text class="item-description">门店接单、制作与取餐状态</text>
        </view>
        <switch :checked="notifyOrder" color="#753A22" @change="notifyOrder = $event.detail.value" />
      </view>
      <view class="settings-item">
        <view class="item-copy">
          <text class="item-label">会员活动</text>
          <text class="item-description">签到、券包与会员权益更新</text>
        </view>
        <switch :checked="notifyActivity" color="#753A22" @change="notifyActivity = $event.detail.value" />
      </view>
      <view class="settings-item">
        <view class="item-copy">
          <text class="item-label">积分变动</text>
          <text class="item-description">积分获得、使用与到期提示</text>
        </view>
        <switch :checked="notifyPoints" color="#753A22" @change="notifyPoints = $event.detail.value" />
      </view>
    </view>

    <view class="settings-section">
      <view class="section-heading">
        <text class="section-title">本机与版本</text>
        <text class="section-description">当前设备上的应用信息</text>
      </view>
      <view class="settings-item" @click="clearCache">
        <view class="item-copy">
          <text class="item-label">清除缓存</text>
          <text class="item-description">不会删除账户、订单或积分记录</text>
        </view>
        <view class="item-right">
          <text class="item-value">{{ cacheSize }}</text>
          <text class="item-arrow">›</text>
        </view>
      </view>
      <view class="settings-item">
        <view class="item-copy">
          <text class="item-label">当前版本</text>
          <text class="item-description">CozyCoffee 小程序</text>
        </view>
        <view class="item-right">
          <text class="item-value">v1.0.0</text>
        </view>
      </view>
    </view>

    <view class="settings-section">
      <view class="section-heading">
        <text class="section-title">关于与支持</text>
        <text class="section-description">正式内容上线后可在此查看</text>
      </view>
      <view class="settings-item">
        <view class="item-copy">
          <text class="item-label">用户协议</text>
        </view>
        <view class="item-right">
          <text class="item-value item-value--quiet">待开放</text>
        </view>
      </view>
      <view class="settings-item">
        <view class="item-copy">
          <text class="item-label">隐私政策</text>
        </view>
        <view class="item-right">
          <text class="item-value item-value--quiet">待开放</text>
        </view>
      </view>
      <view class="settings-item">
        <view class="item-copy">
          <text class="item-label">意见反馈</text>
        </view>
        <view class="item-right">
          <text class="item-value item-value--quiet">待开放</text>
        </view>
      </view>
    </view>

    <view class="logout-section" v-if="isLoggedIn">
      <view class="logout-btn" @click="handleLogout">退出登录</view>
    </view>

    <text class="page-signoff">COZY COFFEE · ROASTED IN HANGZHOU</text>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userPhone = computed(() => userStore.userInfo?.phone || '')

// 通知设置
const notifyOrder = ref(true)
const notifyActivity = ref(true)
const notifyPoints = ref(true)

// 缓存大小
const cacheSize = ref('12.5MB')

// 清除缓存
const clearCache = () => {
  uni.showModal({
    title: '清除缓存',
    content: '确定要清除缓存吗？',
    success: (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '清除中...' })
        setTimeout(() => {
          uni.hideLoading()
          cacheSize.value = '0MB'
          uni.showToast({ title: '清除成功', icon: 'success' })
        }, 1000)
      }
    }
  })
}

// 退出登录
const handleLogout = () => {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        await userStore.logout()
        uni.showToast({ title: '已退出登录', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 1000)
      }
    }
  })
}

// 页面跳转
const goToPage = (url) => {
  uni.navigateTo({ url })
}
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  padding-bottom: max(48rpx, env(safe-area-inset-bottom));
  background: $cozy-bg;
}

.settings-intro {
  padding: 42rpx 32rpx 46rpx;
  background: $cozy-surface-alt;
  color: $cozy-on-dark;
}

.intro-kicker {
  display: block;
  color: $cozy-muted-on-dark;
  font-size: 19rpx;
  font-weight: 750;
  letter-spacing: .13em;
}

.intro-title {
  display: block;
  margin-top: 12rpx;
  font-size: 42rpx;
  line-height: 1.25;
}

.intro-copy {
  display: block;
  margin-top: 14rpx;
  color: $cozy-muted-on-dark;
  font-size: 24rpx;
}

.settings-section {
  padding: 30rpx 32rpx 8rpx;
  border-bottom: 12rpx solid $cozy-surface;
  background: $cozy-bg;
}

.section-heading { margin-bottom: 10rpx; }

.section-title {
  display: block;
  color: $cozy-ink;
  font-size: 30rpx;
  font-weight: 700;
}

.section-description {
  display: block;
  margin-top: 5rpx;
  color: $cozy-muted;
  font-size: 21rpx;
}

.settings-item {
  min-height: 116rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  border-bottom: 1rpx solid $cozy-border;
}

.settings-item:last-child { border-bottom: 0; }

.item-copy { min-width: 0; flex: 1; padding: 22rpx 0; }
.item-label { display: block; color: $cozy-ink; font-size: 27rpx; font-weight: 620; }
.item-description { display: block; margin-top: 6rpx; color: $cozy-muted; font-size: 21rpx; line-height: 1.45; }
.item-right { flex: none; display: flex; align-items: center; gap: 12rpx; }
.item-value { color: $cozy-muted; font-size: 23rpx; }
.item-value--quiet { color: $cozy-placeholder; }
.item-arrow { color: $cozy-placeholder; font-size: 34rpx; line-height: 1; }

.logout-section {
  padding: 32rpx;
  border-bottom: 12rpx solid $cozy-surface;
}

.logout-btn {
  min-height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(180, 60, 48, .28);
  border-radius: $cozy-radius-md;
  background: $cozy-error-soft;
  color: $cozy-error;
  font-size: 27rpx;
  font-weight: 650;
}

.page-signoff {
  display: block;
  padding: 32rpx;
  color: $cozy-placeholder;
  font-size: 18rpx;
  font-weight: 650;
  letter-spacing: .09em;
  text-align: center;
}
</style>
