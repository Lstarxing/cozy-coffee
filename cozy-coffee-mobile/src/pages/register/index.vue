<!--
  注册页 - 品牌字标 + 字段表单（密码眼睛开关）+ 墨黑主按钮
-->
<template>
  <view class="register-page">
    <!-- 品牌区 -->
    <view class="auth-hero">
      <text class="brand-word">COZY COFFEE</text>
      <text class="hero-title">创建会员账户</text>
      <text class="hero-sub">保存订单、积分与券包，继续每一次咖啡日常</text>
    </view>

    <!-- 表单卡 -->
    <view class="auth-card">
      <view class="field">
        <text class="field-label">账号</text>
        <view class="field-input-wrap">
          <input
            v-model="form.username"
            type="text"
            placeholder="手机号 / 邮箱"
            placeholder-class="field-placeholder"
            class="field-input"
          />
        </view>
      </view>
      <view class="field">
        <text class="field-label">密码</text>
        <view class="field-input-wrap">
          <input
            v-model="form.password"
            :password="!showPassword"
            placeholder="至少 6 位"
            placeholder-class="field-placeholder"
            class="field-input"
          />
          <view class="field-toggle" @click="showPassword = !showPassword">
            <CozyIcon :name="showPassword ? 'eye-off' : 'eye'" :size="20" color="#756A63" />
          </view>
        </view>
      </view>
      <view class="field">
        <text class="field-label">确认密码</text>
        <view class="field-input-wrap">
          <input
            v-model="form.confirmPassword"
            :password="!showConfirmPassword"
            placeholder="请再次确认密码"
            placeholder-class="field-placeholder"
            class="field-input"
          />
          <text class="field-toggle" @click="showConfirmPassword = !showConfirmPassword">
            <CozyIcon :name="showConfirmPassword ? 'eye-off' : 'eye'" :size="20" color="#756A63" />
          </text>
        </view>
      </view>
      <view class="field">
        <text class="field-label">邀请码</text>
        <view class="field-input-wrap">
          <input
            v-model="form.inviterCode"
            type="text"
            placeholder="选填，首单后按规则奖励"
            maxlength="8"
            placeholder-class="field-placeholder"
            class="field-input invite-input"
          />
        </view>
      </view>
      <view v-if="form.inviterCode" class="invite-hint">绑定邀请关系后，首单完成时按后端规则发放奖励</view>

      <button class="submit-btn" :class="{ disabled: !canRegister }" :loading="isLoading" :disabled="isLoading" @click="handleRegister">
        {{ isLoading ? '注册中…' : '注册' }}
      </button>

      <view class="auth-links">
        <text class="auth-link" @click="goToLogin">已有账号？立即登录</text>
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
import CozyIcon from '@/components/CozyIcon.vue'

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
      uni.showToast({ title: res.message || res.msg || '注册失败', icon: 'none', duration: 3000 })
    }
  } catch (error) {
    console.error('注册失败', error)
    uni.showToast({ title: error.message || '注册失败，请检查网络后重试', icon: 'none', duration: 3000 })
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
  padding: 0 48rpx calc(48rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
}

/* ── 品牌区 ── */
.auth-hero {
  text-align: center;
  padding: 56rpx 0 40rpx;
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
  font-size: 48rpx;
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
.invite-input {
  text-transform: uppercase;
  letter-spacing: 2rpx;
}
.invite-hint {
  margin-top: 20rpx;
  font-size: 20rpx;
  color: $cozy-accent;
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
}
.auth-link {
  font-size: 24rpx;
  color: $cozy-muted;

  &:active { opacity: .6; }
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
