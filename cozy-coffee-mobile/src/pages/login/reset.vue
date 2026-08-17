<!--
  修改密码页 - 对齐登录页设计：品牌区 + 原密码/新密码/确认新密码 + 墨黑主按钮
  数据源: POST /api/auth/password/change（需登录），成功后会话失效跳登录
-->
<template>
  <view class="reset-page">
    <!-- 品牌区 -->
    <view class="auth-hero">
      <view class="brand-emblem">
        <CozyIcon name="bean" :size="44" color="#753A22" />
      </view>
      <text class="brand-word">COZY COFFEE</text>
      <text class="hero-title">修改密码</text>
      <text class="hero-sub">修改后需重新登录，请牢记新密码</text>
    </view>

    <!-- 表单卡 -->
    <view class="auth-card">
      <view class="field">
        <text class="field-label">原密码</text>
        <view class="field-input-wrap">
          <input
            v-model="oldPassword"
            :password="!showOld"
            placeholder="请输入当前密码"
            placeholder-class="field-placeholder"
            class="field-input"
          />
          <text class="field-toggle" @click="showOld = !showOld">{{ showOld ? '隐藏' : '显示' }}</text>
        </view>
      </view>

      <view class="field">
        <text class="field-label">新密码</text>
        <view class="field-input-wrap">
          <input
            v-model="newPassword"
            :password="!showNew"
            placeholder="6-20 位新密码"
            placeholder-class="field-placeholder"
            class="field-input"
          />
          <text class="field-toggle" @click="showNew = !showNew">{{ showNew ? '隐藏' : '显示' }}</text>
        </view>
      </view>

      <view class="field">
        <text class="field-label">确认新密码</text>
        <view class="field-input-wrap">
          <input
            v-model="confirmPassword"
            :password="!showConfirm"
            placeholder="再次输入新密码"
            placeholder-class="field-placeholder"
            class="field-input"
          />
          <text class="field-toggle" @click="showConfirm = !showConfirm">{{ showConfirm ? '隐藏' : '显示' }}</text>
        </view>
      </view>

      <button class="submit-btn" :class="{ disabled: submitting }" :disabled="submitting" @click="submitChange">
        {{ submitting ? '正在修改…' : '确认修改' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api/auth'
import CozyIcon from '@/components/CozyIcon.vue'

const userStore = useUserStore()

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showOld = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)
const submitting = ref(false)

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 500)
  }
})

async function submitChange() {
  if (!oldPassword.value) {
    uni.showToast({ title: '请输入原密码', icon: 'none' })
    return
  }
  if (newPassword.value.length < 6 || newPassword.value.length > 20) {
    uni.showToast({ title: '新密码需为 6-20 位', icon: 'none' })
    return
  }
  if (newPassword.value === oldPassword.value) {
    uni.showToast({ title: '新密码不能与原密码相同', icon: 'none' })
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const res = await changePassword(oldPassword.value, newPassword.value)
    if (res.code === 200) {
      userStore.clearSession()
      uni.showToast({ title: '密码已修改，请重新登录', icon: 'none', duration: 2000 })
      setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 1200)
    } else {
      uni.showToast({ title: res.message || res.msg || '修改失败', icon: 'none', duration: 2500 })
    }
  } catch (error) {
    uni.showToast({ title: error?.message || '修改失败，请稍后重试', icon: 'none', duration: 2500 })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.reset-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding: 0 48rpx calc(48rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
}

/* ── 品牌区 ── */
.auth-hero {
  text-align: center;
  padding: 120rpx 0 64rpx;
}
.brand-emblem {
  width: 120rpx;
  height: 120rpx;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: $bg-white;
  border: 1rpx solid $cozy-border;
}
.brand-word {
  display: block;
  margin-top: 24rpx;
  font-size: 22rpx;
  font-weight: 800;
  letter-spacing: .3em;
  color: $cozy-ink;
}
.hero-title {
  display: block;
  margin-top: 24rpx;
  font-family: $font-display;
  font-size: 52rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.hero-sub {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: $cozy-muted;
}

/* ── 表单卡 ── */
.auth-card {
  background: $bg-white;
  border-radius: 28rpx;
  padding: 40rpx 40rpx 32rpx;
}
.field {
  padding: 24rpx 0;
  border-bottom: 1rpx solid $cozy-border;
}
.field-label {
  display: block;
  font-size: 22rpx;
  font-weight: 650;
  color: $cozy-muted;
}
.field-input-wrap {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.field-input {
  min-width: 0;
  flex: 1;
  font-size: 30rpx;
  color: $cozy-ink;
}
.field-placeholder { color: $cozy-placeholder; }
.field-toggle {
  flex: none;
  font-size: 22rpx;
  color: $cozy-muted;

  &:active { opacity: .6; }
}

.submit-btn {
  width: 100%;
  height: 92rpx;
  margin-top: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: $cozy-ink;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;

  &:active { opacity: .85; }
  &.disabled { opacity: .4; }
}
.submit-btn::after { border: 0; }
</style>
