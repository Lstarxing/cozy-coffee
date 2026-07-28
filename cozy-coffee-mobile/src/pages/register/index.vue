<!--
  注册页 - 手机号密码注册
-->
<template>
  <view class="register-page">
    <!-- Logo 区域 -->
    <view class="logo-section">
      <view class="logo">COZY</view>
      <text class="brand-name cozy-display">创建会员账户</text>
      <text class="brand-slogan">保存订单、积分与券包，继续每一次咖啡日常</text>
    </view>

    <!-- 表单区域 -->
    <view class="form-section">
      <view class="form-item">
        <text class="form-icon">账号</text>
        <input
          v-model="form.username"
          type="text"
          placeholder="请输入手机号或邮箱"
          class="form-input"
        />
      </view>

      <view class="form-item">
        <text class="form-icon">密码</text>
        <input
          v-model="form.password"
          :password="!showPassword"
          placeholder="请输入密码 (至少6位)"
          class="form-input"
        />
        <text class="toggle-password" @click="showPassword = !showPassword">
          {{ showPassword ? '隐藏' : '显示' }}
        </text>
      </view>

      <view class="form-item">
        <text class="form-icon">确认</text>
        <input
          v-model="form.confirmPassword"
          :password="!showConfirmPassword"
          placeholder="请再次确认密码"
          class="form-input"
        />
        <text class="toggle-password" @click="showConfirmPassword = !showConfirmPassword">
          {{ showConfirmPassword ? '隐藏' : '显示' }}
        </text>
      </view>

      <view class="form-item">
        <text class="form-icon">邀请</text>
        <input
          v-model="form.inviterCode"
          type="text"
          placeholder="邀请码（选填，首单后按规则奖励）"
          maxlength="8"
          class="form-input invite-input"
        />
      </view>
      <view v-if="form.inviterCode" class="invite-hint">
        绑定邀请关系后，首单完成时按后端规则发放奖励
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
  background: $cozy-surface;
  padding: 0 $spacing-lg $spacing-xl;
  display: flex;
  flex-direction: column;
}

.logo-section {
  text-align: center;
  padding: 100rpx 0 56rpx;

  .logo {
    color: $cozy-ink;
    font-size: 38rpx;
    font-weight: 850;
    letter-spacing: .22em;
    margin-bottom: 28rpx;
  }

  .brand-name {
    font-size: 42rpx;
    font-weight: 600;
    color: $cozy-ink;
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
  border-radius: $cozy-radius-lg;
  padding: $spacing-lg;
  box-shadow: none;
}

.form-item {
  display: flex;
  align-items: center;
  padding: $spacing-md 0;
  border-bottom: 1rpx solid $border-color;

  .form-icon {
    width: 68rpx;
    color: $cozy-primary;
    font-size: 19rpx;
    font-weight: 700;
    margin-right: $spacing-md;
  }

  .form-input {
    flex: 1;
    font-size: $font-size-md;
  }

  .invite-input {
    text-transform: uppercase;
    letter-spacing: 2rpx;
  }

  .toggle-password {
    color: $cozy-primary;
    font-size: 21rpx;
    padding: 16rpx 0 16rpx 16rpx;
  }
}

.invite-hint {
  text-align: center;
  color: $primary-color;
  font-size: $font-size-xs;
  margin-top: $spacing-xs;
}

.register-btn {
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
