<!--
  每日签到页 - 精确复现 prototype/signin.html
  咖啡豆轨迹（7 天进度）+ 立即签到 CTA + checkin-foot 提示块 + 权益说明 + 顶部成功消息
-->
<template>
  <view class="signin-page">
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
          <view class="track-line-fill" :style="{ transform: 'scaleX(' + (trackPercent / 100) + ')' }"></view>
        </view>
        <view class="bean-steps">
          <view
            v-for="(day, index) in weekDays"
            :key="index"
            class="bean-step"
            :class="stepClass(index)"
          >
            <view class="bean-icon" :class="beanClass(index)">
              <CozyIcon v-if="index === 6" name="gift" :size="16" :color="index < signedCount || signedAll ? '#F2EDE8' : '#9E948E'" />
              <CozyIcon v-else name="bean" :size="16" :color="index < signedCount || signedAll ? '#F2EDE8' : '#9E948E'" />
            </view>
            <text class="bean-label">{{ label(index) }}</text>
          </view>
        </view>
      </view>

      <view class="signin-btn" :class="{ done: hasSigned || signing }" @click="handleSignin">
        {{ hasSigned ? '今日已签到 ✓' : (signing ? '签到中…' : '立即签到') }}
      </view>
      <view class="checkin-foot" :class="{ success: consecutiveDays >= 7 }" id="signinHint">
        <text v-if="hasSigned && consecutiveDays >= 7">连续签到 7 天！礼包已到账</text>
        <text v-else>再签 <text class="foot-em">{{ Math.max(0, 7 - consecutiveDays) }}</text> 天领满35减10券</text>
      </view>
    </view>

    <!-- 权益说明 -->
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

    <!-- 签到成功浮层 -->
    <view class="toast-mask" :class="{ open: showBanner }" :style="{ top: (statusBarHeight + 16) + 'px' }">
      <view class="signin-toast">
        <view class="toast-check"><CozyIcon name="check" :size="18" stroke-width="3" color="#FFFFFF" /></view>
        <view class="toast-copy">
          <text class="toast-title">签到成功 +{{ earnedPoints }} 积分</text>
          <text class="toast-sub">已连续签到 {{ consecutiveDays }} 天</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { signIn, getMemberInfo } from '@/api/member'
import CozyIcon from '@/components/CozyIcon.vue'

const userStore = useUserStore()

const hasSigned = ref(false)
const consecutiveDays = ref(0)
const currentPoints = ref(0)
const earnedPoints = ref(0)
const signing = ref(false)
const showBanner = ref(false)
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20

const weekDays = Array.from({ length: 7 }, () => ({}))

const getLocalDateText = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 已签到的天数（未签今天时第7天礼盒未激活）
const signedCount = computed(() => hasSigned.value ? Math.min(consecutiveDays.value, 7) : Math.min(consecutiveDays.value, 6))
const signedAll = computed(() => consecutiveDays.value >= 7)
const trackPercent = computed(() => {
  if (signedAll.value) return 100
  return Math.max(0, (consecutiveDays.value / 6) * 100)
})

function stepClass(index) {
  return {
    active: index < signedCount.value || signedAll.value,
    today: !hasSigned.value && index === consecutiveDays.value
  }
}

function beanClass(index) {
  return {
    active: index < signedCount.value || signedAll.value,
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
  padding: 40rpx 40rpx 240rpx;
}

/* ── 签到区 ── */
.checkin-section {
  margin-top: 40rpx;
  padding: 56rpx 48rpx;
  border-radius: 28rpx;
  background: $bg-white;
}
.checkin-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 52rpx;
}
.checkin-title {
  display: block;
  font-family: $font-display;
  font-size: 38rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.checkin-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $cozy-muted;
}
.checkin-streak {
  flex: none;
  font-size: 24rpx;
  color: $cozy-muted;
  padding-bottom: 4rpx;
}

/* 咖啡豆轨迹 */
.bean-track { position: relative; padding: 44rpx 0 12rpx; }
.track-line {
  position: absolute;
  top: 68rpx; left: 32rpx; right: 32rpx;
  height: 6rpx;
  border-radius: 4rpx;
  background: $cozy-border;
  transform: translateY(-50%);
}
.track-line-fill {
  height: 100%;
  border-radius: 4rpx;
  background: $cozy-primary;
  transform-origin: left;
  transition: transform .5s ease;
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
  gap: 16rpx;
  width: 72rpx;
}
.bean-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: $cozy-surface;
  border: 1rpx solid $cozy-border;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .3s;

  &.active {
    background: $cozy-primary;
    border-color: $cozy-primary;
  }
  &.active.today {
    box-shadow: 0 0 0 8rpx rgba(198,156,109,.35);
  }
  &.is-gift { border-radius: 24rpx; }
}
.bean-label {
  font-size: 22rpx;
  color: $cozy-placeholder;
}
.bean-step.active .bean-label { color: $cozy-ink; font-weight: 600; }
.bean-step.active.today .bean-label { color: $cozy-primary; font-weight: 700; }
.bean-step.today .bean-label { color: $cozy-primary; font-weight: 700; }

/* 签到主操作按钮 */
.signin-btn {
  width: 100%;
  height: 96rpx;
  margin-top: 44rpx;
  border: 1rpx solid $cozy-border;
  border-radius: $cozy-radius-md;
  background: $bg-white;
  color: $cozy-ink;
  font-size: 30rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color $cozy-duration $cozy-ease-out;

  &:active { border-color: $cozy-ink; }
  &.done {
    background: $bg-white;
    color: $cozy-muted;
    border: 1rpx solid $cozy-border;
  }
}

/* 提示块（浅色底） */
.checkin-foot {
  margin-top: 36rpx;
  padding: 24rpx 32rpx;
  border-radius: 16rpx;
  background: $cozy-surface;
  font-size: 26rpx;
  color: $cozy-muted;
  text-align: center;
}
.foot-em {
  font-style: normal;
  color: $cozy-primary;
  font-weight: 700;
}
.checkin-foot.success {
  background: $bg-white;
  color: $cozy-primary;
}

/* ── 权益说明（Editorial） ── */
.benefits-section {
  margin-top: 40rpx;
  padding: 56rpx 48rpx;
  border-radius: 28rpx;
  background: $bg-white;
}
.benefits-title {
  display: block;
  margin-bottom: 44rpx;
  font-family: $font-display;
  font-size: 38rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.benefit-card {
  padding: 32rpx 0;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; padding-bottom: 0; }
}
.benefit-head {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
  letter-spacing: .04em;
}
.benefit-desc {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: $cozy-muted;
}

/* ── 签到成功浮层（居中悬浮卡） ── */
.toast-mask {
  position: fixed;
  left: 0; right: 0;
  display: flex;
  justify-content: center;
  z-index: 100;
  pointer-events: none;
  opacity: 0;
  transform: translateY(-220%);
  transition: opacity .3s ease, transform .45s $cozy-ease-out;
}
.toast-mask.open { opacity: 1; transform: translateY(0); }
.signin-toast {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 26rpx 40rpx;
  border-radius: 24rpx;
  background: $cozy-ink;
  box-shadow: $cozy-shadow-raised;
}
.toast-check {
  flex: none;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: $cozy-accent;
  display: flex;
  align-items: center;
  justify-content: center;
}
.toast-copy { flex: none; min-width: 0; }
.toast-title {
  display: block;
  font-size: 28rpx;
  font-weight: 650;
  line-height: 1.3;
  color: #fff;
}
.toast-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, .7);
}
</style>
