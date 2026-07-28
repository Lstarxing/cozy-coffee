<!--
  会员权益页 - 各等级权益详情
-->
<template>
  <view class="benefits-page">
    <!-- 当前等级卡片 -->
    <view class="current-level" :class="[currentLevel, { 'is-dark': isDark }]" :style="themeStyle">
      <view class="level-info">
        <view class="level-badge-wrap"><LevelBadge :level="themeLevel" color="var(--member-accent)" :size="40" /></view>
        <text class="level-name">{{ getLevelName(currentLevel) }}</text>
        <text class="level-desc">{{ getLevelDesc(currentLevel) }}</text>
      </view>
      <view class="level-progress">
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: progressPercent + '%' }"></view>
        </view>
        <text class="progress-text" v-if="currentLevel !== 'black'">
          距离 {{ nextLevelName }} 还需 {{ expToNext }} EXP
        </text>
        <text class="progress-text" v-else>已达到最高等级</text>
      </view>
    </view>

    <!-- 后端真实月度权益状态 -->
    <view class="monthly-benefit-section">
      <view class="section-heading-row">
        <view>
          <text class="section-title compact">本月等级权益</text>
          <text class="section-subtitle">权益内容与领取状态来自会员服务</text>
        </view>
        <text class="month-badge">{{ getLevelName(currentLevel) }}</text>
      </view>
      <view v-if="benefitLoading" class="inline-state">正在查询本月权益…</view>
      <view v-else-if="benefitError" class="inline-state error" @click="loadBenefitsPage">
        {{ benefitError }}，点击重试
      </view>
      <template v-else>
        <text class="benefit-package-name">{{ monthlyBenefitStatus.benefitName || '暂无权益说明' }}</text>
        <view
          class="receive-benefit-btn"
          :class="{ disabled: !canReceiveMonthlyBenefit }"
          @click="handleReceiveMonthlyBenefit"
        >
          {{ benefitActionText }}
        </view>
        <text v-if="shouldShowUpgradeTip" class="upgrade-notice">
          本月已按 {{ getLevelName(monthlyBenefitStatus.claimedLevel) }} 领取，当前等级礼包将在下月生效
        </text>
      </template>
    </view>
    
    <!-- 等级权益列表 -->
    <view class="benefits-section">
      <view class="section-title">会员等级权益</view>
      
      <!-- 等级切换 -->
      <scroll-view scroll-x class="level-tabs">
        <view 
          class="level-tab"
          :class="{ active: selectedLevel === level }"
          v-for="level in levels"
          :key="level"
          @click="selectedLevel = level"
        >
          {{ getLevelName(level) }}
        </view>
      </scroll-view>
      
      <!-- 权益详情 -->
      <view class="benefits-list">
        <view class="benefit-item" v-for="(benefit, index) in currentBenefits" :key="index">
          <text class="benefit-icon">{{ benefit.code }}</text>
          <view class="benefit-info">
            <text class="benefit-name">{{ benefit.name }}</text>
            <text class="benefit-desc">{{ benefit.desc }}</text>
          </view>
          <view class="benefit-status" :class="{ unlocked: isUnlocked(selectedLevel) }">
            {{ isUnlocked(selectedLevel) ? '已解锁' : '未解锁' }}
          </view>
        </view>
      </view>
    </view>

    <!-- 月度挑战任务（当前阶段不展示外卖任务） -->
    <view class="monthly-task-section">
      <view class="section-heading-row">
        <view>
          <text class="section-title compact">月度挑战</text>
          <text class="section-subtitle">{{ monthlyTask.taskMonth || '本月' }} · 完成订单后自动结算积分</text>
        </view>
        <text class="task-summary">{{ completedTaskCount }}/{{ monthlyChallenges.length }} 完成</text>
      </view>
      <view v-if="taskLoading" class="inline-state">正在加载任务进度…</view>
      <view v-else-if="taskError" class="inline-state error" @click="loadBenefitsPage">
        {{ taskError }}，点击重试
      </view>
      <view v-else class="task-list">
        <view v-for="task in monthlyChallenges" :key="task.key" class="task-card">
          <view class="task-topline">
            <view class="task-copy">
              <text class="task-title">{{ task.title }}</text>
              <text class="task-description">{{ task.description }}</text>
            </view>
            <text class="task-reward">+{{ task.reward }} 积分</text>
          </view>
          <view class="task-progress-track">
            <view class="task-progress-fill" :style="{ width: task.progress + '%' }" />
          </view>
          <view class="task-bottomline">
            <text>{{ task.displayCurrent }}/{{ task.target }}</text>
            <text :class="{ completed: task.claimed }">
              {{ task.claimed ? '已完成 · 奖励已发放' : (task.progress >= 100 ? '等待系统结算' : '进行中') }}
            </text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 等级对比表 -->
    <view class="compare-section">
      <view class="section-title">等级对比</view>
      <view class="compare-table">
        <view class="table-header">
          <view class="table-cell">权益</view>
          <view class="table-cell" v-for="level in levels" :key="level">
            {{ getLevelName(level).replace('会员', '') }}
          </view>
        </view>
        <view class="table-row" v-for="(row, index) in compareData" :key="index">
          <view class="table-cell row-name">{{ row.name }}</view>
          <view class="table-cell" v-for="level in levels" :key="level">
            <text v-if="typeof row[level] === 'string'">{{ row[level] }}</text>
            <text class="check" v-else-if="row[level]">✓</text>
            <text class="cross" v-else>-</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 升级指南 -->
    <view class="upgrade-section">
      <view class="section-title">如何升级</view>
      <view class="upgrade-tips">
        <view class="tip-item">
          <text class="tip-icon">EXP</text>
          <text class="tip-text">消费 1 元 = 1 EXP</text>
        </view>
        <view class="tip-item">
          <text class="tip-icon">月</text>
          <text class="tip-text">完成月度挑战可获得额外积分</text>
        </view>
        <view class="tip-item">
          <text class="tip-icon">单</text>
          <text class="tip-text">订单完成后按实付金额累计 EXP</text>
        </view>
      </view>
    </view>
    <DevLevelSwitcher />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getMemberBenefits, getMemberInfo, getMonthlyTask, receiveMonthlyBenefit } from '@/api/member'
