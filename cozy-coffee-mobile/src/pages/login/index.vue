<!--
  登录页 - 手机号密码登录
-->
<template>
  <view class="login-page">
    <!-- Logo 区域 -->
    <view class="logo-section">
      <view class="logo">☕</view>
      <text class="brand-name">CozyCoffee</text>
      <text class="brand-slogan">品质生活，从一杯咖啡开始</text>
    </view>
    
    <!-- 表单区域 -->
    <view class="form-section">
      <view class="form-item">
        <text class="form-icon">📱</text>
        <input 
          v-model="phone" 
          type="number" 
          placeholder="请输入手机号" 
          maxlength="11"
          class="form-input"
        />
      </view>
      
      <view class="form-item">
        <text class="form-icon">🔒</text>
        <input 
          v-model="password" 
          :password="!showPassword"
          placeholder="请输入密码" 
          class="form-input"
        />
        <text class="toggle-password" @click="showPassword = !showPassword">
          {{ showPassword ? '🙈' : '👁️' }}
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
          <text class="icon">💬</text>
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

const userStore = useUserStore()

const phone = ref('')
const password = ref('')
const showPassword = ref(false)
const isLoading = ref(false)
const agreeTerms = ref(false)

// 是否可以登录
const canLogin = computed(() => {
  return phone.value.length === 11 && password.value.length >= 6 && agreeTerms.value
})

// 登录
const handleLogin = async () => {
  if (!canLogin.value) {
    if (!agreeTerms.value) {
      uni.showToast({ title: '请先阅读并同意协议', icon: 'none' })
    }
    return
  }
  
  isLoading.value = true
  
  try {
    const res = await login({ username: phone.value, password: password.value })
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
    }
  } catch (error) {
    console.error(error)
    // 错误已经在 request.js 中处理了提示，或者在这里补充
  } finally {
    isLoading.value = false
  }
}

// 微信登录
const handleWechatLogin = () => {
  if (!agreeTerms.value) {
    uni.showToast({ title: '请先阅读并同意协议', icon: 'none' })
    return
  }
  
  // #ifdef MP-WEIXIN
  uni.getUserProfile({
    desc: '用于完善用户资料',
    success: (res) => {
      console.log('微信用户信息', res)
      // 这里应该调用后端接口进行微信登录
      uni.showToast({ title: '微信登录成功', icon: 'success' })
    },
    fail: (err) => {
      console.log('获取用户信息失败', err)
    }
  })
  // #endif
  
  // #ifdef H5
  uni.showToast({ title: 'H5环境暂不支持微信登录', icon: 'none' })
  // #endif
}

const goToRegister = () => {
  uni.showToast({ title: '注册功能开发中', icon: 'none' })
}

const goToForget = () => {
  uni.showToast({ title: '找回密码功能开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF9F0 0%, $bg-color 100%);
  padding: $spacing-xl $spacing-lg;
  display: flex;
  flex-direction: column;
}

// Logo 区域
.logo-section {
  text-align: center;
  padding: 100rpx 0 80rpx;
  
  .logo {
    font-size: 120rpx;
    margin-bottom: $spacing-md;
  }
  
  .brand-name {
    font-size: $font-size-xxl;
    font-weight: 700;
    color: $primary-color;
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
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  box-shadow: $box-shadow;
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
    font-size: 40rpx;
    margin-right: $spacing-md;
  }
  
  .form-input {
    flex: 1;
    font-size: $font-size-md;
  }
  
  .toggle-password {
    font-size: 36rpx;
    padding: $spacing-xs;
  }
}

.login-btn {
  background: linear-gradient(135deg, $primary-color, $primary-dark);
  color: white;
  text-align: center;
  padding: $spacing-md;
  border-radius: 44rpx;
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
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: $spacing-md $spacing-xl;
      
      .icon {
        font-size: 60rpx;
        margin-bottom: $spacing-xs;
      }
      
      .label {
        font-size: $font-size-sm;
        color: $text-secondary;
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
