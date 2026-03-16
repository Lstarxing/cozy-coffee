<!--
  商品详情页 - 规格选择（杯型、温度、加料）
-->
<template>
  <view class="detail-page" v-if="product">
    <!-- 商品图片 -->
    <image :src="product.image" class="product-hero" mode="aspectFill" />
    
    <!-- 商品信息 -->
    <view class="product-info">
      <text class="product-name">{{ product.name }}</text>
      <text class="product-desc">{{ product.description }}</text>
      <view class="product-price-row">
        <text class="current-price">¥{{ finalPrice }}</text>
        <text class="original-price" v-if="hasAddons">原价 ¥{{ product.price }}</text>
      </view>
    </view>
    
    <!-- 规格选择 -->
    <view class="spec-section">
      <!-- 杯型 -->
      <view class="spec-group">
        <text class="spec-title">杯型</text>
        <view class="spec-options">
          <view 
            class="spec-option"
            :class="{ active: selectedSize === 'medium' }"
            @click="selectedSize = 'medium'"
          >
            <text class="option-name">中杯</text>
            <text class="option-price">标准</text>
          </view>
          <view 
            class="spec-option"
            :class="{ active: selectedSize === 'large' }"
            @click="selectedSize = 'large'"
          >
            <text class="option-name">大杯</text>
            <text class="option-price">+4元</text>
          </view>
        </view>
      </view>
      
      <!-- 温度 -->
      <view class="spec-group">
        <text class="spec-title">温度</text>
        <view class="spec-options">
          <view 
            class="spec-option"
            :class="{ active: selectedTemp === 'hot' }"
            @click="selectedTemp = 'hot'"
          >
            <text class="option-name">热</text>
          </view>
          <view 
            class="spec-option"
            :class="{ active: selectedTemp === 'ice' }"
            @click="selectedTemp = 'ice'"
          >
            <text class="option-name">冰</text>
          </view>
          <view 
            class="spec-option"
            :class="{ active: selectedTemp === 'warm' }"
            @click="selectedTemp = 'warm'"
          >
            <text class="option-name">温</text>
          </view>
        </view>
      </view>
      
      <!-- 糖度 -->
      <view class="spec-group">
        <text class="spec-title">糖度</text>
        <view class="spec-options">
          <view 
            class="spec-option"
            :class="{ active: selectedSugar === 'normal' }"
            @click="selectedSugar = 'normal'"
          >
            <text class="option-name">正常糖</text>
          </view>
          <view 
            class="spec-option"
            :class="{ active: selectedSugar === 'half' }"
            @click="selectedSugar = 'half'"
          >
            <text class="option-name">半糖</text>
          </view>
          <view 
            class="spec-option"
            :class="{ active: selectedSugar === 'less' }"
            @click="selectedSugar = 'less'"
          >
            <text class="option-name">少糖</text>
          </view>
          <view 
            class="spec-option"
            :class="{ active: selectedSugar === 'none' }"
            @click="selectedSugar = 'none'"
          >
            <text class="option-name">无糖</text>
          </view>
        </view>
      </view>
      
      <!-- 加料 -->
      <view class="spec-group">
        <text class="spec-title">加料（可多选）</text>
        <view class="spec-options">
          <view 
            class="spec-option addon"
            :class="{ active: selectedAddons.includes('pearl') }"
            @click="toggleAddon('pearl')"
          >
            <text class="option-name">珍珠</text>
            <text class="option-price">+3元</text>
          </view>
          <view 
            class="spec-option addon"
            :class="{ active: selectedAddons.includes('coconut') }"
            @click="toggleAddon('coconut')"
          >
            <text class="option-name">椰果</text>
            <text class="option-price">+3元</text>
          </view>
          <view 
            class="spec-option addon"
            :class="{ active: selectedAddons.includes('pudding') }"
            @click="toggleAddon('pudding')"
          >
            <text class="option-name">布丁</text>
            <text class="option-price">+4元</text>
          </view>
          <view 
            class="spec-option addon"
            :class="{ active: selectedAddons.includes('cream') }"
            @click="toggleAddon('cream')"
          >
            <text class="option-name">奶盖</text>
            <text class="option-price">+5元</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 数量选择 -->
    <view class="quantity-section">
      <text class="quantity-label">数量</text>
      <view class="quantity-control">
        <view class="qty-btn" @click="decreaseQty">-</view>
        <text class="qty-value">{{ quantity }}</text>
        <view class="qty-btn" @click="increaseQty">+</view>
      </view>
    </view>
    
    <!-- 底部操作栏 -->
    <view class="action-bar safe-area-bottom">
      <view class="action-left">
        <text class="total-label">合计</text>
        <text class="total-price">¥{{ totalPrice }}</text>
      </view>
      <view class="action-btn" @click="addToCart">加入购物车</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useCartStore } from '@/stores/cart'
