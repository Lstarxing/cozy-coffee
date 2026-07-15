<template>
  <view class="reset-page">
    <view class="panel">
      <text class="title">开发环境重置密码</text>
      <text class="description">仅用于本地开发验证。输入注册账号与新密码即可重置。</text>
      <input v-model.trim="username" class="input" placeholder="手机号或邮箱" />
      <input v-model="newPassword" class="input" :password="true" placeholder="新密码（6-20 位）" />
      <input v-model="confirmPassword" class="input" :password="true" placeholder="再次输入新密码" />
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
.reset-page { min-height: 100vh; padding: 48rpx 32rpx; background: $cozy-surface; }
.panel { padding: 40rpx 32rpx; border-radius: $cozy-radius-lg; background: #fff; }
.title { display: block; color: $cozy-ink; font-size: 36rpx; font-weight: 750; }
.description { display: block; margin: 16rpx 0 32rpx; color: $cozy-muted; font-size: 24rpx; line-height: 1.6; }
.input { height: 88rpx; margin-top: 20rpx; padding: 0 24rpx; border: 1rpx solid $cozy-border; border-radius: $cozy-radius-md; background: $cozy-surface; }
.submit { height: 88rpx; margin-top: 32rpx; border-radius: 44rpx; background: $cozy-primary; color: #fff; font-size: 28rpx; }
.submit[disabled] { opacity: .55; }
</style>
