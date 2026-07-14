<!--
  订单页 - 展示用户历史订单列表
-->
<template>
  <view class="order-page">
    <!-- 订单标签页 -->
    <view class="tabs">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'all' }"
        @click="currentTab = 'all'"
      >
        全部
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'pending' }"
        @click="currentTab = 'pending'"
      >
        待处理
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'completed' }"
        @click="currentTab = 'completed'"
      >
        已完成
      </view>
    </view>
    
    <!-- 订单列表 -->
    <view class="order-list" v-if="orders.length > 0">
      <view class="order-card" v-for="order in filteredOrders" :key="order.id" @click="goToDetail(order.id)">
        <view class="order-header">
          <text class="order-no">订单号：{{ order.orderNo }}</text>
          <view class="order-status-wrap">
            <text class="order-status" :class="order.status">{{ getStatusText(order.status) }}</text>
            <text v-if="order.status === 'pending'" class="expire-countdown" :class="{ urgent: isExpiringSoon(order) }">
              {{ formatCountdown(order) }}
            </text>
          </view>
        </view>
        <view class="order-items">
          <view class="order-item" v-for="item in order.items" :key="item.id">
            <image :src="item.image" class="item-image" mode="aspectFill" />
            <view class="item-info">
              <text class="item-name">{{ item.name }}</text>
              <text class="item-spec">{{ item.spec }}</text>
            </view>
            <view class="item-right">
              <text class="item-price">¥{{ item.price }}</text>
              <text class="item-qty">x{{ item.quantity }}</text>
            </view>
          </view>
        </view>
        <view class="order-footer">
          <text class="order-total">共 {{ order.totalQty }} 件，实付 <text class="price">¥{{ order.totalPrice }}</text></text>
          <view class="order-actions">
            <view class="btn-outline" v-if="order.status === 'completed'" @click.stop="reOrder(order)">再来一单</view>
          </view>
        </view>
      </view>
    </view>

    
    <!-- 空状态 -->
    <view class="empty-state" v-else>
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无订单</text>
      <view class="btn-primary" @click="goToMenu">去点单</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onHide, onUnload } from '@dcloudio/uni-app'
import { getOrderList } from '@/api/order'

const currentTab = ref('all')
const orders = ref([])
const loading = ref(false)
const nowTs = ref(Date.now())

let ticker = null

// 加载订单
const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList()
    if (res.code === 200 && res.data) {
      orders.value = res.data.map(order => ({
        ...order,
        totalPrice: order.payAmount || order.totalAmount,
        totalQty: order.totalQuantity || 0,
        items: (order.items || []).map(item => ({
          ...item,
          name: item.productName,
          price: item.unitPrice,
          image: item.productImage || '/static/images/default-product.png',
          spec: [item.cupSize, item.temperature, item.sugarLevel].filter(Boolean).join('/')
        }))
      }))
    }
  } catch (e) {
    console.error('加载订单失败', e)
  } finally {
    loading.value = false
  }
}

onShow(() => {
  startTicker()
  loadOrders()
})

onHide(() => {
  stopTicker()
})

onUnload(() => {
  stopTicker()
})

const startTicker = () => {
  stopTicker()
  ticker = setInterval(() => {
    nowTs.value = Date.now()
  }, 1000)
}

const stopTicker = () => {
  if (ticker) {
    clearInterval(ticker)
    ticker = null
  }
}

const getExpireMs = (order) => {
  if (order?.expireAt) {
    const ts = new Date(order.expireAt).getTime()
    if (!Number.isNaN(ts)) return ts
  }
  if (order?.createdAt) {
    const createdTs = new Date(order.createdAt).getTime()
    if (!Number.isNaN(createdTs)) return createdTs + 60 * 1000
  }
  return null
}

const getRemainingSeconds = (order) => {
  const expireMs = getExpireMs(order)
  if (!expireMs) return null
  const remain = Math.floor((expireMs - nowTs.value) / 1000)
  return remain > 0 ? remain : 0
}

const formatCountdown = (order) => {
  const remain = getRemainingSeconds(order)
  if (remain == null) return '即将超时'
  if (remain <= 0) return '即将自动取消'
  const mm = String(Math.floor(remain / 60)).padStart(2, '0')
  const ss = String(remain % 60).padStart(2, '0')
  return `剩余 ${mm}:${ss}`
}

