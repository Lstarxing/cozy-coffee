<!--
  会员权益页 - 各等级权益详情
-->
<template>
  <view class="benefits-page">
    <!-- 当前等级卡片 -->
    <view class="current-level" :class="currentLevel">
      <view class="level-info">
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
          <text class="benefit-icon">{{ benefit.icon }}</text>
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
            <text class="check" v-if="row[level]">✓</text>
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
          <text class="tip-icon">☕</text>
          <text class="tip-text">消费 1 元 = 1 EXP</text>
        </view>
        <view class="tip-item">
          <text class="tip-icon">🎯</text>
          <text class="tip-text">完成月度任务获得额外 EXP</text>
        </view>
        <view class="tip-item">
          <text class="tip-icon">🎁</text>
          <text class="tip-text">参与活动赢取 EXP 加成</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const levels = ['basic', 'silver', 'gold', 'diamond', 'black']
const currentLevel = ref(userStore.userLevel || 'silver')
const selectedLevel = ref(currentLevel.value)
const currentExp = ref(userStore.memberInfo?.expTotal || 2350)

// 等级门槛
const levelThresholds = {
  basic: 0,
  silver: 1000,
  gold: 3000,
  diamond: 6000,
  black: 10000
}

// 等级名称
const getLevelName = (level) => {
  const map = { basic: '基础会员', silver: '白银会员', gold: '黄金会员', diamond: '钻石会员', black: '黑金会员' }
  return map[level] || '基础会员'
}

// 等级描述
const getLevelDesc = (level) => {
  const map = {
    basic: '开启你的咖啡之旅',
    silver: '累计消费满1000元',
    gold: '累计消费满3000元',
    diamond: '尊贵钻石专属服务',
    black: '顶级黑金至尊体验'
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
  return Math.min(100, ((currentExp.value - curr) / (next - curr)) * 100)
})

// 是否已解锁该等级
const isUnlocked = (level) => {
  return levels.indexOf(currentLevel.value) >= levels.indexOf(level)
}

// 各等级权益
const benefitsData = {
  basic: [
    { icon: '☕', name: '消费积分', desc: '1元=1积分' },
    { icon: '📅', name: '每日签到', desc: '获得积分奖励' }
  ],
  silver: [
    { icon: '☕', name: '消费积分', desc: '1元=1.1积分' },
    { icon: '🎂', name: '生日礼遇', desc: '8折饮品券×1' },
    { icon: '🚚', name: '配送费立减', desc: '每月1次封顶3元' }
  ],
  gold: [
    { icon: '☕', name: '消费积分', desc: '1元=1.2积分' },
    { icon: '🎂', name: '生日礼遇', desc: '美式兑换券×1' },
    { icon: '🧋', name: '免费加料', desc: '每月1次' },
    { icon: '🚚', name: '配送费立减', desc: '每月2次封顶4元' }
  ],
  diamond: [
    { icon: '☕', name: '消费积分', desc: '1元=1.3积分' },
    { icon: '🎂', name: '生日礼遇', desc: '拿铁兑换券×1' },
    { icon: '📐', name: '免费升杯', desc: '每月1次' },
    { icon: '🚚', name: '配送费立减', desc: '每月3次封顶5元' }
  ],
  black: [
    { icon: '🚀', name: '加速包', desc: '每月前300元消费1.7倍积分' },
    { icon: '☕', name: '消费积分', desc: '1元=1.35积分' },
    { icon: '🎂', name: '生日礼遇', desc: '任意饮品券×1' },
    { icon: '🆓', name: '免配送费', desc: '每月1次+立减4次' },
    { icon: '🌟', name: '新品优先', desc: '新品试饮/优先购' }
  ]
}

// 当前选中等级的权益
const currentBenefits = computed(() => benefitsData[selectedLevel.value] || [])

// 对比数据
const compareData = [
  { name: '积分倍率', basic: '1.0x', silver: '1.1x', gold: '1.2x', diamond: '1.3x', black: '1.35x' },
  { name: '生日礼遇', basic: false, silver: true, gold: true, diamond: true, black: true },
  { name: '配送费立减', basic: false, silver: true, gold: true, diamond: true, black: true },
  { name: '免费加料/升杯', basic: false, silver: false, gold: true, diamond: true, black: true },
  { name: '黑卡加速包', basic: false, silver: false, gold: false, diamond: false, black: true }
]
</script>

<style lang="scss" scoped>
.benefits-page {
  min-height: 100vh;
  background: $bg-color;
  padding-bottom: $spacing-xl;
}

// 当前等级卡片
.current-level {
  padding: $spacing-xl $spacing-lg;
  color: white;
  
  &.basic { background: #804A00; }
  &.silver { background: #7F8C8D; }
  &.gold { background: #D4AF37; color: #4A3000; }
  &.diamond { background: #3498DB; }
  &.black { background: #1A1A1A; }
  
  .level-info {
    margin-bottom: $spacing-md;
    
    .level-name {
      font-size: $font-size-xxl;
      font-weight: 700;
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
        background: rgba(255,255,255,0.9);
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
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-primary;
  padding: $spacing-md;
}

// 权益部分
.benefits-section {
  background: $bg-white;
  margin-bottom: $spacing-sm;
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
  border-radius: 30rpx;
  font-size: $font-size-sm;
  color: $text-secondary;
  
  &.active {
    background: $primary-color;
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
    font-size: 48rpx;
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
      background: rgba($success-color, 0.1);
      color: $success-color;
    }
  }
}

// 对比表格
.compare-section {
  background: $bg-white;
  margin-bottom: $spacing-sm;
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
}

.upgrade-tips {
  padding: 0 $spacing-md $spacing-md;
}

.tip-item {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;
  
  .tip-icon {
    font-size: 40rpx;
    margin-right: $spacing-md;
  }
  
  .tip-text {
    font-size: $font-size-md;
    color: $text-secondary;
  }
}
</style>
