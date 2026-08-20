<!--
  积分明细页 - 对齐 prototype/points-history.html：浅色双栏概览 + 下划线筛选 + 白卡流水 + 底部查看积分规则
  数据源: /member/points/transactions (PointsTransactionDTO)
-->
<template>
  <view class="points-history-page">
    <!-- 积分概览 -->
    <view class="overview">
      <view class="overview-col">
        <text class="overview-label">当前可用积分</text>
        <text class="overview-value">{{ formatPoints(memberInfo.currentPoints) }}</text>
      </view>
      <view class="overview-divider"></view>
      <view class="overview-col">
        <text class="overview-label">累计获得</text>
        <text class="overview-value total">{{ formatPoints(memberInfo.totalPoints) }}</text>
      </view>
    </view>

    <!-- 筛选 -->
    <view class="filter-wrap">
      <FilterTabs :options="typeOptions" v-model="filterType" />
    </view>

    <!-- 流水 -->
    <view class="history-card">
      <view v-if="loading && historyList.length === 0" class="load-state">正在加载积分记录…</view>
      <view v-else-if="errorMessage && historyList.length === 0" class="load-state error">
        <text>{{ errorMessage }}</text>
        <view class="retry-button" @click="loadHistory">重新加载</view>
      </view>

      <view v-else-if="filteredHistory.length">
        <view class="history-item" v-for="item in filteredHistory" :key="item.id">
          <view class="item-left">
            <text class="item-desc">{{ item.description }}</text>
            <text class="item-time">{{ item.createTime }}</text>
          </view>
          <view class="item-right">
            <text class="item-amount" :class="item.type">{{ item.sign }}{{ item.amountText }}</text>
            <text class="item-balance">余额 {{ item.balanceText }}</text>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-else class="empty-state">
        <view class="empty-mark">PTS</view>
        <text class="empty-text">暂无积分记录</text>
        <text class="empty-hint">完成门店消费或每日签到后会显示在这里</text>
      </view>
    </view>

    <!-- 底部 -->
    <view class="history-foot">
      <text class="foot-link" @click="goToRules">查看积分规则 →</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getPointsTransactions, getMemberInfo } from '@/api/member'
import FilterTabs from '@/components/common/FilterTabs.vue'
import { formatPoints, formatTime } from '@/utils/format'

const userStore = useUserStore()
const memberInfo = computed(() => userStore.memberInfo)

const filterType = ref('all')
const typeOptions = [
  { value: 'all', label: '全部' },
  { value: 'earn', label: '收入' },
  { value: 'spend', label: '支出' }
]
const historyList = ref([])
const loading = ref(false)
const errorMessage = ref('')

const loadHistory = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const memberRes = await getMemberInfo()
    if (memberRes.code === 200) {
      userStore.setMemberInfo(memberRes.data)
    }
  } catch (e) {
    errorMessage.value = e?.message || '会员积分加载失败'
  }

  try {
    const res = await getPointsTransactions(50)
    if (res.code === 200 && res.data) {
      historyList.value = res.data.map(item => {
        const amount = Number(item.changeAmount || 0)
        return {
          id: item.id,
          description: item.description || item.sourceType || '积分变动',
          createTime: formatTime(item.createdAt),
          amount,
          sign: amount > 0 ? '+' : '',
          amountText: amount.toLocaleString(),
          balanceText: Number(item.balanceAfter || 0).toLocaleString(),
          type: amount >= 0 ? 'earn' : 'spend'
        }
      })
    }
  } catch (e) {
    historyList.value = []
    errorMessage.value = e?.message || '积分记录加载失败'
  } finally {
    loading.value = false
  }
}

onShow(loadHistory)

const filteredHistory = computed(() => {
  if (filterType.value === 'all') {
    return historyList.value
  }
  return historyList.value.filter(item => item.type === filterType.value)
})

function goToRules() { uni.navigateTo({ url: '/pages/points/rules' }) }
</script>

<style lang="scss" scoped>
.points-history-page {
  min-height: 100vh;
  padding: 40rpx 40rpx 220rpx;
  background: $cozy-surface;
}

/* ── 积分概览（浅色 · 双栏） ── */
.overview {
  display: flex;
  align-items: stretch;
  padding: 52rpx 16rpx 44rpx;
}
.overview-col {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.overview-label {
  display: block;
  font-size: 24rpx;
  color: $cozy-muted;
  letter-spacing: .06em;
}
.overview-value {
  display: block;
  margin-top: 20rpx;
  font-family: $font-display;
  font-size: 76rpx;
  font-weight: 600;
  color: $cozy-ink;
  line-height: 1;
}
.overview-col:last-child .overview-value {
  color: $cozy-muted;
  font-size: 60rpx;
  margin-top: 28rpx;
}
.overview-divider {
  width: 1rpx;
  align-self: center;
  background: $cozy-muted;
  opacity: .18;
  height: 92rpx;
}

/* ── 筛选（下划线式，间距由容器提供） ── */
.filter-wrap {
  margin-top: 28rpx;
  padding: 0 8rpx;
}

/* ── 流水 ── */
.history-card {
  margin-top: 36rpx;
  border-radius: 28rpx;
  background: $bg-white;
  padding: 8rpx 40rpx;
}
.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32rpx;
  padding: 36rpx 0;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
}
.item-left { min-width: 0; flex: 1; }
.item-desc {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: $cozy-ink;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-time {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: $cozy-placeholder;
  letter-spacing: .03em;
}
.item-right { text-align: right; flex: none; }
.item-amount {
  display: block;
  font-family: $font-display;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1;

  &.earn { color: $cozy-primary; }
  &.spend { color: $cozy-ink; }
}
.item-balance {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: $cozy-placeholder;
}

/* ── 加载 / 空状态 ── */
.load-state {
  padding: 96rpx 20rpx;
  color: $cozy-muted;
  text-align: center;
}
.load-state text { display: block; }
.load-state.error { color: $error-color; }
.retry-button {
  width: 220rpx;
  margin: 32rpx auto 0;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $cozy-radius-md;
  background: $cozy-primary;
  color: #fff;
  font-size: 24rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 128rpx 0 112rpx;
}
.empty-mark {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  background: $cozy-surface;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: $font-display;
  font-size: 28rpx;
  font-weight: 700;
  letter-spacing: .08em;
  color: $cozy-primary;
}
.empty-text {
  margin-top: 32rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.empty-hint {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: $cozy-muted;
}

/* ── 底部 ── */
.history-foot {
  margin-top: 56rpx;
  text-align: center;
}
.foot-link {
  font-size: 26rpx;
  font-weight: 650;
  color: $cozy-primary;
  letter-spacing: .03em;

  &:active { opacity: .6; }
}
</style>
