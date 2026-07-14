<!--
  订单确认页 - 选择配送方式、地址、支付
  
  功能：
  1. 展示购物车商品
  2. 选择自提/外卖
  3. 选择收货地址（外卖模式）
  4. 选择优惠券
  5. 计算实付金额和预估积分
  6. 提交订单
-->
<template>
  <view class="confirm-page">
    <!-- 配送方式选择 -->
    <view class="delivery-section">
      <view 
        class="delivery-option" 
        :class="{ active: deliveryType === 'pickup' }"
        @click="deliveryType = 'pickup'"
      >
        <text class="option-icon">🏪</text>
        <text class="option-text">到店自提</text>
      </view>
      <view 
        class="delivery-option"
        :class="{ active: deliveryType === 'delivery' }"
        @click="deliveryType = 'delivery'"
      >
        <text class="option-icon">🛵</text>
        <text class="option-text">外卖配送</text>
      </view>
    </view>
    
    <!-- 地址选择（外卖模式） -->
    <view class="address-section" v-if="deliveryType === 'delivery'">
      <view class="section-title">收货地址</view>
      <view class="address-card" v-if="selectedAddress" @click="showAddressPicker = true">
        <view class="address-info">
          <view class="address-top">
            <text class="receiver">{{ selectedAddress.name }}</text>
            <text class="phone">{{ selectedAddress.phone }}</text>
          </view>
          <text class="address-detail">{{ selectedAddress.address }}</text>
        </view>
        <text class="address-arrow">></text>
      </view>
      <view class="add-address" v-else @click="showAddressPicker = true">
        <text>+ 选择收货地址</text>
      </view>
    </view>
    
    <!-- 门店信息（自提模式） -->
    <view class="store-section" v-if="deliveryType === 'pickup'">
      <view class="section-title">自提门店</view>
      <view class="store-card">
        <text class="store-name">CozyCoffee 中关村店</text>
        <text class="store-address">北京市海淀区中关村大街1号</text>
        <text class="store-time">营业时间：08:00 - 22:00</text>
      </view>
    </view>
    
    <!-- 商品列表 -->
    <view class="products-section">
      <view class="section-title">商品清单</view>
      <view class="product-list">
        <view class="product-item" v-for="item in discountedItems" :key="item.id">
          <image :src="item.image" class="product-image" mode="aspectFill" />
          <view class="product-info">
            <text class="product-name">{{ item.name }}</text>
            <text class="product-spec" v-if="item.specText">{{ item.specText }}</text>
          </view>
          <view class="product-right">
            <view class="product-price-row">
              <text v-if="item.discountAmount > 0" class="product-price-original">¥{{ item.price }}</text>
              <text :class="item.discountAmount > 0 ? 'product-price-discounted' : 'product-price'">
                ¥{{ (item.discountedPrice ?? item.price).toFixed(0) }}
              </text>
            </view>
            <text class="product-qty">x{{ item.quantity }}</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- v5.0 加料选择 -->
    <view class="addons-section">
      <view class="section-title">加料/配料</view>
      <view class="addons-list">
        <view 
          class="addon-item" 
          v-for="addon in availableAddons" 
          :key="addon.code"
          :class="{ selected: selectedAddons.includes(addon.code) }"
          @click="toggleAddon(addon)"
        >
          <view class="addon-info">
            <text class="addon-name">{{ addon.name }}</text>
            <text class="addon-price">+¥{{ addon.price }}</text>
          </view>
          <view class="addon-check" v-if="selectedAddons.includes(addon.code)">✓</view>
        </view>
      </view>
      <view class="addons-total" v-if="addonsAmount > 0">
        <text>加料费用: +¥{{ addonsAmount }}</text>
        <text class="addon-discount" v-if="shotCouponDiscount > 0">（浓缩券抵扣: -¥{{ shotCouponDiscount }}）</text>
      </view>
    </view>
    
    <!-- 优惠券选择（主券） -->
    <view class="coupon-section" @click="showCouponPicker = true">
      <text class="section-label">优惠券</text>
      <view class="coupon-value">
        <text v-if="selectedCoupon" class="coupon-discount">-¥{{ selectedCoupon.value }}</text>
        <text v-else class="coupon-placeholder">{{ availableCoupons }} 张可用</text>
        <text class="coupon-arrow">></text>
      </view>
    </view>
    
    <!-- v5.0 附加券选择 -->
    <view class="addon-coupons-section" v-if="addonCoupons.length > 0">
      <view class="section-title">附加券（可与主券叠加）</view>
      <view class="addon-coupon-list">
        <view 
          class="addon-coupon-item" 
          v-for="coupon in addonCoupons" 
          :key="coupon.id"
          :class="{ selected: selectedAddonCoupons.some(c => c.id === coupon.id) }"
          @click="toggleAddonCoupon(coupon)"
        >
          <view class="coupon-info">
            <text class="coupon-name">{{ coupon.name }}</text>
            <text class="coupon-desc">{{ getAddonCouponDesc(coupon) }}</text>
          </view>
          <view class="coupon-check" v-if="selectedAddonCoupons.some(c => c.id === coupon.id)">✓</view>
        </view>
      </view>
    </view>
    
    <!-- 备注 -->
    <view class="remark-section">
      <text class="section-label">备注</text>
      <input 
        v-model="remark" 
        placeholder="如需备注请输入（选填）" 
        class="remark-input"
      />
    </view>
    
    <!-- 价格明细 -->
    <view class="price-section">
      <view class="price-item">
        <text class="price-label">商品金额</text>
        <text class="price-value">¥{{ cartStore.totalPrice }}</text>
      </view>
      <view class="price-item" v-if="deliveryType === 'delivery'">
        <text class="price-label">配送费</text>
        <text class="price-value">¥{{ deliveryFee }}</text>
      </view>
      <view class="price-item" v-if="selectedCoupon">
        <text class="price-label">优惠券</text>
        <text class="price-value discount">-¥{{ selectedCoupon.value }}</text>
      </view>
      <view class="price-item total">
        <text class="price-label">实付金额</text>
        <text class="price-value total-price">¥{{ finalPrice }}</text>
      </view>
      <view class="points-preview">
        <text>预计获得 <text class="points-value">{{ estimatedPoints }}</text> 积分</text>
      </view>
    </view>
    
    <!-- 底部提交栏 -->
    <view class="submit-bar safe-area-bottom">
      <view class="submit-left">
        <text class="submit-total">¥{{ finalPrice }}</text>
        <text class="submit-hint">共 {{ cartStore.totalCount }} 件</text>
      </view>
      <view class="submit-btn" @click="submitOrder">
        提交订单
      </view>
    </view>
    <!-- 优惠券弹窗 -->
    <view class="popup-mask" v-if="showCouponPicker" @click="showCouponPicker = false">
      <view class="popup-content" @click.stop>
        <view class="popup-header">
          <text class="popup-title">选择优惠券</text>
          <text class="popup-close" @click="showCouponPicker = false">×</text>
        </view>
        <scroll-view scroll-y class="popup-list">
          <view class="popup-item" @click="selectCoupon(null)">
            <text class="item-name">不使用优惠券</text>
            <text class="item-check" v-if="!selectedCoupon">✓</text>
          </view>
          <view 
            class="popup-item" 
            v-for="coupon in coupons" 
            :key="coupon.id"
            @click="selectCoupon(coupon)"
          >
            <view class="item-left">
              <text class="item-name">{{ coupon.name }}</text>
              <text class="item-desc" v-if="coupon.minAmount">满{{ coupon.minAmount }}可用</text>
            </view>
            <view class="item-right">
              <text class="item-value">-¥{{ coupon.value }}</text>
              <text class="item-check" v-if="selectedCoupon?.id === coupon.id">✓</text>
            </view>
          </view>
          <view class="empty-tip" v-if="coupons.length === 0">暂无可用优惠券</view>
        </scroll-view>
      </view>
    </view>
    
    <!-- 地址选择弹窗（简化版） -->
    <view class="popup-mask" v-if="showAddressPicker" @click="showAddressPicker = false">
      <view class="popup-content" @click.stop>
        <view class="popup-header">
          <text class="popup-title">选择地址</text>
          <text class="popup-close" @click="showAddressPicker = false">×</text>
        </view>
        <view class="address-tip" style="padding: 40rpx; text-align: center; color: #999;">
          暂无更多地址，请去个人中心添加
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { createOrder } from '@/api/order'
import { getCouponList } from '@/api/coupon'
import { getDefaultAddress, getMemberInfo } from '@/api/member'