import { MEMBER_LEVELS, MEMBER_LEVEL_THRESHOLDS, getMemberLevelName } from '@/constants/member'
import { buildMonthlyChallenges } from '@/domain/member/memberRules'
import { useMemberTheme } from '@/composables/useMemberTheme'
import LevelBadge from '@/components/member/LevelBadge.vue'
import DevLevelSwitcher from '@/components/dev/DevLevelSwitcher.vue'

const userStore = useUserStore()
const { themeStyle, isDark, level: themeLevel } = useMemberTheme()

const levels = MEMBER_LEVELS
const currentLevel = ref(userStore.userLevel || 'basic')
const selectedLevel = ref(currentLevel.value)
const currentExp = ref(Number(userStore.memberInfo?.expTotal) || 0)
const levelThresholds = MEMBER_LEVEL_THRESHOLDS

const benefitLoading = ref(false)
const benefitError = ref('')
const taskLoading = ref(false)
const taskError = ref('')
const claimingBenefit = ref(false)
const monthlyBenefitStatus = ref({
  claimed: false,
  canClaim: false,
  benefitName: '',
  currentLevel: null,
  claimedLevel: null
})
const monthlyTask = ref({})

// 等级名称
const getLevelName = getMemberLevelName

// 等级描述
const getLevelDesc = (level) => {
  const map = {
    basic: '开启你的咖啡之旅',
    silver: '日常消费开始获得更多回馈',
    gold: '积分倍率与饮品礼遇同步提升',
    diamond: '解锁更完整的门店会员权益',
    black: '当前最高等级与月度权益包'
  }
  return map[level]
}

// 下一等级名称
const nextLevelName = computed(() => {
  const idx = levels.indexOf(currentLevel.value)
  if (idx < levels.length - 1) {
    return getLevelName(levels[idx + 1])
  }
  return ''
})

// 升级所需EXP
const expToNext = computed(() => {
  const idx = levels.indexOf(currentLevel.value)
  if (idx < levels.length - 1) {
    return Math.max(0, levelThresholds[levels[idx + 1]] - currentExp.value)
  }
  return 0
})

// 进度百分比
const progressPercent = computed(() => {
  const idx = levels.indexOf(currentLevel.value)
  if (idx >= levels.length - 1) return 100
  
  const curr = levelThresholds[currentLevel.value]
  const next = levelThresholds[levels[idx + 1]]
  return Math.max(0, Math.min(100, ((currentExp.value - curr) / (next - curr)) * 100))
})

