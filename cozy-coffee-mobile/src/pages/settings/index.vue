<!--
  账户设置页 - 章节式品牌设置页：衬线一级标题锚点（页面背景上）+ 白卡设置项 + 自定义胶囊开关
-->
<template>
  <view class="settings-page">
    <!-- 账号与安全 -->
    <view class="settings-section">
      <view class="section-head">
        <text class="section-title">账号与安全</text>
        <text class="section-note">登录信息与密码维护</text>
      </view>
      <view class="settings-card">
        <view class="settings-row" @click="goToPage('/pages/profile/edit')">
          <view class="row-copy">
            <text class="row-label">个人资料</text>
            <text class="row-desc">昵称、头像、生日与联系方式</text>
          </view>
          <view class="row-right"><text class="row-arrow">›</text></view>
        </view>
        <view class="settings-row" @click="goToPage('/pages/login/reset')">
          <view class="row-copy">
            <text class="row-label">修改密码</text>
            <text class="row-desc">开发环境账号可在此重置密码</text>
          </view>
          <view class="row-right"><text class="row-arrow">›</text></view>
        </view>
      </view>
    </view>

    <!-- 消息提醒 -->
    <view class="settings-section">
      <view class="section-head">
        <text class="section-title">消息提醒</text>
        <text class="section-note">仅保留与当前体验相关的通知</text>
      </view>
      <view class="settings-card">
        <view class="settings-row" @click="notifyOrder = !notifyOrder">
          <view class="row-copy">
            <text class="row-label">订单状态</text>
            <text class="row-desc">门店接单、制作与取餐状态</text>
          </view>
          <view class="toggle" :class="{ on: notifyOrder }"><view class="toggle-knob" /></view>
        </view>
        <view class="settings-row" @click="notifyActivity = !notifyActivity">
          <view class="row-copy">
            <text class="row-label">会员活动</text>
            <text class="row-desc">签到、券包与会员权益更新</text>
          </view>
          <view class="toggle" :class="{ on: notifyActivity }"><view class="toggle-knob" /></view>
        </view>
        <view class="settings-row" @click="notifyPoints = !notifyPoints">
          <view class="row-copy">
            <text class="row-label">积分变动</text>
            <text class="row-desc">积分获得、使用与到期提示</text>
          </view>
          <view class="toggle" :class="{ on: notifyPoints }"><view class="toggle-knob" /></view>
        </view>
      </view>
    </view>

    <!-- 本机与版本 -->
    <view class="settings-section">
      <view class="section-head">
        <text class="section-title">本机与版本</text>
        <text class="section-note">当前设备上的应用信息</text>
      </view>
      <view class="settings-card">
        <view class="settings-row" @click="clearCache">
          <view class="row-copy">
            <text class="row-label">清除缓存</text>
            <text class="row-desc">不会删除账户、订单或积分记录</text>
          </view>
          <view class="row-right">
            <text class="row-value">{{ cacheSize }}</text>
            <text class="row-arrow">›</text>
          </view>
        </view>
        <view class="settings-row">
          <view class="row-copy">
            <text class="row-label">当前版本</text>
            <text class="row-desc">CozyCoffee 小程序</text>
          </view>
          <view class="row-right"><text class="row-value">v1.0.0</text></view>
        </view>
      </view>
    </view>

    <!-- 关于与支持 -->
    <view class="settings-section">
      <view class="section-head">
        <text class="section-title">关于与支持</text>
        <text class="section-note">正式内容上线后可在此查看</text>
      </view>
      <view class="settings-card">
        <view class="settings-row">
          <view class="row-copy"><text class="row-label">用户协议</text></view>
          <view class="row-right"><text class="row-value quiet">待开放</text></view>
        </view>
        <view class="settings-row">
          <view class="row-copy"><text class="row-label">隐私政策</text></view>
          <view class="row-right"><text class="row-value quiet">待开放</text></view>
        </view>
        <view class="settings-row">
          <view class="row-copy"><text class="row-label">意见反馈</text></view>
          <view class="row-right"><text class="row-value quiet">待开放</text></view>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" v-if="isLoggedIn" @click="handleLogout">退出登录</view>

    <text class="page-signoff">COZY COFFEE · ROASTED IN HANGZHOU</text>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)

const notifyOrder = ref(true)
const notifyActivity = ref(true)
const notifyPoints = ref(true)

const cacheSize = ref('12.5MB')

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

const goToPage = (url) => {
  uni.navigateTo({ url })
}
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  padding: 24rpx 32rpx max(48rpx, env(safe-area-inset-bottom));
  background: $cozy-bg;
}

/* ── 章节式分组：衬线一级标题锚点 + 白卡设置项 ── */
.settings-section { margin-top: 40rpx; }
.settings-section:first-of-type { margin-top: 0; }
.section-head { padding: 0 8rpx; }
.section-title {
  display: block;
  font-family: $font-display;
  font-size: 38rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.section-note {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $cozy-muted;
}
.settings-card {
  margin-top: 20rpx;
  border-radius: 28rpx;
  background: $bg-white;
  overflow: hidden;
}

.settings-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  min-height: 112rpx;
  padding: 0 40rpx;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
  &:active { opacity: .8; }
}
.row-copy { min-width: 0; flex: 1; padding: 26rpx 0; }
.row-label {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.row-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  line-height: 1.45;
  color: $cozy-muted;
}
.row-right { flex: none; display: flex; align-items: center; gap: 20rpx; }
.row-value { font-size: 26rpx; color: $cozy-muted; }
.row-value.quiet { color: $cozy-placeholder; }
.row-arrow { font-size: 34rpx; color: $cozy-placeholder; line-height: 1; }

/* ── 自定义胶囊开关 ── */
.toggle {
  position: relative;
  width: 88rpx;
  height: 52rpx;
  flex: none;
  border-radius: 999rpx;
  background: $cozy-border;
  transition: background .25s;
}
.toggle.on { background: $cozy-ink; }
.toggle-knob {
  position: absolute;
  top: 6rpx;
  left: 6rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, .2);
  transition: left .25s;
}
.toggle.on .toggle-knob { left: 42rpx; }

/* ── 退出登录 ── */
.logout-btn {
  margin-top: 40rpx;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20rpx;
  border: 1rpx solid rgba(43, 30, 22, .3);
  background: rgba(43, 30, 22, .06);
  color: $cozy-ink;
  font-size: 28rpx;
  font-weight: 650;

  &:active { opacity: .8; }
}

/* ── Footer ── */
.page-signoff {
  display: block;
  margin-top: 48rpx;
  font-size: 18rpx;
  font-weight: 650;
  letter-spacing: .16em;
  color: $cozy-placeholder;
  text-align: center;
}
</style>
