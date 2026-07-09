<template>
  <div class="coupon-card" :class="['theme-' + couponTheme, { expired: isExpired }]">
    <!-- 左侧：价值锚点 -->
    <div class="card-left">
      <div class="value-zone">
        <span class="value-symbol">{{ valueSymbol }}</span>
        <span class="value-type">{{ valueType }}</span>
      </div>
    </div>

    <!-- 中间：信息与限制 -->
    <div class="card-center">
      <div class="header">
        <span class="title" :class="{ premium: isPremium }">
          {{ premiumIcon }}{{ coupon.productName || getCouponTitle() }}
        </span>
        <span v-if="sourceTag" class="source-tag">{{ sourceTag }}</span>
      </div>

      <!-- 结构化限制区 -->
      <div v-if="restrictionTags.length > 0" class="limit-tags">
        <span 
          v-for="(tag, index) in restrictionTags" 
          :key="index"
          class="tag"
          :class="tag.type"
        >
          {{ tag.icon }} {{ tag.text }}
        </span>
      </div>

      <div class="date-row">
        <span class="date" :class="{ urgent: daysLeft <= 3 && daysLeft > 0 }">
          {{ expiryText }}
        </span>
        <span v-if="daysLeft > 0 && daysLeft <= 7" class="days-left">
          剩{{ daysLeft }}天
        </span>
      </div>
    </div>

    <!-- 右侧：行动区 -->
    <div class="card-right">
      <div v-if="daysLeft <= 3 && daysLeft > 0" class="urgent-tip">
        即将过期
      </div>
      <button 
        class="btn-use" 
        :class="{ premium: isPremium, disabled: isExpired }"
        :disabled="isExpired"
        @click="handleUse"
      >
        {{ isExpired ? '已过期' : '去使用' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  coupon: {
    type: Object,
    required: true
  }
})

const router = useRouter()

// 解析券规则
const rules = computed(() => {
  try {
    return JSON.parse(props.coupon.ruleJson || '{}')
  } catch {
    return {}
  }
})

// 券主题色判断
const couponTheme = computed(() => {
  const code = props.coupon.couponCode || ''
  const type = props.coupon.couponType
  
  // 身份等级系列
  if (code.includes('BLACK') || code.includes('尊享')) return 'black-gold'
  if (code.includes('DIAMOND')) return 'diamond'
  if (code.includes('GOLD')) return 'gold'
  
  // 营销社交系列
  if (type === 'BOGO') return 'social'
  if (code.includes('NEW') || code.includes('新品')) return 'promo'
  
  // 辅助系列
  if (code.includes('CAKE') || code.includes('蛋糕')) return 'dessert'
  if (code.includes('DELIVERY') || code.includes('配送')) return 'utility'
  if (code.includes('SHOT') || code.includes('加浓')) return 'utility'
  
  return 'silver' // 默认银色
})

// 左侧价值符号
const valueSymbol = computed(() => {
  const type = props.coupon.couponType
  
  if (type === 'EXCHANGE' || type === 'FREE_ITEM') {
    // 黑金特殊处理
    if (couponTheme.value === 'black-gold') return 'ULTRA'
    return 'Free'
  }
  if (type === 'BOGO') return '1+1'
  if (type === 'DISCOUNT') {
    const rate = rules.value.discountRate || props.coupon.value / 100
    if (rate === 0.5) return '50%'
    if (rate === 0.88) return '8.8折'
    return `${Math.round(rate * 10)}折`
  }
  if (type === 'FULL_REDUCE') return `¥${props.coupon.value || 0}`
  return '🎁'
})

// 左侧类型标签
const valueType = computed(() => {
  const type = props.coupon.couponType
  if (type === 'EXCHANGE' && couponTheme.value === 'black-gold') return '全通兑'
  if (type === 'EXCHANGE') return '免单'
  if (type === 'BOGO') return '买一送一'
  if (type === 'DISCOUNT') return '折扣'
  if (type === 'FULL_REDUCE') return '代金'
  return '兑换'
})

// 是否为尊享券
const isPremium = computed(() => {
  return couponTheme.value === 'black-gold'
})

// 尊享图标
const premiumIcon = computed(() => {
  return isPremium.value ? '👑 ' : ''
})

