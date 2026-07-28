<!--
  每日签到页 - 7天签到进度
-->
<template>
  <view class="signin-page">
    <!-- 顶部积分卡片 -->
    <view class="points-card">
      <view class="points-bg">
        <text class="points-label">当前积分</text>
        <text class="points-value">{{ currentPoints }}</text>
      </view>
    </view>
    
    <!-- 签到日历 -->
    <view class="signin-calendar">
      <view class="calendar-header">
        <text class="calendar-title">每日签到领积分</text>
        <text class="calendar-hint">已连续签到 {{ consecutiveDays }} 天</text>
      </view>
      
      <view class="calendar-days">
        <view 
          class="day-item"
          :class="{ 
            signed: index < consecutiveDays, 
            today: index === consecutiveDays,
            future: index > consecutiveDays 
          }"
          v-for="(day, index) in weekDays" 
          :key="index"
        >
          <view class="day-circle">
            <text class="day-icon" v-if="index < consecutiveDays">✓</text>
            <text class="day-icon gift" v-else-if="index === 6">券</text>
            <text class="day-icon" v-else>{{ index + 1 }}</text>
          </view>
          <text class="day-points">+{{ day.points }}</text>
          <text class="day-label">{{ day.label }}</text>
        </view>
      </view>
      
      <!-- 签到按钮 -->
      <view class="signin-action">
        <view 
          class="signin-btn" 
          :class="{ disabled: hasSigned || signing }"
          @click="handleSignin"
        >
          {{ hasSigned ? '今日已签到' : (signing ? '签到中…' : '立即签到') }}
        </view>
        <text class="signin-hint" v-if="!hasSigned">
          签到可获得 <text class="highlight">{{ todayPoints }}</text> 积分
        </text>
        <text class="signin-hint" v-else>
          明天签到可获得 <text class="highlight">{{ tomorrowPoints }}</text> 积分
        </text>
      </view>
    </view>
    
    <!-- 签到规则 -->
    <view class="rules-section">
      <view class="rules-title">签到规则</view>
      <view class="rules-list">
        <view class="rule-item">
          <text class="rule-dot">•</text>
          <text class="rule-text">每日签到固定获得 2 积分</text>
        </view>
        <view class="rule-item">
          <text class="rule-dot">•</text>
          <text class="rule-text">连续签到 7 天赠送满 35 减 10 元优惠券，有效期 3 天</text>
        </view>
        <view class="rule-item">
          <text class="rule-dot">•</text>
          <text class="rule-text">中断签到后，连续天数将重新计算</text>
        </view>
        <view class="rule-item">
          <text class="rule-dot">•</text>
          <text class="rule-text">签到积分有效期 365 天，以积分流水和券包到账结果为准</text>
        </view>
      </view>
    </view>
    
    <!-- 签到成功弹窗 -->
    <view class="modal-mask" v-if="showSuccessModal" @click="showSuccessModal = false">
      <view class="success-modal" @click.stop>
        <view class="success-icon">✓</view>
        <text class="success-title">签到成功</text>
        <text class="success-points">+{{ earnedPoints }} 积分</text>
        <text class="success-hint">
          {{ consecutiveDays === 7 ? '连续7天达成，满35减10券将发放至券包' : `已连续签到 ${consecutiveDays} 天` }}
        </text>
        <view class="success-btn" @click="showSuccessModal = false">知道了</view>
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
const showSuccessModal = ref(false)
const earnedPoints = ref(0)
const signing = ref(false)

// 7天签到配置
const weekDays = [
  { label: '第1天', points: 2 },
  { label: '第2天', points: 2 },
  { label: '第3天', points: 2 },
  { label: '第4天', points: 2 },
  { label: '第5天', points: 2 },
  { label: '第6天', points: 2 },
  { label: '第7天', points: 2 }
]

const getLocalDateText = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 每次进入页面刷新签到状态
onShow(async () => {
  try {
    const res = await getMemberInfo()
    if (res.code === 200) {
      currentPoints.value = res.data.currentPoints ?? 0
      consecutiveDays.value = res.data.consecutiveSignDays ?? 0
      userStore.setMemberInfo(res.data)
      // 判断今天是否已签到
      if (res.data.lastSigninDate) {
        hasSigned.value = res.data.lastSigninDate === getLocalDateText()
      } else {
        hasSigned.value = false
      }
    }
  } catch (e) {
    console.error('获取会员信息失败', e)
    // 使用 Store 中的数据作为备选
    currentPoints.value = userStore.memberInfo?.currentPoints || 0
  }
})

