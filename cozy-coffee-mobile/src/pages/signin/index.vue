<!--
  每日签到页 - 复现 prototype/signin.html：积分头 + 咖啡豆轨迹 + 立即签到 CTA + 积分说明
-->
<template>
  <view class="signin-page">
    <!-- 积分头 -->
    <view class="points-header">
      <text class="points-label">当前可用积分</text>
      <text class="points-value">{{ currentPoints }}</text>
    </view>

    <!-- 签到区 -->
    <view class="checkin-section">
      <view class="checkin-head">
        <view>
          <text class="checkin-title">每日签到</text>
          <text class="checkin-sub">连续签到 7 天可领惊喜礼包</text>
        </view>
        <text class="checkin-streak">已连续 {{ consecutiveDays }} 天</text>
      </view>

      <view class="bean-track">
        <view class="track-line">
          <view class="track-line-fill" :style="{ width: trackPercent + '%' }"></view>
        </view>
        <view class="bean-steps">
          <view
            v-for="(day, index) in weekDays"
            :key="index"
            class="bean-step"
            :class="stepClass(index)"
          >
            <view class="bean-icon" :class="beanClass(index)">
              <text v-if="index === 6" class="bean-glyph">券</text>
              <text v-else-if="index < signedCount" class="bean-glyph">✓</text>
            </view>
            <text class="bean-label">{{ label(index) }}</text>
          </view>
        </view>
      </view>

      <view class="signin-btn" :class="{ done: hasSigned || signing }" @click="handleSignin">
        {{ hasSigned ? '今日已签到 ✓' : (signing ? '签到中…' : '立即签到') }}
      </view>
      <text class="checkin-hint" :class="{ success: consecutiveDays >= 7 }">
        <template v-if="hasSigned && consecutiveDays >= 7">连续签到 7 天！礼包已到账</template>
        <template v-else>再签 <text class="highlight">{{ Math.max(0, 7 - (hasSigned ? consecutiveDays : consecutiveDays)) }}</text> 天领满35减10券</template>
      </text>
    </view>

    <!-- 积分说明 -->
    <view class="benefits-section">
      <text class="benefits-title">积分说明</text>
      <view class="benefit-card">
        <text class="benefit-head">每日签到</text>
        <text class="benefit-desc">每签到一次固定获得 2 积分。连续签到 7 天额外赠送满 35 减 10 元优惠券，有效期 3 天。</text>
      </view>
      <view class="benefit-card">
        <text class="benefit-head">连续签到</text>
        <text class="benefit-desc">中断签到后连续天数重新计算。坚持每天来打卡，培养你的咖啡仪式感。</text>
      </view>
      <view class="benefit-card">
        <text class="benefit-head">积分使用</text>
        <text class="benefit-desc">签到积分有效期 365 天，可在积分商城兑换优惠券、限量周边与咖啡礼盒。</text>
      </view>
      <view class="benefit-card">
        <text class="benefit-head">会员成长</text>
        <text class="benefit-desc">消费 1 元 = 1 积分 = 1 EXP，积分累积同步提升会员等级，解锁更多专属权益。</text>
      </view>
    </view>

    <!-- 签到成功消息 -->
    <view v-if="showBanner" class="top-banner">
      <view class="banner-icon"><text class="banner-check">✓</text></view>
      <view class="banner-copy">
        <text class="banner-title">签到成功 +{{ earnedPoints }} 积分</text>
        <text class="banner-sub">已连续签到 {{ consecutiveDays }} 天</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { signIn, getMemberInfo } from '@/api/member'

const userStore = useUserStore()

const hasSigned = ref(false)
const consecutiveDays = ref(0)
const currentPoints = ref(0)
const earnedPoints = ref(0)
const signing = ref(false)
const showBanner = ref(false)

const weekDays = Array.from({ length: 7 }, () => ({}))

const getLocalDateText = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const signedCount = computed(() => hasSigned.value ? Math.min(consecutiveDays.value, 7) : Math.min(consecutiveDays.value, 6))
const trackPercent = computed(() => {
  if (consecutiveDays.value >= 7) return 100
  return Math.max(0, (consecutiveDays.value / 6) * 100)
})

function stepClass(index) {
  return {
    active: index <= signedCount.value || index < consecutiveDays.value,
    today: !hasSigned.value && index === consecutiveDays.value
  }
}

function beanClass(index) {
  return {
    active: index < signedCount.value || (index === 6 && consecutiveDays.value >= 7),
    today: !hasSigned.value && index === consecutiveDays.value,
    'is-gift': index === 6
  }
}

function label(index) {
  if (index === 6) return '礼包'
  if (!hasSigned.value && index === consecutiveDays.value) return '今日'
  return '+2'
}

onShow(async () => {
  try {
    const res = await getMemberInfo()
    if (res.code === 200) {
      currentPoints.value = res.data.currentPoints ?? 0
      consecutiveDays.value = res.data.consecutiveSignDays ?? 0
      userStore.setMemberInfo(res.data)
      hasSigned.value = res.data.lastSigninDate === getLocalDateText()
    }
  } catch (e) {
    console.error('获取会员信息失败', e)
    currentPoints.value = userStore.memberInfo?.currentPoints || 0
  }
})

