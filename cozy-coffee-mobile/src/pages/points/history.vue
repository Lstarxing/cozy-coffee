<!--
  积分明细页 - 展示积分流水记录
-->
<template>
  <view class="points-history-page">
    <!-- 积分概览 -->
    <view class="points-overview">
      <view class="overview-item">
        <text class="value">{{ memberInfo.currentPoints || 0 }}</text>
        <text class="label">当前积分</text>
      </view>
      <view class="divider"></view>
      <view class="overview-item">
        <text class="value">{{ memberInfo.totalPoints || 0 }}</text>
        <text class="label">累计获得</text>
      </view>
    </view>
    
    <!-- 筛选标签 -->
    <view class="filter-tabs">
      <view 
        class="tab-item" 
        :class="{ active: filterType === 'all' }"
        @click="filterType = 'all'"
      >
        全部
      </view>
      <view 
        class="tab-item"
        :class="{ active: filterType === 'earn' }"
        @click="filterType = 'earn'"
      >
        收入
      </view>
      <view 
        class="tab-item"
        :class="{ active: filterType === 'spend' }"
        @click="filterType = 'spend'"
      >
        支出
      </view>
    </view>
    
    <!-- 流水列表 -->
    <view class="history-list">
      <view class="load-state" v-if="loading">正在加载积分记录…</view>
      <view class="load-state error" v-else-if="errorMessage">
        <text>{{ errorMessage }}</text>
        <button class="retry-button" @click="loadHistory">重新加载</button>
      </view>
      <view class="history-item" v-for="item in filteredHistory" :key="item.id">
        <view class="item-left">
          <text class="item-desc">{{ item.description }}</text>
          <text class="item-time">{{ item.createTime }}</text>
        </view>
        <view class="item-right">
          <text class="item-amount" :class="item.type">
            {{ item.type === 'earn' ? '+' : '' }}{{ item.amount }}
          </text>
          <text class="item-balance">余额: {{ item.balance }}</text>
        </view>
      </view>
      
      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && !errorMessage && filteredHistory.length === 0">
        <text class="empty-icon">PTS</text>
        <text class="empty-text">暂无积分记录</text>
        <text class="empty-hint">完成门店消费或每日签到后会显示在这里</text>
      </view>
    </view>
    
    <!-- 说明 -->
    <view class="points-tips">
      <view class="tips-title">积分规则说明</view>
      <view class="tips-item">• 消费 1 元 = 1 积分（等级越高倍率越高）</view>
      <view class="tips-item">• 每日签到固定获得 2 积分，连续 7 天赠满 35 减 10 券</view>
      <view class="tips-item">• 积分自获得之日起 365 天内有效</view>
      <view class="tips-item">• 积分可在积分商城兑换精美礼品</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getPointsTransactions, getMemberInfo } from '@/api/member'

const userStore = useUserStore()
const memberInfo = computed(() => userStore.memberInfo)

const filterType = ref('all')
const historyList = ref([])
const loading = ref(false)
const errorMessage = ref('')

const loadHistory = async () => {
  loading.value = true
  errorMessage.value = ''
  // 刷新会员信息
  try {
    const memberRes = await getMemberInfo()
    if (memberRes.code === 200) {
      userStore.setMemberInfo(memberRes.data)
    }
  } catch (e) {
    errorMessage.value = e?.message || '会员积分加载失败'
  }
  
  // 获取积分流水
  try {
    const res = await getPointsTransactions(50)
    if (res.code === 200 && res.data) {
      // 适配字段名
      historyList.value = res.data.map(item => ({
        id: item.id,
        description: item.description || item.sourceType || '积分变动',
        createTime: item.createdAt || '',
        amount: item.changeAmount,
        balance: item.balanceAfter,
        type: item.changeAmount >= 0 ? 'earn' : 'spend'
      }))
    }
  } catch (e) {
    historyList.value = []
    errorMessage.value = e?.message || '积分记录加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadHistory)

const filteredHistory = computed(() => {
  if (filterType.value === 'all') {
    return historyList.value
  }
  return historyList.value.filter(item => item.type === filterType.value)
})
</script>

<style lang="scss" scoped>
.points-history-page {
  min-height: 100vh;
  background: $bg-color;
}

// 积分概览
.points-overview {
  display: flex;
  background: $cozy-surface-alt;
  padding: $spacing-xl $spacing-lg;
  color: white;
  
  .overview-item {
    flex: 1;
    text-align: center;
    
    .value {
      font-size: 56rpx;
      font-weight: 700;
      display: block;
    }
    
    .label {
      font-size: $font-size-sm;
      opacity: 0.8;
    }
  }
  
  .divider {
    width: 1rpx;
    background: rgba(255,255,255,0.3);
    margin: $spacing-sm 0;
  }
}

// 筛选标签
.filter-tabs {
  display: flex;
  background: $bg-white;
  padding: $spacing-sm 0;
  
  .tab-item {
    flex: 1;
    text-align: center;
    padding: $spacing-sm 0;
    font-size: $font-size-md;
    color: $text-secondary;
    
    &.active {
      color: $primary-color;
      font-weight: 600;
    }
  }
}

// 流水列表
.history-list {
  padding: $spacing-md;
}

.load-state { padding: 80rpx 20rpx; color: $cozy-muted; text-align: center; }
.load-state text { display: block; }
.retry-button { width: 220rpx; margin-top: 24rpx; border-radius: $cozy-radius-md; background: $cozy-primary; color: #fff; font-size: 24rpx; }

.history-item {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 28rpx 0;
  border-bottom: 1rpx solid $cozy-border;
  background: transparent;
  
  .item-left {
    .item-desc {
      font-size: $font-size-md;
      color: $text-primary;
      display: block;
      margin-bottom: $spacing-xs;
    }
    
    .item-time {
      font-size: $font-size-xs;
      color: $text-placeholder;
    }
  }
  
  .item-right {
    text-align: right;
    
    .item-amount {
      font-size: $font-size-lg;
      font-weight: 600;
      display: block;
      
      &.earn {
        color: $cozy-accent;
      }
      
      &.spend {
        color: $error-color;
      }
    }
    
    .item-balance {
      font-size: $font-size-xs;
      color: $text-placeholder;
    }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
  
  .empty-icon {
    width: 104rpx;
    height: 104rpx;
    margin-bottom: $spacing-md;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: $cozy-surface;
    color: $cozy-primary;
    font-size: 22rpx;
    font-weight: 800;
    letter-spacing: .06em;
  }
  
  .empty-text {
    color: $cozy-ink;
    font-size: 29rpx;
    font-weight: 700;
  }

  .empty-hint { margin-top: 10rpx; color: $cozy-muted; font-size: 22rpx; }
}

// 说明
.points-tips {
  margin: 8rpx 32rpx 32rpx;
  padding: 28rpx 0 0;
  border-top: 1rpx solid $cozy-border;
  background: $bg-white;
  
  .tips-title {
    font-size: $font-size-md;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: $spacing-sm;
  }
  
  .tips-item {
    font-size: $font-size-sm;
    color: $text-secondary;
    line-height: 2;
  }
}
</style>