const cartStore = useCartStore()
const userStore = useUserStore()

// 配送方式：pickup 自提 / delivery 外卖
const deliveryType = ref('pickup')

// 配送费
const deliveryFee = ref(5)

// 选中的地址
const selectedAddress = ref(null)

// 主券选择
const selectedCoupon = ref(null)
const coupons = ref([])
const availableCoupons = computed(() => coupons.value.filter(c => c.status === 'available').length)

// v5.0 加料选择
const availableAddons = ref([
  { code: 'EXTRA_SHOT', name: '额外浓缩', price: 5 },
  { code: 'OAT_MILK', name: '换燕麦奶', price: 6 },
  { code: 'COCONUT_MILK', name: '换椰奶', price: 5 },
  { code: 'EXTRA_FOAM', name: '加奶泡', price: 3 }
])
const selectedAddons = ref([])

// v5.0 附加券（SHOT/DELIVERY_FEE类型）
const addonCoupons = ref([])
const selectedAddonCoupons = ref([])

// 备注
const remark = ref('')

// 弹窗控制
const showAddressPicker = ref(false)
const showCouponPicker = ref(false)

// 加载状态
const submitting = ref(false)

// 页面加载时获取数据
onMounted(async () => {
  // 刷新会员信息（获取最新的黑卡加速额度）
  try {
    const memberRes = await getMemberInfo()
    if (memberRes.code === 200 && memberRes.data) {
      userStore.setMemberInfo(memberRes.data)
    }
  } catch (e) {
    console.error('刷新会员信息失败', e)
  }

  // 获取默认收货地址
  try {
    const addressRes = await getDefaultAddress()
    if (addressRes.code === 200 && addressRes.data) {
      selectedAddress.value = {
        id: addressRes.data.id,
        name: addressRes.data.receiverName,
        phone: addressRes.data.receiverPhone,
        address: `${addressRes.data.province}${addressRes.data.city}${addressRes.data.district}${addressRes.data.detailAddress}`
      }
    }
  } catch (e) {
    console.error('获取默认地址失败', e)
  }
  
  // 获取可用优惠券
  try {
    const couponRes = await getCouponList('ISSUED')
    console.log('确认页-原始优惠券数据:', couponRes.data)
    
    if (couponRes.code === 200 && couponRes.data) {
      // 宽松过滤：只要是 ISSUED 状态就算可用
      const validCoupons = couponRes.data.filter(c => c.status === 'ISSUED')
      console.log('确认页-筛选后的优惠券:', validCoupons)

      coupons.value = validCoupons
        .filter(c => !['SHOT', 'DELIVERY_FEE'].includes(c.couponType)) // 主券排除附加券类型
        .map(item => ({
        ...item,
        value: item.value || item.couponValue || 0,
        name: item.productName || getCouponName(item.couponType),
        expireDate: formatDate(item.expiresAt),
        status: 'available'
      }))
      
      // v5.0 附加券单独提取
      addonCoupons.value = validCoupons
        .filter(c => ['SHOT', 'DELIVERY_FEE'].includes(c.couponType))
        .map(item => ({
          ...item,
          value: item.value || item.couponValue || 5,
          name: item.productName || getAddonCouponName(item.couponType)
        }))

      // 自动选中优惠力度最大的可用券
      autoSelectBestCoupon()
    }
  } catch (e) {
    console.error('获取优惠券失败', e)
    coupons.value = []
  }
})

