<!--
  下单成功页 - 展示订单结果和积分获得
-->
<template>
  <view class="result-page">
    <!-- 成功图标 -->
    <view class="success-icon">
      <text class="icon">✓</text>
    </view>
    
    <!-- 成功提示 -->
    <text class="success-title">下单成功</text>
    <text class="success-subtitle">您的订单已提交，请耐心等待</text>
    
    <!-- 订单信息卡片 -->
    <view class="order-card">
      <view class="info-item">
        <text class="info-label">订单编号</text>
        <text class="info-value">{{ orderNo }}</text>
      </view>
      <view class="info-item highlight">
        <text class="info-label">订单金额</text>
        <text class="info-value price">¥{{ totalPrice }}</text>
      </view>
      <view class="info-item" v-if="pickupCode">
        <text class="info-label">取餐码</text>
        <text class="info-value pickup-code">{{ pickupCode }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">预计完成</text>
        <text class="info-value">约 15 分钟</text>
      </view>
    </view>
    
    <!-- 积分预告卡片 -->
    <view class="points-card">
      <view class="points-header">
        <text class="points-icon">🎁</text>
        <text class="points-label">订单完成后可获得</text>
      </view>
      <text class="points-value">+{{ earnedPoints }} 积分</text>
      <text class="points-hint">💡 积分将在订单制作完成后自动发放</text>
    </view>
    
    <!-- 操作按钮 -->
    <view class="action-buttons">
      <view class="btn-outline" @click="goToOrders">查看订单</view>
      <view class="btn-primary" @click="goToHome">返回首页</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const earnedPoints = ref(0)
const totalPrice = ref('0.00')
const orderNo = ref('')
const pickupCode = ref('')

// 页面加载时获取参数
onLoad((options) => {
  console.log('订单结果页参数:', options)
  if (options.points) {
    earnedPoints.value = options.points
  }
  if (options.total) {
    totalPrice.value = options.total
  }
  if (options.orderNo) {
    orderNo.value = options.orderNo
  }
  if (options.pickupCode) {
    pickupCode.value = options.pickupCode
  }
})

const goToOrders = () => {
  uni.switchTab({ url: '/pages/order/order' })
}

const goToHome = () => {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.result-page {
  min-height: 100vh;
  background: $bg-color;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx $spacing-lg;
}

// 成功图标
.success-icon {
  width: 160rpx;
  height: 160rpx;
  background: linear-gradient(135deg, $success-color, #73D13D);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-lg;
  
  .icon {
    font-size: 80rpx;
    color: white;
    font-weight: bold;
  }
}

.success-title {
  font-size: $font-size-xxl;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $spacing-sm;
}

.success-subtitle {
  font-size: $font-size-md;
  color: $text-secondary;
  margin-bottom: $spacing-xl;
}

// 积分卡片
.points-card {
  background: linear-gradient(135deg, #FFF9F0, #FFF5E6);
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  width: 100%;
  text-align: center;
  margin-bottom: $spacing-lg;
  
  .points-header {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: $spacing-sm;
    
    .points-icon {
      font-size: 40rpx;
      margin-right: $spacing-sm;
    }
    
    .points-label {
      font-size: $font-size-md;
      color: $text-secondary;
    }
  }
  
  .points-value {
    font-size: 72rpx;
    font-weight: 700;
    color: $primary-color;
    display: block;
    margin-bottom: $spacing-sm;
  }
  
  .points-hint {
    font-size: $font-size-sm;
    color: $text-placeholder;
  }
}

// 订单信息
.order-info {
  background: $bg-white;
  border-radius: $border-radius-md;
  padding: $spacing-md;
  width: 100%;
  margin-bottom: $spacing-xl;
  
  .info-item {
    display: flex;
    justify-content: space-between;
    padding: $spacing-sm 0;
    
    .info-label {
      color: $text-secondary;
    }
    
    .info-value {
      color: $text-primary;
      font-weight: 500;
    }
  }
}

// 操作按钮
.action-buttons {
  display: flex;
  gap: $spacing-md;
  width: 100%;
  
  .btn-outline {
    flex: 1;
    text-align: center;
    padding: $spacing-md;
    border: 2rpx solid $primary-color;
    color: $primary-color;
    border-radius: 40rpx;
    font-size: $font-size-md;
  }
  
  .btn-primary {
    flex: 1;
    text-align: center;
    padding: $spacing-md;
    background: linear-gradient(135deg, $primary-color, $primary-dark);
    color: white;
    border-radius: 40rpx;
    font-size: $font-size-md;
    font-weight: 600;
  }
}
</style>
