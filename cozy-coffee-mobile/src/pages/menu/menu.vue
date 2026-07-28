<template>
  <view class="menu-page">
    <view class="menu-header">
      <view class="menu-brand-row">
        <view>
          <text class="menu-brand">COZY MENU</text>
          <text class="menu-title cozy-display">今天想喝什么？</text>
        </view>
        <view class="search-entry" @click="goToSearch">
          <view class="search-glyph" />
          <text>搜索</text>
        </view>
      </view>
      <view class="store-row">
        <view class="store-copy">
          <text class="store-name">CozyCoffee 中心店</text>
          <text class="store-meta">固定门店自提 · 预计 15 分钟</text>
        </view>
        <text class="store-status"><text class="status-dot" /> 营业中</text>
      </view>
    </view>

    <view class="menu-body">
      <scroll-view scroll-y class="category-sidebar" :scroll-into-view="`cat-${currentCategoryIndex}`" scroll-with-animation>
        <view
          v-for="(category, index) in categories"
          :id="`cat-${index}`"
          :key="category.id"
          class="category-item"
          :class="{ active: currentCategoryIndex === index }"
          @click="selectCategory(index)"
        >
          <text class="category-name">{{ category.name }}</text>
          <text v-if="categoryCount(category.id)" class="category-count">{{ categoryCount(category.id) }}</text>
        </view>
        <view class="sidebar-spacer" />
      </scroll-view>

      <scroll-view scroll-y class="product-scroll" scroll-with-animation>
        <LoadingState v-if="loading" text="正在准备今日菜单…" />
        <OfflineState v-else-if="offline" @retry="loadMenu" />
        <RetryState v-else-if="errorMessage" :description="errorMessage" @retry="loadMenu" />
        <EmptyState v-else-if="!categories.length" title="今日菜单准备中" description="请稍后刷新看看" action-text="刷新菜单" @action="loadMenu" />

        <view v-else class="product-content">
          <view class="group-heading">
            <view>
              <text class="group-title cozy-display">{{ currentCategory.name }}</text>
              <text class="group-description">{{ categoryDescription(currentCategory.id) }}</text>
            </view>
            <text class="group-count">{{ currentCategory.products.length }} 款</text>
          </view>
          <ProductListItem
            v-for="product in currentCategory.products"
            :key="product.id"
            :product="product"
            :count="productCount(product.id)"
            @select="openSpec"
            @add="openSpec"
          />
          <view class="product-spacer" />
        </view>
      </scroll-view>
    </view>

    <CartSheet
      :visible="cartVisible"
      :items="cartStore.items"
      :total="cartStore.subtotal"
      @close="cartVisible = false"
      @clear="clearCart"
      @edit="editCartLine"
      @decrease="cartStore.decreaseQty"
      @increase="cartStore.increaseQty"
    />
    <CartBar :count="cartStore.totalCount" :total="cartStore.subtotal" @open="cartVisible = true" @checkout="goToCheckout" />
    <ProductSpecSheet :visible="specVisible" :product="activeProduct" :line="editingLine" @close="closeSpec" @confirm="saveSpec" />
  </view>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getMenuData } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { NetworkError } from '@/services/errors/AppError'
import ProductListItem from '@/components/order/ProductListItem.vue'
import ProductSpecSheet from '@/components/order/ProductSpecSheet.vue'
import CartBar from '@/components/order/CartBar.vue'
import CartSheet from '@/components/order/CartSheet.vue'
import LoadingState from '@/components/states/LoadingState.vue'
import EmptyState from '@/components/states/EmptyState.vue'
import RetryState from '@/components/states/RetryState.vue'
import OfflineState from '@/components/states/OfflineState.vue'

const cartStore = useCartStore()
const categories = ref([])
const currentCategoryIndex = ref(0)
const loading = ref(true)
const offline = ref(false)
const errorMessage = ref('')
const cartVisible = ref(false)
const specVisible = ref(false)
const activeProduct = ref({})
const editingLine = ref(null)