/**
 * 自动选中折扣最大的可用优惠券
 */
const autoSelectBestCoupon = () => {
  if (selectedCoupon.value) return // 已有选中的券，不覆盖

  const cartTotal = parseFloat(cartStore.totalPrice)
  const usable = coupons.value.filter(c => {
    const minAmount = c.minAmount || 0
    return minAmount <= cartTotal
  })

  if (usable.length === 0) return

  // 按优惠金额降序，选最大的
  usable.sort((a, b) => (b.value || 0) - (a.value || 0))
  selectedCoupon.value = usable[0]
}

// 辅助函数
const getCouponName = (type) => {
  const map = { 
    'EXCHANGE': '咖啡兑换券', 
    'DISCOUNT': '折扣券', 
    'FULL_REDUCE': '满减券',
    'BOGO': '买一送一券'
  }
  return map[type] || '优惠券'
}

const getAddonCouponName = (type) => {
  const map = { 'SHOT': '加浓缩券', 'DELIVERY_FEE': '配送费抵扣券' }
  return map[type] || '附加券'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  } catch { return dateStr }
}

// 选择优惠券
const selectCoupon = (coupon) => {
  selectedCoupon.value = coupon
  showCouponPicker.value = false
}

// v5.0 切换加料选择
const toggleAddon = (addon) => {
  const index = selectedAddons.value.indexOf(addon.code)
  if (index === -1) {
    selectedAddons.value.push(addon.code)
  } else {
    selectedAddons.value.splice(index, 1)
  }
}

