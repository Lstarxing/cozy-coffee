<!--
  点单页 - 复现 prototype/menu.html
  导航胶囊 + 门店信息（详情抽屉）+ 左分类栏（scroll-spy 联动）+ 连续滚动商品区（吸顶标题）+ 沉浸式商品详情 + 底部购物车
-->
<template>
  <view class="menu-page">
    <!-- 自定义导航栏（胶囊保留） -->
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
      <view class="store-row">
        <view class="store-left">
          <text class="store-fav">☆</text>
          <text class="store-name">{{ fixedStore.name }}</text>
        </view>
        <view class="pickup-switch">
          <view class="pickup-opt" :class="{ active: fulfillment === 'pickup' }" @click="fulfillment = 'pickup'">自提</view>
          <view class="pickup-opt" :class="{ active: fulfillment === 'delivery' }" @click="fulfillment = 'delivery'">外送</view>
        </view>
      </view>
      <view class="store-meta">距离您 1.2km</view>
      <view class="store-foot">
        <text class="store-status"><text class="status-icon">☼</text> 精品咖啡每日新鲜烘焙</text>
        <text class="store-more" @click="openStoreDetail">详情 ›</text>
      </view>
    </view>

    <!-- 主体：左分类栏 + 右商品区 -->
    <view class="menu-body">
      <!-- 左分类栏 -->
      <scroll-view scroll-y class="category-sidebar" :show-scrollbar="false">
        <view
          v-for="(category, index) in categories"
          :key="category.id"
          class="category-item"
          :class="{ active: currentCategoryIndex === index }"
          @click="selectCategory(index)"
        >
          <text class="category-name">{{ category.name }}</text>
          <text v-if="categoryCount(category.id) > 0" class="cat-count">{{ categoryCount(category.id) }}</text>
        </view>
      </scroll-view>

      <!-- 右商品区：连续滚动 -->
      <scroll-view
        scroll-y
        class="product-scroll"
        :show-scrollbar="false"
        :scroll-into-view="scrollIntoView"
        scroll-with-animation
        @scroll="onProductScroll"
      >
        <LoadingState v-if="loading" text="正在准备今日菜单…" />
        <OfflineState v-else-if="offline" @retry="loadMenu" />
        <RetryState v-else-if="errorMessage" :description="errorMessage" @retry="loadMenu" />
        <EmptyState v-else-if="!categories.length" title="今日菜单准备中" description="请稍后刷新看看" action-text="刷新菜单" @action="loadMenu" />

        <view v-else class="product-content">
          <view
            v-for="(category, index) in categories"
            :key="category.id"
            :id="'cat-' + index"
            class="category-section"
          >
            <view class="group-heading">
              <text class="group-title">{{ category.name }}</text>
              <text class="group-sub">{{ category.en }}</text>
            </view>
            <view v-for="product in category.products" :key="product.id" class="product-item" @click="openDetail(product)">
              <image :src="product.image" class="product-image" mode="aspectFill" />
              <view class="product-info">
                <view class="product-title-row">
                  <text class="product-name">{{ product.name }}</text>
                  <text v-if="product.isNewProduct" class="product-tag">新品</text>
                </view>
                <text class="product-desc">{{ product.description || '门店现制' }}</text>
                <text v-if="product.sold" class="product-extra">月售 {{ product.sold }} · 好评 {{ product.praise }}%</text>
                <view class="product-foot">
                  <view class="product-price">
                    <text class="currency">¥</text>
                    <text class="price">{{ formatPrice(product.price) }}</text>
                  </view>
                  <view class="add-btn" @click.stop="openDetail(product)">
                    <text class="add-plus">+</text>
                    <text v-if="productCount(product.productId || product.id)" class="add-count">{{ productCount(product.productId || product.id) }}</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
          <view class="product-spacer" />
        </view>
      </scroll-view>
    </view>

    <!-- 底部购物车条 -->
    <CartBar :count="cartStore.totalCount" :total="cartStore.subtotal" @open="cartVisible = true" @checkout="goToCheckout" />
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

    <!-- 门店详情抽屉 -->
    <view v-if="storeSheetVisible" class="store-sheet" @click="closeStoreDetail">
      <view class="sheet-mask" />
      <view class="sheet-content" @click.stop>
        <view class="sheet-header">
          <text class="sheet-title">门店信息</text>
          <text class="sheet-close" @click="closeStoreDetail">×</text>
        </view>
        <view class="sheet-row">
          <text class="sheet-label">地址</text>
          <text class="sheet-value">CozyCoffee 中心店 · 杭州市西湖区</text>
        </view>
        <view class="sheet-row">
          <text class="sheet-label">电话</text>
          <text class="sheet-value">400-888-8888</text>
        </view>
        <view class="sheet-row">
          <text class="sheet-label">营业时间</text>
          <text class="sheet-value">08:00–22:00</text>
        </view>
        <view class="sheet-row">
          <text class="sheet-label">取餐方式</text>
          <text class="sheet-value">到店自提 · 约 15 分钟</text>
        </view>
      </view>
    </view>

    <!-- 商品详情覆盖层（沉浸式） -->
    <view v-if="detailVisible" class="detail-overlay">
      <view class="detail-scroll">
        <view class="detail-hero">
          <view class="detail-close" @click="closeDetail">×</view>
          <image :src="detailProduct?.image" class="detail-image" mode="aspectFill" />
          <view class="detail-hero-info">
            <text v-if="detailTag" class="detail-tag">{{ detailTag }}</text>
            <text class="detail-eyebrow">{{ detailEyebrow }}</text>
            <text class="detail-name">{{ detailProduct?.name }}</text>
            <text class="detail-notes">{{ detailNotes }}</text>
          </view>
        </view>

        <!-- 规格选择 -->
        <view class="detail-spec-section">
          <view v-if="sizeOptions.length > 1" class="detail-spec-group">
            <view class="spec-title-row"><text class="spec-title">杯型</text></view>
            <view class="spec-options">
              <view v-for="option in sizeOptions" :key="option.value" class="spec-option" :class="{ active: specForm.cupSize === option.value }" @click="specForm.cupSize = option.value">
                <text>{{ option.label }}</text>
                <text v-if="option.extra" class="spec-extra">+¥{{ option.extra }}</text>
              </view>
            </view>
          </view>
          <view v-if="tempOptions.length > 1" class="detail-spec-group">
            <view class="spec-title-row"><text class="spec-title">温度</text></view>
            <view class="spec-options">
              <view v-for="option in tempOptions" :key="option.value" class="spec-option" :class="{ active: specForm.temperature === option.value }" @click="specForm.temperature = option.value">{{ option.label }}</view>
            </view>
          </view>
          <view v-if="sugarOptions.length > 1" class="detail-spec-group">
            <view class="spec-title-row"><text class="spec-title">甜度</text></view>
            <view class="spec-options">
              <view v-for="option in sugarOptions" :key="option.value" class="spec-option" :class="{ active: specForm.sugarLevel === option.value }" @click="specForm.sugarLevel = option.value">{{ option.label }}</view>
            </view>
          </view>
          <view v-if="isCoffee" class="detail-spec-group">
            <view class="spec-title-row"><text class="spec-title">咖啡浓度</text></view>
            <view class="spec-options">
              <view class="spec-option" :class="{ active: specForm.coffeeStrength === 'NORMAL' }" @click="specForm.coffeeStrength = 'NORMAL'">标准</view>
              <view class="spec-option" :class="{ active: specForm.coffeeStrength === 'STRONG' }" @click="specForm.coffeeStrength = 'STRONG'">加浓 <text class="spec-extra">+¥5</text></view>
            </view>
          </view>
        </view>

        <!-- 商品详情 -->
        <view class="detail-card">
          <text class="detail-card-title">商品详情</text>
          <view v-if="detailOrigin" class="detail-row"><text class="detail-row-label">产地</text><text class="detail-row-value">{{ detailOrigin }}</text></view>
          <view v-if="detailProcess" class="detail-row"><text class="detail-row-label">处理法</text><text class="detail-row-value">{{ detailProcess }}</text></view>
          <view v-if="detailFlavor" class="detail-row"><text class="detail-row-label">风味</text><text class="detail-row-value">{{ detailFlavor }}</text></view>
          <view v-if="detailRoast" class="detail-row"><text class="detail-row-label">烘焙度</text><text class="detail-row-value">{{ detailRoast }}</text></view>
        </view>

        <view class="detail-disclaimer">本商品按所选规格现制，出品以门店实物为准。</view>
      </view>

      <!-- 底部固定加购栏 -->
      <view class="detail-bottom safe-area-bottom">
        <view class="detail-bottom-top">
          <view class="detail-price-col">
            <text class="detail-bottom-price">¥{{ detailTotalPrice }}</text>
            <text class="detail-selected">{{ selectedSpecsText }}</text>
          </view>
          <view class="detail-qty">
            <view class="qty-btn" @click="changeQty(-1)">−</view>
            <text class="qty-value">{{ detailQty }}</text>
            <view class="qty-btn" @click="changeQty(1)">+</view>
          </view>
        </view>
        <view class="detail-add" @click="addToCart">加入购物车</view>
      </view>
    </view>

    <!-- 下架商品提示 -->
    <view v-if="offShelfVisible" class="off-shelf-mask" @click="closeOffShelf">
      <view class="off-shelf-card" @click.stop>
        <text class="off-shelf-title">{{ offShelfTitle }}</text>
        <view class="off-shelf-list">
          <text v-for="line in offShelfLines" :key="line" class="off-shelf-line">· {{ line }}</text>
        </view>
        <view class="off-shelf-btn" @click="closeOffShelf">知道了</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { FIXED_STORE } from '@/config/store'
