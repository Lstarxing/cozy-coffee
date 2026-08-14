<template>
  <view class="edit-page">
    <view class="profile-hero">
      <view class="avatar-row">
        <image :src="avatarUrl" class="avatar" mode="aspectFill" />
        <view class="avatar-copy">
          <text class="avatar-title">会员头像</text>
          <text class="avatar-note">最大 5MB</text>
          <button class="avatar-button" :loading="avatarUploading" :disabled="avatarUploading" @click="chooseAvatar">
            {{ avatarUploading ? '上传中' : '更换头像' }}
          </button>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-heading">
        <text class="section-title">基础资料</text>
        <text class="section-note">昵称 2～20 个字符</text>
      </view>

      <view class="field-row readonly">
        <text class="field-label">登录账号</text>
        <text class="field-value">{{ userInfo.username || '微信开发账号' }}</text>
      </view>
      <label class="field-row">
        <text class="field-label">昵称</text>
        <input v-model="form.nickname" class="field-input" maxlength="20" placeholder="请输入昵称" />
      </label>
      <label class="field-row">
        <text class="field-label">手机号</text>
        <input v-model="form.phone" class="field-input" type="number" maxlength="11" placeholder="请输入手机号" />
      </label>
      <label class="field-row">
        <text class="field-label">邮箱</text>
        <input v-model="form.email" class="field-input" type="text" maxlength="100" placeholder="请输入邮箱" />
      </label>
      <picker mode="date" :value="form.birthday" start="1900-01-01" :end="today" @change="handleBirthdayChange">
        <view class="field-row">
          <text class="field-label">生日</text>
          <text class="field-value" :class="{ placeholder: !form.birthday }">{{ form.birthday || '请选择生日' }}</text>
          <text class="field-arrow">›</text>
        </view>
      </picker>
      <text class="birthday-tip">生日首次设置后，每年只能修改一次；保存生日可能触发生日会员权益。</text>

      <button class="primary-button" :loading="saving" :disabled="saving" @click="saveProfile">
        {{ saving ? '保存中' : '保存资料' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getUserInfo, updateProfile, uploadAvatar } from '@/api/auth'
import { useSessionStore } from '@/stores/session'
import { getImageUrl } from '@/utils/image'

const sessionStore = useSessionStore()
const userInfo = computed(() => sessionStore.userInfo || {})
const saving = ref(false)
const avatarUploading = ref(false)
const avatarPreview = ref('')
const today = new Date().toISOString().slice(0, 10)
const form = reactive({ nickname: '', phone: '', email: '', birthday: '' })
let snapshot = { nickname: '', phone: '', email: '', birthday: '' }

const avatarUrl = computed(() => avatarPreview.value || resolveAvatarUrl(userInfo.value.avatar))

onShow(async () => {
  if (!sessionStore.isLoggedIn) {
    uni.redirectTo({ url: '/pages/login/index' })
    return
  }
  await refreshUserInfo()
})

function resolveAvatarUrl(value) {
  if (!value || value === '/images/default-avatar.png') return '/static/images/default-avatar.png'
  return getImageUrl(value)
}

function syncForm(info = {}) {
  const next = {
    nickname: info.nickname || '',
    phone: info.phone || '',
    email: info.email || '',
    birthday: info.birthday || ''
  }
  Object.assign(form, next)
  snapshot = { ...next }
}

async function refreshUserInfo() {
  try {
    const response = await getUserInfo()
    if (response.code === 200 && response.data) {
      sessionStore.setLoginInfo(sessionStore.token, response.data)
      syncForm(response.data)
    }
  } catch (error) {
    uni.showToast({ title: error?.message || '资料加载失败', icon: 'none' })
  }
}

function handleBirthdayChange(event) {
  form.birthday = event.detail.value
}

function validateProfile() {
  const nickname = form.nickname.trim()
  const phone = form.phone.trim()
  const email = form.email.trim()
  if (!phone && !email) return '手机号或邮箱至少保留一项（账号凭此登录）'
  if (nickname.length < 2 || nickname.length > 20) return '昵称长度需在 2～20 个字符之间'
  if (phone && !/^1[3-9]\d{9}$/.test(phone)) return '请输入正确的手机号'
  if (email && !/^[\w.-]+@[\w.-]+\.\w+$/.test(email)) return '请输入正确的邮箱地址'
  return ''
}

async function saveProfile() {
  if (saving.value) return
  const validationMessage = validateProfile()
  if (validationMessage) {
    uni.showToast({ title: validationMessage, icon: 'none' })
    return
  }

  const normalized = {
    nickname: form.nickname.trim(),
    phone: form.phone.trim(),
    email: form.email.trim(),
    birthday: form.birthday
  }
  const payload = {}
  Object.keys(normalized).forEach((key) => {
    if (normalized[key] !== snapshot[key]) payload[key] = normalized[key]
  })
  if (!Object.keys(payload).length) {
    uni.showToast({ title: '资料没有变化', icon: 'none' })
    return
  }

  saving.value = true
  try {
    await updateProfile(payload)
    await refreshUserInfo()
    uni.showToast({ title: '资料已更新', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error?.message || '资料保存失败', icon: 'none', duration: 3000 })
  } finally {
    saving.value = false
  }
}

function chooseAvatar() {
  if (avatarUploading.value) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (result) => {
      const file = result.tempFiles?.[0]
      const filePath = file?.path || result.tempFilePaths?.[0]
      if (!filePath) return
      if (file?.size && file.size > 5 * 1024 * 1024) {
        uni.showToast({ title: '图片大小不能超过 5MB', icon: 'none' })
        return
      }

      avatarPreview.value = filePath
      avatarUploading.value = true
      try {
        const uploadResponse = await uploadAvatar(filePath)
        const avatar = uploadResponse?.data?.url
        if (!avatar) throw new Error('头像地址缺失')
        await updateProfile({ avatar })
        await refreshUserInfo()
        avatarPreview.value = ''
        uni.showToast({ title: '头像已更新', icon: 'success' })
      } catch (error) {
        avatarPreview.value = ''
        uni.showToast({ title: error?.message || '头像更新失败', icon: 'none', duration: 3000 })
      } finally {
        avatarUploading.value = false
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.edit-page { min-height: 100vh; padding-bottom: 80rpx; background: $cozy-surface; }
.profile-hero { margin: 24rpx; padding: 28rpx 36rpx; border-radius: 28rpx; background: $bg-white; color: $cozy-ink; }
.avatar-row { display: flex; align-items: center; gap: 24rpx; }
.avatar { width: 124rpx; height: 124rpx; flex: none; border: 2rpx solid $cozy-border; border-radius: 50%; background: $bg-white; }
.avatar-copy { min-width: 0; flex: 1; }
.avatar-title { display: block; font-size: 26rpx; font-weight: 700; }
.avatar-note { display: block; margin-top: 6rpx; color: $cozy-muted; font-size: 18rpx; }
.avatar-button { width: max-content; min-height: 58rpx; margin: 15rpx 0 0; padding: 0 20rpx; display: flex; align-items: center; border: 1rpx solid $cozy-border; border-radius: $cozy-radius-md; background: transparent; color: $cozy-ink; font-size: 20rpx; line-height: 1; }
.avatar-button::after, .primary-button::after { border: 0; }

.section { margin: 0 24rpx 24rpx; padding: 28rpx; border-radius: 28rpx; background: $bg-white; }
.section-heading { padding-bottom: 20rpx; border-bottom: 1rpx solid $cozy-border; }
.section-title { display: block; color: $cozy-ink; font-size: 29rpx; font-weight: 720; }
.section-note { display: block; margin-top: 6rpx; color: $cozy-muted; font-size: 18rpx; }
.field-row { min-height: 104rpx; display: flex; align-items: center; gap: 20rpx; border-bottom: 1rpx solid $cozy-border; }
.field-row.readonly { color: $cozy-muted; }
.field-label { width: 120rpx; flex: none; color: $cozy-muted; font-size: 22rpx; }
.field-input { min-width: 0; flex: 1; color: $cozy-ink; font-size: 24rpx; text-align: right; }
.field-value { min-width: 0; flex: 1; overflow: hidden; color: $cozy-ink; font-size: 23rpx; text-align: right; text-overflow: ellipsis; white-space: nowrap; }
.field-value.placeholder { color: $cozy-placeholder; }
.field-arrow { flex: none; color: $cozy-placeholder; font-size: 38rpx; }
.birthday-tip { display: block; margin-top: 16rpx; color: $cozy-muted; font-size: 18rpx; line-height: 1.55; }
.primary-button { min-height: 88rpx; margin-top: 28rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: $cozy-ink; color: #fff; font-size: 26rpx; font-weight: 600; }
</style>