const monthlyChallenges = computed(() => buildMonthlyChallenges(monthlyTask.value))
const completedTaskCount = computed(() => monthlyChallenges.value.filter(task => task.claimed).length)
const canReceiveMonthlyBenefit = computed(() => (
  monthlyBenefitStatus.value.canClaim &&
  !monthlyBenefitStatus.value.claimed &&
  !claimingBenefit.value
))
const benefitActionText = computed(() => {
  if (claimingBenefit.value) return '领取中…'
  if (monthlyBenefitStatus.value.claimed) return '本月权益已领取'
  if (!monthlyBenefitStatus.value.canClaim) return '本月暂无可领权益'
  return '领取到券包'
})
const shouldShowUpgradeTip = computed(() => {
  if (!monthlyBenefitStatus.value.claimed) return false
  const currentIndex = levels.indexOf(monthlyBenefitStatus.value.currentLevel)
  const claimedIndex = levels.indexOf(monthlyBenefitStatus.value.claimedLevel)
  return currentIndex > claimedIndex && claimedIndex >= 0
})

// 是否已解锁该等级
const isUnlocked = (level) => {
  return levels.indexOf(currentLevel.value) >= levels.indexOf(level)
}

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

// 当前选中等级的权益
const currentBenefits = computed(() => benefitsData[selectedLevel.value] || [])

// 对比数据
const compareData = [
  { name: '积分倍率', basic: '1.0x', silver: '1.1x', gold: '1.2x', diamond: '1.3x', black: '1.35x' },
  { name: '积分兑换', basic: '原价', silver: '9.8折', gold: '9.5折', diamond: '9折', black: '8.5折' },
  { name: '月度礼包', basic: true, silver: true, gold: true, diamond: true, black: true },
  { name: '黑卡加速包', basic: false, silver: false, gold: false, diamond: false, black: true }
]