import { getProductDetail } from '@/api/product'

const cartStore = useCartStore()

const product = ref(null)
const productId = ref(null)

// 规格选择
const selectedSize = ref('medium')
const selectedTemp = ref('hot')
const selectedSugar = ref('normal')
const selectedAddons = ref([])
const quantity = ref(1)

// 加料价格映射
const addonPrices = {
  pearl: 3,
  coconut: 3,
  pudding: 4,
  cream: 5
}

// 杯型加价
const sizePrices = {
  medium: 0,
  large: 4
}

onLoad((options) => {
  if (options.id) {
    productId.value = options.id
    loadProduct()
  }
})

const loadProduct = async () => {
  try {
    const res = await getProductDetail(productId.value)
    if (res.code === 200 && res.data) {
      const data = res.data
      product.value = {
        ...data,
        image: data.imageUrl || data.image || '/static/images/default-product.png',
        price: data.price || 0
      }
    }
  } catch (e) {
    console.error('加载商品详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

// 是否有加料
const hasAddons = computed(() => {
  return selectedAddons.value.length > 0 || selectedSize.value === 'large'
})

// 单杯最终价格
const finalPrice = computed(() => {
  if (!product.value) return 0
  let price = parseFloat(product.value.price)
  
  // 杯型加价
  price += sizePrices[selectedSize.value]
  
  // 加料总价
  selectedAddons.value.forEach(addon => {
    price += addonPrices[addon] || 0
  })
  
  return price.toFixed(0)
})

// 总价（单价 × 数量）
const totalPrice = computed(() => {
  return (parseFloat(finalPrice.value) * quantity.value).toFixed(2)
})

// 切换加料
const toggleAddon = (addon) => {
  const index = selectedAddons.value.indexOf(addon)
  if (index === -1) {
    selectedAddons.value.push(addon)
  } else {
    selectedAddons.value.splice(index, 1)
  }
}

// 增减数量
const increaseQty = () => { quantity.value++ }
const decreaseQty = () => { if (quantity.value > 1) quantity.value-- }

// 加入购物车
const addToCart = () => {
  if (!product.value) return
  
  // 生成规格描述
  const sizeText = selectedSize.value === 'large' ? '大杯' : '中杯'
  const tempText = { hot: '热', ice: '冰', warm: '温' }[selectedTemp.value]
  const sugarText = { normal: '正常糖', half: '半糖', less: '少糖', none: '无糖' }[selectedSugar.value]
  const addonNames = { pearl: '珍珠', coconut: '椰果', pudding: '布丁', cream: '奶盖' }
  const addonsText = selectedAddons.value.map(a => addonNames[a]).join('+')
  
  const specText = `${sizeText}/${tempText}/${sugarText}${addonsText ? '/' + addonsText : ''}`
  
  // 添加到购物车（这里简化处理，实际应该带规格信息）
  for (let i = 0; i < quantity.value; i++) {
    cartStore.addItem({
      id: `${product.value.id}_${Date.now()}_${i}`, // 带规格的唯一ID
      name: `${product.value.name}(${specText})`,
      price: finalPrice.value,
      image: product.value.image
    })
  }
  
  uni.showToast({
    title: `已加入${quantity.value}件`,
    icon: 'success'
  })
  
  setTimeout(() => {
    uni.navigateBack()
  }, 1000)
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: $bg-color;
  padding-bottom: 140rpx;
}

// 商品大图
.product-hero {
  width: 100%;
  height: 500rpx;
}

// 商品信息
.product-info {
  background: $bg-white;
  padding: $spacing-lg;
  margin-bottom: $spacing-sm;
  
  .product-name {
    font-size: $font-size-xl;
    font-weight: 600;
    color: $text-primary;
    display: block;
    margin-bottom: $spacing-xs;
  }
  
  .product-desc {
    font-size: $font-size-sm;
    color: $text-secondary;
    display: block;
    margin-bottom: $spacing-md;
    line-height: 1.5;
  }
  
  .product-price-row {
    display: flex;
    align-items: baseline;
    
    .current-price {
      font-size: $font-size-xxl;
      font-weight: 700;
      color: $primary-color;
    }
    
    .original-price {
      font-size: $font-size-sm;
      color: $text-placeholder;
      margin-left: $spacing-md;
      text-decoration: line-through;
    }
  }
}

// 规格选择
.spec-section {
  background: $bg-white;
  padding: $spacing-md $spacing-lg;
  margin-bottom: $spacing-sm;
}

.spec-group {
  margin-bottom: $spacing-lg;
  
  &:last-child {
    margin-bottom: 0;
  }
  
  .spec-title {
    font-size: $font-size-md;
    font-weight: 600;
    color: $text-primary;
    display: block;
    margin-bottom: $spacing-sm;
  }
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.spec-option {
  min-width: 120rpx;
  padding: $spacing-sm $spacing-md;
  border: 2rpx solid $border-color;
  border-radius: $border-radius-md;
  text-align: center;
  transition: all 0.2s;
  
  .option-name {
    font-size: $font-size-md;
    color: $text-primary;
    display: block;
  }
  
  .option-price {
    font-size: $font-size-xs;
    color: $text-placeholder;
    display: block;
    margin-top: 4rpx;
  }
  
  &.active {
    border-color: $primary-color;
    background: rgba($primary-color, 0.1);
    
    .option-name {
      color: $primary-color;
    }
  }
  
  &.addon.active {
    background: $primary-color;
    
    .option-name, .option-price {
      color: white;
    }
  }
}

// 数量选择
.quantity-section {
  background: $bg-white;
  padding: $spacing-md $spacing-lg;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .quantity-label {
    font-size: $font-size-md;
    font-weight: 600;
    color: $text-primary;
  }
}

.quantity-control {
  display: flex;
  align-items: center;
  
  .qty-btn {
    width: 56rpx;
    height: 56rpx;
    border: 2rpx solid $border-color;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32rpx;
    color: $text-secondary;
    
    &:active {
      background: $bg-gray;
    }
  }
  
  .qty-value {
    width: 80rpx;
    text-align: center;
    font-size: $font-size-lg;
    font-weight: 600;
  }
}

// 底部操作栏
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  background: $bg-white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $spacing-lg;
  box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.05);
  
  .action-left {
    .total-label {
      font-size: $font-size-sm;
      color: $text-secondary;
      margin-right: $spacing-xs;
    }
    
    .total-price {
      font-size: $font-size-xl;
      font-weight: 700;
      color: $primary-color;
    }
  }
  
  .action-btn {
    background: linear-gradient(135deg, $primary-color, $primary-dark);
    color: white;
    padding: $spacing-sm $spacing-xl;
    border-radius: 40rpx;
    font-size: $font-size-md;
    font-weight: 600;
  }
}
</style>
