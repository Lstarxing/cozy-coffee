<!--
  会员权益页 - 复现 prototype/benefits.html 极简 Editorial
  页头（安静身份行）→ 本月可用（行式）→ 权益对比（每级一行）→ 升级规则 → 会员进度（web 端进度条置底）
-->
<template>
  <view class="benefits-page">
    <!-- 页头：安静身份行 -->
    <view class="page-head">
      <view class="head-identity">
        <LevelBadge :level="currentLevel" :color="levelAccent" :size="44" />
        <text class="head-title">会员权益</text>
      </view>
      <text class="head-sub">{{ getLevelName(currentLevel) }} <em>· {{ currentLevel.toUpperCase() }}</em></text>
    </view>

    <!-- 本月可用 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">本月可用</text>
        <text class="section-sub">{{ getLevelName(currentLevel) }}专享</text>
      </view>

      <view class="benefit-row">
        <text class="b-icon">券</text>
        <view class="b-copy">
          <text class="b-name">月度兑换券</text>
          <text class="b-desc">{{ monthlyBenefitText }}</text>
        </view>
        <view
          class="b-btn"
          :class="{ muted: benefitDone }"
          @click="handleReceiveMonthlyBenefit"
        >{{ benefitActionText }}</view>
      </view>

      <view class="benefit-row">
        <text class="b-icon">礼</text>
        <view class="b-copy">
          <text class="b-name">生日礼遇</text>
          <text class="b-desc">生日当月赠送双倍积分</text>
        </view>
        <view class="b-btn muted" @click="birthdayTip">去领取</view>
      </view>
    </view>

    <!-- 权益对比（每级一行） -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">会员权益对比</text>
      </view>
      <view class="compare-list">
        <view
          v-for="(c, i) in compare"
          :key="c.level"
          class="compare-row"
          :class="{ current: c.level === currentLevel }"
        >
          <text class="compare-name">{{ c.name }}</text>
          <text class="compare-text">{{ c.text }}</text>
        </view>
      </view>
    </view>

    <!-- 升级规则 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">升级规则</text>
      </view>
      <view class="rule-table">
        <view class="rule-row rule-head">
          <text class="rule-cell">会员等级</text>
          <text class="rule-cell">升级门槛</text>
        </view>
        <view
          v-for="row in ruleRows"
          :key="row.level"
          class="rule-row"
          :class="{ current: row.level === currentLevel }"
        >
          <text class="rule-cell">{{ row.name }}</text>
          <text class="rule-cell">{{ row.threshold }} EXP</text>
        </view>
      </view>
      <view class="rule-note">消费 1 元 = 1 EXP，达到门槛自动升级，升级后权益永久生效。</view>
    </view>

    <!-- 会员进度（web 端进度条复用，置底） -->
    <view class="member-progress">
      <view class="progress-top">
        <text class="progress-level">{{ getLevelName(currentLevel) }} <em>{{ currentLevel.toUpperCase() }}</em></text>
        <text class="progress-exp">{{ formatExp(currentExp) }} <i>/ {{ nextThreshold != null ? formatExp(nextThreshold) : '—' }} EXP</i></text>
      </view>
      <view class="progress-track">
        <view class="progress-fill" :style="{ width: progressPercent + '%', background: levelAccent }"></view>
      </view>
      <view class="progress-motivation">{{ progressMotivation }}</view>
    </view>

    <DevLevelSwitcher />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getMemberBenefits, getMemberInfo, receiveMonthlyBenefit } from '@/api/member'
import { MEMBER_LEVELS, MEMBER_LEVEL_THRESHOLDS, getMemberLevelName, MEMBER_LEVEL_THEMES } from '@/constants/member'
import LevelBadge from '@/components/member/LevelBadge.vue'
import DevLevelSwitcher from '@/components/dev/DevLevelSwitcher.vue'

const userStore = useUserStore()
const getLevelName = getMemberLevelName
const levels = MEMBER_LEVELS
const levelThresholds = MEMBER_LEVEL_THRESHOLDS

const currentLevel = ref(userStore.userLevel || 'basic')
const currentExp = ref(Number(userStore.memberInfo?.expTotal) || 0)
const benefitLoading = ref(false)
const benefitError = ref('')
const claimingBenefit = ref(false)
const monthlyBenefitStatus = ref({
  claimed: false, canClaim: false, benefitName: '', currentLevel: null, claimedLevel: null
})

const levelAccent = computed(() => (MEMBER_LEVEL_THEMES[currentLevel.value] || {}).accent || '#753A22')

// 各等级权益对比（对齐原型 / web 端）
const compare = [
  { level: 'basic', name: '基础会员', text: '1× 积分' },
  { level: 'silver', name: '白银会员', text: '1.2× 积分 · 95 折兑换' },
  { level: 'gold', name: '黄金会员', text: '1.5× 积分 · 9 折兑换' },
  { level: 'diamond', name: '钻石会员', text: '2× 积分 · 85 折兑换' },
  { level: 'black', name: '黑金会员', text: '3× 积分 · 8 折兑换' }
]

// 升级门槛表格
const ruleRows = levels.map(level => ({
  level,
  name: getLevelName(level),
  threshold: formatExp(levelThresholds[level])
}))

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
  return '再积 ' + formatExp(nextThreshold.value - currentExp.value) + ' EXP 升级至' + nextLevelName.value
})

