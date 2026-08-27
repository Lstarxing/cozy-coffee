<!--
  会员权益页 - 我的会员价值面板
  结构：分类栏(FilterTabs) → 页头身份 → Tab(我的权益 | 全部等级)
    我的权益：一张卡五条(消费积分/兑换折扣/每月权益/生日/会员日) + 升级预告(进度+激励+权益差异)
    全部等级：等级轨道(柔光渐变卡) + 选中等级详情
  数据：/member/benefits/overview 单一数据源（currentLevel + benefits + upgradePreview + allLevels）
-->
<template>
  <view class="benefits-page">
    <!-- 分类栏：紧贴原生导航之下 -->
    <FilterTabs :options="tabs" v-model="tab" />

    <view class="page-content">
    <!-- 页头：身份行（分类栏下方，两个 tab 均显示） -->
    <view class="page-head">
      <view class="head-identity">
        <LevelBadge :level="currentLevel" :size="44" />
        <view class="head-level">
          <text class="head-level-name">{{ currentName }}</text>
          <text class="head-level-en">· {{ currentLevel.toUpperCase() }}</text>
        </view>
      </view>
      <text v-if="!isMax" class="head-exp">{{ formatExp(exp) }} EXP</text>
    </view>

    <!-- ══════ 我的权益 ══════ -->
    <view v-if="tab === 'mine'">
      <!-- 我的权益卡：一张卡 5 条 -->
      <view class="benefit-card">
        <view class="card-head">
          <text class="card-title">我的权益</text>
          <text class="card-sub">{{ currentName }}专属</text>
        </view>
        <view v-for="b in benefits" :key="b.type" class="benefit-row">
          <view class="b-icon"><CozyIcon :name="benefitIcon(b.type)" :size="20" color="#753A22" /></view>
          <view class="b-copy">
            <text class="b-name">{{ b.title }}</text>
            <text class="b-value">{{ b.value }}</text>
            <text class="b-desc">{{ b.description }}</text>
          </view>
          <MiniButton
            v-if="b.action === 'mall'"
            @click="goMall"
          >去商城<view class="b-arrow">›</view></MiniButton>
          <MiniButton
            v-else-if="b.action === 'claim'"
            :variant="b.canClaim ? '' : 'muted'"
            @click="handleClaim"
          >{{ b.canClaim ? '领取' : '暂无可领' }}</MiniButton>
          <MiniButton
            v-else-if="b.type === 'MONTHLY_REWARD'"
            variant="muted"
          >已领取 ✓</MiniButton>
        </view>
      </view>

      <!-- 升级预告 -->
      <view v-if="!isMax" class="upgrade-card">
        <view>
          <text class="up-title">距{{ nextLevelName }}还差 <text class="up-em">{{ formatExp(remaining) }}</text> EXP</text>
        </view>
        <view class="up-track">
          <view class="up-fill" :style="{ width: percentage + '%', background: progressColor }"></view>
        </view>
        <view class="up-benefit">
          <text class="up-benefit-title">升级后</text>
          <text v-for="(nb, i) in newBenefits" :key="i" class="up-benefit-item">· {{ nb }}</text>
        </view>
        <view class="up-link" @click="tab = 'all'">查看全部等级 <view class="b-arrow">›</view></view>
      </view>
      <view v-else class="upgrade-card max">
        <text class="up-title">已尊享全部等级权益</text>
        <text class="up-desc">解锁黑金全部专属礼遇</text>
      </view>
    </view>

    <!-- ══════ 全部等级 ══════ -->
    <view v-else>
      <!-- 等级轨道（与签到豆轨同设计语言：连线填充到当前等级，无背景卡） -->
      <view class="level-track">
        <view class="track-line-base">
          <view class="track-line-fill" :style="{ width: trackFillWidth + '%', background: trackFillColor }"></view>
        </view>
        <view class="track-nodes">
          <view
            v-for="lv in levelOrder"
            :key="lv"
            class="track-node"
            :class="{ current: selLevel === lv, reached: isReached(lv) }"
            @click="selLevel = lv"
          >
            <view
              class="node-dot"
              :style="{ background: levelColor(lv), boxShadow: selLevel === lv ? '0 0 0 6rpx ' + hexToRgba(levelColor(lv), 0.25) : '' }"
            ></view>
            <text class="node-name">{{ getLevelName(lv) }}</text>
            <text class="node-exp">{{ formatExp(thresholdOf(lv)) }} EXP</text>
          </view>
        </view>
      </view>
      <view class="level-detail">
        <view class="ld-head">
          <text class="ld-name" :style="{ color: levelColor(selLevel) }">{{ getLevelName(selLevel) }}</text>
          <text v-if="thresholdOf(selLevel) > 0" class="ld-threshold">满 {{ formatExp(thresholdOf(selLevel)) }} EXP 晋升</text>
          <text v-else class="ld-threshold">注册即达</text>
        </view>
        <view v-for="row in levelBenefitRows" :key="row.label" class="ld-row">
          <text class="ld-label">{{ row.label }}</text>
          <text class="ld-value">{{ row.value }}</text>
        </view>
      </view>
    </view>

    <DevLevelSwitcher />
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getMemberOverview, receiveMonthlyBenefit } from '@/api/member'
import { MEMBER_LEVELS, MEMBER_LEVEL_THRESHOLDS, getMemberLevelName } from '@/constants/member'
import LevelBadge from '@/components/member/LevelBadge.vue'
import CozyIcon from '@/components/CozyIcon.vue'
import FilterTabs from '@/components/common/FilterTabs.vue'
import MiniButton from '@/components/common/MiniButton.vue'
import DevLevelSwitcher from '@/components/dev/DevLevelSwitcher.vue'

