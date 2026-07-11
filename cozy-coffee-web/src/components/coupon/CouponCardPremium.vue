<template>
  <div class="coupon-card" :class="['theme-' + couponTheme, { expired: couponExpired }]">
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
        <span class="title" :class="{ premium: couponPremium }">
          {{ premiumIcon }}{{ coupon.productName || getCouponTitle(props.coupon) }}
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
        :class="{ premium: couponPremium, disabled: couponExpired }"
        :disabled="couponExpired"
        @click="handleUse"
      >
        {{ couponExpired ? '已过期' : '去使用' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  getRules, getCouponTheme, getValueSymbol, getValueType, isPremium,
  getPremiumIcon, getSourceTag, getRestrictionTags, getExpiryText,
  getDaysLeft, isExpired, getCouponTitle
} from '@/composables/useCouponDisplay'

const props = defineProps({
  coupon: { type: Object, required: true }
})

const router = useRouter()

const rules = computed(() => getRules(props.coupon))
const couponTheme = computed(() => getCouponTheme(props.coupon))
const valueSymbol = computed(() => getValueSymbol(props.coupon))
const valueType = computed(() => getValueType(props.coupon))
const couponPremium = computed(() => isPremium(props.coupon))
const premiumIcon = computed(() => getPremiumIcon(props.coupon))
const sourceTag = computed(() => getSourceTag(props.coupon))
const restrictionTags = computed(() => getRestrictionTags(props.coupon))
const expiryText = computed(() => getExpiryText(props.coupon))
const daysLeft = computed(() => getDaysLeft(props.coupon))
const couponExpired = computed(() => isExpired(props.coupon))

const handleUse = () => {
  if (couponExpired.value) return
  const filter = []
  const r = rules.value
  if (r.skuLimit === 'STANDARD_ONLY') filter.push('standard_only')
  if (r.categoryBlocklist?.includes('signature')) filter.push('exclude_signature')
  if (r.categoryBlocklist?.includes('soe')) filter.push('exclude_soe')
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
