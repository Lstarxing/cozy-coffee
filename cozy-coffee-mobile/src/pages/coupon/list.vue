<!--
  优惠券页 - 对齐 prototype/coupons.html：顶部固定筛选（原生导航标题）+ 券卡（面值/条件 + 名称/范围/到期 + 去使用/状态）
  数据源: /member/mall/coupons (UserCouponDTO，displayTitle/displaySubTitle 后端已算好)
-->
<template>
  <view class="coupon-page">
    <!-- 筛选 tabs（顶部固定条，对齐订单页） -->
    <view class="filter-tabs">
      <view
        class="filter-tab"
        :class="{ active: currentTab === 'available' }"
        @click="switchTab('available')"
      >可使用</view>
      <view
        class="filter-tab"
        :class="{ active: currentTab === 'used' }"
        @click="switchTab('used')"
      >已使用</view>
      <view
        class="filter-tab"
        :class="{ active: currentTab === 'expired' }"
        @click="switchTab('expired')"
      >已过期</view>
    </view>

    <!-- 券列表 -->
    <view v-if="filteredCoupons.length" class="coupon-list">
      <view
        v-for="item in filteredCoupons"
        :key="item.id"
        class="coupon-card"
        :class="item.status"
      >
        <view class="coupon-left">
          <text class="coupon-value">{{ item.displayTitle }}</text>
          <text class="coupon-condition">{{ item.displaySubTitle }}</text>
        </view>
        <view class="coupon-right">
          <text class="coupon-name">{{ item.name }}</text>
          <text class="coupon-scope">{{ item.scope }}</text>
          <text class="coupon-expire">{{ item.expireText }}</text>
        </view>
        <view class="coupon-action">
          <view v-if="item.status === 'available'" class="use-btn" @click="useCoupon(item)">去使用</view>
          <text v-else class="status-tag">{{ statusText(item.status) }}</text>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-state">
      <view class="empty-mark"><CozyIcon name="coupon" :size="36" color="#753A22" /></view>
      <text class="empty-text">暂无{{ tabText }}的优惠券</text>
      <text class="empty-hint">去积分商城兑换更多优惠券</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCouponList } from '@/api/coupon'
import CozyIcon from '@/components/CozyIcon.vue'

const currentTab = ref('available')
const coupons = ref([])

const TAB_TEXT = { available: '可使用', used: '已使用', expired: '已过期' }
const STATUS_TEXT = { available: '可使用', used: '已使用', expired: '已过期' }

const TYPE_NAME = {
  EXCHANGE: '咖啡兑换券',
  DISCOUNT: '折扣券',
  FULL_REDUCE: '满减券',
  BOGO: '买一赠一券',
  DELIVERY_FEE: '配送费抵扣券',
  SHOT: '加浓缩券'
}

const TYPE_SCOPE = {
  EXCHANGE: '全场饮品通兑',
  DISCOUNT: '全场通用',
  FULL_REDUCE: '全场通用',
  BOGO: '现制饮品',
  DELIVERY_FEE: '仅限外送',
  SHOT: '附加券 · 可叠加'
}