import { getMenuData } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { NetworkError } from '@/services/errors/AppError'
import { restoreOrderToCart } from '@/services/order/ReorderService'
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
const scrollIntoView = ref('')
const loading = ref(true)
const offline = ref(false)
const errorMessage = ref('')
const cartVisible = ref(false)
const storeSheetVisible = ref(false)
const fulfillment = ref('pickup')
const statusBarHeight = ref(20)
const navRight = ref(16)
let scrollSpyTimer = null
let scrollLocked = false

// 商品详情
const detailVisible = ref(false)
const detailProduct = ref(null)
const detailQty = ref(1)
const specForm = reactive({ cupSize: 'STANDARD', temperature: 'HOT', sugarLevel: 'STANDARD', coffeeStrength: 'NORMAL' })

// 下架提示
const offShelfVisible = ref(false)
const offShelfTitle = ref('')
const offShelfLines = ref([])

const currentCategory = computed(() => categories.value[currentCategoryIndex.value] || { id: '', name: '', en: '', products: [] })

onMounted(() => {
  const info = uni.getSystemInfoSync()
  statusBarHeight.value = info.statusBarHeight || 20
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
  // 再来一单：恢复订单商品到购物车
  applyReorder()
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
          categoryMap.set(cid, { id: cid, name: categoryName(cid), en: categoryEn(cid), products: [] })
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
      nextTick(() => scrollToCategory(currentCategoryIndex.value))
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
function categoryEn(code) {
  return ({
    espresso: 'ESPRESSO', coffee: 'CLASSIC COFFEE', latte: 'LATTE', signature: 'SIGNATURE',
    soe: 'HAND BREW', bakery: 'BAKERY', dessert: 'DESSERT', addon: 'ADD-ON', other: 'MORE'
  })[code] || 'COZY'
}

function selectCategory(index) {
  currentCategoryIndex.value = index
  scrollToCategory(index)
}

function scrollToCategory(index) {
  scrollLocked = true
  scrollIntoView.value = ''
  nextTick(() => {
    scrollIntoView.value = 'cat-' + index
    setTimeout(() => { scrollLocked = false }, 500)
  })
}

function onProductScroll(e) {
  if (scrollLocked) return
  if (scrollSpyTimer) clearTimeout(scrollSpyTimer)
  scrollSpyTimer = setTimeout(() => {
    const scrollTop = e.detail.scrollTop
    let best = 0
    categories.value.forEach((_, i) => {
      const q = uni.createSelectorQuery()
      q.select('#cat-' + i).boundingClientRect()
      q.exec(res => {
        if (res[0] && res[0].top <= 200 && i >= best) best = i
      })
    })
    setTimeout(() => { if (best !== currentCategoryIndex.value) currentCategoryIndex.value = best }, 30)
  }, 80)
}

function categoryCount(categoryId) {
  return cartStore.items
    .filter(l => String(l.category || 'other').toLowerCase() === categoryId)
    .reduce((sum, l) => sum + Number(l.quantity || 0), 0)
}

function productCount(productId) {
  return cartStore.items
    .filter(l => String(l.productId) === String(productId))
    .reduce((sum, l) => sum + Number(l.quantity || 0), 0)
}

function formatPrice(value) { return Number(value || 0).toFixed(0) }

// ── 门店抽屉 ──
function openStoreDetail() { storeSheetVisible.value = true }
function closeStoreDetail() { storeSheetVisible.value = false }

// ── 商品详情 ──
const isFood = computed(() => ['bakery', 'dessert', 'food', 'addon'].includes(String(detailProduct.value?.category || '').toLowerCase()))
const isCoffee = computed(() => !isFood.value && ['coffee', 'espresso', 'signature', 'soe', 'latte', 'other'].includes(String(detailProduct.value?.category || 'coffee').toLowerCase()))

const sizeOptions = computed(() => {
  if (isFood.value) return [{ value: 'STANDARD', label: '单份', extra: 0 }]
  const type = detailProduct.value?.sizeType || 'MEDIUM_LARGE'
  if (type === 'DEFAULT') return [{ value: 'STANDARD', label: '标准杯', extra: 0 }]
  if (type === 'ALL_SIZES') return [
    { value: 'SMALL', label: '小杯', extra: 0 },
    { value: 'MEDIUM', label: '中杯', extra: 0 },
    { value: 'LARGE', label: '大杯', extra: 3 }
  ]
  return [
    { value: 'MEDIUM', label: '中杯', extra: 0 },
    { value: 'LARGE', label: '大杯', extra: 3 }
  ]
})
const sugarOptions = computed(() => {
  if (isFood.value) return [{ value: '', label: '默认' }]
  const type = detailProduct.value?.sugarType || 'FREE_CHOICE'
  if (type === 'NO_SUGAR_ONLY') return [{ value: 'NONE', label: '无糖' }]
  const values = [{ value: 'STANDARD', label: '标准糖' }, { value: 'LESS', label: '少糖' }, { value: 'HALF', label: '半糖' }]
  if (type !== 'MIN_LESS_SWEET') values.push({ value: 'NONE', label: '无糖' })
  return values
})
const tempOptions = computed(() => {
  if (isFood.value) return [{ value: '', label: '默认' }]
  const type = detailProduct.value?.tempType || 'ALL_OK'
  if (type === 'COLD_ONLY') return [{ value: 'COLD', label: '冰' }]
  if (type === 'HOT_ONLY') return [{ value: 'HOT', label: '热' }]
  if (type === 'NO_HOT') return [{ value: 'COLD', label: '冰' }, { value: 'WARM', label: '温' }]
  return [{ value: 'HOT', label: '热' }, { value: 'COLD', label: '冰' }, { value: 'WARM', label: '温' }]
})

const unitPrice = computed(() => {
  const base = Number(detailProduct.value?.basePrice ?? detailProduct.value?.price ?? 0)
  const size = specForm.cupSize === 'LARGE' ? 3 : 0
  const strength = specForm.coffeeStrength === 'STRONG' ? 5 : 0
  return Number((base + size + strength).toFixed(2))
})
const detailTotalPrice = computed(() => (unitPrice.value * detailQty.value).toFixed(2))
const selectedSpecsText = computed(() => {
  const parts = []
  if (sizeOptions.value.length > 1) parts.push(specForm.cupSize === 'LARGE' ? '大杯' : '中杯')
  if (tempOptions.value.length > 1) parts.push({ HOT: '热', COLD: '冰', WARM: '温' }[specForm.temperature] || specForm.temperature)
  if (sugarOptions.value.length > 1) parts.push(specForm.sugarLevel === 'NONE' ? '无糖' : (specForm.sugarLevel === 'HALF' ? '半糖' : (specForm.sugarLevel === 'LESS' ? '少糖' : '标准糖')))
  if (isCoffee.value) parts.push(specForm.coffeeStrength === 'STRONG' ? '加浓' : '标准')
  return parts.length ? parts.join(' · ') : '默认规格'
})
const detailTag = computed(() => detailProduct.value?.tag || detailProduct.value?.isNewProduct ? '新品' : '')
const detailEyebrow = computed(() => categoryEn(String(detailProduct.value?.category || '').toLowerCase()))
const detailNotes = computed(() => detailProduct.value?.notes || detailProduct.value?.description || '')
const detailOrigin = computed(() => detailProduct.value?.origin || '')
const detailProcess = computed(() => detailProduct.value?.process || '')
const detailFlavor = computed(() => detailProduct.value?.notes || detailProduct.value?.flavorNotes || '')
const detailRoast = computed(() => detailProduct.value?.roast || '')

function openDetail(product) {
  detailProduct.value = product
  detailQty.value = 1
  specForm.cupSize = sizeOptions.value[0]?.value || 'STANDARD'
  specForm.temperature = tempOptions.value[0]?.value || 'HOT'
  specForm.sugarLevel = sugarOptions.value[0]?.value || 'STANDARD'
  specForm.coffeeStrength = 'NORMAL'
  detailVisible.value = true
}

function closeDetail() { detailVisible.value = false; detailProduct.value = null }

function changeQty(delta) {
  detailQty.value = Math.max(1, Math.min(10, detailQty.value + delta))
}

function addToCart() {
  if (!detailProduct.value) return
  const product = detailProduct.value
  const addons = []
  if (specForm.coffeeStrength === 'STRONG') addons.push({ code: 'EXTRA_SHOT', name: '加浓', price: 5 })

  cartStore.addItem({
    ...product,
    productId: String(product.productId || product.id),
    id: product.id,
    name: product.name,
    image: product.image,
    basePrice: Number(product.basePrice ?? product.price ?? 0),
    price: unitPrice.value,
    cupSize: specForm.cupSize,
    temperature: specForm.temperature,
    sugarLevel: isFood.value ? '' : specForm.sugarLevel,
    coffeeStrength: isCoffee.value ? specForm.coffeeStrength : '',
    addons,
    quantity: detailQty.value
  }, detailQty.value)
  try { uni.vibrateShort({ type: 'light' }) } catch (_) {}
  uni.showToast({ title: `已加入购物车 · ${product.name} ×${detailQty.value}`, icon: 'none', duration: 900 })
  closeDetail()
}

// ── 购物车 ──
function editCartLine(line) {
  // 从购物车跳规格：用商品数据打开详情
  const product = cartStore.items.find(l => String(l.lineKey) === String(line.lineKey)) || line
  detailProduct.value = { ...product, image: product.image || '/static/images/default-product.png' }
  detailQty.value = Number(product.quantity || 1)
  specForm.cupSize = product.cupSize || 'STANDARD'
  specForm.temperature = product.temperature || 'HOT'
  specForm.sugarLevel = product.sugarLevel || 'STANDARD'
  specForm.coffeeStrength = product.coffeeStrength || 'NORMAL'
  detailVisible.value = true
  cartVisible.value = false
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

// ── 再来一单：恢复订单商品，下架提示 ──
async function applyReorder() {
  const raw = uni.getStorageSync('cozy_reorder')
  if (!raw) return
  uni.removeStorageSync('cozy_reorder')
  let payload
  try { payload = JSON.parse(raw) } catch (e) { return }
  const items = payload?.items || []
  if (!items.length) return

  const avail = new Set()
  categories.value.forEach(c => c.products.forEach(p => avail.add(p.name)))

  const offShelf = []
  let added = 0
  items.forEach(it => {
    if (!avail.has(it.productName)) { offShelf.push(it.name || it.productName); return }
    const product = categories.value.flatMap(c => c.products).find(p => p.name === it.productName)
    if (!product) { offShelf.push(it.name || it.productName); return }
    cartStore.addItem({ ...product, quantity: Number(it.qty || 1) }, Number(it.qty || 1))
    added += Number(it.qty || 1)
  })

  if (offShelf.length) {
    offShelfTitle.value = added > 0 ? `已加入 ${added} 件 · ${offShelf.length} 款商品已下架` : '以下商品已下架'
    offShelfLines.value = offShelf
    offShelfVisible.value = true
  } else if (added) {
    cartVisible.value = true
    uni.showToast({ title: `已加入购物车 ${added} 件商品`, icon: 'none' })
  }
}

function closeOffShelf() {
  offShelfVisible.value = false
  if (cartStore.totalCount) cartVisible.value = true
}
</script>

<style lang="scss" scoped>
.menu-page { height: 100vh; display: flex; flex-direction: column; overflow: hidden; background: $cozy-bg; }

/* ── 导航胶囊 ── */
.menu-nav { flex: none; background: #fff; }
.nav-inner { height: 56px; padding: 0 20rpx; display: flex; align-items: center; }
.nav-capsule { display: flex; align-items: center; height: 36px; padding: 0 4px; border: 1rpx solid #E8E4DE; border-radius: 24px; background: #fff; }
.nav-capsule__icon { width: 36px; height: 32px; display: flex; align-items: center; justify-content: center; }
.nav-capsule__divider { width: 1px; height: 18px; background: #E8E4DE; }

/* ── 门店信息 ── */
.store-info { flex: none; padding: 14rpx 28rpx 16rpx; background: #fff; border-bottom: 1rpx solid $cozy-border; }
.store-row { display: flex; align-items: center; justify-content: space-between; }
.store-left { display: flex; align-items: center; gap: 8rpx; }
.store-fav { color: $cozy-primary; font-size: 26rpx; }
.store-name { color: $cozy-ink; font-size: 30rpx; font-weight: 650; }
.pickup-switch { display: flex; border: 1rpx solid $cozy-border; border-radius: 12rpx; overflow: hidden; }
.pickup-opt { padding: 10rpx 28rpx; font-size: 22rpx; color: $cozy-muted; background: $cozy-surface; }
.pickup-opt.active { background: $cozy-primary; color: #fff; }
.store-meta { margin-top: 10rpx; font-size: 22rpx; color: $cozy-muted; }
.store-foot { margin-top: 12rpx; display: flex; align-items: center; justify-content: space-between; font-size: 22rpx; color: $cozy-muted; }
.status-icon { color: $cozy-accent; }
.store-more { color: $cozy-muted; }

/* ── 主体 ── */
.menu-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }

/* 左分类栏 */
.category-sidebar { flex: none; width: 168rpx; height: 100%; background: $cozy-surface; }
.category-item { position: relative; padding: 30rpx 12rpx; text-align: center; font-size: 24rpx; color: $cozy-muted; }
.category-item.active { background: #fff; color: $cozy-primary; font-weight: 700; }
.category-item.active::before { content: ''; position: absolute; left: 0; top: 34rpx; bottom: 34rpx; width: 6rpx; border-radius: 0 999rpx 999rpx 0; background: $cozy-primary; }
.cat-count { position: absolute; right: 10rpx; top: 50%; transform: translateY(-50%); min-width: 30rpx; height: 30rpx; padding: 0 6rpx; border-radius: 999rpx; background: $cozy-primary; color: #fff; font-size: 18rpx; font-weight: 600; display: inline-flex; align-items: center; justify-content: center; }

/* 右商品区 */
.product-scroll { min-width: 0; flex: 1; height: 100%; background: #fff; }
.product-content { padding: 0 24rpx 40rpx; }
.category-section { padding-top: 12rpx; }
.group-heading { position: sticky; top: 0; z-index: 2; padding: 22rpx 0 14rpx; background: #fff; display: flex; align-items: baseline; justify-content: space-between; }
.group-title { font-size: 30rpx; font-weight: 700; color: $cozy-ink; letter-spacing: .02em; }
.group-sub { font-size: 17rpx; font-weight: 600; letter-spacing: .14em; color: $cozy-placeholder; }

.product-item { display: flex; gap: 24rpx; padding: 24rpx 0; border-bottom: 1rpx solid $cozy-border; }
.product-image { flex: none; width: 150rpx; height: 150rpx; border-radius: 20rpx; background: $cozy-surface; }
.product-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.product-title-row { display: flex; align-items: center; gap: 10rpx; }
.product-name { font-family: $font-display; font-size: 30rpx; font-weight: 600; color: $cozy-ink; line-height: 1.3; }
.product-tag { flex: none; padding: 4rpx 10rpx; border-radius: 6rpx; background: $cozy-accent-soft; color: $cozy-accent; font-size: 18rpx; font-weight: 600; }
.product-desc { display: -webkit-box; overflow: hidden; margin-top: 6rpx; color: $cozy-muted; font-size: 21rpx; line-height: 1.5; -webkit-box-orient: vertical; -webkit-line-clamp: 1; }
.product-extra { margin-top: 6rpx; font-size: 19rpx; color: $cozy-placeholder; }
.product-foot { margin-top: auto; display: flex; justify-content: space-between; align-items: center; }
.product-price { display: flex; align-items: baseline; color: $cozy-primary; }
.currency { font-size: 20rpx; font-weight: 700; }
.price { font-size: 32rpx; font-weight: 750; line-height: 1; }
.add-btn { position: relative; width: 52rpx; height: 52rpx; border-radius: 50%; background: $cozy-primary; color: #fff; display: flex; align-items: center; justify-content: center; }
.add-plus { font-size: 30rpx; font-weight: 500; line-height: 1; }
.add-count { position: absolute; top: -10rpx; right: -8rpx; min-width: 32rpx; height: 32rpx; padding: 0 6rpx; display: flex; align-items: center; justify-content: center; border: 2rpx solid #fff; border-radius: 999rpx; background: $cozy-primary; color: #fff; font-size: 18rpx; box-sizing: border-box; }
.product-spacer { height: 160rpx; }

/* ── 门店详情抽屉 ── */
.store-sheet { position: fixed; inset: 0; z-index: 30; }
.sheet-mask { position: absolute; inset: 0; background: rgba(28,20,14,.45); }
.sheet-content { position: absolute; bottom: 0; left: 0; right: 0; height: 66.67vh; overflow-y: auto; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 0 40rpx 56rpx; box-sizing: border-box; }
.sheet-header { display: flex; align-items: center; justify-content: space-between; padding: 34rpx 0 28rpx; border-bottom: 1rpx solid $cozy-border; }
.sheet-title { font-family: $font-display; font-size: 36rpx; font-weight: 600; color: $cozy-ink; }
.sheet-close { font-size: 40rpx; color: $cozy-muted; padding: 4rpx 8rpx; }
.sheet-row { display: flex; align-items: flex-start; gap: 24rpx; padding: 28rpx 0; border-bottom: 1rpx solid $cozy-border; }
.sheet-row:last-child { border-bottom: 0; }
.sheet-label { flex: none; width: 120rpx; font-size: 24rpx; color: $cozy-muted; }
.sheet-value { flex: 1; font-size: 26rpx; color: $cozy-ink; line-height: 1.5; }

/* ── 商品详情覆盖层 ── */
.detail-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 25; background: #fff; display: flex; flex-direction: column; }
.detail-scroll { flex: 1; min-height: 0; overflow-y: auto; }
.detail-hero { position: relative; }
.detail-image { width: 100%; height: 520rpx; background: linear-gradient(135deg,#E8DDD2,#D8C8B4); }
.detail-close { position: absolute; top: 24rpx; left: 24rpx; width: 68rpx; height: 68rpx; border-radius: 50%; background: rgba(255,255,255,.9); display: flex; align-items: center; justify-content: center; font-size: 44rpx; color: $cozy-ink; z-index: 2; }
.detail-hero-info { padding: 40rpx 40rpx 12rpx; }
.detail-tag { display: inline-block; margin-bottom: 18rpx; padding: 6rpx 18rpx; border-radius: 8rpx; background: $cozy-primary; color: #fff; font-size: 22rpx; font-weight: 700; letter-spacing: .06em; }
.detail-eyebrow { display: block; font-size: 20rpx; font-weight: 700; letter-spacing: .24em; color: $cozy-muted; }
.detail-name { display: block; margin-top: 14rpx; font-family: $font-display; font-size: 56rpx; font-weight: 600; color: $cozy-ink; line-height: 1.2; }
.detail-notes { display: block; margin-top: 20rpx; font-family: $font-display; font-size: 28rpx; color: $cozy-muted; letter-spacing: .02em; }

.detail-spec-section { border-top: 1rpx solid $cozy-border; padding: 36rpx 40rpx 8rpx; }
.detail-spec-group { margin-top: 28rpx; }
.detail-spec-group:first-child { margin-top: 0; }
.spec-title-row { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 20rpx; }
.spec-title { font-size: 26rpx; font-weight: 600; color: $cozy-ink; }
.spec-options { display: flex; flex-wrap: wrap; gap: 20rpx; }
.spec-option { height: 72rpx; padding: 0 40rpx; display: flex; align-items: center; justify-content: center; gap: 8rpx; font-size: 26rpx; color: $cozy-ink; border-radius: 10rpx; border: 1rpx solid $cozy-border; background: #fff; }
.spec-option.active { background: #F1E4DA; border-color: $cozy-primary; color: $cozy-primary; font-weight: 600; }
.spec-extra { color: $cozy-muted; font-size: 20rpx; }
.spec-option.active .spec-extra { color: $cozy-primary; }

.detail-card { border-top: 1rpx solid $cozy-border; margin-top: 40rpx; padding: 36rpx 40rpx 4rpx; }
.detail-card-title { display: block; margin-bottom: 20rpx; font-family: $font-display; font-size: 32rpx; font-weight: 600; color: $cozy-ink; }
.detail-row { display: flex; align-items: flex-start; gap: 32rpx; padding: 16rpx 0; }
.detail-row-label { flex: none; width: 104rpx; font-size: 22rpx; font-weight: 700; letter-spacing: .1em; color: $cozy-muted; }
.detail-row-value { flex: 1; font-size: 26rpx; line-height: 1.6; color: $cozy-ink; }
.detail-disclaimer { border-top: 1rpx solid $cozy-border; margin-top: 40rpx; padding: 28rpx 40rpx 48rpx; font-size: 22rpx; line-height: 1.7; color: $cozy-placeholder; }

.detail-bottom { flex: none; padding: 20rpx 32rpx max(20rpx, env(safe-area-inset-bottom)); background: #fff; border-top: 1rpx solid $cozy-border; }
.detail-bottom-top { display: flex; align-items: center; gap: 24rpx; margin-bottom: 20rpx; }
.detail-price-col { flex: 1; min-width: 0; }
.detail-bottom-price { font-size: 44rpx; font-weight: 700; color: $cozy-primary; line-height: 1.1; }
.detail-selected { display: block; margin-top: 8rpx; font-size: 22rpx; color: $cozy-muted; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.detail-qty { display: flex; align-items: center; gap: 16rpx; border: 1rpx solid $cozy-border; border-radius: 12rpx; padding: 8rpx 18rpx; }
.qty-btn { width: 48rpx; height: 48rpx; display: flex; align-items: center; justify-content: center; font-size: 32rpx; color: $cozy-ink; }
.qty-value { min-width: 36rpx; text-align: center; font-size: 28rpx; font-weight: 600; color: $cozy-ink; }
.detail-add { width: 100%; height: 92rpx; display: flex; align-items: center; justify-content: center; border-radius: 12rpx; background: $cozy-primary; color: #fff; font-size: 30rpx; font-weight: 650; }
.detail-add:active { background: $cozy-primary-hover; }

/* ── 下架提示 ── */
.off-shelf-mask { position: fixed; inset: 0; z-index: 50; display: flex; align-items: center; justify-content: center; padding: 0 72rpx; background: rgba(44,30,24,.42); }
.off-shelf-card { width: 100%; max-width: 600rpx; border-radius: 28rpx; background: #fff; padding: 52rpx 48rpx 40rpx; text-align: center; }
.off-shelf-title { display: block; font-size: 30rpx; font-weight: 700; color: $cozy-ink; }
.off-shelf-list { margin-top: 28rpx; text-align: left; }
.off-shelf-line { display: block; padding: 14rpx 0; font-size: 26rpx; color: $cozy-muted; }
.off-shelf-btn { margin-top: 36rpx; height: 84rpx; border-radius: 18rpx; background: $cozy-primary; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 28rpx; font-weight: 600; }
</style>