// 今日可得积分
const todayPoints = computed(() => {
  const dayIndex = Math.min(consecutiveDays.value, 6)
  return weekDays[dayIndex].points
})

// 明日可得积分
const tomorrowPoints = computed(() => {
  const dayIndex = Math.min(consecutiveDays.value, 6)
  return weekDays[Math.min(dayIndex + 1, 6)].points
})

// 执行签到
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
      showSuccessModal.value = true
      
      // 更新用户状态
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
  background: $bg-color;
}

// 积分卡片
.points-card {
  background: $cozy-surface-alt;
  padding: $spacing-xl $spacing-lg;
  
  .points-bg {
    text-align: center;
    color: white;
    
    .points-label {
      font-size: $font-size-sm;
      opacity: 0.8;
      display: block;
      margin-bottom: $spacing-xs;
    }
    
    .points-value {
      font-size: 72rpx;
      font-weight: 700;
    }
  }
}

// 签到日历
.signin-calendar {
  background: $bg-white;
  margin: -40rpx $spacing-md 0;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  position: relative;
  box-shadow: $box-shadow;
  
  .calendar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;
    
    .calendar-title {
      font-size: $font-size-lg;
      font-weight: 600;
      color: $text-primary;
    }
    
    .calendar-hint {
      font-size: $font-size-sm;
      color: $primary-color;
    }
  }
}

.calendar-days {
  display: flex;
  justify-content: space-between;
  margin-bottom: $spacing-lg;
}

.day-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .day-circle {
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    border: 2rpx solid $border-color;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: $spacing-xs;
    background: $bg-gray;
    
    .day-icon {
      font-size: 28rpx;
      color: $text-placeholder;
      
      &.gift {
        font-size: 32rpx;
      }
    }
  }
  
  .day-points {
    font-size: $font-size-xs;
    color: $text-placeholder;
    margin-bottom: 4rpx;
  }
  
  .day-label {
    font-size: 20rpx;
    color: $text-placeholder;
  }
  
  // 已签到
  &.signed {
    .day-circle {
      background: $primary-color;
      border-color: $primary-color;
      
      .day-icon {
        color: white;
      }
    }
    
    .day-points {
      color: $primary-color;
    }
  }
  
  // 今天
  &.today {
    .day-circle {
      border-color: $primary-color;
      border-width: 3rpx;
      
      .day-icon {
        color: $primary-color;
      }
    }
  }
}

.signin-action {
  text-align: center;
  
  .signin-btn {
    background: $cozy-surface-alt;
    color: white;
    padding: $spacing-md $spacing-xl;
    border-radius: $cozy-radius-md;
    font-size: $font-size-lg;
    font-weight: 600;
    display: inline-block;
    min-width: 300rpx;
    
    &.disabled {
      background: #ccc;
    }
  }
  
  .signin-hint {
    display: block;
    margin-top: $spacing-sm;
    font-size: $font-size-sm;
    color: $text-secondary;
    
    .highlight {
      color: $primary-color;
      font-weight: 600;
    }
  }
}

// 规则
.rules-section {
  margin: $spacing-md;
  padding: $spacing-md;
  background: $bg-white;
  border-radius: $border-radius-md;
  
  .rules-title {
    font-size: $font-size-md;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: $spacing-sm;
  }
  
  .rule-item {
    display: flex;
    margin-bottom: $spacing-xs;
    
    .rule-dot {
      color: $primary-color;
      margin-right: $spacing-xs;
    }
    
    .rule-text {
      font-size: $font-size-sm;
      color: $text-secondary;
      line-height: 1.6;
    }
  }
}

// 成功弹窗
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.success-modal {
  width: 500rpx;
  background: $bg-white;
  border-radius: $border-radius-lg;
  padding: $spacing-xl;
  text-align: center;
  
  .success-icon {
    font-size: 100rpx;
    margin-bottom: $spacing-md;
  }
  
  .success-title {
    font-size: $font-size-xl;
    font-weight: 600;
    color: $text-primary;
    display: block;
    margin-bottom: $spacing-sm;
  }
  
  .success-points {
    font-size: 56rpx;
    font-weight: 700;
    color: $primary-color;
    display: block;
    margin-bottom: $spacing-sm;
  }
  
  .success-hint {
    font-size: $font-size-sm;
    color: $text-secondary;
    display: block;
    margin-bottom: $spacing-lg;
  }
  
  .success-btn {
    background: $primary-color;
    color: white;
    padding: $spacing-sm $spacing-xl;
    border-radius: $cozy-radius-md;
    font-size: $font-size-md;
    display: inline-block;
  }
}
</style>
