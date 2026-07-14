<template>
  <view class="menu-page">
    <view class="store-strip">
      <view class="store-copy">
        <view class="store-line">
          <text class="store-name">CozyCoffee 中心店</text>
          <text class="store-status">营业中</text>
        </view>
        <text class="store-meta">到店自提 · 预计 15 分钟</text>
      </view>
      <view class="search-entry" @click="goToSearch">搜索</view>
    </view>

    <view class="menu-container">
      <scroll-view scroll-y class="category-sidebar" :scroll-into-view="`cat-${currentCategoryIndex}`">
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

      <scroll-view scroll-y class="product-scroll" :scroll-into-view="scrollTarget" scroll-with-animation>
        <LoadingState v-if="loading" text="正在准备菜单…" />
        <OfflineState v-else-if="offline" @retry="loadMenu" />
        <RetryState v-else-if="errorMessage" :description="errorMessage" @retry="loadMenu" />
        <EmptyState v-else-if="!categories.length" title="今日菜单准备中" description="请稍后刷新看看" action-text="刷新菜单" @action="loadMenu" />

        <view v-else class="product-content">
          <view class="menu-notice">
            <text class="notice-title">高效点单</text>
            <text class="notice-copy">选择商品后直接配置规格，无需跳转详情页</text>
          </view>

          <view v-for="(category, index) in categories" :id="`products-${index}`" :key="category.id" class="product-group">
            <view class="group-heading">
              <text class="group-title">{{ category.name }}</text>
              <text class="group-count">{{ category.products.length }} 款</text>
            </view>
            <ProductListItem
              v-for="product in category.products"
              :key="product.id"
              :product="product"
              :count="productCount(product.id)"
              @select="openSpec"
              @add="openSpec"
            />
          </view>
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
import { onLoad } from '@dcloudio/uni-app'
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
const scrollTarget = ref('')
const loading = ref(true)
const offline = ref(false)
const errorMessage = ref('')
const cartVisible = ref(false)
const specVisible = ref(false)
const activeProduct = ref({})
const editingLine = ref(null)

const allProducts = computed(() => categories.value.flatMap(category => category.products))

onLoad(loadMenu)

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

function selectCategory(index) {
  currentCategoryIndex.value = index
  scrollTarget.value = ''
  nextTick(() => { scrollTarget.value = `products-${index}` })
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
.store-strip { min-height: 116rpx; padding: 20rpx 28rpx; display: flex; align-items: center; gap: 20rpx; border-bottom: 1rpx solid $cozy-border; background: #fff; box-sizing: border-box; }
.store-copy { min-width: 0; flex: 1; }
.store-line { display: flex; align-items: center; gap: 12rpx; }
.store-name { overflow: hidden; color: $cozy-ink; font-size: 30rpx; font-weight: 750; white-space: nowrap; text-overflow: ellipsis; }
.store-status { padding: 4rpx 10rpx; border-radius: 999rpx; background: $cozy-accent-soft; color: $cozy-accent; font-size: 19rpx; font-weight: 650; }
.store-meta { display: block; margin-top: 8rpx; color: $cozy-muted; font-size: 22rpx; }
.search-entry { min-width: 104rpx; height: 72rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: $cozy-surface; color: $cozy-ink; font-size: 24rpx; }
.menu-container { min-height: 0; flex: 1; display: flex; overflow: hidden; }
.category-sidebar { width: 172rpx; height: 100%; flex: none; background: $cozy-surface; }
.category-item { position: relative; min-height: 104rpx; padding: 18rpx 18rpx 18rpx 24rpx; display: flex; align-items: center; justify-content: center; color: $cozy-muted; font-size: 24rpx; text-align: center; box-sizing: border-box; }
.category-item.active { background: #fff; color: $cozy-ink; font-weight: 700; }
.category-item.active::before { position: absolute; left: 0; top: 34rpx; bottom: 34rpx; width: 7rpx; border-radius: 0 999rpx 999rpx 0; background: $cozy-primary; content: ''; }
.category-name { line-height: 1.35; }
.category-count { position: absolute; top: 10rpx; right: 8rpx; min-width: 30rpx; height: 30rpx; padding: 0 5rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: $cozy-primary; color: #fff; font-size: 18rpx; box-sizing: border-box; }
.sidebar-spacer { height: 220rpx; }
.product-scroll { min-width: 0; flex: 1; height: 100%; background: #fff; }
.product-content { padding: 0 26rpx; }
.menu-notice { margin: 24rpx 0 8rpx; padding: 24rpx; border-radius: $cozy-radius-md; background: $cozy-surface-alt; }
.notice-title { display: block; color: #fff; font-size: 28rpx; font-weight: 700; }
.notice-copy { display: block; margin-top: 7rpx; color: $cozy-muted-on-dark; font-size: 21rpx; line-height: 1.45; }
.product-group { scroll-margin-top: 0; }
.group-heading { position: sticky; top: 0; z-index: 5; display: flex; align-items: baseline; justify-content: space-between; padding: 28rpx 0 12rpx; background: #fff; }
.group-title { color: $cozy-ink; font-size: 30rpx; font-weight: 750; }
.group-count { color: $cozy-muted; font-size: 21rpx; }
.product-spacer { height: 220rpx; }
</style>
