<template>
  <div class="coupon-tabs">
    <div class="tab-header">
      <div 
        v-for="tab in tabs" 
        :key="tab.value"
        class="tab-item"
        :class="{ active: activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </div>
    </div>

    <div v-loading="loading" class="coupon-list">
      <div v-if="coupons.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>暂无优惠券</p>
      </div>
      
      <div 
        v-for="coupon in coupons"
        v-else 
        :key="coupon.id" 
        class="coupon-card"
        :class="['theme-' + getCouponTheme(coupon), { expired: isExpired(coupon) }]"
        @click="useCoupon(coupon)"
      >
        <!-- 左侧：大型语义图标 + 类型标签 -->
        <div class="card-left" :class="'theme-' + getCouponTheme(coupon)">
          <!-- v5.3.11: 大型纯白语义图标 (48px) -->
          <component :is="getCouponIcon(coupon)" :size="48" class="main-icon" />
          <!-- 类型标签 -->
          <div class="value-type">{{ getValueType(coupon) }}</div>
        </div>
        
        <!-- 中间：信息与限制 -->
        <div class="card-center">
          <div class="header">
            <span class="title" :class="{ premium: isPremium(coupon) }">
              {{ getPremiumIcon(coupon) }}{{ coupon.productName || getCouponTitle(coupon) }}
            </span>
            <span v-if="getSourceTag(coupon)" class="source-tag">{{ getSourceTag(coupon) }}</span>
          </div>

          <!-- 限制标签 -->
          <div v-if="getRestrictionTags(coupon).length > 0" class="limit-tags">
            <span 
              v-for="(tag, index) in getRestrictionTags(coupon)" 
              :key="index"
              class="tag"
              :class="tag.type"
            >
              {{ tag.icon }} {{ tag.text }}
            </span>
          </div>

          <div class="date-row">
            <span class="date" :class="{ urgent: getDaysLeft(coupon) <= 3 && getDaysLeft(coupon) > 0 }">
              {{ getExpiryText(coupon) }}
            </span>
            <span v-if="getDaysLeft(coupon) > 0 && getDaysLeft(coupon) <= 7" class="days-left">
              剩{{ getDaysLeft(coupon) }}天
            </span>
          </div>
        </div>

        <!-- 右侧：行动区 -->
        <div class="card-right">
          <div v-if="activeTab === 'ISSUED' && getDaysLeft(coupon) <= 3 && getDaysLeft(coupon) > 0" class="urgent-tip">
            即将过期
          </div>
          <button 
            class="btn-use" 
            :class="{ 
              premium: isPremium(coupon) && activeTab === 'ISSUED', 
              disabled: activeTab !== 'ISSUED' || isExpired(coupon) 
            }"
            @click.stop="useCoupon(coupon)"
          >
            {{ activeTab === 'ISSUED' ? '去使用' : (activeTab === 'USED' ? '已使用' : '已过期') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getUserCoupons } from '@/api/mall'
import {
  getRules, getCouponTheme, getValueType, getCouponIcon,
  isPremium, getPremiumIcon, getSourceTag, getRestrictionTags,
  getExpiryText, getDaysLeft, isExpired, getCouponTitle
} from '@/composables/useCouponDisplay'

const activeTab = ref('ISSUED')
const loading = ref(false)
const coupons = ref([])

const tabs = [
  { label: '可使用', value: 'ISSUED' },
  { label: '已使用', value: 'USED' },
  { label: '已过期', value: 'EXPIRED' }
]

const loadCoupons = async () => {
  loading.value = true
  try {
    const response = await getUserCoupons(activeTab.value)
    if (response.code === 200 || response.success) {
      coupons.value = response.data || []
    } else {
      coupons.value = []
    }
  } catch (e) {
    console.error('获取优惠券失败', e)
    coupons.value = []
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => { loadCoupons() })
onMounted(() => { loadCoupons() })

const emit = defineEmits(['use-coupon'])
const useCoupon = (coupon) => {
  if (isExpired(coupon)) return
  if (activeTab.value !== 'ISSUED') return
  const filter = []
  const r = getRules(coupon)
  if (r.skuLimit === 'STANDARD_ONLY') filter.push('standard_only')
  if (r.categoryBlocklist?.includes('signature')) filter.push('exclude_signature')
  if (r.categoryBlocklist?.includes('soe')) filter.push('exclude_soe')
  emit('use-coupon', {
    couponId: coupon.id,
    couponCode: coupon.couponCode,
    filter: filter.join(','),
    autoSelect: r.skuLimit === 'STANDARD_ONLY' ? 'medium' : null
  })
}
</script>

<style scoped lang="scss">
/* 主容器: 垂直堆叠布局 */
.coupon-tabs {
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 0;
}

/* 标签头: 水平居中对齐 */
.tab-header {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: 0;
  width: 100%;
  margin-bottom: 1.5rem;

  border-radius: 12px 12px 0 0;
  overflow: hidden;
  
  .tab-item {
    flex: 1;
    max-width: 150px;
    text-align: center;
    padding: 16px 24px;
    cursor: pointer;
    font-weight: 500;
    font-size: 15px;
    color: #757575;
    border-bottom: 3px solid transparent;
    transition: all 0.3s ease;
    white-space: nowrap;
    position: relative;
    
    &.active {
      color: #C69C6D;
      font-weight: 600;
      border-bottom-color: #C69C6D;
      background: white;
      
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 40px;
        height: 3px;
        background: linear-gradient(90deg, #C69C6D, #D4B170);
        border-radius: 3px 3px 0 0;
      }
    }
    
    &:hover:not(.active) {
      color: #A67C52;
      background: rgba(198, 156, 109, 0.08);
    }
  }
}

/* 券列表: 占满宽度，垂直排列 */
.coupon-list {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 1rem;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #999;

  .empty-icon {
    font-size: 64px;
    margin-bottom: 16px;
  }
}

/* ========== 优惠券卡片主体 ========== */
.coupon-card {
  display: flex;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
  min-height: 120px;
  position: relative;

  &:hover:not(.expired) {
    transform: translateY(-3px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  }

  &.expired {
    opacity: 0.55;
    filter: grayscale(0.6);
    cursor: not-allowed;
  }

  /* 左侧价值区 - v5.3.11 简化布局：大型图标 + 标签 */
  .card-left {
    width: 120px;
    min-width: 120px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: white;
    font-weight: bold;
    padding: 1.2rem 0.8rem;
    position: relative;
    overflow: hidden;
    
    /* 锯齿分割效果 */
    &::after {
      content: '';
      position: absolute;
      right: 0;
      top: 0;
      bottom: 0;
      width: 8px;
      background: 
        radial-gradient(circle at 0 8px, transparent 5px, white 5.5px) repeat-y;
      background-size: 8px 16px;
      z-index: 2;
    }
    
    /* v5.3.11: 大型主图标 - 纯白色 48px */
    .main-icon {
      color: white;
      filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
    }

    /* v5.3.11: 类型标签 - 图标下方 */
    .value-type {
      font-size: 13px;
      font-weight: 600;
      text-align: center;
      letter-spacing: 1px;
      padding: 4px 14px;
      background: rgba(255,255,255,0.2);
      border-radius: 14px;
      backdrop-filter: blur(4px);
    }
  }

  /* 中间信息区 */
  .card-center {
    flex: 1;
    min-width: 0;
    padding: 14px 16px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 10px;
    background: #FEFEFE;

    .header {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .title {
        font-size: 16px;
        font-weight: 600;
        color: #333;
        line-height: 1.3;

        &.premium {
          background: linear-gradient(135deg, #D4AF37 0%, #FFD700 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          font-weight: 700;
        }
      }

      .source-tag {
        font-size: 10px;
        padding: 3px 8px;
        border-radius: 10px;
        background: linear-gradient(135deg, #FFE0B2 0%, #FFCC80 100%);
        color: #E65100;
        font-weight: 600;
      }
    }

    /* 限制标签 - 语义化样式 */
    .limit-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;

      .tag {
        display: inline-flex;
        align-items: center;
        font-size: 11px;
        padding: 4px 10px;
        border-radius: 12px;
        white-space: nowrap;
        font-weight: 500;
        border: 1px solid transparent;

        /* 允许 - 绿色 */
        &.check {
          background: #E8F5E9;
          color: #2E7D32;
          border-color: #C8E6C9;
        }

        /* 锁定 - 灰色 */
        &.lock {
          background: #F5F5F5;
          color: #616161;
          border-color: #E0E0E0;
        }

        /* 禁止 - 红色 */
        &.ban {
          background: #FFEBEE;
          color: #C62828;
          border-color: #FFCDD2;
        }

        /* 警告/封顶 - 橙色 */
        &.warning {
          background: #FFF3E0;
          color: #E65100;
          border-color: #FFE0B2;
        }

        /* 规则说明 - 蓝色 */
        &.rule {
          background: #E3F2FD;
          color: #1565C0;
          border-color: #BBDEFB;
        }
      }
    }

    .date-row {
      display: flex;
      align-items: center;
      gap: 8px;

      .date {
        font-size: 12px;
        color: #9E9E9E;

        &.urgent {
          color: #FF5722;
          font-weight: 500;
        }
      }

      .days-left {
        font-size: 10px;
        padding: 2px 6px;
        border-radius: 8px;
        background: linear-gradient(135deg, #FFF3E0, #FFE0B2);
        color: #E65100;
        font-weight: 600;
        animation: pulse 2s infinite;
      }
    }
  }

  /* 右侧行动区 */
  .card-right {
    width: 90px;
    min-width: 90px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 10px;
    border-left: 2px dashed rgba(0,0,0,0.06);
    background: #FAFAFA;

    .urgent-tip {
      font-size: 11px;
      color: #FF5722;
      font-weight: 600;
      animation: blink 1.5s infinite;
    }

    .btn-use {
      width: 72px;
      padding: 10px 0;
      border: none;
      border-radius: 20px;
      background: linear-gradient(135deg, #C69C6D 0%, #A67C52 100%);
      color: white;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
      box-shadow: 0 2px 8px rgba(166, 124, 82, 0.3);

      &:hover:not(.disabled) {
        background: linear-gradient(135deg, #A67C52 0%, #8B6914 100%);
        transform: scale(1.05);
        box-shadow: 0 4px 12px rgba(166, 124, 82, 0.4);
      }

      &.premium {
        background: linear-gradient(135deg, #D4AF37 0%, #B8860B 100%);
        box-shadow: 0 3px 12px rgba(212, 175, 55, 0.4);
        
        &:hover:not(.disabled) {
          background: linear-gradient(135deg, #FFD700 0%, #D4AF37 100%);
        }
      }

      &.disabled {
        background: #BDBDBD;
        cursor: not-allowed;
        box-shadow: none;
      }
    }
  }

  /* ========== 主题色 - 身份等级系列 ========== */
  
  /* 黑金尊享 */
  &.theme-black-gold {
    border: 1px solid rgba(212, 175, 55, 0.3);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    
    .card-left {
      background: linear-gradient(135deg, #2C2C2C 0%, #1a1a1a 100%);
      color: #D4AF37;
      
      &::after {
        background: 
          radial-gradient(circle at 0 8px, transparent 5px, white 5.5px) repeat-y,
          linear-gradient(to right, rgba(212, 175, 55, 0.3), transparent);
        background-size: 8px 16px;
      }
      
      .value-symbol {
        text-shadow: 0 0 20px rgba(212, 175, 55, 0.5);
      }
      
      .value-type {
        background: rgba(212, 175, 55, 0.2);
        border: 1px solid rgba(212, 175, 55, 0.3);
      }
    }
    
    /* v5.3.10: 黑金专属按钮 - 金底黑字 */
    .card-right .btn-use:not(.disabled) {
      background: linear-gradient(135deg, #D4AF37 0%, #B8860B 100%);
      color: #1a1a1a;
      font-weight: 700;
      
      &:hover {
        background: linear-gradient(135deg, #FFD700 0%, #D4AF37 100%);
        box-shadow: 0 4px 16px rgba(212, 175, 55, 0.5);
      }
    }
    
    &:hover:not(.expired) {
      box-shadow: 0 8px 24px rgba(212, 175, 55, 0.2);
    }
  }

  /* 钻石 */
  &.theme-diamond {
    .card-left {
      background: linear-gradient(135deg, #6C8EA8 0%, #4A6FA5 100%);
    }
    /* v5.3.10: 钻石按钮 - 白底蓝字 */
    .card-right .btn-use:not(.disabled) {
      background: white;
      color: #4A6FA5;
      border: 1px solid #6C8EA8;
      
      &:hover {
        background: #4A6FA5;
        color: white;
      }
    }
  }

  /* 黄金 */
  &.theme-gold {
    .card-left {
      background: linear-gradient(135deg, #D4B170 0%, #B8935F 100%);
    }
    /* v5.3.10: 黄金按钮 - 白底金字 */
    .card-right .btn-use:not(.disabled) {
      background: white;
      color: #B8935F;
      border: 1px solid #D4B170;
      
      &:hover {
        background: #B8935F;
        color: white;
      }
    }
  }

  /* 白银 */
  &.theme-silver .card-left {
    background: linear-gradient(135deg, #A8A8A8 0%, #8E8E8E 100%);
  }

  /* ========== 主题色 - 营销社交系列 ========== */
  
  /* 新品促销 */
  &.theme-promo {
    .card-left {
      background: linear-gradient(135deg, #FF7043 0%, #FF5722 100%);
    }
    /* v5.3.10: 促销按钮 - 白底橙字 */
    .card-right .btn-use:not(.disabled) {
      background: white;
      color: #FF5722;
      border: 1px solid #FF7043;
      
      &:hover {
        background: #FF5722;
        color: white;
      }
    }
  }

  /* 社交裂变 (BOGO) */
  &.theme-social {
    .card-left {
      background: linear-gradient(135deg, #689F38 0%, #558B2F 100%);
    }
    /* v5.3.10: BOGO按钮 - 白底绿字 */
    .card-right .btn-use:not(.disabled) {
      background: white;
      color: #558B2F;
      border: 1px solid #689F38;
      
      &:hover {
        background: #558B2F;
        color: white;
      }
    }
  }

  /* ========== 主题色 - 辅助系列 ========== */
  
  /* 烘焙甜品 */
  &.theme-dessert {
    .card-left {
      background: linear-gradient(135deg, #F48FB1 0%, #EC407A 100%);
    }
    /* v5.3.10: 甜品按钮 - 白底粉字 */
    .card-right .btn-use:not(.disabled) {
      background: white;
      color: #EC407A;
      border: 1px solid #F48FB1;
      
      &:hover {
        background: #EC407A;
        color: white;
      }
    }
  }

  /* 工具券 (配送/加浓) */
  &.theme-utility {
    .card-left {
      background: linear-gradient(135deg, #78909C 0%, #546E7A 100%);
    }
    /* v5.3.11: 工具券按钮 - 白底灰字 */
    .card-right .btn-use:not(.disabled) {
      background: white;
      color: #546E7A;
      border: 1px solid #78909C;
      
      &:hover {
        background: #546E7A;
        color: white;
      }
    }
  }
  
  /* v5.3.12: 新用户券 - 活力橙色主题 */
  &.theme-new-user {
    border: 1px solid rgba(255, 112, 67, 0.3);
    
    .card-left {
      background: linear-gradient(135deg, #FF7043 0%, #FF5722 100%);
      
      .main-icon {
        color: white;
        filter: drop-shadow(0 2px 6px rgba(0,0,0,0.25));
      }
    }
    
    /* 新用户券按钮 - 橙色背景白字 */
    .card-right .btn-use:not(.disabled) {
      background: linear-gradient(135deg, #FF7043 0%, #FF5722 100%);
      color: white;
      border: none;
      box-shadow: 0 3px 10px rgba(255, 87, 34, 0.35);
      
      &:hover {
        background: linear-gradient(135deg, #FF5722 0%, #E64A19 100%);
        box-shadow: 0 4px 14px rgba(255, 87, 34, 0.45);
        transform: scale(1.05);
      }
    }
    
    &:hover:not(.expired) {
      box-shadow: 0 6px 20px rgba(255, 112, 67, 0.2);
    }
  }
  
  /* v5.8: 积分商城兑换券 - Cozy Coffee 品牌咖啡色主题 */
  &.theme-points-redeem {
    border: 1px solid rgba(198, 156, 109, 0.4);
    box-shadow: 0 4px 16px rgba(93, 64, 55, 0.12);
    
    .card-left {
      background: linear-gradient(135deg, #5D4037 0%, #3E2723 100%);
      
      .main-icon {
        color: #D7CCC8;
        filter: drop-shadow(0 2px 6px rgba(0,0,0,0.3));
      }
      
      .value-type {
        background: rgba(198, 156, 109, 0.3);
        border: 1px solid rgba(198, 156, 109, 0.4);
        color: #EFEBE9;
      }
    }
    
    /* 积分券按钮 - hover前白底咖啡字, hover后填充 */
    .card-right .btn-use:not(.disabled) {
      background: white;
      color: #5D4037;
      border: 1px solid #8D6E63;
      box-shadow: none;
      
      &:hover {
        background: linear-gradient(135deg, #8D6E63 0%, #5D4037 100%);
        color: white;
        border-color: transparent;
        box-shadow: 0 4px 14px rgba(93, 64, 55, 0.4);
        transform: scale(1.05);
      }
    }
    
    &:hover:not(.expired) {
      box-shadow: 0 8px 24px rgba(93, 64, 55, 0.18);
      border-color: rgba(198, 156, 109, 0.6);
    }
  }
  
  /* v5.8: 代金券 - 金黄色主题 */
  &.theme-voucher {
    border: 1px solid rgba(245, 158, 11, 0.3);
    box-shadow: 0 4px 16px rgba(245, 158, 11, 0.1);
    
    .card-left {
      background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
      
      .main-icon {
        color: #FFFBEB;
        filter: drop-shadow(0 2px 6px rgba(0,0,0,0.2));
      }
      
      .value-type {
        background: rgba(255, 255, 255, 0.25);
        border: 1px solid rgba(255, 255, 255, 0.35);
        color: #FFFBEB;
      }
    }
    
    /* 代金券按钮 - hover前白底黄字, hover后填充 */
    .card-right .btn-use:not(.disabled) {
      background: white;
      color: #D97706;
      border: 1px solid #F59E0B;
      box-shadow: none;
      
      &:hover {
        background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
        color: white;
        border-color: transparent;
        box-shadow: 0 4px 14px rgba(245, 158, 11, 0.4);
        transform: scale(1.05);
      }
    }
    
    &:hover:not(.expired) {
      box-shadow: 0 8px 24px rgba(245, 158, 11, 0.15);
      border-color: rgba(245, 158, 11, 0.5);
    }
  }
  
  /* v5.8: 折扣券 - 青绿色主题 */
  &.theme-discount {
    border: 1px solid rgba(6, 182, 212, 0.3);
    box-shadow: 0 4px 16px rgba(6, 182, 212, 0.1);
    
    .card-left {
      background: linear-gradient(135deg, #06B6D4 0%, #0891B2 100%);
      
      .main-icon {
        color: #ECFEFF;
        filter: drop-shadow(0 2px 6px rgba(0,0,0,0.2));
      }
      
      .value-type {
        background: rgba(255, 255, 255, 0.25);
        border: 1px solid rgba(255, 255, 255, 0.35);
        color: #ECFEFF;
      }
    }
    
    /* 折扣券按钮 - hover前白底青字, hover后填充 */
    .card-right .btn-use:not(.disabled) {
      background: white;
      color: #0891B2;
      border: 1px solid #06B6D4;
      box-shadow: none;
      
      &:hover {
        background: linear-gradient(135deg, #06B6D4 0%, #0891B2 100%);
        color: white;
        border-color: transparent;
        box-shadow: 0 4px 14px rgba(6, 182, 212, 0.4);
        transform: scale(1.05);
      }
    }
    
    &:hover:not(.expired) {
      box-shadow: 0 8px 24px rgba(6, 182, 212, 0.15);
      border-color: rgba(6, 182, 212, 0.5);
    }
  }
  
  /* v6.0: 加料券 - 深咖啡色浓缩主题 */
  &.theme-addon {
    border: 1px solid rgba(121, 85, 72, 0.3);
    box-shadow: 0 4px 16px rgba(93, 64, 55, 0.1);
    
    .card-left {
      background: linear-gradient(135deg, #6D4C41 0%, #3E2723 100%);
      
      .main-icon {
        color: #EFEBE9;
        filter: drop-shadow(0 2px 6px rgba(0,0,0,0.25));
      }
      
      .value-type {
        background: rgba(255, 255, 255, 0.2);
        border: 1px solid rgba(255, 255, 255, 0.3);
        color: #EFEBE9;
        font-weight: 700;
        letter-spacing: 1px;
      }
    }
    
    /* 加料券按钮 - hover前白底咖字, hover后填充 */
    .card-right .btn-use:not(.disabled) {
      background: white;
      color: #5D4037;
      border: 1px solid #795548;
      box-shadow: none;
      
      &:hover {
        background: linear-gradient(135deg, #6D4C41 0%, #4E342E 100%);
        color: white;
        border-color: transparent;
        box-shadow: 0 4px 14px rgba(93, 64, 55, 0.4);
        transform: scale(1.05);
      }
    }
    
    &:hover:not(.expired) {
      box-shadow: 0 8px 24px rgba(93, 64, 55, 0.2);
      border-color: rgba(93, 64, 55, 0.5);
    }
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}
</style>
