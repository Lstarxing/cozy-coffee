<template>
  <div class="mall-view">
    <header class="content-header">
      <h3>积分商城</h3>
      <span class="points-badge">{{ userStore.userInfo?.currentPoints || 0 }} 积分</span>
    </header>

    <!-- 分类标签栏 -->
    <div class="mall-category-tabs">
      <button
        v-for="cat in mallCategories"
        :key="cat.value"
        class="mall-category-tab"
        :class="{ active: activeMallCategory === cat.value }"
        @click="activeMallCategory = cat.value"
      >
        <component :is="cat.icon" :size="18" :stroke-width="1.8" class="tab-icon-svg" />
        <span>{{ cat.label }}</span>
      </button>
    </div>

    <!-- 商品列表 -->
    <div class="products-grid">
      <div v-for="product in filteredMallProducts" :key="product.id" class="mall-card">
        <div class="card-image" :style="{ backgroundImage: `url(${getImageUrl(product.imageUrl, '/images/products/default.png')})` }"></div>
        <div class="card-details">
          <h4>{{ product.name }}</h4>
          <p class="product-desc">{{ product.description }}</p>
          <span class="price">{{ product.pointsPrice }} 积分</span>
          <span class="stock" :class="{ low: product.stock < 10 }">库存: {{ product.stock }}</span>
          <!-- 月度限购提示 -->
          <div v-if="product.monthlyLimit && product.monthlyLimit > 0" class="limit-info">
            <span class="limit-tag">月限 {{ product.monthlyLimit }}</span>
            <span class="redeemed-text">已兑: {{ product.currentUserMonthlyRedeemed || 0 }}</span>
          </div>
          <button
            class="redeem-btn"
            :disabled="product.stock === 0 || (userStore.userInfo?.currentPoints || 0) < product.pointsPrice || (product.monthlyLimit && (product.currentUserMonthlyRedeemed || 0) >= product.monthlyLimit)"
            @click="openRedeemDialog(product)">
            {{ product.stock === 0 ? '已售罄' : ((product.monthlyLimit && (product.currentUserMonthlyRedeemed || 0) >= product.monthlyLimit) ? '本月限额已满' : '立即兑换') }}
          </button>
        </div>
      </div>
      <div v-if="filteredMallProducts.length === 0" class="no-data">暂无商品</div>
    </div>

    <!-- 兑换确认弹窗 -->
    <div v-if="showRedeemModal" class="redeem-modal" @click.self="showRedeemModal = false">
      <div class="modal-content">
        <h3>确认兑换</h3>
        <div class="product-preview">
          <img :src="getImageUrl(selectedProduct?.imageUrl, '/images/products/default.png')" />
          <div>
            <p class="name">{{ selectedProduct?.name }}</p>
            <p class="unit-price">单价: {{ selectedProduct?.pointsPrice }} 积分</p>
            <p class="stock-info">库存: {{ selectedProduct?.stock }}</p>
          </div>
        </div>

        <!-- 兑换数量选择 -->
        <div class="quantity-section">
          <label>兑换数量：</label>
          <div class="quantity-control">
            <button class="qty-btn" :disabled="redeemQuantity <= 1" @click="redeemQuantity > 1 && redeemQuantity--">&minus;</button>
            <input v-model.number="redeemQuantity" type="number" min="1" :max="Math.min(selectedProduct?.stock || 1, 10)" class="qty-input" />
            <button class="qty-btn" :disabled="redeemQuantity >= Math.min(selectedProduct?.stock || 1, 10)" @click="redeemQuantity < Math.min(selectedProduct?.stock || 1, 10) && redeemQuantity++">+</button>
          </div>
          <span class="qty-tip">单次最多兑换10件</span>
        </div>

        <!-- 交付方式 -->
        <div v-if="redeemFulfillmentType !== 'VIRTUAL' && selectedProduct?.productType !== 'VIRTUAL' && !selectedProduct?.name?.includes('券')" class="fulfillment-section">
          <label>交付方式：</label>
          <div class="radio-group-modern">
            <label class="radio-card" :class="{ active: redeemFulfillmentType === 'PICKUP' }">
              <input v-model="redeemFulfillmentType" type="radio" value="PICKUP" class="hidden-radio">
              <span class="icon">🏠</span>
              <span class="text">门店自提</span>
            </label>
            <label class="radio-card" :class="{ active: redeemFulfillmentType === 'DELIVERY' }">
              <input v-model="redeemFulfillmentType" type="radio" value="DELIVERY" class="hidden-radio">
              <span class="icon">🚚</span>
              <span class="text">快递配送</span>
            </label>
          </div>
        </div>

        <!-- 地址选择 -->
        <div v-if="redeemFulfillmentType === 'DELIVERY'" class="address-section">
          <div class="section-title-row">
            <label>收货地址：</label>
            <button v-if="!addresses || addresses.length === 0" class="text-btn" @click="showAddAddressModal = true">去添加</button>
          </div>
          <select v-model="redeemAddressId" class="modern-select">
            <option value="" disabled>请选择收货地址</option>
            <option v-for="addr in addresses" :key="addr.id" :value="addr.id">
              {{ addr.receiverName }} {{ addr.receiverPhone }} ({{ addr.province }}{{ addr.city }}{{ addr.district || '' }} {{ addr.detailAddress }})
            </option>
          </select>
        </div>

        <div v-if="redeemFulfillmentType === 'PICKUP' && selectedProduct?.productType !== 'VIRTUAL'" class="pickup-hint">
          <p>📍 取货门店: <strong>Cozy Coffee 旗舰店</strong></p>
          <p class="sub-text">下单后请凭取货码到店领取</p>
        </div>

        <div class="redeem-summary-card">
          <div class="row">
            <span>原价</span>
            <span class="original">{{ (selectedProduct?.pointsPrice || 0) * redeemQuantity }} 积分</span>
          </div>
          <div v-if="getRedeemDiscount() < 1" class="row">
            <span>会员折扣 ({{ Math.round((1 - getRedeemDiscount()) * 100) }}% OFF)</span>
            <span class="discount">-{{ Math.round(((selectedProduct?.pointsPrice || 0) * redeemQuantity) * (1 - getRedeemDiscount())) }} 积分</span>
          </div>
          <div class="divider"></div>
          <div class="row total">
            <span>实付</span>
            <strong>{{ getDiscountedCost() }} 积分</strong>
          </div>
          <p class="balance-refer">当前余额: {{ userStore.userInfo?.currentPoints || 0 }} 积分</p>
        </div>

        <div class="redeem-warning-box">
          ⚠️ 温馨提示：积分商品兑换后不支持取消或退换。
        </div>

        <div class="modal-actions">
          <button class="cancel-btn" @click="showRedeemModal = false">取消</button>
          <button class="confirm-btn" :disabled="isRedeeming || (redeemFulfillmentType === 'DELIVERY' && !redeemAddressId)" @click="handleRedeemProduct">
            {{ isRedeeming ? '处理中...' : '确认兑换' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, markRaw } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Tag, Ticket, Gift } from 'lucide-vue-next'
