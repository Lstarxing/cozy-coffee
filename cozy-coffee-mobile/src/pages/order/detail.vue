<!--
  订单详情页 - 展示订单完整信息
-->
<template>
  <view class="order-detail-page" v-if="order">
    <!-- 订单状态 -->
    <view class="status-section" :class="order.status">
      <text class="status-icon">{{ getStatusIcon(order.status) }}</text>
      <text class="status-text">{{ getStatusText(order.status) }}</text>
      <text v-if="order.status === 'pending'" class="countdown-text" :class="{ urgent: isExpiringSoon }">
        {{ countdownText }}
      </text>
      <text class="status-desc">{{ getStatusDesc(order.status) }}</text>
    </view>
    
    <!-- 配送信息 -->
    <view class="delivery-section">
      <view class="delivery-header">
        <text class="delivery-type">{{ order.deliveryType === 'delivery' ? '🛵 外卖配送' : '🏪 到店自提' }}</text>
      </view>
      <view class="delivery-content" v-if="order.deliveryType === 'delivery'">
        <text class="receiver">{{ order.receiver }} {{ order.phone }}</text>
        <text class="address">{{ order.address }}</text>
      </view>
      <view class="delivery-content" v-else>
        <text class="store-name">{{ order.storeName }}</text>
        <text class="store-address">{{ order.storeAddress }}</text>
      </view>
    </view>
    
    <!-- 商品列表 -->
    <view class="products-section">
      <view class="section-title">商品信息</view>
      <view class="product-item" v-for="item in order.items" :key="item.id">
        <image :src="item.image" class="product-image" mode="aspectFill" />
        <view class="product-info">
          <text class="product-name">{{ item.name }}</text>
          <text class="product-spec">{{ item.spec }}</text>
        </view>
        <view class="product-right">
          <text class="product-price">¥{{ item.price }}</text>
          <text class="product-qty">x{{ item.quantity }}</text>
        </view>
      </view>
    </view>
    
    <!-- 价格明细 -->
    <view class="price-section">
      <view class="price-item">
        <text class="price-label">商品金额</text>
        <text class="price-value">¥{{ order.productTotal }}</text>
      </view>
      <view class="price-item" v-if="order.deliveryFee > 0">
        <text class="price-label">配送费</text>
        <text class="price-value">¥{{ order.deliveryFee }}</text>
      </view>
      <view class="price-item" v-if="order.discount > 0">
        <text class="price-label">优惠</text>
        <text class="price-value discount">-¥{{ order.discount }}</text>
      </view>
      <view class="price-item total">
        <text class="price-label">实付金额</text>
        <text class="price-value total-price">¥{{ order.totalPrice }}</text>
      </view>
      <view class="points-earned">
        <text>本单获得 <text class="points-value">{{ order.pointsEarned }}</text> 积分</text>
      </view>
    </view>
    
    <!-- 订单信息 -->
    <view class="info-section">
      <view class="section-title">订单信息</view>
      <view class="info-item">
        <text class="info-label">订单编号</text>
        <text class="info-value">{{ order.orderNo }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">下单时间</text>
        <text class="info-value">{{ order.createTime }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">支付方式</text>
        <text class="info-value">{{ order.payMethod }}</text>
      </view>
      <view class="info-item" v-if="order.remark">
        <text class="info-label">备注</text>
        <text class="info-value">{{ order.remark }}</text>
      </view>
    </view>
    
    <!-- 底部操作 -->
    <view class="action-bar safe-area-bottom">
      <view class="action-btn outline" @click="contactService">联系客服</view>
      <view class="action-btn primary" v-if="order.status === 'completed'" @click="reOrder">再来一单</view>
      <view class="action-btn primary" v-if="order.status === 'pending'" @click="cancelOrder">取消订单</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app'
import { getOrderDetail, cancelOrder as cancelOrderApi } from '@/api/order'

const order = ref(null)
const orderId = ref(null)
const nowTs = ref(Date.now())

let ticker = null

onLoad(async (options) => {
  if (options.id) {
    orderId.value = options.id
    startTicker()
    loadOrderDetail()
  }
})

onShow(() => {
  startTicker()
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

const remainingSeconds = computed(() => {
  if (!order.value || order.value.status !== 'pending') return null

  let expireMs = null
  if (order.value.expireAt) {
    const ts = new Date(order.value.expireAt).getTime()
    if (!Number.isNaN(ts)) {
      expireMs = ts
    }
  }

  if (!expireMs && order.value.createdAt) {
    const createdTs = new Date(order.value.createdAt).getTime()
    if (!Number.isNaN(createdTs)) {
      expireMs = createdTs + 60 * 1000
    }
  }

  if (!expireMs) return null
  const remain = Math.floor((expireMs - nowTs.value) / 1000)
  return remain > 0 ? remain : 0
})

const countdownText = computed(() => {
  if (remainingSeconds.value == null) return '即将超时'
  if (remainingSeconds.value <= 0) return '即将自动取消'
  const mm = String(Math.floor(remainingSeconds.value / 60)).padStart(2, '0')
  const ss = String(remainingSeconds.value % 60).padStart(2, '0')
  return `剩余 ${mm}:${ss}`
})

const isExpiringSoon = computed(() => {
  return remainingSeconds.value != null && remainingSeconds.value <= 30
})

const loadOrderDetail = async () => {
  try {
    const res = await getOrderDetail(orderId.value)
    if (res.code === 200 && res.data) {
      // 适配后端字段
      const data = res.data
      order.value = {
        ...data,
        createTime: data.createdAt || data.createTime,
        items: (data.items || []).map(item => ({
          ...item,
          image: item.productImage || item.image || '/static/images/default-product.png',
          spec: item.specDescription || item.spec || ''
        })),
        totalPrice: data.paymentAmount || data.totalPrice,
        productTotal: data.totalAmount || data.productTotal
      }
    }
  } catch (e) {
    console.error('获取订单详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

const getStatusIcon = (status) => {
  const s = status?.toLowerCase()
  const map = { pending_payment: '⏳', preparing: '👨‍🍳', delivering: '🛵', completed: '✅', cancelled: '❌', pending: '⏳' }
  return map[s] || '📋'
}

const getStatusText = (status) => {
  const s = status?.toLowerCase()
  const map = { pending_payment: '待支付', preparing: '制作中', delivering: '配送中', completed: '已完成', cancelled: '已取消', pending: '待支付' }
  return map[s] || '未知'
}

const getStatusDesc = (status) => {
  const s = status?.toLowerCase()
  const map = {
    pending_payment: '请在15分钟内完成支付',
    pending: '请在15分钟内完成支付',
    preparing: '咖啡师正在为您精心制作',
    delivering: '骑手正在快马加鞭赶来',
    completed: '订单已完成，期待您的下次光临',
    cancelled: '订单已取消'
  }
  return map[s] || ''
}

const contactService = () => {
  uni.showToast({ title: '客服功能开发中', icon: 'none' })
}

const reOrder = () => {
  uni.switchTab({ url: '/pages/menu/menu' })
}

const cancelOrder = () => {
  uni.showModal({
    title: '确认取消',
    content: '确定要取消这个订单吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          const cancelRes = await cancelOrderApi(orderId.value)
          if (cancelRes.code === 200) {
            uni.showToast({ title: '订单已取消', icon: 'success' })
            loadOrderDetail() // 重新加载
          }
        } catch (e) {
          console.error('取消订单失败', e)
          uni.showToast({ title: '取消失败', icon: 'none' })
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.order-detail-page {
  min-height: 100vh;
  background: $bg-color;
  padding-bottom: 140rpx;
}

// 状态区域
.status-section {
  padding: $spacing-xl;
  text-align: center;
  color: white;
  
  &.pending { background: #F39C12; }
  &.preparing { background: #3498DB; }
  &.delivering { background: #1ABC9C; }
  &.completed { background: $cozy-surface-alt; }
  &.cancelled { background: #95A5A6; }
  
  .status-icon {
    font-size: 80rpx;
    display: block;
    margin-bottom: $spacing-sm;
  }
  
  .status-text {
    font-size: $font-size-xl;
    font-weight: 600;
    display: block;
    margin-bottom: $spacing-xs;
  }
  
  .status-desc {
    font-size: $font-size-sm;
    opacity: 0.8;
  }

  .countdown-text {
    margin-bottom: 8rpx;
    font-size: $font-size-sm;
    opacity: 0.9;

    &.urgent {
      font-weight: 600;
      color: #FFE4E6;
    }
  }
}

// 配送信息
.delivery-section {
  background: $bg-white;
  margin: $spacing-md;
  padding: $spacing-md;
  border-radius: $cozy-radius-md;
  
  .delivery-header {
    margin-bottom: $spacing-sm;
    
    .delivery-type {
      font-size: $font-size-md;
      font-weight: 600;
      color: $text-primary;
    }
  }
  
  .delivery-content {
    .receiver, .store-name {
      font-size: $font-size-md;
      color: $text-primary;
      display: block;
      margin-bottom: $spacing-xs;
    }
    
    .address, .store-address {
      font-size: $font-size-sm;
      color: $text-secondary;
    }
  }
}

// 商品列表
.products-section {
  background: $bg-white;
  margin: 0 $spacing-md $spacing-md;
  padding: $spacing-md;
  border-radius: $cozy-radius-md;
}

.section-title {
  font-size: $font-size-md;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $spacing-md;
}

.product-item {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;
  border-bottom: 1rpx solid $border-color;
  
  &:last-child {
    border-bottom: none;
  }
  
  .product-image {
    width: 100rpx;
    height: 100rpx;
    border-radius: $cozy-radius-sm;
  }
  
  .product-info {
    flex: 1;
    margin-left: $spacing-sm;
    
    .product-name {
      font-size: $font-size-md;
      color: $text-primary;
      display: block;
    }
    
    .product-spec {
      font-size: $font-size-sm;
      color: $text-placeholder;
    }
  }
  
  .product-right {
    text-align: right;
    
    .product-price {
      font-size: $font-size-md;
      display: block;
    }
    
    .product-qty {
      font-size: $font-size-sm;
      color: $text-placeholder;
    }
  }
}

// 价格明细
.price-section {
  background: $bg-white;
  margin: 0 $spacing-md $spacing-md;
  padding: $spacing-md;
  border-radius: $cozy-radius-md;
  
  .price-item {
    display: flex;
    justify-content: space-between;
    padding: $spacing-xs 0;
    
    .price-label {
      color: $text-secondary;
    }
    
    .price-value {
      color: $text-primary;
      
      &.discount {
        color: $error-color;
      }
      
      &.total-price {
        font-size: $font-size-xl;
        font-weight: 700;
        color: $primary-color;
      }
    }
    
    &.total {
      padding-top: $spacing-sm;
      margin-top: $spacing-sm;
      border-top: 1rpx solid $border-color;
    }
  }
  
  .points-earned {
    text-align: right;
    font-size: $font-size-sm;
    color: $text-placeholder;
    margin-top: $spacing-xs;
    
    .points-value {
      color: $success-color;
      font-weight: 600;
    }
  }
}

// 订单信息
.info-section {
  background: $bg-white;
  margin: 0 $spacing-md;
  padding: $spacing-md;
  border-radius: $cozy-radius-md;
  
  .info-item {
    display: flex;
    justify-content: space-between;
    padding: $spacing-xs 0;
    
    .info-label {
      color: $text-secondary;
      font-size: $font-size-sm;
    }
    
    .info-value {
      color: $text-primary;
      font-size: $font-size-sm;
    }
  }
}

// 底部操作
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  background: $bg-white;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 $spacing-md;
  gap: $spacing-md;

  .action-btn {
    padding: $spacing-sm $spacing-lg;
    border-radius: 40rpx;
    font-size: $font-size-md;
    
    &.outline {
      border: 2rpx solid $border-color;
      color: $text-secondary;
    }
    
    &.primary {
      background: $primary-color;
      color: white;
    }
  }
}
</style>
