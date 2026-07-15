<!--
  积分商城页 - 展示可兑换商品
-->
<template>
  <view class="mall-page">
    <!-- 积分余额卡片 -->
    <view class="points-header">
      <view class="points-info">
        <text class="points-label">当前积分</text>
        <text class="points-value">{{ userStore.memberInfo.currentPoints || 0 }}</text>
      </view>
      <view class="points-action" @click="goToHistory">
        <text>积分明细 ></text>
      </view>
    </view>
    
    <!-- 商品列表 -->
    <view class="page-state" v-if="loading && products.length === 0">正在加载积分商品…</view>
    <view class="page-state error" v-else-if="errorMessage">
      <text>{{ errorMessage }}</text>
      <button class="retry-button" @click="loadMallData">重新加载</button>
    </view>
    <view class="page-state" v-else-if="products.length === 0">暂无可兑换商品</view>
    <view class="products-grid" v-else>
      <view 
        class="product-card" 
        v-for="item in products" 
        :key="item.id"
        @click="openRedeemModal(item)"
      >
        <image :src="item.image" class="product-image" mode="aspectFill" />
        <view class="product-info">
          <text class="product-name">{{ item.name }}</text>
          <text class="product-desc">{{ item.desc }}</text>
          <view class="product-footer">
            <view class="product-price">
              <text class="price-value">{{ item.pointsPrice }}</text>
              <text class="price-unit">积分</text>
            </view>
            <view class="redeem-btn" v-if="canRedeem(item)">兑换</view>
            <view class="redeem-btn disabled" v-else>积分不足</view>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 兑换确认弹窗 -->
    <view class="modal-mask" v-if="showRedeemModal" @click="showRedeemModal = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">确认兑换</text>
          <text class="modal-close" @click="showRedeemModal = false">×</text>
        </view>
        <view class="modal-body" v-if="selectedProduct">
          <image :src="selectedProduct.image" class="modal-image" mode="aspectFill" />
          <text class="modal-name">{{ selectedProduct.name }}</text>
          <text class="modal-price">{{ selectedProduct.pointsPrice }} 积分</text>
          <text class="modal-stock">库存: {{ selectedProduct.stock }}</text>
        </view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @click="showRedeemModal = false">取消</view>
          <view class="modal-btn confirm" @click="confirmRedeem">确认兑换</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getPointsProducts } from '@/api/product'
import { redeemPoints, getMemberInfo } from '@/api/member'

const userStore = useUserStore()

const products = ref([])
const showRedeemModal = ref(false)
const selectedProduct = ref(null)
const loading = ref(false)
const errorMessage = ref('')