onMounted(async () => {
  try {
    const res = await getCouponList()
    if (res.code === 200 && res.data) {
      coupons.value = res.data.map(item => {
        const mappedStatus = mapStatus(item)
        return {
          ...item,
          status: mappedStatus,
          displayTitle: item.displayTitle || (item.value ? `¥${item.value}` : TYPE_NAME[item.couponType] || '优惠券'),
          displaySubTitle: item.displaySubTitle || (item.minAmount ? `满${item.minAmount}可用` : '无门槛'),
          name: item.productName || item.title || TYPE_NAME[item.couponType] || '优惠券',
          scope: TYPE_SCOPE[item.couponType] || '全场通用',
          expireText: formatExpire(item, mappedStatus)
        }
      })
    }
  } catch (e) {
    console.error('获取优惠券失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
})

// 后端返回 ISSUED/USED/EXPIRED + available，映射为前端三态
function mapStatus(item) {
  if (item.status === 'USED') return 'used'
  if (item.status === 'EXPIRED' || item.available === false) return 'expired'
  return 'available'
}

function formatExpire(item, status) {
  if (status !== 'available') return STATUS_TEXT[status]
  if (!item.expiresAt) return ''
  try {
    const d = new Date(item.expiresAt)
    if (Number.isNaN(d.getTime())) return String(item.expiresAt).slice(0, 10)
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `至 ${mm}-${dd} 到期`
  } catch {
    return ''
  }
}

const filteredCoupons = computed(() => coupons.value.filter(c => c.status === currentTab.value))
const tabText = computed(() => TAB_TEXT[currentTab.value])

const statusText = (status) => STATUS_TEXT[status] || status

function switchTab(value) {
  if (currentTab.value === value) return
  currentTab.value = value
}

function useCoupon(coupon) {
  uni.switchTab({ url: '/pages/menu/menu' })
}
</script>

<style lang="scss" scoped>
.coupon-page {
  min-height: 100vh;
  padding-bottom: 220rpx;
  background: $cozy-surface;
}

/* ── 筛选 tabs（顶部固定条 · 下划线式，对齐订单页） ── */
.filter-tabs {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  height: 96rpx;
  background: $bg-white;
  border-bottom: 1rpx solid $cozy-border;
}
.filter-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: $cozy-muted;
  position: relative;
  transition: color $cozy-duration $cozy-ease-out;

  &.active { color: $cozy-ink; font-weight: 600; }
  &.active::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: 0;
    transform: translateX(-50%);
    width: 44rpx;
    height: 4rpx;
    border-radius: 2rpx;
    background: $cozy-ink;
  }
}

/* ── 券列表 ── */
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin: 32rpx 40rpx 0;
}
.coupon-card {
  display: flex;
  align-items: stretch;
  border-radius: 28rpx;
  background: $bg-white;
  border: 1rpx solid $cozy-border;
  overflow: hidden;

  &:not(.used):not(.expired) { border-color: #E3CDB6; }
}

.coupon-left {
  flex: none;
  width: 208rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 36rpx 12rpx;
  border-right: 1rpx dashed $cozy-border;
}
.coupon-value {
  font-family: $font-display;
  font-size: 44rpx;
  font-weight: 700;
  color: $cozy-primary;
  line-height: 1;
  text-align: center;
}
.coupon-condition {
  font-size: 20rpx;
  color: $cozy-muted;
  text-align: center;
  line-height: 1.4;
}

.coupon-right {
  flex: 1;
  min-width: 0;
  padding: 32rpx 28rpx;
}
.coupon-name {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.coupon-scope {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: $cozy-muted;
}
.coupon-expire {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $cozy-placeholder;
}

.coupon-action {
  flex: none;
  width: 148rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 20rpx;
}
.use-btn {
  width: 100%;
  height: 68rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  background: $cozy-primary;
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;

  &:active { background: $cozy-primary-hover; }
}
.status-tag {
  font-size: 24rpx;
  font-weight: 600;
  color: $cozy-placeholder;
}

/* 已使用 / 已过期 */
.coupon-card.used,
.coupon-card.expired {
  background: $cozy-surface;
  border-color: $cozy-border;
}
.coupon-card.used .coupon-value,
.coupon-card.expired .coupon-value { color: $cozy-placeholder; }
.coupon-card.used .coupon-name,
.coupon-card.expired .coupon-name { color: $cozy-muted; }

/* ── 空状态 ── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 320rpx);
  padding: 80rpx 40rpx;
}
.empty-mark {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  background: $cozy-surface;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $cozy-primary;
}
.empty-text {
  margin-top: 40rpx;
  font-family: $font-display;
  font-size: 40rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.empty-hint {
  margin-top: 20rpx;
  font-size: 26rpx;
  color: $cozy-muted;
}
</style>
