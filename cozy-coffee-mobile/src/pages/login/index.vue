<!--
  登录页 - 手机号密码登录
-->
<template>
  <view class="login-page">
    <!-- Logo 区域 -->
    <view class="logo-section">
      <view class="logo">COZY</view>
      <text class="brand-name cozy-display">欢迎回来</text>
      <text class="brand-slogan">登录后继续你的点单与会员日常</text>
    </view>
    
    <!-- 表单区域 -->
    <view class="form-section">
      <view class="form-item">
        <text class="form-icon">账号</text>
        <input 
          v-model="account"
          type="text"
          placeholder="请输入手机号或邮箱"
          maxlength="100"
          class="form-input"
        />
      </view>
      
      <view class="form-item">
        <text class="form-icon">密码</text>
        <input 
          v-model="password" 
          :password="!showPassword"
          placeholder="请输入密码" 
          class="form-input"
        />
        <text class="toggle-password" @click="showPassword = !showPassword">
          {{ showPassword ? '隐藏' : '显示' }}
        </text>
      </view>
      
      <view class="login-btn" :class="{ disabled: !canLogin }" @click="handleLogin">
        {{ isLoading ? '登录中...' : '登录' }}
      </view>
      
      <view class="form-footer">
        <text class="link" @click="goToRegister">新用户注册</text>
        <text class="divider">|</text>
        <text class="link" @click="goToForget">忘记密码</text>
      </view>
    </view>
    
    <!-- 其他登录方式 -->
    <view class="other-login">
      <view class="divider-line">
        <view class="line"></view>
        <text class="text">其他登录方式</text>
        <view class="line"></view>
      </view>
      
      <view class="social-login">
        <view class="social-btn wechat" @click="handleWechatLogin">
          <text class="icon">WX</text>
          <text class="label">微信登录</text>
        </view>
      </view>
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

// 是否可以登录
const canLogin = computed(() => {
  return isValidAccount.value && password.value.length >= 6 && agreeTerms.value && !isLoading.value
})

// 登录
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
      // 1. 保存 Token
      // 假设后端返回结构: { code: 200, data: { token: '...', ... } }
      const token = res.data.token || res.data
      
      // 2. 获取用户信息 (如果登录接口未返回完整用户信息)
      let userInfo = res.data.user || {}
      if (!userInfo.id && token) {
         // 临时保存 token 以便后续请求携带
         uni.setStorageSync('token', token)
         const userRes = await getUserInfo()
         if (userRes.code === 200) {
           userInfo = userRes.data
         }
      }

      // 保存登录信息到 Store
      userStore.setLoginInfo(token, userInfo)

      uni.showToast({ title: '登录成功', icon: 'success' })

      // 跳转到首页
      setTimeout(() => {
        uni.switchTab({ url: '/pages/index/index' })
      }, 1000)
    } else {
      // 业务错误（如密码错误、账号被禁用）- 展示后端返回的错误信息
      uni.showToast({
        title: res.message || res.msg || '登录失败',
        icon: 'none',
        duration: 3000
      })
    }
  } catch (error) {
    console.error('登录失败', error)
    // 网络错误或 request.js 未处理的异常
    uni.showToast({
      title: error.message || '登录失败，请检查网络后重试',
      icon: 'none',
      duration: 3000
    })
  } finally {
    isLoading.value = false
  }
}

// 微信登录
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
  padding: 0 $spacing-lg $spacing-xl;
  display: flex;
  flex-direction: column;
}

// Logo 区域
.logo-section {
  text-align: center;
  padding: 120rpx 0 68rpx;
  
  .logo {
    color: $cozy-ink;
    font-size: 40rpx;
    font-weight: 850;
    letter-spacing: .22em;
    margin-bottom: 30rpx;
  }
  
  .brand-name {
    font-size: 44rpx;
    font-weight: 600;
    color: $cozy-ink;
    display: block;
    margin-bottom: $spacing-xs;
  }
  
  .brand-slogan {
    font-size: $font-size-sm;
    color: $text-secondary;
  }
}

// 表单区域
.form-section {
  background: $bg-white;
  border-radius: $cozy-radius-lg;
  padding: $spacing-lg;
  box-shadow: none;
}

.form-item {
  display: flex;
  align-items: center;
  padding: $spacing-md 0;
  border-bottom: 1rpx solid $border-color;
  
  &:last-of-type {
    border-bottom: none;
  }
  
  .form-icon {
    width: 68rpx;
    color: $cozy-primary;
    font-size: 20rpx;
    font-weight: 700;
    margin-right: $spacing-md;
  }
  
  .form-input {
    flex: 1;
    font-size: $font-size-md;
  }
  
  .toggle-password {
    color: $cozy-primary;
    font-size: 21rpx;
    padding: 16rpx 0 16rpx 16rpx;
  }
}

.login-btn {
  min-height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $cozy-primary;
  color: white;
  text-align: center;
  padding: 0 $spacing-md;
  border-radius: $cozy-radius-md;
  font-size: $font-size-lg;
  font-weight: 600;
  margin-top: $spacing-lg;
  
  &.disabled {
    opacity: 0.5;
  }
}

.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: $spacing-md;
  
  .link {
    font-size: $font-size-sm;
    color: $primary-color;
  }
  
  .divider {
    margin: 0 $spacing-md;
    color: $border-color;
  }
}

// 其他登录方式
.other-login {
  margin-top: $spacing-xl;
  
  .divider-line {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-lg;
    
    .line {
      flex: 1;
      height: 1rpx;
      background: $border-color;
    }
    
    .text {
      padding: 0 $spacing-md;
      font-size: $font-size-sm;
      color: $text-placeholder;
    }
  }
  
  .social-login {
    display: flex;
    justify-content: center;
    
    .social-btn {
      min-width: 250rpx;
      min-height: 88rpx;
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: center;
      gap: 16rpx;
      padding: 0 $spacing-xl;
      border: 1rpx solid $cozy-border;
      border-radius: $cozy-radius-md;
      background: #fff;
      
      .icon {
        color: $cozy-accent;
        font-size: 20rpx;
        font-weight: 800;
      }
      
      .label {
        font-size: $font-size-sm;
        color: $cozy-ink;
      }
    }
  }
}

// 协议
.agreement {
  display: flex;
  align-items: flex-start;
  margin-top: auto;
  padding-top: $spacing-xl;
  
  .checkbox {
    width: 36rpx;
    height: 36rpx;
    border: 2rpx solid $border-color;
    border-radius: 6rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: $spacing-sm;
    margin-top: 4rpx;
    flex-shrink: 0;
    
    &.checked {
      background: $primary-color;
      border-color: $primary-color;
      color: white;
      font-size: 24rpx;
    }
  }
  
  .agreement-text {
    font-size: $font-size-xs;
    color: $text-placeholder;
    line-height: 1.6;
    
    .link {
      color: $primary-color;
    }
  }
}
</style>
