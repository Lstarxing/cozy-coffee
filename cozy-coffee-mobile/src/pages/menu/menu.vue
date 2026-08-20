<!--
  点单页 - 复现 prototype/menu.html
  导航胶囊 + 门店信息（详情抽屉）+ 左分类栏（scroll-spy 联动）+ 连续滚动商品区（吸顶标题）+ 沉浸式商品详情 + 底部购物车
-->
<template>
  <view class="menu-page" :style="{ height: menuPageHeight + 'px' }">
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
        <view class="store-left" :class="{ 'tap-target': fulfillment === 'delivery' }" @click="fulfillment === 'delivery' && goToAddress()">
          <template v-if="fulfillment === 'delivery'">
            <CozyIcon name="pin" :size="22" color="#756A63" />
            <text class="store-name delivery-addr">{{ deliveryAddressText }}</text>
            <text class="delivery-arrow">›</text>
          </template>
          <template v-else>
            <text class="store-fav">☆</text>
            <text class="store-name">{{ fixedStore.name }}</text>
          </template>
        </view>
        <view class="pickup-switch">
          <view class="pickup-opt" :class="{ active: fulfillment === 'pickup' }" @click="setFulfillment('pickup')">自提</view>
          <view class="pickup-opt" :class="{ active: fulfillment === 'delivery' }" @click="setFulfillment('delivery')">外送</view>
        </view>
      </view>
      <view class="store-meta" :class="{ 'store-meta--delivery': fulfillment === 'delivery' }">
        <template v-if="fulfillment === 'delivery'">
          <CozyIcon name="swap" :size="14" color="#756A63" />
          <text class="store-meta-text">CozyCoffee 中心店 | 配送距离 1.2km</text>
        </template>
        <template v-else>距离您 1.2km</template>
      </view>
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
          <text class="sheet-title">活动</text>
          <text class="sheet-close" @click="closeStoreDetail">×</text>
        </view>

        <!-- Cozy Day（周五会员日） -->
        <view class="sheet-row plain">
          <text class="sheet-label">会员日</text>
          <text class="sheet-value cozy-today">每周五会员日 · 积分倍率提升 0.5</text>
        </view>

        <view class="sheet-subheader">
          <text class="sheet-title">门店信息</text>
        </view>

        <view class="sheet-row">
          <text class="sheet-label">地址</text>
          <text class="sheet-value">CozyCoffee 中心店 · 杭州市西湖区</text>
          <text class="sheet-icon">↗</text>
        </view>
        <view class="sheet-row">
          <text class="sheet-label">电话</text>
          <text class="sheet-value">400-888-8888</text>
          <text class="sheet-icon">☎</text>
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

    <!-- 选择收货地址抽屉 -->
    <view v-if="addressSheetVisible" class="store-sheet" @click="addressSheetVisible = false">
      <view class="sheet-mask" />
      <view class="address-sheet-content" @click.stop>
        <view class="sheet-header">
          <text class="sheet-title">请选择收货地址</text>
          <text class="sheet-close" @click="addressSheetVisible = false">×</text>
        </view>
        <scroll-view scroll-y class="address-sheet-list">
          <view v-if="addressLoading" class="address-sheet-loading">加载中…</view>
          <view v-for="addr in addressList" :key="addr.id" class="address-row" @click="pickAddress(addr)">
            <view class="address-check">
              <CozyIcon v-if="isCurrentAddress(addr)" name="check" :size="16" color="#753A22" />
            </view>
            <view class="address-copy">
              <view class="address-addr-line">
                <text v-if="addr.label" class="address-label">{{ labelText(addr.label) }}</text>
                <text class="address-addr">{{ addressText(addr) }}</text>
              </view>
              <view class="address-contact-line">
                <text class="address-name">{{ contactNameText(addr) }}</text>
                <text class="address-phone">{{ maskPhone(addr.phone) }}</text>
              </view>
            </view>
            <view class="address-edit" @click.stop="editAddress(addr)"><CozyIcon name="pencil" :size="16" color="#756A63" /></view>
          </view>
        </scroll-view>
        <view class="address-add" @click="addAddress">
          <CozyIcon name="plus" :size="18" color="#753A22" />
          <text>新增收货地址</text>
        </view>
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
import { computed, nextTick, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { FIXED_STORE } from '@/config/store'
import { getMenuData } from '@/api/product'
import { get } from '@/api/request'
import { useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import { NetworkError } from '@/services/errors/AppError'
import { restoreOrderToCart } from '@/services/order/ReorderService'
import CartBar from '@/components/order/CartBar.vue'
import CartSheet from '@/components/order/CartSheet.vue'
import CozyIcon from '@/components/CozyIcon.vue'
import LoadingState from '@/components/states/LoadingState.vue'
import EmptyState from '@/components/states/EmptyState.vue'
import RetryState from '@/components/states/RetryState.vue'
import OfflineState from '@/components/states/OfflineState.vue'
import { resolveDeliveryAddress } from '@/services/address/DeliveryAddressResolver'

const cartStore = useCartStore()
const checkoutStore = useCheckoutStore()
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
async function setFulfillment(value) {
  if (fulfillment.value === value) return
  fulfillment.value = value
  checkoutStore.diningMethod = value === 'delivery' ? 'DELIVERY' : 'TAKEOUT'
  if (value === 'delivery') {
    const addr = await resolveDeliveryAddress(checkoutStore)
    if (!addr) uni.navigateTo({ url: '/pages/address/edit' })
  }
}
const deliveryAddressText = computed(() => {
  const a = checkoutStore.deliveryAddress
  if (!a) return '未设置配送地址，点击添加'
  return [a.region, a.detail].filter(Boolean).join(' ').replace(/\s+/g, '')
})
function goToAddress() {
  openAddressSheet()
}

// ── 选择收货地址抽屉 ──
const addressSheetVisible = ref(false)
const addressList = ref([])
const addressLoading = ref(false)
async function openAddressSheet() {
  addressSheetVisible.value = true
  addressLoading.value = true
  try {
    const res = await get('/member/addresses')
    if (res.code === 200 && Array.isArray(res.data)) {
      addressList.value = res.data.map(a => ({
        ...a,
        name: a.receiverName,
        gender: a.gender || 'MALE',
        phone: a.receiverPhone,
        region: [a.province, a.city, a.district].filter(Boolean).join(' '),
        detail: a.detailAddress,
        isDefault: a.isDefault
      }))
    }
  } catch (e) {
    // 加载失败时抽屉内不展示地址，用户可点新增配置
  } finally {
    addressLoading.value = false
  }
}
const maskPhone = (phone) => String(phone || '').replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
const genderSuffix = (gender) => String(gender || '').toUpperCase() === 'FEMALE' ? '女士' : '先生'
const labelText = (label) => ({ HOME: '家', COMPANY: '公司', SCHOOL: '学校' }[label] || label || '')
function contactNameText(addr) {
  return `${addr.name || ''}（${genderSuffix(addr.gender)}）`
}
function addressText(addr) {
  return [addr.region, addr.detail].filter(Boolean).join(' ').replace(/\s+/g, '')
}
function isCurrentAddress(addr) {
  return checkoutStore.deliveryAddressId && String(addr.id) === String(checkoutStore.deliveryAddressId)
}
function pickAddress(addr) {
  checkoutStore.deliveryAddress = addr
  checkoutStore.deliveryAddressId = addr.id || null
  if (addr.phone && !checkoutStore.phone) checkoutStore.phone = addr.phone
  addressSheetVisible.value = false
}
function editAddress(addr) {
  addressSheetVisible.value = false
  uni.setStorageSync('cozy_edit_address', addr)
  uni.navigateTo({ url: `/pages/address/edit?id=${addr.id}` })
}
function addAddress() {
  addressSheetVisible.value = false
  uni.navigateTo({ url: '/pages/address/edit' })
}
const sysInfo = uni.getSystemInfoSync()
const statusBarHeight = ref(sysInfo.statusBarHeight || 20)
const navRight = ref(100)
const menuPageHeight = ref(sysInfo.windowHeight || 667)
let scrollLocked = false
let scrollSpyTick = false

// 下架提示
const offShelfVisible = ref(false)
const offShelfTitle = ref('')
const offShelfLines = ref([])

const currentCategory = computed(() => categories.value[currentCategoryIndex.value] || { id: '', name: '', en: '', products: [] })

onLoad((options) => {
  pendingCategory = (options && options.category) ? String(options.category).toLowerCase() : ''
  loadMenu()
})

onShow(async () => {
  // 首页自提/外送按钮传入的选择优先
  const presetDining = uni.getStorageSync('cozy_dining_method')
  if (presetDining) {
    uni.removeStorageSync('cozy_dining_method')
    fulfillment.value = presetDining === 'delivery' ? 'delivery' : 'pickup'
    checkoutStore.diningMethod = fulfillment.value === 'delivery' ? 'DELIVERY' : 'TAKEOUT'
  } else {
    // 否则恢复结算 store 中的选择
    fulfillment.value = checkoutStore.diningMethod === 'DELIVERY' ? 'delivery' : 'pickup'
  }
  // 外送：静默解析默认地址（无地址时由确认页引导添加）
  if (checkoutStore.diningMethod === 'DELIVERY' && !checkoutStore.deliveryAddressId) {
    await resolveDeliveryAddress(checkoutStore)
  }
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
    // 锁释放后同步一次到滚动落点，避免动画期间 spy 被抑制导致不同步
    setTimeout(() => {
      scrollLocked = false
      pickActive()
    }, 800)
  })
}

// 吸顶判定：哪个分类标题最接近容器顶部（吸顶位置 top:0），它即当前分类
function pickActive() {
  if (scrollLocked) return
  const query = uni.createSelectorQuery()
  query.select('.product-scroll').boundingClientRect()
  query.selectAll('.group-heading').boundingClientRect()
  query.exec((res) => {
    const container = res[0]
    const headings = res[1]
    if (!container || !headings || !headings.length) return
    const containerTop = container.top
    let best = currentCategoryIndex.value
    let bestAbs = Infinity
    headings.forEach((h, i) => {
      const abs = Math.abs(h.top - containerTop)
      if (abs < bestAbs) {
        bestAbs = abs
        best = i
      }
    })
    if (best !== currentCategoryIndex.value) currentCategoryIndex.value = best
  })
}

// 滚动监听：节流调用 pickActive，吸附瞬间立即更新左侧
function onProductScroll() {
  if (scrollLocked || scrollSpyTick) return
  scrollSpyTick = true
  pickActive()
  setTimeout(() => { scrollSpyTick = false }, 16)
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

// ── 商品详情 → 独立选规格页 ──
function openDetail(product) {
  uni.setStorageSync('cozy_spec', { product, line: null })
  uni.navigateTo({ url: '/pages/menu/spec' })
}

// ── 购物车 ──
function editCartLine(line) {
  // 从购物车跳规格：带行数据进独立选规格页
  const product = cartStore.items.find(l => String(l.lineKey) === String(line.lineKey)) || line
  cartVisible.value = false
  uni.setStorageSync('cozy_spec', { product, line })
  uni.navigateTo({ url: '/pages/menu/spec' })
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
.menu-page { display: flex; flex-direction: column; overflow: hidden; background: $cozy-surface; }

/* ── 导航胶囊（与原型 menu.html 一致） ── */
.menu-nav { flex: none; background: #fff; }
.nav-inner { height: 56px; padding: 0 32rpx; display: flex; align-items: center; }
.nav-capsule { display: flex; align-items: center; gap: 2px; padding: 4px 6px; border: 1rpx solid $cozy-border; border-radius: 20px; background: #fff; }
.nav-capsule__icon { width: 34px; height: 30px; display: flex; align-items: center; justify-content: center; }
.nav-capsule__divider { width: 1px; height: 16px; background: $cozy-border; }

/* ── 门店信息 ── */
.store-info {
  flex: none;
  margin: 0;
  padding: 24rpx 28rpx 26rpx;
  background: #fff;
}
.store-row { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.store-left { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8rpx; }
.store-fav { color: $cozy-ink; font-size: 26rpx; }
.store-name { color: $cozy-ink; font-size: 30rpx; font-weight: 650; }
.pickup-switch { flex: none; display: flex; border: 1rpx solid $cozy-border; border-radius: 12rpx; overflow: hidden; }
.pickup-opt { padding: 10rpx 28rpx; font-size: 22rpx; color: $cozy-muted; background: $cozy-surface; }
.pickup-opt.active { background: $cozy-ink; color: #fff; }
.store-meta { margin-top: 4rpx; font-size: 22rpx; color: $cozy-muted; }
.store-meta--delivery {
  margin-top: 6rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.tap-target { cursor: pointer; }
.delivery-addr {
  flex: 1;
  min-width: 0;
  color: $cozy-ink;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.delivery-arrow {
  flex: none;
  font-size: 24rpx;
  line-height: 1;
  color: $cozy-placeholder;
}
.store-foot { margin-top: 12rpx; display: flex; align-items: center; justify-content: space-between; font-size: 22rpx; color: $cozy-muted; }
.status-icon { color: $cozy-muted; }
.store-more { color: $cozy-muted; }

/* ── 主体 ── */
.menu-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }

/* 左分类栏 */
.category-sidebar { flex: none; width: 180rpx; height: 100%; background: $cozy-surface; border-right: 1rpx solid $cozy-border; }
.category-item { position: relative; padding: 40rpx 16rpx; text-align: center; font-size: 26rpx; color: $cozy-muted; }
.category-item.active { background: #fff; color: $cozy-ink; font-weight: 700; }

/* 右商品区 */
.product-scroll { min-width: 0; flex: 1; height: 100%; background: #fff; }
.product-content { padding-bottom: 40rpx; }
/* 吸附后观感与原型一致（标题距顶28rpx、栏高52rpx）；top 向上多伸10rpx遮住微信 scroll-view 内 sticky 顶边的合成缝隙，padding 精确补偿标题位置 */
.group-heading { position: sticky; top: -13rpx; z-index: 2; padding: 38rpx 24rpx 24rpx; background: #fff; display: flex; align-items: baseline; justify-content: space-between; }
.group-title { font-size: 30rpx; font-weight: 700; color: $cozy-ink; letter-spacing: .02em; }
.group-sub { font-size: 17rpx; font-weight: 600; letter-spacing: .14em; color: $cozy-placeholder; }

.product-item { display: flex; gap: 24rpx; padding: 24rpx 24rpx; border-bottom: 1rpx solid $cozy-border; }
.product-image { flex: none; width: 150rpx; height: 150rpx; border-radius: 20rpx; background: $cozy-surface; }
.product-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.product-title-row { display: flex; align-items: center; gap: 10rpx; }
.product-name { font-family: $font-display; font-size: 30rpx; font-weight: 600; color: $cozy-ink; line-height: 1.3; }
.product-tag { flex: none; padding: 4rpx 10rpx; border-radius: 6rpx; background: $cozy-surface; color: $cozy-ink; font-size: 18rpx; font-weight: 600; }
.product-desc { display: -webkit-box; overflow: hidden; margin-top: 6rpx; color: $cozy-muted; font-size: 21rpx; line-height: 1.5; -webkit-box-orient: vertical; -webkit-line-clamp: 1; }
.product-extra { margin-top: 6rpx; font-size: 19rpx; color: $cozy-placeholder; }
.product-foot { margin-top: auto; display: flex; justify-content: space-between; align-items: center; }
.product-price { display: flex; align-items: baseline; color: $cozy-ink; }
.currency { font-size: 20rpx; font-weight: 700; }
.price { font-size: 32rpx; font-weight: 750; line-height: 1; }
.add-btn { position: relative; width: 44rpx; height: 44rpx; border-radius: 50%; background: $cozy-ink; color: #fff; text-align: center; line-height: 44rpx; }
.add-plus { font-size: 34rpx; font-weight: 600; line-height: 44rpx; }
.add-count { position: absolute; top: -8rpx; right: -6rpx; min-width: 32rpx; height: 32rpx; padding: 0 6rpx; display: flex; align-items: center; justify-content: center; border: 2rpx solid #fff; border-radius: 999rpx; background: $cozy-ink; color: #fff; font-size: 18rpx; box-sizing: border-box; }
.product-spacer { height: 160rpx; }

/* ── 门店详情抽屉 ── */
.store-sheet { position: fixed; inset: 0; z-index: 30; }
.sheet-mask { position: absolute; inset: 0; background: rgba(28,20,14,.45); }
.sheet-content { position: absolute; bottom: 0; left: 0; right: 0; height: 66.67vh; overflow-y: auto; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 0 40rpx 56rpx; box-sizing: border-box; }
.sheet-header { display: flex; align-items: center; justify-content: space-between; padding: 36rpx 0 32rpx; border-bottom: 1rpx solid $cozy-border; }
.sheet-title { font-family: $font-display; font-size: 36rpx; font-weight: 600; color: $cozy-ink; }
.sheet-close { font-size: 40rpx; color: $cozy-muted; padding: 4rpx 8rpx; }

/* ── 选择收货地址抽屉 ── */
.address-sheet-content {
  position: absolute; bottom: 0; left: 0; right: 0;
  max-height: 68vh;
  display: flex; flex-direction: column;
  background: #fff; border-radius: 32rpx 32rpx 0 0;
}
.address-sheet-list {
  flex: 1 1 auto;
  min-height: 0;
  max-height: 48vh;
  padding: 0 40rpx 8rpx;
  box-sizing: border-box;
}
.address-sheet-loading { padding: 48rpx 0; text-align: center; font-size: 24rpx; color: $cozy-muted; }
.address-row {
  display: flex; align-items: center; gap: 20rpx;
  margin-top: 20rpx;
  padding: 20rpx 24rpx;
  border: 1rpx solid $cozy-border; border-radius: 16rpx;
  background: $bg-white;
}
.address-check { flex: none; width: 40rpx; display: flex; align-items: center; justify-content: center; }
.address-copy { flex: 1; min-width: 0; }
.address-addr-line { display: flex; align-items: center; gap: 10rpx; }
.address-label {
  flex: none;
  padding: 0 10rpx;
  border: 1rpx solid $cozy-primary;
  border-radius: 4rpx;
  color: $cozy-primary;
  font-size: 18rpx;
  line-height: 1.6;
}
.address-addr {
  flex: 1;
  min-width: 0;
  font-size: 24rpx; color: $cozy-ink;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.address-contact-line { display: flex; align-items: center; gap: 12rpx; margin-top: 8rpx; }
.address-name { font-size: 22rpx; font-weight: 600; color: $cozy-ink; }
.address-phone { font-size: 22rpx; color: $cozy-muted; }
.address-edit { flex: none; padding: 8rpx; }
.address-add {
  display: flex; align-items: center; justify-content: center; gap: 12rpx;
  margin: 16rpx 40rpx 32rpx; height: 84rpx;
  border: 1rpx solid $cozy-border; border-radius: 12rpx;
  font-size: 28rpx; font-weight: 600; color: $cozy-primary;
}
.address-add:active { background: $cozy-surface; }
.sheet-subheader { margin-top: 20rpx; padding-bottom: 20rpx; border-bottom: 1rpx solid $cozy-border; }
.sheet-row { display: flex; align-items: center; gap: 28rpx; padding: 28rpx 0; border-bottom: 1rpx solid $cozy-border; }
.sheet-row.plain { border-bottom: 0; }
.sheet-row:last-child { border-bottom: 0; }
.sheet-label { flex: none; width: 120rpx; font-size: 24rpx; color: $cozy-muted; }
.sheet-value { flex: 1; font-size: 26rpx; color: $cozy-ink; line-height: 1.4; }
.sheet-value.cozy-today { color: $cozy-ink; font-weight: 600; }
.sheet-icon { flex: none; font-size: 32rpx; color: $cozy-muted; }

/* ── 下架提示 ── */
.off-shelf-mask { position: fixed; inset: 0; z-index: 50; display: flex; align-items: center; justify-content: center; padding: 0 72rpx; background: rgba(44,30,24,.42); }
.off-shelf-card { width: 100%; max-width: 600rpx; border-radius: 28rpx; background: #fff; padding: 52rpx 48rpx 40rpx; text-align: center; }
.off-shelf-title { display: block; font-size: 30rpx; font-weight: 700; color: $cozy-ink; }
.off-shelf-list { margin-top: 28rpx; text-align: left; }
.off-shelf-line { display: block; padding: 14rpx 0; font-size: 26rpx; color: $cozy-muted; }
.off-shelf-btn { margin-top: 36rpx; height: 84rpx; border-radius: 18rpx; background: $cozy-ink; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 28rpx; font-weight: 600; }
</style>
