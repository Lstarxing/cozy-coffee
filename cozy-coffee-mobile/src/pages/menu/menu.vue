<template>
  <view class="menu-page">
    <!-- 自定义导航栏 -->
    <view class="menu-nav" :style="{ paddingTop: statusBarHeight + 'px', paddingRight: navRight + 'px' }">
      <view class="nav-inner">
        <view class="nav-capsule">
          <view class="nav-capsule__icon" @click="switchToTab('/pages/index/index')">
            <CozyIcon name="house" size="20" />
          </view>
          <view class="nav-capsule__divider" />
          <view class="nav-capsule__icon" @click="goToSearch">
            <CozyIcon name="search" size="20" />
          </view>
        </view>
      </view>
    </view>

    <!-- 门店信息 -->
    <view class="store-info">
      <view class="store-info__row">
        <view class="store-info__left">
          <text class="store-fav">☆</text>
          <text class="store-name">{{ fixedStore.name }}</text>
        </view>
        <view class="pickup-switch">
          <view class="pickup-opt active"><text>自提</text></view>
          <view class="pickup-opt"><text>配送</text></view>
        </view>
      </view>
      <view class="store-info__meta">
        <text class="store-distance">距离您 1.2km</text>
      </view>
      <view class="store-info__footer">
        <text class="store-status"><text class="status-icon">☼</text> 精品咖啡每日新鲜烘焙</text>
        <text class="store-more">更多 ↓</text>
      </view>
    </view>

    <!-- 菜单 -->
    <view class="menu-body">
      <scroll-view scroll-y class="category-sidebar">
        <view
          v-for="(category, index) in categories"
          :key="category.id"
          class="category-item"
          :class="{ active: currentCategoryIndex === index }"
          @click="selectCategory(index)"
        >
          <text class="category-name">{{ category.name }}</text>
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
import { computed, nextTick, onMounted, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { FIXED_STORE } from '@/config/store'
import { getMenuData } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { NetworkError } from '@/services/errors/AppError'
import ProductListItem from '@/components/order/ProductListItem.vue'
import ProductSpecSheet from '@/components/order/ProductSpecSheet.vue'
import CartBar from '@/components/order/CartBar.vue'
import CartSheet from '@/components/order/CartSheet.vue'
import CozyIcon from '@/components/CozyIcon.vue'
import LoadingState from '@/components/states/LoadingState.vue'
import EmptyState from '@/components/states/EmptyState.vue'
import RetryState from '@/components/states/RetryState.vue'
import OfflineState from '@/components/states/OfflineState.vue'

const cartStore = useCartStore()
const fixedStore = FIXED_STORE
const categories = ref([])
const currentCategoryIndex = ref(0)
const loading = ref(true)
const offline = ref(false)
const errorMessage = ref('')
const cartVisible = ref(false)
const specVisible = ref(false)
const activeProduct = ref({})
const editingLine = ref(null)
const statusBarHeight = ref(20)
const navRight = ref(16)

const currentCategory = computed(() => categories.value[currentCategoryIndex.value] || { id: '', name: '', products: [] })
const allProducts = computed(() => categories.value.flatMap(c => c.products))

onMounted(() => {
  const info = uni.getSystemInfoSync()
  statusBarHeight.value = info.statusBarHeight || 20
  // 避免 getMenuButtonBoundingClientRect 在某些环境下挂起；用安全宽度兜底
  navRight.value = 100
})

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
      .filter(p => !p.status || String(p.status).toLowerCase() === 'active')
      .forEach(p => {
        const cid = String(p.category || 'other').toLowerCase()
        if (!categoryMap.has(cid)) {
          categoryMap.set(cid, { id: cid, name: categoryName(cid), products: [] })
        }
        categoryMap.get(cid).products.push({
          ...p,
          productId: String(p.id),
          image: p.imageUrl || p.image || '/static/images/default-product.png',
          price: Number(p.price || 0)
        })
      })

    categories.value = Array.from(categoryMap.values())
    if (pendingCategory) {
      const idx = categories.value.findIndex(c => c.id === pendingCategory)
      currentCategoryIndex.value = idx >= 0 ? idx : 0
      pendingCategory = ''
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

function selectCategory(index) { currentCategoryIndex.value = index }

function productCount(productId) {
  return cartStore.items
    .filter(l => String(l.productId) === String(productId))
    .reduce((sum, l) => sum + Number(l.quantity || 0), 0)
}

function openSpec(p) { activeProduct.value = p; editingLine.value = null; specVisible.value = true }
function editCartLine(line) {
  activeProduct.value = allProducts.value.find(p => String(p.id) === String(line.productId)) || line
  editingLine.value = line; cartVisible.value = false; specVisible.value = true
}
function closeSpec() { specVisible.value = false; editingLine.value = null }
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
    title: '清空购物车', content: '确认移除全部已选商品？',
    success: r => { if (r.confirm) { cartStore.clearCart(); cartVisible.value = false } }
  })
}
function goToCheckout() { if (cartStore.totalCount) uni.navigateTo({ url: '/pages/order/confirm' }) }
function switchToTab(url) { uni.switchTab({ url }) }
function goToSearch() { uni.navigateTo({ url: '/pages/search/index' }) }
</script>