const userStore = useUserStore()
const getLevelName = getMemberLevelName
const levelOrder = MEMBER_LEVELS

const tab = ref('mine')
const tabs = [
  { value: 'mine', label: '我的权益' },
  { value: 'all', label: '全部等级' }
]
const selLevel = ref(userStore.userLevel || 'basic')

// 面板数据（/member/benefits/overview 单一数据源）
const overview = ref(null)
const currentLevel = ref(userStore.userLevel || 'basic')
const currentName = computed(() => getLevelName(currentLevel.value))
const exp = computed(() => overview.value?.currentLevel?.exp ?? 0)
const isMax = computed(() => !!overview.value?.upgradePreview?.isMax)
const nextLevelName = computed(() => overview.value?.upgradePreview?.nextLevelName || '')
const remaining = computed(() => overview.value?.upgradePreview?.remainingExp ?? 0)
const percentage = computed(() => overview.value?.upgradePreview?.percentage ?? 0)
const newBenefits = computed(() => overview.value?.upgradePreview?.newBenefits || [])
const benefits = computed(() => overview.value?.benefits || [])
const allLevels = computed(() => overview.value?.allLevels || [])

// 升级进度条按等级配色（对齐 web 端 .progress-fill.basic/silver/...）
const PROGRESS_COLORS = {
  basic: 'linear-gradient(90deg, #D7CCC8, #A1887F)',
  silver: 'linear-gradient(90deg, #ECEFF1, #B0BEC5)',
  gold: 'linear-gradient(90deg, #FFF176, #FFB300)',
  diamond: 'linear-gradient(90deg, #64B5F6, #1976D2)',
  black: 'linear-gradient(90deg, #757575, #212121)'
}
const progressColor = computed(() => PROGRESS_COLORS[currentLevel.value] || PROGRESS_COLORS.basic)

// 月权益已领取（后端 claimed 时该条 action 为空）
const monthlyClaimed = computed(() => {
  const mb = benefits.value.find(b => b.type === 'MONTHLY_REWARD')
  return !!mb && mb.action !== 'claim'
})

function benefitIcon(type) {
  return {
    POINT_MULTIPLIER: 'coffee',
    REDEEM_DISCOUNT: 'coupon',
    MONTHLY_REWARD: 'gift',
    BIRTHDAY: 'star',
    COZY_DAY: 'sun'
  }[type] || 'gift'
}