// v5.0 切换附加券选择
const toggleAddonCoupon = (coupon) => {
  const index = selectedAddonCoupons.value.findIndex(c => c.id === coupon.id)
  if (index === -1) {
    // 最多选择2张附加券
    if (selectedAddonCoupons.value.length < 2) {
      selectedAddonCoupons.value.push(coupon)
    }
  } else {
    selectedAddonCoupons.value.splice(index, 1)
  }
}

// v5.0 获取附加券描述
const getAddonCouponDesc = (coupon) => {
  if (coupon.couponType === 'SHOT') return '抵扣加浓缩费用 ¥5'
  if (coupon.couponType === 'DELIVERY_FEE') return '抵扣配送费'
  return ''
}

// v5.0 计算加料总金额
const addonsAmount = computed(() => {
  return selectedAddons.value.reduce((sum, code) => {
    const addon = availableAddons.value.find(a => a.code === code)
    return sum + (addon?.price || 0)
  }, 0)
})

// v5.0 计算浓缩券抵扣金额
const shotCouponDiscount = computed(() => {
  const shotCoupon = selectedAddonCoupons.value.find(c => c.couponType === 'SHOT')
  if (shotCoupon && selectedAddons.value.includes('EXTRA_SHOT')) {
    return Math.min(shotCoupon.value || 5, 5) // 最多抵扣5元
  }
  return 0
})

// v5.0 计算配送费抵扣金额
const deliveryFeeDiscount = computed(() => {
  const deliveryCoupon = selectedAddonCoupons.value.find(c => c.couponType === 'DELIVERY_FEE')
  if (deliveryCoupon && deliveryType.value === 'delivery') {
    return Math.min(deliveryCoupon.value || 10, deliveryFee.value)
  }
  return 0
})

/**
 * 计算实付金额
 */
const finalPrice = computed(() => {
  let total = parseFloat(cartStore.totalPrice)
  
  // v5.0 加上加料费用
  total += addonsAmount.value
  
  // 加配送费
  if (deliveryType.value === 'delivery') {
    total += deliveryFee.value
  }
  
  // 减主券优惠
  if (selectedCoupon.value) {
    total -= (selectedCoupon.value.value || selectedCoupon.value.couponValue || 0)
  }
  
  // v5.0 减附加券优惠
  total -= shotCouponDiscount.value
  total -= deliveryFeeDiscount.value
  
  return Math.max(0, total).toFixed(2)
})

/**
 * 预估积分（简化计算：实付 × 1.0，后续可接入等级倍率）
 */
const estimatedPoints = computed(() => {
  const level = userStore.userLevel
  const price = parseFloat(finalPrice.value)
  
  if (level === 'black') {
    // 黑卡加速包逻辑
    // 获取本月剩余加速额度（注意：应从 memberInfo 中获取）
    const remainingQuota = parseFloat(userStore.memberInfo?.monthlyAccelerateRemaining || 0)
    
    if (remainingQuota > 0) {
      if (price <= remainingQuota) {
        console.log(`[积分计算] 黑卡全额加速: ${price} * 1.7`)
        return Math.round(price * 1.7)
      } else {
        const acceleratedPart = remainingQuota * 1.7
        const normalPart = (price - remainingQuota) * 1.35
        console.log(`[积分计算] 黑卡混合计算: 额度内 ${remainingQuota}*1.7 + 超出 ${price-remainingQuota}*1.35`)
        return Math.round(acceleratedPart + normalPart)
      }
    } else {
      console.log(`[积分计算] 黑卡额度用完: ${price} * 1.35`)
      return Math.round(price * 1.35)
    }
  }

  const rateMap = { basic: 1, silver: 1.1, gold: 1.2, diamond: 1.3 }
  const rate = rateMap[level] || 1
  console.log(`[积分计算] 普通等级 ${level}: ${price} * ${rate}`)
  return Math.round(price * rate)
})