// 来源标签
const sourceTag = computed(() => {
  const code = props.coupon.couponCode || ''
  if (code.includes('BIRTHDAY')) return '生日礼'
  if (code.includes('MONTHLY')) return '月权益'
  if (code.includes('UPGRADE')) return '晋升礼'
  if (code.includes('NEW_USER')) return '新人礼'
  return ''
})

// 限制标签生成
const restrictionTags = computed(() => {
  const tags = []
  const r = rules.value
  
  // 杯型限制
  if (r.skuLimit === 'STANDARD_ONLY') {
    tags.push({ icon: '🔒', text: '仅限标准杯', type: 'lock' })
  } else if (r.skuLimit === 'ALL') {
    tags.push({ icon: '✅', text: '不限杯型', type: 'check' })
  }
  
  // 品类限制
  const blocklist = r.categoryBlocklist || []
  if (blocklist.length === 0 && r.skuLimit === 'ALL') {
    tags.push({ icon: '✅', text: '含SOE手冲', type: 'check' })
  } else if (blocklist.includes('signature')) {
    tags.push({ icon: '🚫', text: '排除特调', type: 'ban' })
  } else if (!blocklist.includes('signature') && r.skuLimit) {
    tags.push({ icon: '✅', text: '含特调', type: 'check' })
  }
  
  if (blocklist.includes('soe')) {
    tags.push({ icon: '🚫', text: '不含SOE', type: 'ban' })
  }
  
  // 免费加料
  if (r.freeAddon) {
    tags.push({ icon: '✅', text: '免1份加料', type: 'check' })
  }
  
  // 单品限制
  if (r.limit === 'SINGLE_ITEM') {
    tags.push({ icon: '🔒', text: '限单饮品', type: 'lock' })
  }
  
  // 封顶提示
  if (r.maxDiscount && r.maxDiscount < 100) {
    tags.push({ icon: '💰', text: `封顶¥${r.maxDiscount}`, type: 'warning' })
  } else if (r.maxDiscountAmount) {
    tags.push({ icon: '💰', text: `最高抵¥${r.maxDiscountAmount}`, type: 'warning' })
  }
  
  // BOGO特殊规则
  if (props.coupon.couponType === 'BOGO') {
    tags.push({ icon: '👥', text: '免低价杯', type: 'rule' })
    if (r.maxDiscount) {
      tags.push({ icon: '💰', text: `赠品封顶¥${r.maxDiscount}`, type: 'warning' })
    }
  }
  
  // 配送费券
  if (props.coupon.couponCode?.includes('DELIVERY')) {
    tags.push({ icon: '🛵', text: '仅限外卖', type: 'rule' })
  }
  
  // 甜品券
  if (props.coupon.couponCode?.includes('CAKE')) {
    tags.push({ icon: '🍰', text: '限切片蛋糕', type: 'rule' })
  }
  
  return tags.slice(0, 3) // 最多显示3个标签
})