const benefitDone = computed(() => monthlyBenefitStatus.value.claimed)
const monthlyBenefitText = computed(() => {
  if (monthlyBenefitStatus.value.claimed) return '本月兑换券已领取，可在券包查看'
  return '95 折兑换券 · 本月有效'
})
const benefitActionText = computed(() => {
  if (benefitLoading.value) return '查询中…'
  if (claimingBenefit.value) return '领取中…'
  if (monthlyBenefitStatus.value.claimed) return '已领取 ✓'
  if (!monthlyBenefitStatus.value.canClaim) return '暂无可领'
  return '去领取'
})

function formatExp(value) { return Number(value || 0).toLocaleString() }

async function loadBenefitsPage() {
  benefitLoading.value = true
  benefitError.value = ''
  try {
    const memberResponse = await getMemberInfo()
    if (memberResponse.code === 200 && memberResponse.data) {
      userStore.setMemberInfo(memberResponse.data)
      currentLevel.value = memberResponse.data.memberLevel || 'basic'
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
  if (benefitDone.value || claimingBenefit.value) return
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

function birthdayTip() {
  uni.showToast({ title: '生日当月自动赠送双倍积分', icon: 'none' })
}

onShow(loadBenefitsPage)
</script>

<style lang="scss" scoped>
.benefits-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding: 40rpx 44rpx 80rpx;
}

/* ── 页头（安静身份行，非卡） ── */
.page-head { padding: 12rpx 4rpx 40rpx; }
.head-identity { display: flex; align-items: center; gap: 20rpx; }
.head-title {
  font-family: $font-display;
  font-size: 48rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.head-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: $cozy-muted;
}
.head-sub em {
  font-style: normal;
  color: $cozy-primary;
  font-weight: 650;
  font-size: 18rpx;
  letter-spacing: .08em;
}

/* ── Editorial section：线性分隔 + 留白 ── */
.section { margin-top: 48rpx; }
.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 4rpx;
}
.section-title {
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: .18em;
  color: $cozy-muted;
}
.section-sub { font-size: 20rpx; color: $cozy-muted; }

/* 本月可用：行式 */
.benefit-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 34rpx 4rpx;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
}
.b-icon {
  flex: none;
  width: 76rpx;
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: $cozy-bg;
  color: $cozy-primary;
  font-family: $font-display;
  font-size: 26rpx;
  font-weight: 700;
}
.b-copy { flex: 1; min-width: 0; }
.b-name {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.b-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $cozy-muted;
  line-height: 1.5;
}
.b-btn {
  flex: none;
  padding: 14rpx 30rpx;
  border-radius: 12rpx;
  border: 1rpx solid $cozy-primary;
  background: transparent;
  color: $cozy-primary;
  font-size: 22rpx;
  font-weight: 600;

  &:active { background: $cozy-bg; }

  &.muted {
    border-color: $cozy-border;
    color: $cozy-muted;
  }
}

/* 权益对比：每级一行 */
.compare-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20rpx;
  padding: 28rpx 4rpx;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
}
.compare-name { font-size: 28rpx; color: $cozy-ink; font-weight: 500; }
.compare-text { font-size: 22rpx; color: $cozy-muted; }
.compare-row.current .compare-name { color: $cozy-primary; font-weight: 700; }
.compare-row.current .compare-text { color: $cozy-primary; font-weight: 600; }
.compare-row.current .compare-name::before {
  content: '';
  display: inline-block;
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: $cozy-primary;
  margin-right: 14rpx;
  vertical-align: 2rpx;
}

/* 升级规则：一行门槛 */
.rule-table {
  margin-top: 20rpx;
  border: 1rpx solid $cozy-border;
  border-radius: 12rpx;
  overflow: hidden;
}
.rule-row {
  display: flex;
  align-items: center;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
  &.current { background: $cozy-bg; }
}
.rule-head {
  background: $cozy-bg;
}
.rule-cell {
  flex: 1;
  padding: 22rpx 28rpx;
  font-size: 24rpx;
  color: $cozy-ink;

  &:last-child { text-align: right; }
}
.rule-head .rule-cell {
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: .06em;
  color: $cozy-muted;
}
.rule-row.current .rule-cell {
  color: $cozy-primary;
  font-weight: 650;
}
.rule-note {
  padding: 16rpx 4rpx 0;
  font-size: 20rpx;
  line-height: 1.7;
  color: $cozy-muted;
}

/* ── 会员进度（web 端进度条复用，适配 Editorial） ── */
.member-progress {
  margin-top: 64rpx;
  padding: 36rpx 4rpx 0;
  border-top: 1rpx solid $cozy-border;
}
.progress-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 20rpx;
  margin-bottom: 26rpx;
}
.progress-level {
  font-family: $font-display;
  font-size: 36rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.progress-level em {
  font-style: normal;
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: .12em;
  color: $cozy-muted;
  margin-left: 12rpx;
}
.progress-exp { font-size: 26rpx; font-weight: 650; color: $cozy-ink; }
.progress-exp i {
  font-style: normal;
  font-size: 20rpx;
  font-weight: 400;
  color: $cozy-muted;
}
.progress-track {
  height: 6rpx;
  border-radius: 4rpx;
  background: $cozy-border;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 4rpx;
  transition: width .6s ease;
}
.progress-motivation {
  margin-top: 16rpx;
  font-size: 20rpx;
  text-align: center;
  color: $cozy-muted;
}
</style>
