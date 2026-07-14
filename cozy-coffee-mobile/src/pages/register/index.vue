<!--
  注册页 - 手机号密码注册
-->
<template>
  <view class="register-page">
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
          v-model="form.username"
          type="text"
          placeholder="请输入手机号或邮箱"
          class="form-input"
        />
      </view>

      <view class="form-item">
        <text class="form-icon">🔒</text>
        <input
          v-model="form.password"
          :password="!showPassword"
          placeholder="请输入密码 (至少6位)"
          class="form-input"
        />
        <text class="toggle-password" @click="showPassword = !showPassword">
          {{ showPassword ? '🙈' : '👁️' }}
        </text>
      </view>

      <view class="form-item">
        <text class="form-icon">🔒</text>
        <input
          v-model="form.confirmPassword"
          :password="!showConfirmPassword"
          placeholder="请再次确认密码"
          class="form-input"
        />
        <text class="toggle-password" @click="showConfirmPassword = !showConfirmPassword">
          {{ showConfirmPassword ? '🙈' : '👁️' }}
        </text>
      </view>

      <view class="form-item">
        <text class="form-icon">🎁</text>
        <input
          v-model="form.inviterCode"
          type="text"
          placeholder="邀请码（选填，可获得额外积分）"
          maxlength="8"
          class="form-input invite-input"
        />
      </view>
      <view v-if="form.inviterCode" class="invite-hint">
        填写好友邀请码，双方各得积分奖励
      </view>

      <view class="register-btn" :class="{ disabled: !canRegister }" @click="handleRegister">
        {{ isLoading ? '注册中...' : '注册' }}
      </view>

      <view class="form-footer">
        <text class="link" @click="goToLogin">已有账号？立即登录</text>
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
import { ref, reactive, computed } from 'vue'
import { register } from '@/api/auth'

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  inviterCode: ''
})

const showPassword = ref(false)
const showConfirmPassword = ref(false)
const isLoading = ref(false)
const agreeTerms = ref(false)

const canRegister = computed(() => {
  return form.username.length > 0 &&
    form.password.length >= 6 &&
    form.password === form.confirmPassword &&
    agreeTerms.value
})

const handleRegister = async () => {
  if (!canRegister.value) {
    if (!agreeTerms.value) {
      uni.showToast({ title: '请先阅读并同意协议', icon: 'none' })
      return
    }
    if (form.password !== form.confirmPassword) {
      uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
      return
    }
    if (form.password.length < 6) {
      uni.showToast({ title: '密码长度至少6位', icon: 'none' })
      return
    }
    return
  }

  isLoading.value = true

  try {
    const res = await register({
      username: form.username,
      password: form.password,
      inviterCode: form.inviterCode || undefined
    })

    if (res.code === 200 || res.success) {
      uni.showToast({ title: '注册成功，请登录', icon: 'success' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1000)
    } else {
      uni.showToast({
        title: res.message || res.msg || '注册失败',
        icon: 'none',
        duration: 3000
      })
    }
  } catch (error) {
    console.error('注册失败', error)
    uni.showToast({
      title: error.message || '注册失败，请检查网络后重试',
      icon: 'none',
      duration: 3000
    })
  } finally {
    isLoading.value = false
  }
}

const goToLogin = () => {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF9F0 0%, $bg-color 100%);
  padding: $spacing-xl $spacing-lg;
  display: flex;
  flex-direction: column;
}

.logo-section {
  text-align: center;
  padding: 80rpx 0 60rpx;

  .logo {
    font-size: 100rpx;
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
    color: $text-placeholder;
  }
}

.form-section {
  background: $bg-white;
  border-radius: 24rpx;
  padding: $spacing-lg;
  box-shadow: $box-shadow;
}

.form-item {
  display: flex;
  align-items: center;
  padding: $spacing-md 0;
  border-bottom: 1rpx solid $border-color;

  .form-icon {
    font-size: 40rpx;
    margin-right: $spacing-md;
  }

  .form-input {
    flex: 1;
    font-size: $font-size-md;
  }

  .invite-input {
    text-transform: uppercase;
    letter-spacing: 2rpx;
    font-family: monospace;
  }

  .toggle-password {
    font-size: 36rpx;
    padding: $spacing-xs;
  }
}

.invite-hint {
  text-align: center;
  color: $primary-color;
  font-size: $font-size-xs;
  margin-top: $spacing-xs;
}

.register-btn {
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
}

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
