<!--
  优惠券列表页 - 展示用户的优惠券
-->
<template>
  <view class="coupon-page">
    <!-- 标签切换 -->
    <view class="tabs">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'available' }"
        @click="currentTab = 'available'"
      >
        可使用 ({{ availableCount }})
      </view>
      <view 
        class="tab-item"
        :class="{ active: currentTab === 'used' }"
        @click="currentTab = 'used'"
      >
        已使用
      </view>
      <view 
        class="tab-item"
        :class="{ active: currentTab === 'expired' }"
        @click="currentTab = 'expired'"
      >
        已过期
      </view>
    </view>
    
    <!-- 券列表 -->
    <scroll-view scroll-y class="coupon-list">
      <view 
        class="coupon-card" 
        :class="[item.status, item.type]"
        v-for="item in filteredCoupons" 
        :key="item.id"
      >
        <!-- 左侧金额 -->
        <view class="coupon-left">
          <view class="coupon-value">
            <text class="currency" v-if="item.value">¥</text>
            <text class="amount">{{ item.value || (item.discount * 10) }}</text>
            <text class="unit" v-if="!item.value">折</text>
          </view>
          <text class="coupon-condition">
            {{ item.minAmount ? `满${item.minAmount}可用` : '无门槛' }}
          </text>
        </view>
        
        <!-- 右侧信息 -->
        <view class="coupon-right">
          <text class="coupon-name">{{ item.name }}</text>
          <text class="coupon-scope">全场通用</text>
          <text class="coupon-expire">{{ item.expireDate }} 到期</text>
        </view>
        
        <!-- 使用按钮 -->
        <view class="coupon-action" v-if="item.status === 'available'">
          <view class="use-btn" @click="useCoupon(item)">去使用</view>
        </view>
        
        <!-- 状态标记 -->
        <view class="status-tag" v-if="item.status !== 'available'">
          {{ item.status === 'used' ? '已使用' : '已过期' }}
        </view>
      </view>
      
      <!-- 空状态 -->
      <view class="empty-state" v-if="filteredCoupons.length === 0">
        <text class="empty-icon">🎫</text>
        <text class="empty-text">暂无{{ tabText }}的优惠券</text>
      </view>
    </scroll-view>
    
    <!-- 兑换入口 -->
    <view class="exchange-entry" @click="goToMall">
      <text class="entry-text">👉 去积分商城兑换更多优惠券</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCouponList } from '@/api/coupon'

const currentTab = ref('available')
const coupons = ref([])

onMounted(async () => {
  try {
    // 不传 status 参数，获取所有状态的券
    const res = await getCouponList()
    console.log('优惠券接口返回:', res)
    
    if (res.code === 200 && res.data) {
      coupons.value = res.data.map(item => {
        // 后端返回状态: ISSUED(可用), USED(已用), EXPIRED(过期)
        // 前端需要的状态: available, used, expired
        let mappedStatus = 'available'
        if (item.status === 'ISSUED' && item.available === true) {
          mappedStatus = 'available'
        } else if (item.status === 'USED') {
          mappedStatus = 'used'
        } else if (item.status === 'EXPIRED' || item.available === false) {
          mappedStatus = 'expired'
        }
        
        return {
          ...item,
          value: item.value || 0,
          name: item.productName || getCouponName(item.couponType),
          expireDate: formatDate(item.expiresAt),
          status: mappedStatus
        }
      })
      console.log('处理后的优惠券:', coupons.value)
    }
  } catch (e) {
    console.error('获取优惠券失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
})

// 根据券类型获取默认名称
const getCouponName = (type) => {
  const map = {
    'EXCHANGE': '咖啡兑换券',
    'DISCOUNT': '折扣券',
    'FULL_REDUCE': '满减券'
  }
  return map[type] || '优惠券'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  } catch {
    return dateStr
  }
}

const availableCount = computed(() => {
  return coupons.value.filter(c => c.status === 'available').length
})

const filteredCoupons = computed(() => {
  return coupons.value.filter(c => c.status === currentTab.value)
})

const tabText = computed(() => {
  const map = { available: '可使用', used: '已使用', expired: '已过期' }
  return map[currentTab.value]
})

const useCoupon = (coupon) => {
  uni.switchTab({ url: '/pages/menu/menu' })
}

const goToMall = () => {
  uni.navigateTo({ url: '/pages/mall/index' })
}
</script>

<style lang="scss" scoped>
.coupon-page {
  min-height: 100vh;
  background: $bg-color;
  display: flex;
  flex-direction: column;
}

// 标签
.tabs {
  display: flex;
  background: $bg-white;
  position: sticky;
  top: 0;
  z-index: 10;
  
  .tab-item {
    flex: 1;
    text-align: center;
    padding: $spacing-md 0;
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
        width: 60rpx;
        height: 4rpx;
        background: $primary-color;
        border-radius: 2rpx;
      }
    }
  }
}

// 券列表
.coupon-list {
  flex: 1;
  padding: $spacing-md;
}

.coupon-card {
  display: flex;
  background: $bg-white;
  border-radius: $border-radius-md;
  margin-bottom: $spacing-md;
  overflow: hidden;
  position: relative;
  
  // 已使用/已过期样式
  &.used, &.expired {
    opacity: 0.6;
    
    .coupon-left {
      background: #ccc;
    }
  }
  
  .coupon-left {
    width: 200rpx;
    background: linear-gradient(135deg, $primary-color, $primary-dark);
    color: white;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: $spacing-md;
    
    .coupon-value {
      display: flex;
      align-items: baseline;
      
      .currency {
        font-size: $font-size-md;
      }
      
      .amount {
        font-size: 56rpx;
        font-weight: 700;
      }
      
      .unit {
        font-size: $font-size-md;
        margin-left: 4rpx;
      }
    }
    
    .coupon-condition {
      font-size: $font-size-xs;
      opacity: 0.8;
      margin-top: $spacing-xs;
    }
  }
  
  .coupon-right {
    flex: 1;
    padding: $spacing-md;
    display: flex;
    flex-direction: column;
    justify-content: center;
    
    .coupon-name {
      font-size: $font-size-md;
      font-weight: 600;
      color: $text-primary;
      margin-bottom: $spacing-xs;
    }
    
    .coupon-scope {
      font-size: $font-size-xs;
      color: $text-placeholder;
      margin-bottom: $spacing-xs;
    }
    
    .coupon-expire {
      font-size: $font-size-xs;
      color: $warning-color;
    }
  }
  
  .coupon-action {
    display: flex;
    align-items: center;
    padding-right: $spacing-md;
    
    .use-btn {
      background: $primary-color;
      color: white;
      padding: $spacing-xs $spacing-md;
      border-radius: 30rpx;
      font-size: $font-size-sm;
    }
  }
  
  .status-tag {
    position: absolute;
    top: 20rpx;
    right: -40rpx;
    background: rgba(0,0,0,0.5);
    color: white;
    padding: 4rpx 50rpx;
    font-size: $font-size-xs;
    transform: rotate(45deg);
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
  
  .empty-icon {
    font-size: 100rpx;
    margin-bottom: $spacing-md;
  }
  
  .empty-text {
    color: $text-placeholder;
  }
}

// 兑换入口
.exchange-entry {
  background: $bg-white;
  padding: $spacing-md;
  text-align: center;
  
  .entry-text {
    color: $primary-color;
    font-size: $font-size-sm;
  }
}
</style>
