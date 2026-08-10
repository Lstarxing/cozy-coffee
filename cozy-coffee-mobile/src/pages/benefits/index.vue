<!--
  会员权益页 - 复现 web 端 MemberBenefits：等级 tabs + 每级 feature 卡（3D 卡面 + 权益列表 + 领取）+ 会员进度 footer
-->
<template>
  <view class="benefits-page">
    <!-- 等级 tabs -->
    <view class="level-tabs">
      <view
        v-for="level in levels"
        :key="level"
        class="level-tab"
        :class="selectedLevel === level ? 'active ' + level : ''"
        @click="selectedLevel = level"
      >{{ getLevelName(level) }}</view>
    </view>

    <!-- 每级 feature 卡 -->
    <view class="feature-card" :class="[selectedLevel, { 'is-black': selectedLevel === 'black' }]">
      <!-- 3D 卡面 -->
      <view class="card-visual">
        <view class="physical-card" :class="selectedLevel">
          <view class="card-face">
            <view class="card-brand"><LevelBadge :level="selectedLevel" :size="34" :color="theme.accent" /></view>
            <text class="card-logo">COZY {{ selectedLevel.toUpperCase() }}</text>
            <text class="card-number">8888 8888 8888 8888</text>
            <text class="card-member">MEMBER</text>
          </view>
        </view>
      </view>

      <!-- 权益列表 -->
      <view class="benefit-list">
        <view class="benefit-row" v-for="(b, i) in currentBenefits" :key="i">
          <text class="b-icon">{{ b.code }}</text>
          <view class="b-copy">
            <text class="b-name">{{ b.name }}</text>
            <text class="b-desc">{{ b.desc }}</text>
          </view>
        </view>
      </view>

      <!-- 领取（当前等级） -->
      <view v-if="currentLevel === selectedLevel" class="benefit-action">
        <view v-if="benefitLoading" class="inline-state">正在查询本月权益…</view>
        <view v-else-if="benefitError" class="inline-state error" @click="loadBenefitsPage">{{ benefitError }}，点击重试</view>
        <template v-else>
          <view
            class="receive-btn"
            :class="[selectedLevel, { disabled: !canReceiveMonthlyBenefit }]"
            @click="handleReceiveMonthlyBenefit"
          >{{ benefitActionText }}</view>
          <text v-if="shouldShowUpgradeTip" class="upgrade-tip">恭喜升级！{{ getLevelName(currentLevel) }}月度权益将在下月 1 日生效</text>
        </template>
      </view>
    </view>

    <!-- 会员进度 footer -->
    <view class="member-progress-footer">
      <view class="progress-info">
        <view class="current-status">
          <text class="status-icon">{{ levelEmoji }}</text>
          <text class="status-text">{{ getLevelName(currentLevel) }} {{ currentLevel.toUpperCase() }}</text>
        </view>
        <view class="progress-numbers">
          <text class="current-exp">{{ currentExp }}</text>
          <text class="total-exp">{{ nextThreshold != null ? '/ ' + nextThreshold + ' EXP' : '' }}</text>
        </view>
      </view>
      <view class="progress-track">
        <view class="progress-fill" :class="currentLevel" :style="{ width: progressPercent + '%' }"></view>
      </view>
      <view class="progress-motivation">{{ progressMotivation }}</view>
    </view>

    <DevLevelSwitcher />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getMemberBenefits, getMemberInfo, receiveMonthlyBenefit } from '@/api/member'
import { MEMBER_LEVELS, MEMBER_LEVEL_THRESHOLDS, getMemberLevelName } from '@/constants/member'
import LevelBadge from '@/components/member/LevelBadge.vue'
import DevLevelSwitcher from '@/components/dev/DevLevelSwitcher.vue'

const userStore = useUserStore()

const levels = MEMBER_LEVELS
const currentLevel = ref(userStore.userLevel || 'basic')
const selectedLevel = ref(currentLevel.value)
const currentExp = ref(Number(userStore.memberInfo?.expTotal) || 0)
const levelThresholds = MEMBER_LEVEL_THRESHOLDS

const benefitLoading = ref(false)
const benefitError = ref('')
const claimingBenefit = ref(false)
const monthlyBenefitStatus = ref({
  claimed: false, canClaim: false, benefitName: '', currentLevel: null, claimedLevel: null
})

const getLevelName = getMemberLevelName