/**
 * 每件商品的优惠后价格
 * 将选中的优惠券折扣按价格比例分配到每个商品
 */
const discountedItems = computed(() => {
  const items = cartStore.items.map(i => ({
    ...i,
    discountedPrice: parseFloat(i.price),
    discountAmount: 0
  }))

  if (!selectedCoupon.value) return items

  const coupon = selectedCoupon.value
  const couponValue = parseFloat(coupon.value || coupon.couponValue || 0)
  if (couponValue <= 0) return items

  const totalAmount = items.reduce((s, i) => s + parseFloat(i.price) * i.quantity, 0)
  if (totalAmount <= 0) return items

  // 按价格比例分配折扣，余数给最后一个商品
  let remaining = couponValue
  items.forEach((item, idx) => {
    if (idx < items.length - 1) {
      item.discountAmount = parseFloat((couponValue * (parseFloat(item.price) * item.quantity / totalAmount) / item.quantity).toFixed(2))
      remaining -= item.discountAmount * item.quantity
    }
  })
  const last = items[items.length - 1]
  last.discountAmount = parseFloat((remaining / last.quantity).toFixed(2))

  items.forEach(item => {
    item.discountAmount = Math.max(0, item.discountAmount)
    item.discountedPrice = Math.max(0, parseFloat((parseFloat(item.price) - item.discountAmount).toFixed(2)))
  })

  return items
})