// 各等级代表色（对齐 web 端 .progress-fill.* 渐变深端）
const LEVEL_COLORS = {
  basic: '#A1887F',
  silver: '#B0BEC5',
  gold: '#FFB300',
  diamond: '#1976D2',
  black: '#212121'
}
function levelColor(lv) { return LEVEL_COLORS[lv] || LEVEL_COLORS.basic }
function hexToRgba(hex, alpha) {
  const n = parseInt(hex.slice(1), 16)
  return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${alpha})`
}

// 等级轨道：连线填充到当前等级节点中心
const currentIdx = computed(() => {
  const i = levelOrder.indexOf(currentLevel.value)
  return i < 0 ? 0 : i
})
const trackFillWidth = computed(() => {
  if (levelOrder.length <= 1) return 0
  return (currentIdx.value / (levelOrder.length - 1)) * 100
})
const trackFillColor = computed(() => levelColor(currentLevel.value))
function isReached(lv) {
  return levelOrder.indexOf(lv) <= currentIdx.value
}

// 轨道卡背景：当前等级色柔光径向渐变（柔边虚化观感，不用毛玻璃，符合 Warm Reserve 规范）
function formatExp(value) { return Number(value || 0).toLocaleString() }
function formatRate(n) {
  const v = Number(n) || 1
  return (Number.isInteger(v) ? v : v.toFixed(1)) + '×'
}
function formatDiscount(discount, level) {
  if (level === 'basic') return '原价兑换'
  return (Number(discount) * 10).toFixed(1) + ' 折'
}

function thresholdOf(level) {
  const item = allLevels.value.find(x => x.level === level)
  return item && item.threshold != null ? Number(item.threshold) : (MEMBER_LEVEL_THRESHOLDS[level] || 0)
}

// 全部等级页：选中等级的行式明细（数据来自后端 allLevels）
const levelBenefitRows = computed(() => {
  const item = allLevels.value.find(x => x.level === selLevel.value)
  if (!item) return []
  const rows = [
    { label: '消费积分', value: formatRate(item.pointsRate) },
    { label: '兑换折扣', value: formatDiscount(item.redeemDiscount, item.level) }
  ]
  if (item.monthlyBenefit) rows.push({ label: '每月权益', value: item.monthlyBenefit })
  if (item.birthdayBenefit) rows.push({ label: '生日礼遇', value: item.birthdayBenefit })
  rows.push({ label: '会员日', value: formatRate((Number(item.pointsRate) || 1) + 0.5) })
  return rows
})

function goMall() {
  uni.navigateTo({ url: '/pages/mall/index' })
}

async function loadOverview() {
  try {
    const res = await getMemberOverview()
    if (res.code === 200 && res.data) {
      overview.value = res.data
      currentLevel.value = res.data.currentLevel?.id || userStore.userLevel || 'basic'
      selLevel.value = currentLevel.value
      userStore.setMemberInfo({ ...userStore.memberInfo, memberLevel: currentLevel.value, expTotal: res.data.currentLevel?.exp })
    }
  } catch (e) {
    console.warn('会员权益面板加载失败', e)
    uni.showToast({ title: '权益信息加载失败', icon: 'none' })
  }
}

async function handleClaim() {
  if (monthlyClaimed.value) return
  try {
    await receiveMonthlyBenefit()
    uni.showToast({ title: '权益已发放至券包', icon: 'success' })
    await loadOverview()
  } catch (error) {
    uni.showToast({ title: error?.message || '领取失败，请稍后重试', icon: 'none' })
  }
}

onShow(loadOverview)
</script>

<style lang="scss" scoped>
.benefits-page {
  min-height: 100vh;
  background: $cozy-surface;
}
.page-content {
  padding: 0 44rpx 80rpx;
}

/* ── 页头（安静身份行，上下留白一致） ── */
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 4rpx;
}
.head-identity { display: flex; align-items: center; gap: 20rpx; }
.head-level { display: flex; align-items: baseline; gap: 12rpx; }
.head-level-name { font-size: 30rpx; font-weight: 650; color: $cozy-ink; }
.head-level-en { font-size: 20rpx; font-weight: 700; letter-spacing: .12em; color: $cozy-muted; }
.head-exp { font-size: 24rpx; color: $cozy-muted; }

/* ── 我的权益卡（一张卡 5 条） ── */
.benefit-card {
  padding: 40rpx 44rpx;
  border-radius: 28rpx;
  background: $bg-white;
}
.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 8rpx;
}
.card-title { font-size: 20rpx; font-weight: 700; letter-spacing: .18em; color: $cozy-muted; }
.card-sub { font-size: 20rpx; color: $cozy-muted; }

.benefit-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 30rpx 0;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
}
.b-icon {
  flex: none;
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: $cozy-primary-soft;
}
.b-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4rpx; }
.b-name { font-size: 28rpx; font-weight: 600; color: $cozy-ink; }
.b-value { font-size: 24rpx; color: $cozy-primary; font-weight: 650; }
.b-desc { font-size: 20rpx; color: $cozy-placeholder; }
.b-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 4rpx;
  font-size: 26rpx;
  line-height: 1;
}

/* ── 升级预告卡（浅底，进度条按等级配色） ── */
.upgrade-card {
  margin-top: 28rpx;
  padding: 40rpx 44rpx;
  border-radius: 28rpx;
  background: $bg-white;

  &.max { display: flex; flex-direction: column; gap: 8rpx; }
}
.up-title { font-size: 28rpx; font-weight: 650; color: $cozy-ink; }
.up-em { color: $cozy-primary; font-weight: 700; }
.up-track {
  margin-top: 28rpx;
  height: 8rpx;
  border-radius: 4rpx;
  background: $cozy-surface;
  overflow: hidden;
}
.up-fill { height: 100%; border-radius: 4rpx; transition: width .6s $cozy-ease-out; }
.up-benefit {
  margin-top: 28rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}
.up-benefit-title {
  font-size: 20rpx;
  letter-spacing: .14em;
  color: $cozy-muted;
}
.up-benefit-item { font-size: 24rpx; color: $cozy-ink; line-height: 1.6; }
.up-desc { font-size: 24rpx; color: $cozy-muted; }
.up-link {
  margin-top: 28rpx;
  font-size: 22rpx;
  color: $cozy-primary;
  display: flex;
  align-items: center;
}

/* ── 全部等级：等级轨道（与签到豆轨同设计语言，无背景卡） ── */
.level-track { position: relative; padding: 24rpx 0 8rpx; }
.track-line-base {
  position: absolute;
  /* 节点中心 = padding-top 24rpx + 普通节点 20rpx/2 = 34rpx（当前节点 32rpx 有光环，线仍在光环内） */
  top: 34rpx; left: calc(100% / 10); right: calc(100% / 10);
  height: 4rpx;
  border-radius: 2rpx;
  background: $cozy-border;
  transform: translateY(-50%);
  z-index: 1;
}
.track-line-fill {
  height: 100%;
  border-radius: 2rpx;
  transition: width .5s $cozy-ease-out;
}
.track-nodes { display: flex; position: relative; z-index: 2; }
.track-node {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.node-dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.1);
  transition: all .3s $cozy-ease-out;
}
/* 未达节点淡化（未来） */
.track-node:not(.reached) .node-dot { opacity: .35; }
/* 当前节点：等级色圆 + 白色内芯（准星感），光环由 JS 内联 boxShadow */
.track-node.current .node-dot {
  width: 32rpx;
  height: 32rpx;
  position: relative;
}
.track-node.current .node-dot::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #fff;
}
.node-name {
  font-size: 20rpx;
  color: $cozy-placeholder;
  font-weight: 600;
}
.track-node.reached .node-name { color: $cozy-ink; }
.node-exp { font-size: 22rpx; color: $cozy-muted; }
.level-detail {
  margin-top: 24rpx;
  padding: 40rpx 44rpx;
  border-radius: 28rpx;
  background: $bg-white;
}
.ld-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid $cozy-border;
}
.ld-name { font-family: $font-display; font-size: 34rpx; font-weight: 600; color: $cozy-ink; }
.ld-threshold { font-size: 20rpx; color: $cozy-placeholder; }
.ld-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20rpx;
  padding: 26rpx 0;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; padding-bottom: 0; }
}
.ld-label { font-size: 26rpx; color: $cozy-ink; }
.ld-value { flex: 1; min-width: 0; text-align: right; font-size: 24rpx; color: $cozy-muted; line-height: 1.5; }
</style>