import { getPointsProducts, redeemProduct } from '@/api/mall'
import { useAddresses } from '@/composables/useAddresses'
import { getImageUrl } from '@/utils/image'

const userStore = useUserStore()

const products = ref([])
const showRedeemModal = ref(false)
const selectedProduct = ref(null)
const redeemQuantity = ref(1)
const redeemFulfillmentType = ref('PICKUP')
const isRedeeming = ref(false)
const showAddAddressModal = ref(false)

const { addresses, selectedAddressId: redeemAddressId, loadAddresses } = useAddresses()

const mallCategories = [
  { value: 'all', label: '全部', icon: markRaw(Tag) },
  { value: 'coupon', label: '优惠券', icon: markRaw(Ticket) },
  { value: 'gift', label: '实物礼品', icon: markRaw(Gift) }
]
const activeMallCategory = ref('all')

const filteredMallProducts = computed(() => {
  if (activeMallCategory.value === 'all') return products.value
  return products.value.filter(p => p.category === activeMallCategory.value)
})

function getRedeemDiscount() {
  const level = userStore.userLevel || 'basic'
  const map = { basic: 1, silver: 0.98, gold: 0.95, diamond: 0.90, black: 0.85 }
  return map[level] || 1
}

function getDiscountedCost() {
  const original = (selectedProduct.value?.pointsPrice || 0) * redeemQuantity.value
  return Math.ceil(original * getRedeemDiscount())
}