async function loadBenefitsPage() {
  benefitLoading.value = true
  taskLoading.value = true
  benefitError.value = ''
  taskError.value = ''

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

  try {
    const taskResponse = await getMonthlyTask()
    monthlyTask.value = taskResponse.data || {}
  } catch (error) {
    taskError.value = error?.message || '月度任务加载失败'
  } finally {
    taskLoading.value = false
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
  padding-bottom: $spacing-xl;
}

// 当前等级卡片
.current-level {
  position: relative;
  overflow: hidden;
  padding: 44rpx 32rpx 38rpx;
  color: var(--member-text, white);
  background: var(--member-surface, #{$cozy-surface-alt});

  .level-info, .level-progress { position: relative; z-index: 1; }
  &.is-dark.black::before {
    content: ''; position: absolute; inset: 0; z-index: 0;
    background-image: radial-gradient(#{$member-black-pattern} 1px, transparent 1px);
    background-size: 18rpx 18rpx; pointer-events: none;
  }
  .level-badge-wrap { display: inline-flex; margin-bottom: $spacing-sm; }

  .level-info {
    margin-bottom: $spacing-md;

    .level-name {
      font-family: $font-display;
      font-size: 42rpx;
      font-weight: 600;
      display: block;
      margin-bottom: $spacing-xs;
    }

    .level-desc {
      font-size: $font-size-sm;
      opacity: 0.8;
    }
  }

  .level-progress {
    .progress-bar {
      height: 12rpx;
      background: rgba(255,255,255,0.3);
      border-radius: 6rpx;
      overflow: hidden;

      .progress-fill {
        height: 100%;
        background: var(--member-accent, #{$cozy-accent});
        border-radius: 6rpx;
      }
    }
    
    .progress-text {
      font-size: $font-size-xs;
      opacity: 0.8;
      display: block;
      margin-top: $spacing-xs;
    }
  }
}

// 区块标题
.section-title {
  font-family: $font-display;
  font-size: 34rpx;
  font-weight: 600;
  color: $text-primary;
  padding: $spacing-md;

  &.compact {
    padding: 0;
    display: block;
  }
}

.section-heading-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.section-subtitle {
  display: block;
  margin-top: 6rpx;
  color: $text-secondary;
  font-size: $font-size-xs;
}

.monthly-benefit-section,
.monthly-task-section {
  margin: 20rpx 24rpx 0;
  padding: $spacing-md;
  border-radius: $cozy-radius-lg;
  background: $bg-white;
}

.month-badge,
.task-summary {
  flex-shrink: 0;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: $cozy-accent-soft;
  color: $cozy-primary;
  font-size: $font-size-xs;
  font-weight: 600;
}

.benefit-package-name {
  display: block;
  padding: $spacing-md;
  border-radius: $cozy-radius-md;
  background: $cozy-surface;
  color: $text-primary;
  font-size: $font-size-sm;
  line-height: 1.7;
}

.receive-benefit-btn {
  margin-top: $spacing-md;
  padding: 22rpx 24rpx;
  border-radius: $cozy-radius-md;
  background: $cozy-primary;
  color: #fff;
  text-align: center;
  font-size: $font-size-md;
  font-weight: 600;

  &.disabled {
    background: $bg-gray;
    color: $text-placeholder;
  }
}

.upgrade-notice {
  display: block;
  margin-top: $spacing-sm;
  color: $cozy-accent;
  font-size: $font-size-xs;
  line-height: 1.5;
}

.inline-state {
  padding: 36rpx 12rpx;
  color: $text-secondary;
  text-align: center;
  font-size: $font-size-sm;

  &.error {
    color: $error-color;
  }
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.task-card {
  padding: $spacing-md;
  border: 1rpx solid $border-color;
  border-radius: $cozy-radius-md;
}

.task-topline,
.task-bottomline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: $spacing-sm;
}

.task-copy {
  min-width: 0;
  flex: 1;
}

.task-title,
.task-description {
  display: block;
}

.task-title {
  color: $text-primary;
  font-size: $font-size-md;
  font-weight: 600;
}

.task-description {
  margin-top: 4rpx;
  color: $text-secondary;
  font-size: $font-size-xs;
}

.task-reward {
  flex-shrink: 0;
  color: $cozy-primary;
  font-size: $font-size-sm;
  font-weight: 600;
}

.task-progress-track {
  height: 10rpx;
  margin: $spacing-sm 0;
  overflow: hidden;
  border-radius: 999rpx;
  background: $bg-gray;
}

.task-progress-fill {
  height: 100%;
  border-radius: inherit;
  background: $cozy-accent;
}

.task-bottomline {
  color: $text-placeholder;
  font-size: $font-size-xs;

  .completed {
    color: $success-color;
  }
}

// 权益部分
.benefits-section {
  background: $bg-white;
  margin: 20rpx 24rpx 0;
  border-radius: $cozy-radius-lg;
}

.level-tabs {
  white-space: nowrap;
  padding: 0 $spacing-md $spacing-md;
}

.level-tab {
  display: inline-block;
  padding: $spacing-sm $spacing-md;
  margin-right: $spacing-sm;
  background: $bg-gray;
  border-radius: 999rpx;
  font-size: $font-size-sm;
  color: $text-secondary;
  
  &.active {
    background: $cozy-surface-alt;
    color: white;
  }
}

.benefits-list {
  padding: 0 $spacing-md $spacing-md;
}

.benefit-item {
  display: flex;
  align-items: center;
  padding: $spacing-md 0;
  border-bottom: 1rpx solid $border-color;
  
  &:last-child {
    border-bottom: none;
  }
  
  .benefit-icon {
    width: 62rpx;
    height: 62rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $cozy-radius-md;
    background: $cozy-surface;
    color: $cozy-primary;
    font-size: 21rpx;
    font-weight: 750;
    margin-right: $spacing-md;
  }
  
  .benefit-info {
    flex: 1;
    
    .benefit-name {
      font-size: $font-size-md;
      font-weight: 500;
      color: $text-primary;
      display: block;
    }
    
    .benefit-desc {
      font-size: $font-size-sm;
      color: $text-secondary;
    }
  }
  
  .benefit-status {
    font-size: $font-size-xs;
    padding: 4rpx 16rpx;
    border-radius: 20rpx;
    background: $bg-gray;
    color: $text-placeholder;
    
    &.unlocked {
      background: $cozy-accent-soft;
      color: $cozy-accent;
    }
  }
}

// 对比表格
.compare-section {
  background: $bg-white;
  margin: 20rpx 24rpx 0;
  border-radius: $cozy-radius-lg;
}

.compare-table {
  padding: 0 $spacing-md $spacing-md;
  overflow-x: auto;
}

.table-header, .table-row {
  display: flex;
}

.table-cell {
  flex: 1;
  min-width: 100rpx;
  padding: $spacing-sm;
  text-align: center;
  font-size: $font-size-sm;
  
  &.row-name {
    flex: 1.5;
    text-align: left;
    color: $text-secondary;
  }
  
  .check {
    color: $success-color;
    font-weight: bold;
  }
  
  .cross {
    color: $text-placeholder;
  }
}

.table-header .table-cell {
  font-weight: 600;
  color: $text-primary;
  background: $bg-gray;
}

.table-row {
  border-bottom: 1rpx solid $border-color;
}

// 升级指南
.upgrade-section {
  background: $bg-white;
  margin: 20rpx 24rpx 0;
  border-radius: $cozy-radius-lg;
}

.upgrade-tips {
  padding: 0 $spacing-md $spacing-md;
}

.tip-item {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;
  
  .tip-icon {
    width: 58rpx;
    color: $cozy-primary;
    font-size: 19rpx;
    font-weight: 750;
    margin-right: $spacing-md;
  }
  
  .tip-text {
    font-size: $font-size-md;
    color: $text-secondary;
  }
}
</style>
