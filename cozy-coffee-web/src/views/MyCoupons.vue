<template>
  <div class="my-coupons-page">
    <div class="page-header">
      <h1>我的券包</h1>
      <p class="subtitle">{{ totalCoupons }}张可用 · {{ expiringCount }}张即将过期</p>
    </div>

    <!-- Tab切换 -->
    <div class="tabs">
      <div 
        v-for="tab in tabs" 
        :key="tab.value"
        class="tab-item"
        :class="{ active: activeTab === tab.value }"
        @click="changeTab(tab.value)"
      >
        <span>{{ tab.label }}</span>
        <span v-if="tab.count > 0" class="count">{{ tab.count }}</span>
      </div>
    </div>

    <!-- 券列表 -->
    <div v-loading="loading" class="coupon-list">
      <div v-if="coupons.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>{{ emptyText }}</p>
      </div>

      <CouponCardPremium 
        v-for="coupon in coupons"
        v-else 
        :key="coupon.id"
        :coupon="coupon"
      />
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore && coupons.length > 0" class="load-more">
      <button :disabled="loading" @click="loadMore">
        {{ loading ? '加载中...' : '加载更多' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserCoupons } from '@/api/mall'
import CouponCardPremium from '@/components/coupon/CouponCardPremium.vue'

const activeTab = ref('ISSUED')
const loading = ref(false)
const coupons = ref([])
const page = ref(1)
const pageSize = ref(20)
const hasMore = ref(false)

const tabs = [
  { label: '可使用', value: 'ISSUED', count: 0 },
  { label: '已使用', value: 'USED', count: 0 },
  { label: '已过期', value: 'EXPIRED', count: 0 }
]

// 总券数
const totalCoupons = computed(() => {
  return tabs.find(t => t.value === 'ISSUED')?.count || 0
})

// 即将过期券数
const expiringCount = computed(() => {
  return coupons.value.filter(c => {
    if (!c.expiresAt) return false
    const diff = new Date(c.expiresAt) - new Date()
    const days = Math.ceil(diff / (1000 * 60 * 60 * 24))
    return days > 0 && days <= 7
  }).length
})

// 空状态文案
const emptyText = computed(() => {
  if (activeTab.value === 'ISSUED') return '暂无可用优惠券，快去赚取吧~'
  if (activeTab.value === 'USED') return '暂无已使用的优惠券'
  return '暂无过期优惠券'
})

// 加载券列表
const loadCoupons = async (reset = false) => {
  if (loading.value) return
  
  loading.value = true
  try {
    if (reset) {
      page.value = 1
      coupons.value = []
    }

    const res = await getUserCoupons(activeTab.value, page.value, pageSize.value)
    
    if (res.code === 200 || res.success) {
      const data = res.data || []
      if (reset) {
        coupons.value = data
      } else {
        coupons.value.push(...data)
      }
      
      hasMore.value = data.length >= pageSize.value
    }
  } catch (e) {
    console.error('获取优惠券失败', e)
  } finally {
    loading.value = false
  }
}

// 加载所有Tab的计数
const loadCounts = async () => {
  for (const tab of tabs) {
    try {
      const res = await getUserCoupons(tab.value, 1, 1)
      if (res.code === 200 || res.success) {
        tab.count = res.total || 0
      }
    } catch (e) {
      console.error(`加载${tab.label}计数失败`, e)
    }
  }
}

// 切换Tab
const changeTab = (value) => {
  if (activeTab.value === value) return
  activeTab.value = value
  loadCoupons(true)
}

// 加载更多
const loadMore = () => {
  page.value++
  loadCoupons(false)
}

onMounted(() => {
  loadCoupons(true)
  loadCounts()
})
</script>

<style scoped lang="scss">
.my-coupons-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FFFFFF 30%);
  padding: 20px;

  .page-header {
    text-align: center;
    margin-bottom: 24px;

    h1 {
      font-size: 28px;
      font-weight: 600;
      color: #333;
      margin-bottom: 8px;
    }

    .subtitle {
      font-size: 14px;
      color: #999;
    }
  }

  .tabs {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-bottom: 24px;
    padding: 8px;
    background: white;
    border-radius: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .tab-item {
      flex: 1;
      max-width: 120px;
      padding: 12px 20px;
      border-radius: 12px;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      color: #666;

      &:hover {
        background: #f5f5f5;
      }

      &.active {
        background: linear-gradient(135deg, #FF7043 0%, #FF5722 100%);
        color: white;
        box-shadow: 0 2px 8px rgba(255, 112, 67, 0.4);

        .count {
          background: rgba(255, 255, 255, 0.3);
          color: white;
        }
      }

      .count {
        display: inline-block;
        min-width: 20px;
        padding: 2px 6px;
        border-radius: 10px;
        background: #f0f0f0;
        color: #666;
        font-size: 12px;
        font-weight: 600;
      }
    }
  }

  .coupon-list {
    max-width: 800px;
    margin: 0 auto;
  }

  .empty-state {
    text-align: center;
    padding: 60px 20px;
    color: #999;

    .empty-icon {
      font-size: 64px;
      margin-bottom: 16px;
    }

    p {
      font-size: 14px;
    }
  }

  .load-more {
    text-align: center;
    margin-top: 32px;

    button {
      padding: 12px 40px;
      border: none;
      border-radius: 24px;
      background: white;
      color: #FF7043;
      border: 1px solid #FF7043;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover:not(:disabled) {
        background: #FF7043;
        color: white;
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }
}
</style>
