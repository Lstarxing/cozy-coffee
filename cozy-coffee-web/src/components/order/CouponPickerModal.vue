<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="coupon-picker-overlay" @click.self="handleClose">
        <div class="coupon-picker-modal">
          <!-- 头部 -->
          <div class="modal-header">
            <h3>选择优惠券</h3>
            <button class="close-btn" @click="handleClose">
              <X :size="20" />
            </button>
          </div>
          
          <!-- 券列表 -->
          <div class="coupon-list">
            <!-- 不使用优惠券选项 -->
            <div 
              class="coupon-row no-coupon"
              :class="{ selected: selectedCode === '' }"
              @click="selectCoupon('')"
            >
              <div class="radio-indicator">
                <div v-if="selectedCode === ''" class="radio-dot"></div>
              </div>
              <div class="coupon-info">
                <span class="coupon-name">不使用优惠券</span>
              </div>
              <div class="coupon-discount">
                <span class="discount-value">¥0</span>
              </div>
            </div>
            
            <!-- 可用券列表 -->
            <div 
              v-for="coupon in sortedCoupons" 
              :key="coupon.couponCode"
              class="coupon-row"
              :class="{ 
                selected: selectedCode === coupon.couponCode,
                disabled: !coupon.meetsThreshold,
                recommended: coupon.isRecommended
              }"
              :style="{ '--tier-color': getTierColor(coupon) }"
              @click="coupon.meetsThreshold && selectCoupon(coupon.couponCode)"
            >
              <!-- 左侧等级色条 -->
              <div class="tier-bar"></div>
              
              <!-- 单选按钮 -->
              <div class="radio-indicator">
                <div v-if="selectedCode === coupon.couponCode" class="radio-dot"></div>
              </div>
              
              <!-- 券信息 -->
              <div class="coupon-info">
                <div class="name-row">
                  <span class="coupon-name">{{ coupon.displayName }}</span>
                  <span v-if="coupon.isRecommended" class="recommend-tag">推荐</span>
                  <span v-if="!coupon.meetsThreshold" class="unavailable-tag">{{ coupon.unavailableReason || '不可用' }}</span>
                </div>
              </div>
              
              <!-- 折扣金额 -->
              <div class="coupon-discount">
                <span class="discount-value" :class="{ disabled: !coupon.meetsThreshold }">
                  {{ coupon.meetsThreshold ? `-¥${coupon.estimatedDiscount.toFixed(0)}` : '-' }}
                </span>
              </div>
            </div>
          </div>
          
          <!-- 底部按钮 -->
          <div class="modal-footer">
            <button class="confirm-btn" @click="confirmSelection">
              确认选择
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { X } from 'lucide-vue-next'

const props = defineProps({
  visible: { type: Boolean, default: false },
  coupons: { type: Array, default: () => [] },
  currentSelection: { type: String, default: '' }
})

const emit = defineEmits(['update:visible', 'select'])

const selectedCode = ref(props.currentSelection)

// 监听外部选择变化
watch(() => props.currentSelection, (newVal) => {
  selectedCode.value = newVal
})

// 按折扣金额降序排序，推荐最大折扣
const sortedCoupons = computed(() => {
  const available = props.coupons.filter(c => c.meetsThreshold)
  const unavailable = props.coupons.filter(c => !c.meetsThreshold)
  
  // 按折扣金额降序排序
  available.sort((a, b) => (b.estimatedDiscount || 0) - (a.estimatedDiscount || 0))
  
  // 标记推荐券（折扣最大的）
  if (available.length > 0) {
    available[0].isRecommended = true
  }
  
  return [...available, ...unavailable]
})

// 获取券等级颜色
const getTierColor = (coupon) => {
  const name = (coupon.displayName || coupon.productName || '').toLowerCase()
  const code = (coupon.couponCode || '').toUpperCase()
  
  if (code.includes('BLACK') || name.includes('黑金') || name.includes('尊享')) return '#D4AF37'
  if (code.includes('DIAMOND') || name.includes('钻石')) return '#6C8EA8'
  if (code.includes('GOLD') || name.includes('黄金')) return '#D4B170'
  if (name.includes('新用户') || name.includes('新人')) return '#FF7043'
  if (name.includes('新品')) return '#FF5722'
  if (name.includes('生日')) return '#EC407A'
  if (name.includes('买一') || code.includes('BOGO')) return '#689F38'
  
  return '#C69C6D' // 默认咖啡色
}

const selectCoupon = (code) => {
  selectedCode.value = code
}

const confirmSelection = () => {
  emit('select', selectedCode.value)
  handleClose()
}

const handleClose = () => {
  emit('update:visible', false)
}
</script>

<style scoped lang="scss">
.coupon-picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(2px);
}

.coupon-picker-modal {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 480px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  
  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }
  
  .close-btn {
    background: none;
    border: none;
    padding: 4px;
    cursor: pointer;
    color: #999;
    border-radius: 50%;
    transition: all 0.2s;
    
    &:hover {
      background: #f5f5f5;
      color: #666;
    }
  }
}

.coupon-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.coupon-row {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  border-left: 4px solid transparent;
  border-left-color: var(--tier-color, #C69C6D);
  
  &:hover:not(.disabled) {
    background: #FAFAFA;
  }
  
  &.selected {
    background: rgba(198, 156, 109, 0.08);
    
    .radio-indicator {
      border-color: #C69C6D;
      
      .radio-dot {
        background: #C69C6D;
      }
    }
  }
  
  &.disabled {
    opacity: 0.5;
    cursor: not-allowed;
    
    .tier-bar {
      background: #BDBDBD;
    }
  }
  
  &.no-coupon {
    border-left-color: #E0E0E0;
  }
}

.tier-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--tier-color, #C69C6D);
}

.radio-indicator {
  width: 20px;
  height: 20px;
  border: 2px solid #BDBDBD;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 14px;
  flex-shrink: 0;
  transition: all 0.2s;
  
  .radio-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #C69C6D;
  }
}

.coupon-info {
  flex: 1;
  min-width: 0;
  
  .name-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
  
  .coupon-name {
    font-size: 15px;
    font-weight: 500;
    color: #333;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .recommend-tag {
    font-size: 11px;
    padding: 2px 6px;
    background: linear-gradient(135deg, #FF7043, #FF5722);
    color: white;
    border-radius: 4px;
    font-weight: 500;
  }
  
  .unavailable-tag {
    font-size: 11px;
    padding: 2px 6px;
    background: #F5F5F5;
    color: #999;
    border-radius: 4px;
  }
}

.coupon-discount {
  margin-left: 12px;
  flex-shrink: 0;
  
  .discount-value {
    font-size: 16px;
    font-weight: 600;
    color: #E53935;
    font-family: 'DIN Alternate', 'Roboto Condensed', sans-serif;
    
    &.disabled {
      color: #BDBDBD;
    }
  }
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  
  .confirm-btn {
    width: 100%;
    padding: 14px;
    border: none;
    border-radius: 24px;
    background: linear-gradient(135deg, #C69C6D 0%, #A67C52 100%);
    color: white;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      background: linear-gradient(135deg, #A67C52 0%, #8B6914 100%);
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(166, 124, 82, 0.3);
    }
  }
}

/* 动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.3s ease;
  
  .coupon-picker-modal {
    transition: all 0.3s ease;
  }
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
  
  .coupon-picker-modal {
    transform: scale(0.95) translateY(20px);
  }
}
</style>