// 每级卡面主题（对齐 web 端 physical-card 配色；accent 为卡面品牌徽章色，按明暗调整对比）
const LEVEL_THEMES = {
  basic:   { card: 'linear-gradient(135deg,#A1887F,#8D6E63)', text: '#3E2723', accent: '#3E2723' },
  silver:  { card: 'linear-gradient(135deg,#E0E0E0,#BDBDBD 45%,#FFFFFF 55%,#9E9E9E)', text: '#424242', accent: '#FFFFFF' },
  gold:    { card: 'linear-gradient(135deg,#FFECB3,#FFC107 45%,#FF8F00)', text: '#795548', accent: '#795548' },
  diamond: { card: 'linear-gradient(135deg,#01579B,#0288D1 50%,#29B6F6)', text: '#01579B', accent: '#FFFFFF' },
  black:   { card: 'linear-gradient(135deg,#1a1a1a,#000)', text: '#FFD700', accent: '#FFD700' }
}
const theme = computed(() => LEVEL_THEMES[selectedLevel.value])

// 进度
const nextThreshold = computed(() => {
  const idx = levels.indexOf(currentLevel.value)
  return idx < levels.length - 1 ? levelThresholds[levels[idx + 1]] : null
})
const nextLevelName = computed(() => {
  const idx = levels.indexOf(currentLevel.value)
  return idx < levels.length - 1 ? getLevelName(levels[idx + 1]) : ''
})
const progressPercent = computed(() => {
  const idx = levels.indexOf(currentLevel.value)
  if (idx >= levels.length - 1) return 100
  const cur = levelThresholds[currentLevel.value]
  const next = levelThresholds[levels[idx + 1]]
  return Math.max(0, Math.min(100, ((currentExp.value - cur) / (next - cur)) * 100))
})
const progressMotivation = computed(() => {
  if (nextThreshold.value == null) return '已到达最高等级，尊享全部会员权益'
  return '再积 ' + (nextThreshold.value - currentExp.value) + ' EXP 升级至 ' + nextLevelName.value
})
const levelEmoji = computed(() => ({ basic: '☕', silver: '🥈', gold: '🏆', diamond: '💎', black: '👑' })[currentLevel.value] || '☕')

// 各等级权益
const benefitsData = {
  basic: [
    { code: '积', name: '消费积分', desc: '1元=1积分' },
    { code: '签', name: '每日签到', desc: '每日+2积分，连签7天送满35减10券' },
    { code: '包', name: '月度权益', desc: '每月可领取等级礼包' }
  ],
  silver: [
    { code: '积', name: '消费积分', desc: '1元=1.1积分' },
    { code: '兑', name: '积分兑换', desc: '积分商城9.8折' },
    { code: '包', name: '月度权益', desc: '每月可领取白银等级礼包' }
  ],
  gold: [
    { code: '积', name: '消费积分', desc: '1元=1.2积分' },
    { code: '兑', name: '积分兑换', desc: '积分商城9.5折' },
    { code: '包', name: '月度权益', desc: '每月可领取黄金等级礼包' }
  ],
  diamond: [
    { code: '积', name: '消费积分', desc: '1元=1.3积分' },
    { code: '兑', name: '积分兑换', desc: '积分商城9折' },
    { code: '包', name: '月度权益', desc: '每月可领取钻石等级礼包' }
  ],
  black: [
    { code: '速', name: '积分加速包', desc: '每月前300元消费1.7倍积分' },
    { code: '积', name: '消费积分', desc: '1元=1.35积分' },
    { code: '兑', name: '积分兑换', desc: '积分商城8.5折' },
    { code: '包', name: '月度权益包', desc: '按月领取专属会员礼遇' },
    { code: '新', name: '新品优先', desc: '新品试饮与优先购' }
  ]
}
const currentBenefits = computed(() => benefitsData[selectedLevel.value] || [])

const canReceiveMonthlyBenefit = computed(() => (
  monthlyBenefitStatus.value.canClaim &&
  !monthlyBenefitStatus.value.claimed &&
  !claimingBenefit.value
))
const benefitActionText = computed(() => {
  if (claimingBenefit.value) return '领取中…'
  if (monthlyBenefitStatus.value.claimed) return '本月权益已领取'
  if (!monthlyBenefitStatus.value.canClaim) return '本月暂无可领权益'
  return '领取月度权益礼包'
})
const shouldShowUpgradeTip = computed(() => {
  if (!monthlyBenefitStatus.value.claimed) return false
  const currentIndex = levels.indexOf(monthlyBenefitStatus.value.currentLevel)
  const claimedIndex = levels.indexOf(monthlyBenefitStatus.value.claimedLevel)
  return currentIndex > claimedIndex && claimedIndex >= 0
})