const loadMallData = async () => {
  loading.value = true
  errorMessage.value = ''
  // 刷新会员积分
  try {
    const memberRes = await getMemberInfo()
    if (memberRes.code === 200) {
      userStore.setMemberInfo(memberRes.data)
    }
  } catch (e) {
    errorMessage.value = e?.message || '会员积分加载失败'
  }
  
  // 加载积分商品
  try {
    const res = await getPointsProducts()
    if (res.code === 200 && res.data) {
      // 适配字段名
      products.value = res.data.map(item => ({
        ...item,
        image: item.imageUrl || item.image || '/static/images/default-product.png',
        desc: item.description || ''
      }))
    }
  } catch (e) {
    products.value = []
    errorMessage.value = e?.message || '积分商品加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadMallData)

const canRedeem = (item) => {
  return (userStore.memberInfo?.currentPoints || 0) >= item.pointsPrice
}

const openRedeemModal = (item) => {
  if (canRedeem(item)) {
    selectedProduct.value = item
    showRedeemModal.value = true
  } else {
    uni.showToast({ title: '积分不足', icon: 'none' })
  }
}

const confirmRedeem = async () => {
  if (!selectedProduct.value || loading.value) return
  
  loading.value = true
  uni.showLoading({ title: '兑换中...' })
  
  try {
    const res = await redeemPoints({
      productId: selectedProduct.value.id,
      quantity: 1
    })
    
    uni.hideLoading()
    showRedeemModal.value = false
    
    if (res.code === 200) {
      uni.showToast({ title: '兑换成功！', icon: 'success' })
      // 刷新会员积分
      const memberRes = await getMemberInfo()
      if (memberRes.code === 200) {
        userStore.setMemberInfo(memberRes.data)
      }
    } else {
      uni.showToast({ title: res.message || '兑换失败', icon: 'none' })
    }
  } catch (e) {
    uni.hideLoading()
    console.error('兑换失败', e)
    uni.showToast({ title: e?.message || '兑换失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goToHistory = () => {
  uni.navigateTo({ url: '/pages/points/history' })
}
</script>

<style lang="scss" scoped>
.mall-page {
  min-height: 100vh;
  background: $bg-color;
  padding: $spacing-md;
}

// 积分头部
.points-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: $cozy-surface-alt;
  padding: $spacing-lg;
  border-radius: $cozy-radius-lg;
  color: white;
  margin-bottom: $spacing-md;
  
  .points-info {
    .points-label {
      font-size: $font-size-sm;
      opacity: 0.8;
      display: block;
    }
    
    .points-value {
      font-size: 56rpx;
      font-weight: 700;
    }
  }
  
  .points-action {
    font-size: $font-size-sm;
    opacity: 0.8;
  }
}

// 商品网格
.products-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
}

.page-state { padding: 100rpx 24rpx; color: $cozy-muted; text-align: center; }
.page-state text { display: block; }
.retry-button { width: 220rpx; margin-top: 24rpx; border-radius: 40rpx; background: $cozy-primary; color: #fff; font-size: 24rpx; }

.product-card {
  background: $bg-white;
  border-radius: $cozy-radius-md;
  overflow: hidden;
  box-shadow: none;
  
  .product-image {
    width: 100%;
    height: 200rpx;
  }
  
  .product-info {
    padding: $spacing-sm;
    
    .product-name {
      font-size: $font-size-md;
      font-weight: 500;
      display: block;
      margin-bottom: 4rpx;
    }
    
    .product-desc {
      font-size: $font-size-xs;
      color: $text-placeholder;
      display: block;
      margin-bottom: $spacing-sm;
    }
    
    .product-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .product-price {
        .price-value {
          font-size: $font-size-lg;
          font-weight: 600;
          color: $primary-color;
        }
        
        .price-unit {
          font-size: $font-size-xs;
          color: $primary-color;
          margin-left: 4rpx;
        }
      }
      
      .redeem-btn {
        background: $primary-color;
        color: white;
        padding: 8rpx 20rpx;
        border-radius: 20rpx;
        font-size: $font-size-sm;
        
        &.disabled {
          background: #ccc;
        }
      }
    }
  }
}

// 弹窗
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

.modal-content {
  width: 80%;
  background: $bg-white;
  border-radius: $cozy-radius-lg;
  overflow: hidden;
  
  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-md;
    border-bottom: 1rpx solid $border-color;
    
    .modal-title {
      font-size: $font-size-lg;
      font-weight: 600;
    }
    
    .modal-close {
      font-size: 48rpx;
      color: $text-placeholder;
    }
  }
  
  .modal-body {
    padding: $spacing-lg;
    text-align: center;
    
    .modal-image {
      width: 200rpx;
      height: 200rpx;
      border-radius: $cozy-radius-md;
      margin-bottom: $spacing-md;
    }
    
    .modal-name {
      font-size: $font-size-lg;
      font-weight: 600;
      display: block;
      margin-bottom: $spacing-xs;
    }
    
    .modal-price {
      font-size: $font-size-xl;
      color: $primary-color;
      font-weight: 600;
      display: block;
      margin-bottom: $spacing-xs;
    }
    
    .modal-stock {
      font-size: $font-size-sm;
      color: $text-placeholder;
    }
  }
  
  .modal-footer {
    display: flex;
    border-top: 1rpx solid $border-color;
    
    .modal-btn {
      flex: 1;
      text-align: center;
      padding: $spacing-md;
      font-size: $font-size-md;
      
      &.cancel {
        color: $text-secondary;
      }
      
      &.confirm {
        background: $primary-color;
        color: white;
      }
    }
  }
}
</style>