// 到期时间显示
const expiryText = computed(() => {
  if (!props.coupon.expiresAt) return '永久有效'
  const date = new Date(props.coupon.expiresAt)
  return `有效期至 ${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
})

// 剩余天数
const daysLeft = computed(() => {
  if (!props.coupon.expiresAt) return 999
  const now = new Date()
  const expiry = new Date(props.coupon.expiresAt)
  const diff = expiry - now
  return Math.ceil(diff / (1000 * 60 * 60 * 24))
})

// 是否过期
const isExpired = computed(() => {
  return props.coupon.status === 'EXPIRED' || daysLeft.value <= 0
})

// 券标题
const getCouponTitle = () => {
  return props.coupon.description || props.coupon.productName || '优惠券'
}

// 使用券
const handleUse = () => {
  if (isExpired.value) return
  
  // 智能筛选参数
  const filter = []
  const r = rules.value
  
  if (r.skuLimit === 'STANDARD_ONLY') filter.push('standard_only')
  if (r.categoryBlocklist?.includes('signature')) filter.push('exclude_signature')
  if (r.categoryBlocklist?.includes('soe')) filter.push('exclude_soe')
  
  // 跳转到点单页，带上筛选参数和券ID
  router.push({
    path: '/',
    query: {
      couponId: props.coupon.id,
      filter: filter.join(','),
      autoSelect: r.skuLimit === 'STANDARD_ONLY' ? 'medium' : null
    }
  })
}
</script>

<style scoped lang="scss">
.coupon-card {
  display: flex;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  position: relative;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }

  &.expired {
    opacity: 0.6;
    filter: grayscale(0.5);
  }

  // 左侧价值区
  .card-left {
    width: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    position: relative;

    .value-zone {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
    }

    .value-symbol {
      font-size: 32px;
      line-height: 1;
    }

    .value-type {
      font-size: 14px;
      opacity: 0.9;
    }
  }

  // 中间信息区
  .card-center {
    flex: 1;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .header {
      display: flex;
      align-items: center;
      gap: 8px;

      .title {
        font-size: 16px;
        font-weight: 600;
        color: #333;

        &.premium {
          background: linear-gradient(135deg, #D4AF37 0%, #FFD700 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }
      }

      .source-tag {
        font-size: 12px;
        padding: 2px 8px;
        border-radius: 10px;
        background: #f0f0f0;
        color: #666;
      }
    }

    .limit-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;

      .tag {
        display: inline-flex;
        align-items: center;
        font-size: 12px;
        padding: 4px 10px;
        border-radius: 12px;
        white-space: nowrap;

        &.check {
          background: #E8F5E9;
          color: #2E7D32;
        }

        &.lock {
          background: #F5F5F5;
          color: #757575;
        }

        &.ban {
          background: #FFEBEE;
          color: #C62828;
        }

        &.warning {
          background: #FFF3E0;
          color: #E65100;
        }

        &.rule {
          background: #E3F2FD;
          color: #1565C0;
        }
      }
    }

    .date-row {
      display: flex;
      align-items: center;
      gap: 8px;

      .date {
        font-size: 12px;
        color: #999;

        &.urgent {
          color: #FF5722;
        }
      }

      .days-left {
        font-size: 11px;
        padding: 2px 6px;
        border-radius: 8px;
        background: #FFF3E0;
        color: #F57C00;
      }
    }
  }

  // 右侧行动区
  .card-right {
    width: 100px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 16px;

    .urgent-tip {
      font-size: 11px;
      color: #FF5722;
      animation: blink 1.5s infinite;
    }

    .btn-use {
      width: 80px;
      padding: 8px 0;
      border: none;
      border-radius: 20px;
      background: #FF7043;
      color: white;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover:not(.disabled) {
        background: #FF5722;
        transform: scale(1.05);
      }

      &.premium {
        background: linear-gradient(135deg, #D4AF37 0%, #FFD700 100%);
        box-shadow: 0 2px 8px rgba(212, 175, 55, 0.4);
      }

      &.disabled {
        background: #ccc;
        cursor: not-allowed;
      }
    }
  }

  // 主题色 - 身份等级系列
  &.theme-black-gold .card-left {
    background: linear-gradient(135deg, #2C2C2C 0%, #1a1a1a 100%);
    border-right: 2px solid #D4AF37;
    box-shadow: inset -2px 0 10px rgba(212, 175, 55, 0.3);
  }

  &.theme-diamond .card-left {
    background: linear-gradient(135deg, #6C8EA8 0%, #4A6FA5 100%);
  }

  &.theme-gold .card-left {
    background: linear-gradient(135deg, #D4B170 0%, #B8935F 100%);
  }

  &.theme-silver .card-left {
    background: linear-gradient(135deg, #C0C0C0 0%, #A8A8A8 100%);
  }

  // 主题色 - 营销社交系列
  &.theme-promo .card-left {
    background: linear-gradient(135deg, #FF7043 0%, #FF5722 100%);
  }

  &.theme-social .card-left {
    background: linear-gradient(135deg, #689F38 0%, #558B2F 100%);
  }

  // 主题色 - 辅助系列
  &.theme-dessert .card-left {
    background: linear-gradient(135deg, #F48FB1 0%, #EC407A 100%);
  }

  &.theme-utility .card-left {
    background: linear-gradient(135deg, #90A4AE 0%, #607D8B 100%);
    border: 1px dashed #ccc;
    border-left: none;
    border-top: none;
    border-bottom: none;
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