async function loadBenefitsPage() {
  benefitLoading.value = true
  benefitError.value = ''
  try {
    const memberResponse = await getMemberInfo()
    if (memberResponse.code === 200 && memberResponse.data) {
      userStore.setMemberInfo(memberResponse.data)
      currentLevel.value = memberResponse.data.memberLevel || 'basic'
      selectedLevel.value = currentLevel.value
      currentExp.value = Number(memberResponse.data.expTotal) || 0
    }
  } catch (error) {
    console.warn('会员信息加载失败', error)
  }
  try {
    const benefitResponse = await getMemberBenefits()
    monthlyBenefitStatus.value = { ...monthlyBenefitStatus.value, ...(benefitResponse.data || {}) }
  } catch (error) {
    benefitError.value = error?.message || '本月权益加载失败'
  } finally {
    benefitLoading.value = false
  }
}

async function handleReceiveMonthlyBenefit() {
  if (!canReceiveMonthlyBenefit.value) return
  claimingBenefit.value = true
  try {
    await receiveMonthlyBenefit()
    uni.showToast({ title: '权益已发放至券包', icon: 'success' })
    const response = await getMemberBenefits()
    monthlyBenefitStatus.value = { ...monthlyBenefitStatus.value, ...(response.data || {}) }
  } catch (error) {
    uni.showToast({ title: error?.message || '领取失败，请稍后重试', icon: 'none' })
  } finally {
    claimingBenefit.value = false
  }
}

onShow(loadBenefitsPage)
</script>

<style lang="scss" scoped>
.benefits-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding: 24rpx 24rpx 60rpx;
}

