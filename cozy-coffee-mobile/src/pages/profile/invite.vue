<!--
  邀请好友页 - 会员增长/社交权益：我的邀请码 + 分享 + 绑定好友邀请码
-->
<template>
  <view class="invite-page">
    <!-- 页头 -->
    <view class="invite-intro">
      <text class="intro-kicker">COZY REFERRAL</text>
      <text class="intro-title">邀请一位朋友，共享会员礼遇</text>
      <text class="intro-copy">好友完成首单，你们都可获得会员奖励。</text>
    </view>

    <!-- 我的邀请码 -->
    <view class="invite-card">
      <text class="card-label">我的邀请码</text>
      <text class="invite-code">{{ userInfo.inviteCode || '加载中…' }}</text>
      <text class="card-hint">分享给好友，好友注册并完成首单后你即可获得奖励</text>
      <view class="code-actions">
        <button class="code-btn" @click="copyInviteCode">复制邀请码</button>
        <button class="code-btn" open-type="share">分享给好友</button>
      </view>
    </view>

    <!-- 绑定好友邀请码 -->
    <view class="invite-card">
      <view v-if="userInfo.hasAppliedInviteCode" class="bound-state">
        <text class="bound-mark">✓</text>
        <view class="bound-copy">
          <text class="bound-title">已绑定好友邀请码</text>
          <text class="bound-sub">邀请关系只能绑定一次，当前账户已完成绑定。</text>
        </view>
      </view>
      <view v-else>
        <text class="card-label">已有好友邀请码？</text>
        <text class="card-hint">绑定后建立邀请关系，仅可绑定一次</text>
        <view class="apply-row">
          <input v-model="inviteInput" class="invite-input" maxlength="8" placeholder="输入 8 位邀请码" @input="normalizeInviteInput" />
          <button class="apply-button" :loading="applyingInvite" :disabled="applyingInvite" @click="submitInviteCode">绑定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow, onShareAppMessage } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getUserInfo, applyInviteCode } from '@/api/auth'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})
const inviteInput = ref('')
const applyingInvite = ref(false)

onShow(async () => {
  if (!userStore.isLoggedIn) {
    uni.redirectTo({ url: '/pages/login/index' })
    return
  }
  const response = await getUserInfo()
  if (response.code === 200 && response.data) userStore.setLoginInfo(userStore.token, response.data)
})

function copyInviteCode() {
  const code = userInfo.value.inviteCode
  if (!code) {
    uni.showToast({ title: '邀请码加载中，请稍后再试', icon: 'none' })
    return
  }
  uni.setClipboardData({
    data: code,
    success: () => uni.showToast({ title: '邀请码已复制', icon: 'success' })
  })
}

function normalizeInviteInput(event) {
  inviteInput.value = String(event.detail.value || '').toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 8)
}

async function submitInviteCode() {
  if (applyingInvite.value) return
  const code = inviteInput.value.trim().toUpperCase()
  if (!/^[A-Z0-9]{8}$/.test(code)) {
    uni.showToast({ title: '请输入完整的 8 位邀请码', icon: 'none' })
    return
  }
  if (code === userInfo.value.inviteCode) {
    uni.showToast({ title: '不能填写自己的邀请码', icon: 'none' })
    return
  }

  applyingInvite.value = true
  try {
    await applyInviteCode(code)
    inviteInput.value = ''
    const res = await getUserInfo()
    if (res.code === 200 && res.data) userStore.setLoginInfo(userStore.token, res.data)
    uni.showToast({ title: '邀请码绑定成功', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error?.message || '邀请码绑定失败', icon: 'none', duration: 3000 })
  } finally {
    applyingInvite.value = false
  }
}

onShareAppMessage(() => {
  const code = userInfo.value.inviteCode || ''
  return {
    title: `邀请你一起喝咖啡 · 输入邀请码 ${code}，首单双方有礼`,
    path: '/pages/index/index'
  }
})
</script>

<style lang="scss" scoped>
.invite-page {
  min-height: 100vh;
  padding: 24rpx 32rpx 120rpx;
  background: $cozy-bg;
}

/* ── 页头 ── */
.invite-intro { padding: 10rpx 8rpx 20rpx; }
.intro-kicker {
  display: block;
  font-size: 18rpx;
  font-weight: 700;
  letter-spacing: .2em;
  color: $cozy-muted;
}
.intro-title {
  display: block;
  margin-top: 10rpx;
  font-family: $font-display;
  font-size: 44rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.intro-copy {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: $cozy-muted;
}

/* ── 卡片 ── */
.invite-card {
  margin-top: 32rpx;
  padding: 36rpx;
  border: 1rpx solid $cozy-border;
  border-radius: 28rpx;
  background: $bg-white;
}
.card-label {
  display: block;
  font-size: 24rpx;
  font-weight: 650;
  color: $cozy-muted;
}
.invite-code {
  display: block;
  margin-top: 16rpx;
  font-size: 44rpx;
  font-weight: 800;
  letter-spacing: .16em;
  color: $cozy-ink;
}
.card-hint {
  display: block;
  margin-top: 14rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: $cozy-muted;
}
.code-actions {
  margin-top: 28rpx;
  display: flex;
  gap: 20rpx;
}
.code-btn {
  flex: 1;
  height: 72rpx;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid $cozy-border;
  border-radius: $cozy-radius-md;
  background: $bg-white;
  color: $cozy-ink;
  font-size: 24rpx;
  font-weight: 600;

  &:active { opacity: .8; }
}
.code-btn::after { border: 0; }

/* ── 绑定 ── */
.bound-state {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}
.bound-mark {
  width: 44rpx;
  height: 44rpx;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: $cozy-accent;
  color: #fff;
  font-size: 20rpx;
}
.bound-copy { min-width: 0; flex: 1; }
.bound-title { display: block; color: $cozy-ink; font-size: 24rpx; font-weight: 680; }
.bound-sub { display: block; margin-top: 6rpx; color: $cozy-muted; font-size: 20rpx; line-height: 1.5; }
.apply-row {
  margin-top: 20rpx;
  display: flex;
  gap: 16rpx;
}
.invite-input {
  min-width: 0;
  min-height: 72rpx;
  flex: 1;
  padding: 0 20rpx;
  border: 1rpx solid $cozy-border;
  border-radius: $cozy-radius-md;
  background: $bg-white;
  color: $cozy-ink;
  font-size: 26rpx;
  font-weight: 650;
  letter-spacing: .08em;
}
.apply-button {
  width: 140rpx;
  min-height: 72rpx;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid $cozy-border;
  border-radius: $cozy-radius-md;
  background: $bg-white;
  color: $cozy-ink;
  font-size: 24rpx;
  font-weight: 600;

  &:active { opacity: .8; }
}
.apply-button::after { border: 0; }
</style>