const currentCategory = computed(() => categories.value[currentCategoryIndex.value] || { id: '', name: '', products: [] })
const allProducts = computed(() => categories.value.flatMap(category => category.products))

onLoad((options) => {
  pendingCategory = (options && options.category) ? String(options.category).toLowerCase() : ''
  loadMenu()
})
onShow(() => {
  const preset = uni.getStorageSync('cozy_menu_category')
  if (preset) {
    uni.removeStorageSync('cozy_menu_category')
    pendingCategory = String(preset).toLowerCase()
    loadMenu()
    return
  }
  if (!uni.getStorageSync('cozy_open_cart_on_menu')) return
  uni.removeStorageSync('cozy_open_cart_on_menu')
  nextTick(() => { cartVisible.value = cartStore.items.length > 0 })
})

let pendingCategory = ''
async function loadMenu() {
  loading.value = true
  offline.value = false
  errorMessage.value = ''
  try {
    const response = await getMenuData()
    const source = response?.data ?? response
    const products = Array.isArray(source) ? source : []
    const categoryMap = new Map()

    products
      .filter(product => !product.status || String(product.status).toLowerCase() === 'active')
      .forEach(product => {
        const categoryId = String(product.category || 'other').toLowerCase()
        if (!categoryMap.has(categoryId)) {
          categoryMap.set(categoryId, { id: categoryId, name: categoryName(categoryId), products: [] })
        }
        categoryMap.get(categoryId).products.push({
          ...product,
          productId: String(product.id),
          image: product.imageUrl || product.image || '/static/images/default-product.png',
          price: Number(product.price || 0)
        })
      })

    categories.value = Array.from(categoryMap.values())
    if (pendingCategory) {
      const idx = categories.value.findIndex(category => category.id === pendingCategory)
      currentCategoryIndex.value = idx >= 0 ? idx : 0
      pendingCategory = ''
    } else {
      currentCategoryIndex.value = 0
    }
  } catch (error) {
    offline.value = error instanceof NetworkError
    errorMessage.value = error?.message || '菜单加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function categoryName(code) {
  return ({
    espresso: '意式咖啡', coffee: '经典咖啡', latte: '拿铁系列', signature: '季节特调',
    soe: '手冲精品', bakery: '烘焙甜点', dessert: '甜品', addon: '加料', other: '其他'
  })[code] || code
}

function categoryDescription(code) {
  return ({
    espresso: '浓缩基底，直接而有力量', coffee: '熟悉风味，适合日常', latte: '咖啡与奶的平衡',
    signature: '随季节更新的门店表达', soe: '单一产地与清晰风味', bakery: '适合与咖啡一起享用',
    dessert: '轻甜收尾', addon: '为当前饮品增加变化', other: '其他今日供应'
  })[code] || '门店今日供应'
}

function selectCategory(index) {
  currentCategoryIndex.value = index
}

function productCount(productId) {
  return cartStore.items
    .filter(line => String(line.productId) === String(productId))
    .reduce((sum, line) => sum + Number(line.quantity || 0), 0)
}

function categoryCount(categoryId) {
  const ids = new Set(categories.value.find(category => category.id === categoryId)?.products.map(product => String(product.id)) || [])
  return cartStore.items.filter(line => ids.has(String(line.productId))).reduce((sum, line) => sum + Number(line.quantity || 0), 0)
}

function openSpec(product) {
  activeProduct.value = product
  editingLine.value = null
  specVisible.value = true
}

function editCartLine(line) {
  activeProduct.value = allProducts.value.find(product => String(product.id) === String(line.productId)) || line
  editingLine.value = line
  cartVisible.value = false
  specVisible.value = true
}

function closeSpec() {
  specVisible.value = false
  editingLine.value = null
}

function saveSpec(line) {
  const wasEditing = Boolean(editingLine.value?.lineKey)
  if (wasEditing) cartStore.updateOptions(editingLine.value.lineKey, line)
  else cartStore.addItem(line, line.quantity)
  closeSpec()
  try { uni.vibrateShort({ type: 'light' }) } catch (_) {}
  uni.showToast({ title: wasEditing ? '已更新' : '已加入购物车', icon: 'none', duration: 900 })
}

function clearCart() {
  uni.showModal({
    title: '清空购物车',
    content: '确认移除全部已选商品？',
    success: result => {
      if (result.confirm) {
        cartStore.clearCart()
        cartVisible.value = false
      }
    }
  })
}

function goToCheckout() {
  if (!cartStore.totalCount) return
  uni.navigateTo({ url: '/pages/order/confirm' })
}

function goToSearch() { uni.navigateTo({ url: '/pages/search/index' }) }
</script>

<style lang="scss" scoped>
.menu-page { height: 100vh; display: flex; flex-direction: column; overflow: hidden; background: #fff; }
.menu-header { flex: none; padding: 28rpx 28rpx 24rpx; background: $cozy-surface-alt; color: #fff; }
.menu-brand-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 24rpx; }
.menu-brand { display: block; color: $cozy-muted-on-dark; font-size: 18rpx; font-weight: 780; letter-spacing: .14em; }
.menu-title { display: block; margin-top: 10rpx; font-size: 40rpx; }
.search-entry { min-width: 112rpx; height: 70rpx; padding: 0 18rpx; display: flex; align-items: center; justify-content: center; gap: 14rpx; border: 1rpx solid $cozy-border-on-dark; border-radius: $cozy-radius-md; color: #fff; font-size: 22rpx; font-weight: 650; }
.search-glyph { position: relative; width: 21rpx; height: 21rpx; border: 3rpx solid currentColor; border-radius: 50%; }
.search-glyph::after { position: absolute; right: -9rpx; bottom: -6rpx; width: 11rpx; height: 3rpx; border-radius: 2rpx; background: currentColor; content: ''; transform: rotate(45deg); }
.store-row { margin-top: 25rpx; padding-top: 22rpx; display: flex; align-items: flex-end; justify-content: space-between; gap: 20rpx; border-top: 1rpx solid $cozy-border-on-dark; }
.store-copy { min-width: 0; }
.store-name { display: block; overflow: hidden; font-size: 26rpx; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.store-meta { display: block; margin-top: 8rpx; color: $cozy-muted-on-dark; font-size: 20rpx; }
.store-status { flex: none; display: flex; align-items: center; gap: 8rpx; color: #dbe7d4; font-size: 20rpx; font-weight: 650; }
.status-dot { width: 10rpx; height: 10rpx; border-radius: 50%; background: #9fbc8f; }

.menu-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }

.category-sidebar { flex: none; width: 180rpx; height: 100%; background: $cozy-surface; }
.sidebar-spacer { height: 120rpx; }
.category-item { position: relative; min-height: 112rpx; padding: 18rpx 14rpx; display: flex; align-items: center; justify-content: center; color: $cozy-muted; font-size: 24rpx; text-align: center; box-sizing: border-box; }
.category-item.active { background: #fff; color: $cozy-ink; font-weight: 700; }
.category-item.active::before { position: absolute; left: 0; top: 36rpx; bottom: 36rpx; width: 6rpx; border-radius: 0 999rpx 999rpx 0; background: $cozy-primary; content: ''; }
.category-name { line-height: 1.35; }
.category-count { position: absolute; top: 10rpx; right: 10rpx; min-width: 30rpx; height: 30rpx; padding: 0 5rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: $cozy-primary; color: #fff; font-size: 18rpx; box-sizing: border-box; }

.product-scroll { min-width: 0; flex: 1; height: 100%; background: #fff; }
.product-content { padding: 0 26rpx; }
.group-heading { padding: 28rpx 0 16rpx; display: flex; align-items: flex-end; justify-content: space-between; gap: 20rpx; }
.group-title { display: block; color: $cozy-ink; font-size: 36rpx; }
.group-description { display: block; margin-top: 7rpx; color: $cozy-muted; font-size: 19rpx; }
.group-count { flex: none; padding-bottom: 2rpx; color: $cozy-muted; font-size: 20rpx; }
.product-spacer { height: 220rpx; }
</style>