<style lang="scss" scoped>
.menu-page { height: 100vh; display: flex; flex-direction: column; overflow: hidden; background: $cozy-bg; }

// ── 导航栏 ──
.menu-nav { flex: none; background: #fff; }
.nav-inner { height: 56px; padding: 0 20rpx; display: flex; align-items: center; }
.nav-capsule { display: flex; align-items: center; height: 36px; padding: 0 4px; border: 1rpx solid #E8E4DE; border-radius: 24px; background: #fff; }
.nav-capsule__icon { width: 36px; height: 32px; display: flex; align-items: center; justify-content: center; }
.nav-capsule__divider { width: 1px; height: 18px; background: #E8E4DE; }

// ── 门店信息 ──
.store-info { flex: none; padding: 12rpx 28rpx 20rpx; background: #fff; border-bottom: 1rpx solid $cozy-border; }
.store-info__row { display: flex; align-items: center; justify-content: space-between; }
.store-info__left { display: flex; align-items: center; gap: 8rpx; }
.store-fav { color: $cozy-primary; font-size: 26rpx; }
.store-name { color: $cozy-ink; font-size: 26rpx; font-weight: 650; }
.pickup-switch { display: flex; border-radius: 8rpx; overflow: hidden; border: 1rpx solid $cozy-border; }
.pickup-opt { padding: 8rpx 20rpx; font-size: 20rpx; color: $cozy-muted; background: $cozy-surface; }
.pickup-opt.active { background: $cozy-primary; color: #fff; }
.store-info__meta { margin-top: 8rpx; }
.store-distance { color: $cozy-muted; font-size: 19rpx; }
.store-info__footer { margin-top: 8rpx; display: flex; align-items: center; justify-content: space-between; }
.store-status { color: $cozy-muted; font-size: 19rpx; }
.status-icon { color: $cozy-accent; }
.store-more { color: $cozy-muted; font-size: 19rpx; }

// ── 分类 + 商品 ──
.menu-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }
.category-sidebar { flex: none; width: 180rpx; height: 100%; background: $cozy-surface; }
.sidebar-spacer { height: 120rpx; }
.category-item { position: relative; min-height: 96rpx; padding: 14rpx; display: flex; align-items: center; justify-content: center; color: $cozy-muted; font-size: 24rpx; text-align: center; box-sizing: border-box; }
.category-item.active { background: #fff; color: $cozy-primary; font-weight: 700; }
.category-item.active::before { position: absolute; left: 0; top: 28rpx; bottom: 28rpx; width: 6rpx; border-radius: 0 999rpx 999rpx 0; background: $cozy-primary; content: ''; }

.product-scroll { min-width: 0; flex: 1; height: 100%; background: #fff; }
.product-content { padding: 0 26rpx; }
.group-heading { padding: 28rpx 0 16rpx; display: flex; align-items: flex-end; justify-content: space-between; gap: 20rpx; }
.group-title { display: block; color: $cozy-ink; font-size: 36rpx; }
.group-description { display: block; margin-top: 7rpx; color: $cozy-muted; font-size: 19rpx; }
.group-count { flex: none; padding-bottom: 2rpx; color: $cozy-muted; font-size: 20rpx; }
.product-spacer { height: 220rpx; }
</style>
