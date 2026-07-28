<template>
  <view class="reset-page">
    <view class="reset-intro">
      <text class="intro-kicker">ACCOUNT RECOVERY</text>
      <text class="intro-title cozy-display">重置开发账号</text>
      <text class="intro-copy">仅用于本地开发验证，不会触发短信或邮件找回流程。</text>
    </view>

    <view class="form-section">
      <view class="field">
        <text class="field-label">注册账号</text>
        <input v-model.trim="username" class="input" placeholder="手机号或邮箱" />
      </view>
      <view class="field">
        <text class="field-label">新密码</text>
        <input v-model="newPassword" class="input" :password="true" placeholder="请输入 6-20 位新密码" />
      </view>
      <view class="field">
        <text class="field-label">确认新密码</text>
        <input v-model="confirmPassword" class="input" :password="true" placeholder="再次输入新密码" />
      </view>
      <text class="form-note">密码长度需为 6-20 位，两次输入必须一致。</text>
      <button class="submit" :disabled="submitting" @click="submitReset">
        {{ submitting ? '正在重置…' : '重置密码' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { resetPasswordDev } from '@/api/auth'

const username = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const submitting = ref(false)

async function submitReset() {
  if (!username.value || newPassword.value.length < 6 || newPassword.value.length > 20) {
    uni.showToast({ title: '请填写账号和 6-20 位新密码', icon: 'none' })
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await resetPasswordDev(username.value, newPassword.value)
    uni.showToast({ title: '密码已重置', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (error) {
    uni.showToast({ title: error?.message || '重置失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.reset-page { min-height: 100vh; background: $cozy-bg; }
.reset-intro { padding: 44rpx 32rpx 48rpx; background: $cozy-surface-alt; color: $cozy-on-dark; }
.intro-kicker { display: block; color: $cozy-muted-on-dark; font-size: 19rpx; font-weight: 750; letter-spacing: .13em; }
.intro-title { display: block; margin-top: 12rpx; font-size: 42rpx; line-height: 1.25; }
.intro-copy { display: block; margin-top: 14rpx; color: $cozy-muted-on-dark; font-size: 23rpx; line-height: 1.55; }
.form-section { padding: 34rpx 32rpx 48rpx; }
.field { margin-bottom: 24rpx; }
.field-label { display: block; margin-bottom: 10rpx; color: $cozy-ink; font-size: 24rpx; font-weight: 650; }
.input { width: 100%; height: 92rpx; padding: 0 24rpx; border: 1rpx solid $cozy-border; border-radius: $cozy-radius-md; background: $cozy-surface; color: $cozy-ink; font-size: 27rpx; }
.input:focus { border-color: $cozy-primary; background: #fff; }
.form-note { display: block; color: $cozy-muted; font-size: 21rpx; line-height: 1.5; }
.submit { width: 100%; height: 88rpx; margin-top: 30rpx; border-radius: $cozy-radius-md; background: $cozy-primary; color: #fff; font-size: 28rpx; font-weight: 700; }
.submit[disabled] { opacity: .55; }
</style>