async function loadProducts() {
  try {
    const data = await getPointsProducts()
    products.value = data.data || []
  } catch (error) {
    console.error('Failed to load products:', error)
  }
}

function openRedeemDialog(product) {
  selectedProduct.value = product
  redeemQuantity.value = 1
  if (product.productType === 'VIRTUAL' || (product.name && product.name.includes('券'))) {
    redeemFulfillmentType.value = 'VIRTUAL'
  } else {
    redeemFulfillmentType.value = 'PICKUP'
  }
  if (addresses.value && addresses.value.length > 0) {
    const def = addresses.value.find(a => a.isDefault)
    redeemAddressId.value = def ? def.id : addresses.value[0].id
  } else {
    redeemAddressId.value = ''
  }
  showRedeemModal.value = true
}

async function handleRedeemProduct() {
  if (!selectedProduct.value) return
  if (isRedeeming.value) return
  if (redeemFulfillmentType.value === 'DELIVERY' && !redeemAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  isRedeeming.value = true
  try {
    const data = await redeemProduct({
      productId: selectedProduct.value.id,
      quantity: redeemQuantity.value,
      fulfillmentType: redeemFulfillmentType.value,
      addressId: redeemFulfillmentType.value === 'DELIVERY' ? redeemAddressId.value : null
    })
    ElMessage.success('兑换成功！')
    showRedeemModal.value = false
    if (userStore.userInfo) {
      userStore.userInfo.currentPoints -= (data.data.pointsCost || 0)
      userStore.fetchMemberInfo()
    }
    loadProducts()
  } catch (error) {
    console.error('兑换出错:', error)
    ElMessage.error(error.message || '网络错误，请重试')
  } finally {
    isRedeeming.value = false
  }
}

onMounted(() => {
  loadProducts()
  loadAddresses()
})
</script>

<style scoped>
.mall-view {
  animation: fadeIn 0.4s ease-out;
  max-width: 1400px;
  margin: 0 auto;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
}

.content-header h3 {
  font-size: 28px;
  font-weight: 300;
  color: #1a1a1a;
  margin: 0;
}

.points-badge {
  color: #888;
  font-size: 14px;
}

.mall-category-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 28px;
  padding: 4px;
  background: rgba(245, 240, 235, 0.6);
  border-radius: 28px;
  width: fit-content;
}

.mall-category-tab {
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
  white-space: nowrap;
}

.mall-category-tab.active {
  background: #fff;
  color: #5D4037;
  box-shadow: 0 2px 8px rgba(166, 124, 82, 0.15);
  font-weight: 600;
}

.mall-category-tab:hover:not(.active) {
  background: rgba(255, 255, 255, 0.5);
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
}

.mall-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.mall-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-image {
  height: 180px;
  background-size: cover;
  background-position: center;
  background-color: #f5f0eb;
  flex-shrink: 0;
}

.card-details {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-details h4 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #333;
}

.product-desc {
  font-size: 12px;
  color: #888;
  margin-bottom: 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price {
  display: block;
  color: #C69C6D;
  font-weight: 600;
  margin-bottom: 15px;
}

.stock {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
}

.stock.low {
  color: #e74c3c;
}

.limit-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
}

.limit-tag {
  background: rgba(234, 182, 118, 0.15);
  color: #eab676;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid rgba(234, 182, 118, 0.3);
}

.redeemed-text {
  color: #666;
}

.redeem-btn {
  width: 100%;
  padding: 10px;
  background: #f0f0f0;
  border: none;
  color: #333;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: auto;
}

.redeem-btn:hover:not(:disabled) {
  background: #333;
  color: white;
}

.redeem-btn:disabled {
  background: #ccc !important;
  color: #fff !important;
  cursor: not-allowed;
  transform: none !important;
}