const submitOrder = async () => {
  // ...
  if (submitting.value) return
  
  if (cartStore.items.length === 0) {
    uni.showToast({ title: '购物车为空', icon: 'none' })
    return
  }
  
  if (deliveryType.value === 'delivery' && !selectedAddress.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' })
    return
  }
  
  submitting.value = true
  uni.showLoading({ title: '提交中...' })
  
  try {
    // 构建订单请求
    const orderData = {
      items: cartStore.items.map(item => ({
        productId: item.id,
        quantity: item.quantity,
        cupSize: item.cupSize || 'MEDIUM',
        sugarLevel: item.sugarLevel || 'NORMAL',
        temperature: item.temperature || 'HOT'
      })),
      remark: remark.value || ''
    }
    
    // 如果选择了优惠券，传递 couponCode
    if (selectedCoupon.value) {
      // 优先使用 couponCode 字段
      orderData.couponCode = selectedCoupon.value.couponCode || selectedCoupon.value.code
    }
    
    console.log('提交订单数据:', orderData)
    
    const res = await createOrder(orderData)
    
    uni.hideLoading()
    
    if (res.code === 200) {
      console.log('下单成功，后端返回:', res.data)
      console.log('后端计算积分:', res.data.pointsEarned)
      console.log('前端预估积分:', estimatedPoints.value)

      // 清空购物车
      cartStore.clearCart()
      
      // 跳转到成功页
      uni.redirectTo({
        url: `/pages/order/result?orderId=${res.data.id}&orderNo=${res.data.orderNo}&points=${res.data.pointsEarned || estimatedPoints.value}&total=${res.data.payAmount || finalPrice.value}&pickupCode=${res.data.pickupCode || ''}`
      })
    } else {
      console.error('下单失败:', res)
      // 显示具体错误信息（如：未满足满减门槛）
      uni.showModal({
        title: '无法使用优惠券',
        content: res.message || '下单失败',
        showCancel: false
      })
    }
  } catch (e) {
    uni.hideLoading()
    console.error('提交订单失败', e)
    uni.showToast({ title: '网络错误，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.confirm-page {
  min-height: 100vh;
  background: $bg-color;
  padding-bottom: 140rpx;
}

// 配送方式
.delivery-section {
  display: flex;
  gap: $spacing-md;
  padding: $spacing-md;
  background: $bg-white;
  
  .delivery-option {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: $spacing-lg $spacing-md;
    border: 2rpx solid $border-color;
    border-radius: $border-radius-md;
    transition: all 0.3s;
    
    &.active {
      border-color: $primary-color;
      background: rgba($primary-color, 0.05);
    }
    
    .option-icon {
      font-size: 48rpx;
      margin-bottom: $spacing-xs;
    }
    
    .option-text {
      font-size: $font-size-md;
      color: $text-primary;
    }
  }
}

// 地址卡片
.address-section, .store-section {
  background: $bg-white;
  margin-top: $spacing-sm;
  padding: $spacing-md;
}

.section-title {
  font-size: $font-size-sm;
  color: $text-placeholder;
  margin-bottom: $spacing-sm;
}

.address-card {
  display: flex;
  align-items: center;
  
  .address-info {
    flex: 1;
    
    .address-top {
      margin-bottom: $spacing-xs;
      
      .receiver {
        font-size: $font-size-lg;
        font-weight: 600;
        margin-right: $spacing-md;
      }
      
      .phone {
        font-size: $font-size-md;
        color: $text-secondary;
      }
    }
    
    .address-detail {
      font-size: $font-size-sm;
      color: $text-secondary;
    }
  }
  
  .address-arrow {
    color: $text-placeholder;
    font-size: $font-size-lg;
  }
}

.add-address {
  padding: $spacing-lg;
  text-align: center;
  color: $primary-color;
  border: 2rpx dashed $primary-color;
  border-radius: $border-radius-md;
}

.store-card {
  .store-name {
    font-size: $font-size-lg;
    font-weight: 600;
    display: block;
    margin-bottom: $spacing-xs;
  }
  
  .store-address, .store-time {
    font-size: $font-size-sm;
    color: $text-secondary;
    display: block;
  }
}

// 商品列表
.products-section {
  background: $bg-white;
  margin-top: $spacing-sm;
  padding: $spacing-md;
}

.product-item {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;
  border-bottom: 1rpx solid $border-color;
  
  &:last-child {
    border-bottom: none;
  }
  
  .product-image {
    width: 100rpx;
    height: 100rpx;
    border-radius: $border-radius-sm;
  }
  
  .product-info {
    flex: 1;
    margin-left: $spacing-sm;
    
    .product-name {
      font-size: $font-size-md;
      display: block;
    }
    
    .product-spec {
      font-size: $font-size-sm;
      color: $text-placeholder;
    }
  }
  
  .product-right {
    text-align: right;

    .product-price-row {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
    }

    .product-price {
      font-size: $font-size-md;
      display: block;
    }

    .product-price-original {
      font-size: $font-size-xs;
      color: $text-placeholder;
      text-decoration: line-through;
    }

    .product-price-discounted {
      font-size: $font-size-md;
      color: $primary-dark;
      font-weight: 700;
    }

    .product-qty {
      font-size: $font-size-sm;
      color: $text-placeholder;
    }
  }
}

// v5.0 加料选择
.addons-section {
  background: $bg-white;
  margin-top: $spacing-sm;
  padding: $spacing-md;
  
  .addons-list {
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-sm;
    margin-bottom: $spacing-sm;
  }
  
  .addon-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $spacing-sm $spacing-md;
    border: 2rpx solid $border-color;
    border-radius: $border-radius-md;
    min-width: 200rpx;
    transition: all 0.2s;
    
    &.selected {
      border-color: $primary-color;
      background: rgba($primary-color, 0.05);
    }
    
    .addon-info {
      .addon-name {
        font-size: $font-size-sm;
        display: block;
      }
      .addon-price {
        font-size: $font-size-xs;
        color: $primary-color;
      }
    }
    
    .addon-check {
      color: $primary-color;
      font-weight: bold;
      margin-left: $spacing-sm;
    }
  }
  
  .addons-total {
    font-size: $font-size-sm;
    color: $text-secondary;
    
    .addon-discount {
      color: $success-color;
    }
  }
}

// v5.0 附加券选择
.addon-coupons-section {
  background: $bg-white;
  margin-top: $spacing-sm;
  padding: $spacing-md;
  
  .addon-coupon-list {
    display: flex;
    flex-direction: column;
    gap: $spacing-sm;
  }
  
  .addon-coupon-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-sm $spacing-md;
    border: 2rpx solid $border-color;
    border-radius: $border-radius-md;
    background: #f9fafb;
    
    &.selected {
      border-color: $success-color;
      background: rgba($success-color, 0.05);
    }
    
    .coupon-info {
      .coupon-name {
        font-size: $font-size-md;
        font-weight: 600;
        display: block;
      }
      .coupon-desc {
        font-size: $font-size-xs;
        color: $text-placeholder;
      }
    }
    
    .coupon-check {
      color: $success-color;
      font-weight: bold;
    }
  }
}

// 优惠券
.coupon-section, .remark-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md;
  background: $bg-white;
  margin-top: $spacing-sm;
  
  .section-label {
    font-size: $font-size-md;
    color: $text-primary;
  }
  
  .coupon-value {
    display: flex;
    align-items: center;
    
    .coupon-discount {
      color: $error-color;
      font-weight: 600;
    }
    
    .coupon-placeholder {
      color: $text-placeholder;
    }
    
    .coupon-arrow {
      margin-left: $spacing-sm;
      color: $text-placeholder;
    }
  }
}