const handleSignin = async () => {
  if (hasSigned.value || signing.value) return
  signing.value = true
  try {
    const res = await signIn()
    if (res.code === 200) {
      hasSigned.value = true
      consecutiveDays.value = res.data.consecutiveDays ?? consecutiveDays.value + 1
      earnedPoints.value = res.data.pointsEarned ?? 2
      currentPoints.value = res.data.currentPoints ?? currentPoints.value + earnedPoints.value
      showBanner.value = true
      setTimeout(() => { showBanner.value = false }, 2200)

      userStore.setMemberInfo({
        currentPoints: currentPoints.value,
        totalPoints: res.data.totalPoints ?? userStore.memberInfo?.totalPoints,
        consecutiveSignDays: consecutiveDays.value,
        lastSigninDate: getLocalDateText()
      })
    }
  } catch (error) {
    console.error('签到失败', error)
    uni.showToast({ title: error?.message || '签到失败', icon: 'none' })
  } finally {
    signing.value = false
  }
}
</script>

<style lang="scss" scoped>
.signin-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding-bottom: 60rpx;
}

/* ── 积分头 ── */
.points-header {
  padding: 44rpx 40rpx 30rpx;
  background: $cozy-surface;
}
.points-label {
  display: block;
  font-size: 23rpx;
  color: $cozy-muted;
  letter-spacing: .04em;
}
.points-value {
  display: block;
  margin-top: 14rpx;
  font-family: $font-display;
  font-size: 84rpx;
  font-weight: 600;
  color: $cozy-ink;
  line-height: 1;
}

/* ── 签到区 ── */
.checkin-section {
  margin-top: 32rpx;
  padding: 44rpx 40rpx;
  border-radius: $cozy-radius-lg;
  background: $bg-white;
}
.checkin-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 40rpx;
}
.checkin-title {
  display: block;
  font-family: $font-display;
  font-size: 34rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.checkin-sub {
  display: block;
  margin-top: 7rpx;
  font-size: 21rpx;
  color: $cozy-muted;
}
.checkin-streak {
  flex: none;
  font-size: 21rpx;
  color: $cozy-muted;
  padding-bottom: 4rpx;
}

/* 咖啡豆轨迹 */
.bean-track { position: relative; padding: 34rpx 0 10rpx; }
.track-line {
  position: absolute;
  top: 52rpx; left: 24rpx; right: 24rpx;
  height: 5rpx;
  border-radius: 3rpx;
  background: $cozy-border;
  transform: translateY(-50%);
}
.track-line-fill {
  height: 100%;
  border-radius: 3rpx;
  background: $cozy-primary;
  transition: width .5s ease;
}
.bean-steps {
  display: flex;
  justify-content: space-between;
  position: relative;
  z-index: 2;
}
.bean-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  width: 56rpx;
}
.bean-icon {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background: $cozy-surface;
  color: $cozy-placeholder;
  border: 1rpx solid $cozy-border;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .3s;

  &.active {
    background: $cozy-primary;
    color: #F2EDE8;
    border-color: $cozy-primary;
  }
  &.active.today {
    box-shadow: 0 0 0 6rpx rgba(198,156,109,.35);
  }
  &.is-gift { border-radius: 16rpx; }
}
.bean-glyph { font-size: 22rpx; font-weight: 700; }
.bean-label {
  font-size: 20rpx;
  color: $cozy-placeholder;
}
.bean-step.active .bean-label { color: $cozy-ink; font-weight: 600; }
.bean-step.today .bean-label { color: $cozy-primary; font-weight: 700; }

/* 签到主操作按钮 */
.signin-btn {
  width: 100%;
  height: 88rpx;
  margin-top: 34rpx;
  border-radius: $cozy-radius-md;
  background: $cozy-primary;
  color: #fff;
  font-size: $font-size-md;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background $cozy-duration $cozy-ease-out;

  &:active { background: $cozy-primary-hover; }
  &.done {
    background: $bg-white;
    color: $cozy-primary;
    border: 1rpx solid $cozy-primary;
  }
}
.checkin-hint {
  display: block;
  margin-top: 20rpx;
  font-size: 21rpx;
  color: $cozy-muted;
  letter-spacing: .04em;
  text-align: center;

  &.success { color: $cozy-primary; }
  .highlight { color: $cozy-primary; font-weight: 700; }
}

/* ── 权益说明 ── */
.benefits-section {
  margin-top: 32rpx;
  padding: 44rpx 40rpx;
  border-radius: $cozy-radius-lg;
  background: $bg-white;
}
.benefits-title {
  display: block;
  margin-bottom: 34rpx;
  font-family: $font-display;
  font-size: 34rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.benefit-card {
  padding: 26rpx 0;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; padding-bottom: 0; }
}
.benefit-head {
  display: block;
  font-size: $font-size-md;
  font-weight: 600;
  color: $cozy-ink;
  letter-spacing: .04em;
}
.benefit-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 23rpx;
  line-height: 1.7;
  color: $cozy-muted;
}

/* ── 签到成功消息 ── */
.top-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  padding: 26rpx 32rpx;
  background: $cozy-surface-alt;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 20rpx;
  z-index: 50;
  animation: banner-slide .35s cubic-bezier(.32,.72,.32,1);
}
@keyframes banner-slide {
  from { transform: translateY(-100%); }
  to { transform: translateY(0); }
}
.banner-icon {
  flex: none;
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: rgba(255,255,255,.14);
  display: flex;
  align-items: center;
  justify-content: center;
}
.banner-check { font-size: 24rpx; font-weight: 700; }
.banner-copy { flex: 1; min-width: 0; }
.banner-title {
  display: block;
  font-size: $font-size-md;
  font-weight: 650;
  line-height: 1.3;
}
.banner-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 21rpx;
  color: rgba(255,255,255,.6);
}
</style>