const isExpiringSoon = (order) => {
  const remain = getRemainingSeconds(order)
  return remain != null && remain <= 30
}

// 根据标签过滤订单
const filteredOrders = computed(() => {
  if (currentTab.value === 'all') return orders.value
  return orders.value.filter(o => o.status === currentTab.value)
})

// 获取状态文本
const getStatusText = (status) => {
  const map = { pending: '待处理', preparing: '制作中', processing: '制作中', completed: '已完成', cancelled: '已取消' }
  return map[status] || status
}

// 跳转到菜单页
const goToMenu = () => {
  uni.switchTab({ url: '/pages/menu/menu' })
}

// 跳转到订单详情
const goToDetail = (orderId) => {
  uni.navigateTo({ url: `/pages/order/detail?id=${orderId}` })
}

// 再来一单
const reOrder = (order) => {
  // 可以将订单商品加入购物车，这里简化为跳转到菜单页
  uni.switchTab({ url: '/pages/menu/menu' })
}
</script>


<style lang="scss" scoped>
.order-page {
  min-height: 100vh;
  background: $bg-color;
  padding-bottom: $tabbar-height;
}

// 标签页
.tabs {
  display: flex;
  background: $bg-white;
  padding: $spacing-sm 0;
  position: sticky;
  top: 0;
  z-index: 10;
  
  .tab-item {
    flex: 1;
    text-align: center;
    padding: $spacing-sm 0;
    font-size: $font-size-md;
    color: $text-secondary;
    position: relative;
    
    &.active {
      color: $primary-color;
      font-weight: 600;
      
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 4rpx;
        background: $primary-color;
        border-radius: 2rpx;
      }
    }
  }
}

// 订单卡片
.order-card {
  background: $bg-white;
  margin: $spacing-sm;
  border-radius: $cozy-radius-md;
  padding: $spacing-md;
  
  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding-bottom: $spacing-sm;
    border-bottom: 1rpx solid $border-color;
    
    .order-no {
      font-size: $font-size-sm;
      color: $text-secondary;
    }
    
    .order-status-wrap {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 8rpx;
    }

    .order-status {
      font-size: $font-size-sm;
      padding: 4rpx 12rpx;
      border-radius: 4rpx;

      &.pending { background: #FFF7E6; color: #FA8C16; }
      &.preparing, &.processing { background: #E6F7FF; color: #1890FF; }
      &.completed { background: #F6FFED; color: #52C41A; }
      &.cancelled { background: #FFF1F0; color: #FF4D4F; }
    }

    .expire-countdown {
      font-size: 22rpx;
      color: #8c8c8c;

      &.urgent {
        color: #ff4d4f;
        font-weight: 600;
      }
    }
  }
  
  .order-items {
    padding: $spacing-sm 0;
  }
  
  .order-item {
    display: flex;
    align-items: center;
    padding: $spacing-xs 0;
    
    .item-image {
      width: 100rpx;
      height: 100rpx;
      border-radius: $cozy-radius-sm;
    }
    
    .item-info {
      flex: 1;
      margin-left: $spacing-sm;
      
      .item-name {
        font-size: $font-size-md;
        color: $text-primary;
        display: block;
      }
      
      .item-spec {
        font-size: $font-size-sm;
        color: $text-placeholder;
      }
    }
    
    .item-right {
      text-align: right;
      
      .item-price {
        font-size: $font-size-md;
        color: $text-primary;
        display: block;
      }
      
      .item-qty {
        font-size: $font-size-sm;
        color: $text-placeholder;
      }
    }
  }
  
  .order-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: $spacing-sm;
    border-top: 1rpx solid $border-color;
    
    .order-total {
      font-size: $font-size-sm;
      color: $text-secondary;
      
      .price {
        color: $primary-color;
        font-weight: 600;
      }
    }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
  
  .empty-icon {
    font-size: 120rpx;
    margin-bottom: $spacing-md;
  }
  
  .empty-text {
    color: $text-placeholder;
    margin-bottom: $spacing-lg;
  }
}
</style>
