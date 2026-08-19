<!--
  登录页 - 品牌字标 + 字段表单（密码眼睛开关）+ 墨黑主按钮 + 瓷器微信登录
-->
<template>
  <view class="login-page">
    <!-- 品牌区 -->
    <view class="auth-hero">
      <text class="brand-word">COZY COFFEE</text>
      <text class="hero-title">欢迎回来</text>
      <text class="hero-sub">登录后继续你的点单与会员日常</text>
    </view>

    <!-- 表单卡 -->
    <view class="auth-card">
      <view class="field">
        <text class="field-label">账号</text>
        <view class="field-input-wrap">
          <input
            v-model="account"
            type="text"
            placeholder="手机号 / 邮箱"
            maxlength="100"
            placeholder-class="field-placeholder"
            class="field-input"
          />
        </view>
      </view>
      <view class="field">
        <text class="field-label">密码</text>
        <view class="field-input-wrap">
          <input
            v-model="password"
            :password="!showPassword"
            placeholder="请输入密码"
            placeholder-class="field-placeholder"
            class="field-input"
          />
          <view class="field-toggle" @click="showPassword = !showPassword">
            <CozyIcon :name="showPassword ? 'eye-off' : 'eye'" :size="20" color="#756A63" />
          </view>
        </view>
      </view>

      <button class="submit-btn" :class="{ disabled: !canLogin }" :loading="isLoading" :disabled="isLoading" @click="handleLogin">
        {{ isLoading ? '登录中…' : '登录' }}
      </button>

      <view class="auth-links">
        <text class="auth-link" @click="goToRegister">新用户注册</text>
        <view class="auth-dot" />
        <text class="auth-link" @click="goToForget">忘记密码</text>
      </view>
    </view>

    <!-- 其他登录 -->
    <view class="alt-login">
      <view class="divider">
        <view class="divider-line" />
        <text class="divider-text">其他登录方式</text>
        <view class="divider-line" />
      </view>
      <button class="alt-btn" @click="handleWechatLogin">
        <CozyIcon name="wechat" :size="26" color="#753A22" />
        <text class="alt-label">微信登录</text>
      </button>
    </view>

    <!-- 协议 -->
    <view class="agreement">
      <view class="checkbox" :class="{ checked: agreeTerms }" @click="agreeTerms = !agreeTerms">
        <text v-if="agreeTerms">✓</text>
      </view>
      <text class="agreement-text">
        我已阅读并同意 <text class="link">《用户协议》</text> 和 <text class="link">《隐私政策》</text>
      </text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { login, getUserInfo } from '@/api/auth'
import { useSessionStore } from '@/stores/session'
import { SessionService } from '@/services/session/SessionService'
import CozyIcon from '@/components/CozyIcon.vue'

const userStore = useUserStore()
const sessionStore = useSessionStore()
const sessionService = new SessionService({ sessionStore })

const account = ref('')
const password = ref('')
const showPassword = ref(false)
const isLoading = ref(false)
const agreeTerms = ref(false)

const PHONE_PATTERN = /^1[3-9]\d{9}$/
const EMAIL_PATTERN = /^[\w.-]+@[\w.-]+\.\w+$/
const normalizedAccount = computed(() => account.value.trim())
const isValidAccount = computed(() => PHONE_PATTERN.test(normalizedAccount.value) || EMAIL_PATTERN.test(normalizedAccount.value))

const canLogin = computed(() => {
  return isValidAccount.value && password.value.length >= 6 && agreeTerms.value && !isLoading.value
})

const handleLogin = async () => {
  if (!canLogin.value) {
    if (!agreeTerms.value) {
      uni.showToast({ title: '请先阅读并同意协议', icon: 'none' })
    } else if (!isValidAccount.value) {
      uni.showToast({ title: '请输入正确的手机号或邮箱', icon: 'none' })
    } else if (password.value.length < 6) {
      uni.showToast({ title: '密码长度至少为 6 位', icon: 'none' })
    }
    return
  }

  isLoading.value = true

  try {
    const res = await login({ username: normalizedAccount.value, password: password.value })
    if (res.code === 200) {
      const token = res.data.token || res.data
      let userInfo = res.data.user || {}
      if (!userInfo.id && token) {
        uni.setStorageSync('token', token)
        const userRes = await getUserInfo()
        if (userRes.code === 200) userInfo = userRes.data
      }
      userStore.setLoginInfo(token, userInfo)
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/index/index' })
      }, 1000)
    } else {
      uni.showToast({ title: res.message || res.msg || '登录失败', icon: 'none', duration: 3000 })
    }
  } catch (error) {
    console.error('登录失败', error)
    uni.showToast({ title: error.message || '登录失败，请检查网络后重试', icon: 'none', duration: 3000 })
  } finally {
    isLoading.value = false
  }
}

const handleWechatLogin = async () => {
  if (!agreeTerms.value) {
    uni.showToast({ title: '请先阅读并同意协议', icon: 'none' })
    return
  }

  // #ifdef MP-WEIXIN
  try {
    isLoading.value = true
    await sessionService.establishSilentSession()
    const userRes = await getUserInfo()
    sessionStore.setLoginInfo(sessionStore.token, userRes.data || {})
    uni.showToast({ title: '微信开发登录成功', icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500)
  } catch (error) {
    uni.showToast({ title: error?.message || '微信登录失败', icon: 'none' })
  } finally {
    isLoading.value = false
  }
  // #endif

  // #ifdef H5
  uni.showToast({ title: 'H5环境暂不支持微信登录', icon: 'none' })
  // #endif
}

const goToRegister = () => {
  uni.navigateTo({ url: '/pages/register/index' })
}

const goToForget = () => {
  uni.navigateTo({ url: '/pages/login/reset' })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding: 0 48rpx calc(48rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
}

/* ── 品牌区 ── */
.auth-hero {
  text-align: center;
  padding: 64rpx 0 48rpx;
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
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 56rpx;
  height: 56rpx;
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

.auth-links {
  margin-top: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20rpx;
}
.auth-link {
  font-size: 24rpx;
  color: $cozy-muted;

  &:active { opacity: .6; }
}
.auth-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: $cozy-border;
}

/* ── 其他登录 ── */
.alt-login {
  margin-top: 44rpx;
}
.divider {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.divider-line {
  flex: 1;
  height: 1rpx;
  background: $cozy-border;
}
.divider-text {
  font-size: 22rpx;
  color: $cozy-placeholder;
}
.alt-btn {
  width: 100%;
  height: 92rpx;
  margin-top: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  border: 1rpx solid $cozy-border;
  border-radius: 999rpx;
  background: $bg-white;
  color: $cozy-ink;

  &:active { opacity: .8; }
}
.alt-btn::after { border: 0; }
.alt-label {
  font-size: 26rpx;
  font-weight: 600;
}

/* ── 协议 ── */
.agreement {
  display: flex;
  align-items: flex-start;
  margin-top: auto;
  padding-top: 48rpx;
}
.checkbox {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid $cozy-border;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  margin-top: 4rpx;
  flex-shrink: 0;

  &.checked {
    background: $cozy-ink;
    border-color: $cozy-ink;
    color: #fff;
    font-size: 24rpx;
  }
}
.agreement-text {
  font-size: 22rpx;
  color: $cozy-placeholder;
  line-height: 1.6;

  .link {
    color: $cozy-muted;
  }
}
</style>
