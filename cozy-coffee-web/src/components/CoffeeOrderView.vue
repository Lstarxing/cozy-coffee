<template>
  <div class="coffee-order-view">
    <div class="view-header">
      <h3><Coffee class="header-icon" :size="22" :stroke-width="2" /> 菜单</h3>
      <div class="header-actions">
        <span class="points-hint"><Sparkles :size="14" :stroke-width="2" /> 消费享 {{ pointsMultiplier }}倍积分</span>
        <button class="cart-trigger" @click="showCart = true">
          <ShoppingCartIcon :size="20" :stroke-width="2" />
          <span v-if="cartItems.length > 0" class="cart-count">{{ cartItems.length }}</span>
        </button>
      </div>
    </div>

    <!-- 分类标签栏 -->
    <div class="category-tabs">
      <button 
        v-for="cat in categories" 
        :key="cat.value" 
        class="category-tab"
        :class="{ active: activeCategory === cat.value }"
        @click="activeCategory = cat.value"
      >
        <component :is="cat.icon" :size="18" :stroke-width="1.8" class="tab-icon-svg" />
        <span class="tab-name">{{ cat.label }}</span>
      </button>
    </div>

    <div v-if="!isLoading" class="products-grid">
      <div v-if="filteredProducts.length === 0" class="empty-state">
        <Coffee :size="48" class="empty-icon" />
        <p>暂无商品</p>
      </div>

      <div v-for="product in filteredProducts" v-else :key="product.id" class="product-card">
        <div class="product-image-wrapper">
          <img 
            :src="getImageUrl(product.imageUrl)" 
            class="product-img"
            :alt="product.name"
            @error="handleImageError"
          >
          <!-- Plan A: Floating Pill NEW Label -->
          <div v-if="product.isNewProduct" class="new-badge-pill">
             <Sparkles :size="12" fill="currentColor" />
             <span>NEW</span>
          </div>
          <!-- 悬停显示的快速添加按钮 -->
          <button class="quick-add-btn" @click.stop="openCustomizer(product)">
            <Plus :size="20" />
          </button>
        </div>
        
        <div class="product-info">
          <h4>{{ product.name }}</h4>
          <p class="product-desc">{{ product.description }}</p>
          <div class="product-footer">
            <span class="price">¥{{ product.price }}</span>
            <button class="add-btn" @click="openCustomizer(product)">选规格</button>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="loading-state">
      加载中...
    </div>

    <!-- 产品定制器 -->
    <ProductCustomizer
v-if="showCustomizer" :product="selectedProduct" @close="showCustomizer = false"
      @add-to-cart="handleAddToCart" />

    <!-- 购物车 -->
    <ShoppingCart
      ref="cartRef"
      :is-open="showCart"
      :cart-items="cartItems"
      :points-multiplier="pointsMultiplier"
      :upsell-products="upsellProducts"
      @close="showCart = false"
      @update-cart="updateCart"
      @checkout="handleCheckout"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, markRaw } from 'vue'
import { getCoffeeProducts, createOrder } from '@/api/order'
import { getImageUrl, handleImageError } from '@/utils/image'
import { useCart } from '@/composables/useCart'
import ProductCustomizer from './ProductCustomizer.vue'
import ShoppingCart from './cart/ShoppingCart.vue'
import { ElMessage } from 'element-plus'
import {
  Coffee, Sparkles, ShoppingCart as ShoppingCartIcon,
  Tag, Flame, Star, Cake, Plus
} from 'lucide-vue-next'

const props = defineProps({
  userInfo: Object,
  pointsMultiplier: {
    type: Number,
    default: 1
  }
})

const emit = defineEmits(['order-created', 'refresh-user'])

const products = ref([])
const isLoading = ref(false)
const showCustomizer = ref(false)
const selectedProduct = ref(null)
const showCart = ref(false)
const cartRef = ref(null)

const { cartItems, addToCart, clearCart } = useCart()

// 筛选凑单商品 (燕麦曲奇、牛角包)
const upsellProducts = computed(() => {
  const targetNames = ['手工燕麦曲奇', '海盐焦糖牛角包']
  return products.value.filter(p => targetNames.includes(p.name))
})

const updateCart = (newCart) => {
  cartItems.value = newCart
}

// 分类标签 - 使用 Lucide 图标
const categories = [
  { value: 'all', label: '全部', icon: markRaw(Tag) },
  { value: 'espresso', label: '意式咖啡', icon: markRaw(Coffee) },
  { value: 'signature', label: '季节限定', icon: markRaw(Star) },
  { value: 'soe', label: '精品手冲', icon: markRaw(Coffee) },
  { value: 'bakery', label: '烘焙甜品', icon: markRaw(Cake) }
]
const activeCategory = ref('all')

// 筛选后的商品
const filteredProducts = computed(() => {
  // 始终过滤掉 addon 类型的商品（因为它们已经在规格里了）
  let displayProducts = products.value.filter(p => p.category !== 'addon' && p.name !== '额外浓缩')
  
  if (activeCategory.value === 'all') {
    return displayProducts
  }
  return displayProducts.filter(p => p.category === activeCategory.value)
})