.remark-input {
  flex: 1;
  text-align: right;
  font-size: $font-size-md;
}

// 价格明细
.price-section {
  background: $bg-white;
  margin-top: $spacing-sm;
  padding: $spacing-md;
  
  .price-item {
    display: flex;
    justify-content: space-between;
    padding: $spacing-xs 0;
    
    .price-label {
      color: $text-secondary;
    }
    
    .price-value {
      color: $text-primary;
      
      &.discount {
        color: $error-color;
      }
      
      &.total-price {
        font-size: $font-size-xl;
        font-weight: 700;
        color: $primary-color;
      }
    }
    
    &.total {
      padding-top: $spacing-sm;
      margin-top: $spacing-sm;
      border-top: 1rpx solid $border-color;
    }
  }
  
  .points-preview {
    text-align: right;
    font-size: $font-size-sm;
    color: $text-placeholder;
    margin-top: $spacing-xs;
    
    .points-value {
      color: $success-color;
      font-weight: 600;
    }
  }
}

// 底部提交栏
.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  background: $bg-white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $spacing-md;
  box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.05);
  
  .submit-left {
    .submit-total {
      font-size: $font-size-xl;
      font-weight: 700;
      color: $primary-color;
    }
    
    .submit-hint {
      font-size: $font-size-xs;
      color: $text-placeholder;
      margin-left: $spacing-sm;
    }
  }
  
  .submit-btn {
    background: linear-gradient(135deg, $primary-color, $primary-dark);
    color: white;
    padding: $spacing-sm $spacing-xl;
    border-radius: 40rpx;
    font-size: $font-size-md;
    font-weight: 600;
  }
}

// 弹窗样式
.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 100;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  
  .popup-content {
    background: white;
    border-radius: 24rpx 24rpx 0 0;
    min-height: 500rpx;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
    
    .popup-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: $spacing-md;
      border-bottom: 1rpx solid $border-color;
      
      .popup-title {
        font-size: $font-size-lg;
        font-weight: 600;
      }
      
      .popup-close {
        font-size: 40rpx;
        color: $text-secondary;
        padding: 0 $spacing-sm;
      }
    }
    
    .popup-list {
      flex: 1;
      overflow-y: auto;
      
      .popup-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: $spacing-md;
        border-bottom: 1rpx solid $border-color;
        
        .item-name {
          font-size: $font-size-md;
          color: $text-primary;
        }
        
        .item-desc {
          font-size: $font-size-xs;
          color: $text-secondary;
          margin-top: 6rpx;
          display: block;
        }
        
        .item-right {
          text-align: right;
          
          .item-value {
            color: $error-color;
            font-weight: 600;
            display: block;
          }
          
          .item-check {
            color: $primary-color;
            font-weight: bold;
          }
        }
      }
      
      .empty-tip {
        padding: 50rpx;
        text-align: center;
        color: $text-placeholder;
      }
    }
  }
}
</style>
