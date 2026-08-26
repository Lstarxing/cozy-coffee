<!--
  每日签到页 — 复刻 web 端 SigninWidget 视觉
  - 头部右上 CTA（深棕底浅黄字）
  - 单根进度连线 + 7 颗豆(第7天礼盒矩形)
  - 32rpx 圆框：未签灰白 / 已签深棕；当前日主题色光环
  - 底部"再签 X 天"提示块
  - 顶部成功浮层 toast
-->
<template>
  <view class="signin-page">
    <!-- 签到区 -->
    <view class="signin-widget">
      <view class="widget-header">
        <view>
          <text class="widget-title">每日签到</text>
          <text class="widget-sub">连续签到 7 天可领惊喜礼包</text>
        </view>
        <view
          class="signin-action-btn"
          :class="{ done: hasSigned }"
          @click="handleSignin"
        >
          {{ hasSigned ? '今日已签' : (signing ? '签到中…' : '签到领豆') }}
        </view>
      </view>

      <view class="bean-track-wrapper">
        <!-- 单根进度线（web 端 track-line-base + track-line-fill） -->
        <view class="track-line-base">
          <view
            class="track-line-fill"
            :style="{ width: progressWidth + '%' }"
          ></view>
        </view>
        <view class="bean-steps">
          <view
            v-for="(day, index) in weekDays"
            :key="index"
            class="bean-step"
            :class="stepClass(index)"
          >
            <view class="bean-icon-box" :class="beanClass(index)">
              <!-- 第 7 天礼盒（一张奶油黄礼盒 + CSS 渐变棕矩形底座，web 端无论是否激活底色不变） -->
              <image
                v-if="index === 6"
                src="/static/images/signin/gift.png"
                class="gift-svg"
                mode="aspectFit"
              />
              <!-- 咖啡豆（PNG 豆 + CSS 圈） -->
              <image
                v-else
                :src="index < cycleDay || signedAll ? '/static/images/signin/bean-active.png' : '/static/images/signin/bean-muted.png'"
                class="bean-svg"
                mode="aspectFit"
              />
              <!-- 今日已激活时的额外主题色光环（对齐 web 端 is-today） -->
              <view v-if="hasSigned && index === cycleDay - 1" class="today-glow"></view>
            </view>
            <text class="step-label">{{ label(index) }}</text>
          </view>
        </view>
      </view>

      <view v-if="cycleDay < 7 || !hasSigned" class="widget-footer-info">
        <text class="info-icon">✨</text>
        <text>再签 <text class="foot-em">{{ Math.max(0, 7 - cycleDay) }}</text> 天领满35减10券</text>
      </view>
      <view v-else class="widget-footer-info success">
        <text class="info-icon">🎁</text>
        <text>连续签到7天！礼包已到账</text>
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
        <view class="toast-check"><CozyIcon name="check" :size="18" stroke-width="3" color="#2B1E16" /></view>
        <view class="toast-copy">
          <text class="toast-title">签到成功 +{{ earnedPoints }} 积分</text>
          <text class="toast-sub">已连续签到 {{ cycleDay }} 天</text>
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
const getYesterdayText = () => {
  const now = new Date(Date.now() - 24 * 60 * 60 * 1000)
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 断签检测：最近签到既不是今天也不是昨天 → 连续天数应重置为 0
const lastSigninDate = ref('')
const effectiveConsecutiveDays = computed(() => {
  if (!lastSigninDate.value) return 0
  if (lastSigninDate.value === getLocalDateText()) return consecutiveDays.value
  if (lastSigninDate.value === getYesterdayText()) return consecutiveDays.value
  return 0
})

// 当前周期第几天（对齐 web 端 currentSignInCycleDay: %7 归一，满 7 即 7）
const cycleDay = computed(() => {
  const d = effectiveConsecutiveDays.value
  if (d === 0) return 0
  return d % 7 === 0 ? 7 : d % 7
})
const signedAll = computed(() => cycleDay.value >= 7)

// 进度条宽度：cycleDay=1 → 0%; cycleDay=7 → 100%（web 端公式）
const progressWidth = computed(() => Math.max(0, (cycleDay.value - 1) / 6 * 100))

function stepClass(index) {
  return {
    'is-active': index < cycleDay.value || signedAll.value,
    'is-today': !hasSigned.value && index === cycleDay.value
  }
}
function beanClass(index) {
  return {
    active: index < cycleDay.value || signedAll.value,
    today: !hasSigned.value && index === cycleDay.value,
    'is-gift': index === 6
  }
}
function label(index) {
  if (index === 6) return '礼包'
  if (!hasSigned.value && index === cycleDay.value) return '今日'
  return '+2'
}

onShow(async () => {
  try {
    const res = await getMemberInfo()
    if (res.code === 200) {
      currentPoints.value = res.data.currentPoints ?? 0
      consecutiveDays.value = res.data.consecutiveSignDays ?? 0
      lastSigninDate.value = res.data.lastSigninDate || ''
      userStore.setMemberInfo(res.data)
      hasSigned.value = lastSigninDate.value === getLocalDateText()
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
      lastSigninDate.value = getLocalDateText()
      userStore.setMemberInfo({
        currentPoints: currentPoints.value,
        totalPoints: res.data.totalPoints ?? userStore.memberInfo?.totalPoints,
        consecutiveSignDays: consecutiveDays.value,
        lastSigninDate: lastSigninDate.value
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
// ── 容器 ──
.signin-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding: 40rpx 40rpx 240rpx;
}

// ── 主卡片（对齐 web 端 .signin-premium-widget） ──
.signin-widget {
  margin-top: 40rpx;
  padding: 48rpx 48rpx;
  border-radius: 28rpx;
  background: $bg-white;
  box-shadow: 0 8rpx 30rpx rgba(0,0,0,0.04);
}

/* ── 头部（标题 + 右上 CTA） ── */
.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 48rpx;
}
.widget-title {
  display: block;
  font-family: $font-display;
  font-size: 36rpx;
  font-weight: 700;
  color: $cozy-ink;
}
.widget-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: $cozy-muted;
}
.signin-action-btn {
  flex: none;
  padding: 16rpx 48rpx;
  border-radius: 40rpx;
  background: #3E2723; /* web 端按钮底色（深烘棕） */
  color: #FBEEA8;
  font-size: 26rpx;
  font-weight: 600;
  transition: transform $cozy-duration $cozy-ease-out;
  &:active:not(.done) { transform: translateY(-4rpx); box-shadow: 0 8rpx 24rpx rgba(62, 39, 35, 0.3); }
  &.done {
    background: #E0E0E0;
    color: #9E9E9E;
  }
}

/* ── 豆轨（单根进度线 + 7 颗豆） ── */
.bean-track-wrapper { position: relative; padding: 40rpx 0 24rpx; }
.track-line-base {
  position: absolute;
  /* 豆中心 = padding-top 40rpx + icon 64rpx/2 = 72rpx；translateY(-50%) 居中 */
  top: 72rpx; left: 40rpx; right: 40rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: #F5F5F5;
  transform: translateY(-50%);
  z-index: 1;
}
.track-line-fill {
  height: 100%;
  background: $cozy-primary;
  border-radius: 4rpx;
  transition: width .5s $cozy-ease-out;
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
  width: 80rpx;
}
.bean-icon-box {
  position: relative;
  width: 64rpx; height: 64rpx;
  border-radius: 50%;
  background: #EFEBE9; /* web 未签圈底 */
  border: 4rpx solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
  transition: all .3s $cozy-ease-out;

  /* 已签（web .bean-step.is-active .bean-icon-box） */
  &.active {
    background: #5D4037; /* 深棕圈 */
    transform: scale(1.1);
  }
  /* 今日待签（未签时定位） */
  &.today:not(.active) {
    animation: bean-breathe 1.8s ease-in-out infinite;
  }
  /* 第 7 天礼盒矩形（web .bean-icon-box.is-gift：永远渐变棕底 + 奶油黄礼盒，激活时 scale(1.1)） */
  &.is-gift {
    width: 80rpx; height: 80rpx;
    border-radius: 24rpx;
    background: linear-gradient(135deg, #8D6E63 0%, #5D4037 100%);
    margin-top: -8rpx; /* 上浮突出 */
  }
}
.bean-svg { width: 36rpx; height: 36rpx; display: block; }
.gift-svg { width: 40rpx; height: 40rpx; display: block; }

/* 今日已激活的额外主题色光环（web .bean-step.is-today 强化） */
.today-glow {
  position: absolute;
  inset: -8rpx;
  border-radius: inherit;
  box-shadow: 0 0 0 8rpx rgba(198, 156, 109, 0.3);
  pointer-events: none;
}

/* 今日待签呼吸光环 */
@keyframes bean-breathe {
  0%, 100% { box-shadow: 0 0 0 8rpx rgba(198,156,109,.2); }
  50%      { box-shadow: 0 0 0 28rpx rgba(198,156,109,0); }
}

/* step 标签 */
.step-label {
  font-size: 22rpx;
  color: #9E9E9E;
  font-weight: 500;
}
.bean-step.is-active .step-label { color: #5D4037; font-weight: 700; }
.bean-step.is-today .step-label { color: $cozy-primary; font-weight: 700; }

/* ── 底部提示块（web .widget-footer-info） ── */
.widget-footer-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 32rpx;
  padding: 24rpx 32rpx;
  border-radius: 16rpx;
  background: #F9F9F9;
  font-size: 26rpx;
  color: #555;
}
.widget-footer-info.success { background: #F3E9DF; color: #753A22; }
.info-icon { font-size: 26rpx; }
.foot-em { font-style: normal; color: $cozy-primary; font-weight: 700; }

/* ── 权益说明 ── */
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

/* ── 签到成功浮层 ── */
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
  width: 48rpx; height: 48rpx;
  border-radius: 50%;
  background: #fff;
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