const loadProducts = async () => {
  isLoading.value = true
  try {
    const res = await getCoffeeProducts()

    if (res.success) {
      products.value = res.data || []
    } else {
      ElMessage.warning(res.data?.message || res.message || '暂无商品数据')
    }
  } catch (error) {
    console.error('加载商品失败', error)
    ElMessage.error(error.response?.data?.message || error.message || '加载商品失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

const openCustomizer = (product) => {
  selectedProduct.value = product
  showCustomizer.value = true
}

const handleAddToCart = (item) => {
  addToCart(item)
  ElMessage.success('已加入购物车')
}

const handleCheckout = async (orderData) => {
  try {
    // v5.7.2: 正确构造 addonsJson 供后端解析
    const payload = {
      ...orderData,
      items: orderData.items.map(item => {
        // 构造 addonsJson 数组格式
        const addons = []
        if (item.extraPrices?.strength > 0) {
          addons.push({ code: 'EXTRA_SHOT', name: '加浓', price: item.extraPrices.strength })
        }
        if (item.extraPrices?.milk > 0) {
          addons.push({ code: 'SPECIAL_MILK', name: item.milkType || '特殊奶', price: item.extraPrices.milk })
        }
        
        return {
          ...item,
          optionsJson: JSON.stringify({
            milkType: item.milkType,
            extraPrices: item.extraPrices
          }),
          addonsJson: addons.length > 0 ? JSON.stringify(addons) : null
        }
      })
    }
    const res = await createOrder(payload)
    if (res.success) {
      ElMessage.success(res.message || '订单创建成功！')
      clearCart()
      showCart.value = false
      emit('order-created', res.data)
      emit('refresh-user')
    } else {
      ElMessage.error(res.message || '订单创建失败')
      cartRef.value?.resetSubmitting()
    }
  } catch (error) {
    console.error('创建订单失败', error)
    ElMessage.error('下单失败，请稍后重试')
    cartRef.value?.resetSubmitting()
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.coffee-order-view {
  width: 100%;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.view-header h3 {
  margin: 0;
  font-size: 20px;
  color: #5D4037;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.points-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #C69C6D;
  background: #FFF8E1;
  padding: 4px 10px;
  border-radius: 12px;
}

/* 购物车触发按钮优化 */
.cart-trigger {
  position: relative;
  background: #FFF;
  border: 1px solid #E0E0E0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #5D4037;
  transition: all 0.2s;
  box-shadow: none; /*移除原有阴影，保持扁平*/
}

.cart-trigger:hover {
  background: #5D4037;
  color: #FFF;
  border-color: #5D4037;
  box-shadow: 0 4px 12px rgba(93, 64, 55, 0.2);
}

.cart-count {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #D84315;
  color: #fff;
  font-size: 10px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

/* 分类标签栏 - Dribbble 风格 */
.category-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 28px;
  padding: 4px;
  background: rgba(245, 240, 235, 0.6);
  border-radius: 28px;
  width: fit-content;
}

.category-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  border: none;
  font-size: 14px;
  color: #8D6E63;
  cursor: pointer;
  border-radius: 24px;
  transition: all 0.3s ease;
}

.category-tab.active {
  background: #fff;
  color: #5D4037;
  box-shadow: 0 2px 8px rgba(166, 124, 82, 0.15);
  font-weight: 600;
}

.category-tab:hover:not(.active) {
  background: rgba(255, 255, 255, 0.5);
}

/* 商品网格 */
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
}

.product-card {
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(141, 110, 99, 0.05);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  border: 1px solid rgba(0,0,0,0.02);
  display: flex;
  flex-direction: column;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(141, 110, 99, 0.12);
}

.product-image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%; /* 1:1 Aspect Ratio */
  overflow: hidden;
  background: #F9F9F9;
}

.product-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.product-card:hover .product-img {
  transform: scale(1.05);
}

/* Plan A: Floating Pill Style */
.new-badge-pill {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 5;
  background: rgba(230, 126, 34, 0.95); /* Caramel Orange #E67E22 */
  backdrop-filter: blur(4px);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 20px; /* Pill shape */
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 4px 12px rgba(230, 126, 34, 0.3);
  letter-spacing: 0.5px;
  transform: translateZ(0); /* Hardware accel for blur */
}

/* Hover effect for the pill */
.product-card:hover .new-badge-pill {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(230, 126, 34, 0.4);
}

.quick-add-btn {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5D4037;
  cursor: pointer;
  opacity: 0;
  transform: translateY(10px) scale(0.9);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.product-card:hover .quick-add-btn {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.quick-add-btn:hover {
  background: #5D4037;
  color: #fff;
}

.product-info {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-info h4 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #3E2723;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-desc {
  font-size: 12px;
  color: #8D6E63;
  margin: 0 0 16px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.price {
  font-size: 18px;
  font-weight: 700;
  color: #D84315;
}

.add-btn {
  padding: 6px 16px;
  background: #FFF;
  border: 1px solid #5D4037;
  color: #5D4037;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.add-btn:hover {
  background: #5D4037;
  color: #FFF;
}

/* 状态样式 */
.empty-state, .loading-state {
  text-align: center;
  padding: 60px 0;
  color: #8D6E63;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-icon {
  margin-bottom: 16px;
  color: #D7CCC8;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #EFEBE9;
  border-top-color: #5D4037;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