.no-data {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 14px;
  grid-column: 1 / -1;
}

/* Redeem Modal */
.redeem-modal {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.redeem-modal .modal-content {
  background: white;
  border-radius: 16px;
  padding: 30px !important;
  width: 650px !important;
  max-width: 95vw !important;
  max-height: 90vh !important;
  overflow-y: auto !important;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1);
}

.modal-content h3 {
  margin: 0 0 24px;
  text-align: center;
  font-size: 20px;
}

.product-preview {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f9f9f9;
  border-radius: 12px;
  margin-bottom: 24px;
}

.product-preview img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.product-preview .name { font-weight: 500; margin-bottom: 8px; }
.unit-price, .stock-info { font-size: 13px; color: #666; margin: 4px 0; }

.quantity-section {
  margin: 20px 0;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 10px;
}

.quantity-section label { font-weight: 500; color: #333; margin-right: 15px; }

.quantity-control {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
}

.qty-btn {
  width: 32px; height: 32px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  font-size: 18px;
  cursor: pointer;
  color: #333;
}

.qty-btn:hover:not(:disabled) { border-color: #C69C6D; color: #C69C6D; }
.qty-btn:disabled { background: #f0f0f0; color: #ccc; cursor: not-allowed; }

.qty-input {
  width: 60px; height: 32px;
  text-align: center;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
}

.qty-tip { display: block; margin-top: 8px; font-size: 12px; color: #999; }

.fulfillment-section { margin: 16px 0; }
.fulfillment-section label { display: block; margin-bottom: 8px; font-weight: 500; color: #374151; }

.radio-group-modern { display: flex; gap: 12px; }

.radio-card {
  flex: 1;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #f9fafb;
}

.radio-card.active { border-color: #d97706; background: #fffbeb; color: #d97706; font-weight: 500; }
.radio-card:hover { border-color: #d1d5db; }
.hidden-radio { display: none; }

.address-section .section-title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.text-btn { color: #d97706; font-size: 0.875rem; background: none; border: none; cursor: pointer; }

.modern-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background-color: #f9fafb;
  font-size: 0.95rem;
  color: #374151;
  outline: none;
  margin-top: 8px;
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 16px;
}

.pickup-hint { background: #f3f4f6; padding: 12px; border-radius: 8px; margin: 12px 0; border-left: 3px solid #d97706; }
.pickup-hint p { margin: 0; font-size: 0.9rem; color: #374151; }
.pickup-hint .sub-text { font-size: 0.8rem; color: #6b7280; margin-top: 4px; }

.redeem-summary-card {
  background: #fffbeb;
  padding: 16px;
  border-radius: 8px;
  margin-top: 20px;
}

.redeem-summary-card .row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 0.9rem; color: #4b5563; }
.redeem-summary-card .divider { border-top: 1px dashed #d1d5db; margin: 8px 0; }
.redeem-summary-card .row.total { font-weight: 600; font-size: 1.1rem; color: #d97706; margin-bottom: 0; }
.balance-refer { text-align: right; font-size: 0.8rem; color: #9ca3af; margin-top: 4px; }

.redeem-warning-box { margin-top: 16px; font-size: 0.8rem; color: #ef4444; background: #fef2f2; padding: 8px 12px; border-radius: 6px; }

.modal-actions {
  display: flex !important;
  gap: 20px !important;
  justify-content: center !important;
  margin-top: 24px !important;
}

.modal-actions .cancel-btn {
  min-width: 120px;
  padding: 10px 20px;
  background: #fff;
  border: 1px solid #d1d5db;
  color: #374151;
  border-radius: 8px;
  cursor: pointer;
}

.modal-actions .confirm-btn {
  min-width: 120px;
  padding: 10px 24px;
  background: linear-gradient(135deg, #d97706, #b45309);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}

.modal-actions .confirm-btn:disabled { background: #ccc; cursor: not-allowed; }

@media (max-width: 768px) {
  .redeem-modal .modal-content { width: 95vw !important; padding: 20px !important; }
}
</style>