/* ── 等级 tabs ── */
.level-tabs {
  display: flex;
  gap: 16rpx;
  overflow-x: auto;
  white-space: nowrap;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid $border-color;
}
.level-tab {
  flex-shrink: 0;
  padding: 12rpx 28rpx;
  border-radius: 999rpx;
  background: $bg-white;
  border: 1rpx solid $border-color;
  font-size: $font-size-sm;
  color: $text-secondary;
  transition: all .25s;

  &.active {
    color: #fff;
    font-weight: 600;
  }
  &.active.basic { background: #8D6E63; border-color: #8D6E63; }
  &.active.silver { background: #90A4AE; border-color: #90A4AE; }
  &.active.gold { background: #FF8F00; border-color: #FF8F00; }
  &.active.diamond { background: #1565C0; border-color: #1565C0; }
  &.active.black { background: #171411; border-color: #171411; color: #E6C97A; }
}

/* ── feature 卡 ── */
.feature-card {
  margin-top: 24rpx;
  padding: 36rpx 32rpx 40rpx;
  border-radius: 24rpx;
  background: $bg-white;
  box-shadow: 0 8rpx 30rpx rgba(0,0,0,.06);

  &.is-black {
    background: #0C0C0C;
    box-shadow: 0 16rpx 40rpx rgba(0,0,0,.4);
  }
}

/* 3D 卡面 */
.card-visual {
  display: flex;
  justify-content: center;
  padding: 8rpx 0 34rpx;
}
.physical-card {
  position: relative;
  width: 560rpx;
  height: 340rpx;
  border-radius: 24rpx;
  transform: perspective(1400rpx) rotateY(-14deg) rotateX(6deg);
  box-shadow: -16rpx 20rpx 40rpx rgba(0,0,0,.28), inset 0 0 30rpx rgba(0,0,0,.2);
  transition: transform .5s ease;

  .card-face {
    position: absolute;
    inset: 0;
    padding: 28rpx;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    border-radius: inherit;
    background-image: linear-gradient(45deg, rgba(255,255,255,.06) 25%, transparent 25%, transparent 50%, rgba(255,255,255,.06) 50%, rgba(255,255,255,.06) 75%, transparent 75%, transparent);
    background-size: 30rpx 30rpx;
  }
  .card-brand { display: flex; }
  .card-logo {
    align-self: flex-end;
    font-family: serif;
    font-weight: 900;
    letter-spacing: 2rpx;
    font-size: 26rpx;
    color: #fff;
    text-shadow: 0 2rpx 4rpx rgba(0,0,0,.4);
  }
  .card-number {
    font-family: monospace;
    letter-spacing: 4rpx;
    font-size: 30rpx;
    color: #fff;
    opacity: .92;
    text-shadow: 0 2rpx 4rpx #000;
  }
  .card-member {
    font-size: 18rpx;
    letter-spacing: 2rpx;
    color: rgba(255,255,255,.55);
  }

  &.basic { background: linear-gradient(135deg,#A1887F,#8D6E63); }
  &.basic .card-logo, &.basic .card-number { color: #3E2723; text-shadow: 0 2rpx 0 rgba(255,255,255,.2); }
  &.basic .card-member { color: rgba(62,39,35,.6); }

  &.silver { background: linear-gradient(135deg,#E0E0E0,#BDBDBD 45%,#FFFFFF 55%,#9E9E9E); }
  &.silver .card-logo, &.silver .card-number { color: #424242; text-shadow: 0 2rpx 2rpx rgba(255,255,255,.8); }
  &.silver .card-member { color: #616161; }

  &.gold { background: linear-gradient(135deg,#FFECB3,#FFC107 45%,#FF8F00); box-shadow: inset 0 0 30rpx rgba(255,215,0,.3); }
  &.gold .card-logo, &.gold .card-number { color: #795548; text-shadow: 0 2rpx 2rpx rgba(255,255,255,.5); }
  &.gold .card-member { color: #8D6E63; }

  &.diamond { background: linear-gradient(135deg,#01579B,#0288D1 50%,#29B6F6); border: 1rpx solid rgba(255,255,255,.5); }
  &.diamond .card-logo, &.diamond .card-number { color: #E1F5FE; text-shadow: 0 0 16rpx rgba(255,255,255,.5); }
  &.diamond .card-member { color: #81D4FA; }

  &.black { background: linear-gradient(135deg,#1a1a1a,#000); border: 1rpx solid rgba(255,215,0,.3); }
  &.black .card-logo, &.black .card-number { color: #FFD700; }
  &.black .card-member { color: rgba(255,215,0,.5); }
}

/* 权益列表 */
.benefit-list {
  padding: 0 8rpx;
}
.benefit-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $border-color;

  &:last-child { border-bottom: none; }

  .b-icon {
    flex-shrink: 0;
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 16rpx;
    background: $cozy-surface;
    color: $cozy-primary;
    font-size: 22rpx;
    font-weight: 750;
  }
  .b-copy { flex: 1; min-width: 0; }
  .b-name {
    display: block;
    font-size: $font-size-md;
    font-weight: 600;
    color: $text-primary;
  }
  .b-desc {
    display: block;
    margin-top: 4rpx;
    font-size: $font-size-sm;
    color: $text-secondary;
  }
}

/* 黑金卡权益文字 */
.feature-card.is-black .benefit-row .b-icon { background: rgba(255,215,0,.14); color: #FFD700; }
.feature-card.is-black .benefit-row .b-name { color: #F7E7CE; }
.feature-card.is-black .benefit-row .b-desc { color: rgba(247,231,206,.55); }
.feature-card.is-black .benefit-row { border-bottom: 1rpx solid rgba(255,255,255,.08); }

/* 领取按钮 */
.benefit-action { margin-top: 8rpx; }
.receive-btn {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16rpx;
  font-size: $font-size-md;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg,#A1887F,#8D6E63);
  box-shadow: 0 6rpx 20rpx rgba(141,110,99,.3);

  &.silver { background: linear-gradient(135deg,#CFD8DC,#90A4AE); }
  &.gold { background: linear-gradient(135deg,#FFD54F,#FF8F00); }
  &.diamond { background: linear-gradient(135deg,#64B5F6,#1565C0); }
  &.black { background: linear-gradient(135deg,#424242,#212121); color: #FFD700; }
  &.disabled { background: $bg-gray; color: $text-placeholder; box-shadow: none; }
}
.upgrade-tip {
  display: block;
  margin-top: 16rpx;
  text-align: center;
  font-size: $font-size-xs;
  color: $cozy-accent;
}
.inline-state {
  padding: 28rpx 12rpx;
  color: $text-secondary;
  text-align: center;
  font-size: $font-size-sm;
  &.error { color: $error-color; }
}

/* ── 会员进度 footer ── */
.member-progress-footer {
  margin-top: 24rpx;
  padding: 32rpx;
  border-radius: 24rpx;
  background: $bg-white;
}
.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.current-status { display: flex; align-items: center; gap: 12rpx; }
.status-icon { font-size: 40rpx; }
.status-text { font-size: $font-size-md; font-weight: 800; color: $text-primary; }
.progress-numbers { display: flex; align-items: baseline; gap: 6rpx; }
.current-exp { font-size: 44rpx; font-weight: 800; color: #222; }
.total-exp { font-size: 22rpx; color: $text-placeholder; }

.progress-track {
  height: 14rpx;
  border-radius: 999rpx;
  background: $bg-gray;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: inherit;
  transition: width .6s ease;
}
.progress-fill.basic { background: linear-gradient(90deg,#D7CCC8,#A1887F); }
.progress-fill.silver { background: linear-gradient(90deg,#ECEFF1,#B0BEC5); }
.progress-fill.gold { background: linear-gradient(90deg,#FFF176,#FFB300); }
.progress-fill.diamond { background: linear-gradient(90deg,#64B5F6,#1976D2); }
.progress-fill.black { background: linear-gradient(90deg,#757575,#212121); }

.progress-motivation {
  margin-top: 16rpx;
  text-align: center;
  font-size: $font-size-xs;
  color: $text-placeholder;
}
</style>
