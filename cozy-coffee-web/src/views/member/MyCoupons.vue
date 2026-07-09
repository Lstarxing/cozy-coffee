<template>
  <div class="coupons-view">
    <header class="content-header">
      <h3>我的券包</h3>
      <button
        class="refresh-btn"
        :disabled="isRefreshingCoupons"
        title="刷新优惠券"
        @click="handleRefreshCoupons"
      >
        <RefreshCw :size="18" :class="{ 'spinning': isRefreshingCoupons }" />
      </button>
    </header>

    <div class="coupons-container">
      <CouponTabs :key="couponTabsKey" @use-coupon="handleUseCouponFromTabs" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshCw } from 'lucide-vue-next'
import CouponTabs from '@/components/coupon/CouponTabs.vue'

const router = useRouter()

const isRefreshingCoupons = ref(false)
const couponTabsKey = ref(0)

function handleRefreshCoupons() {
  if (isRefreshingCoupons.value) return
  isRefreshingCoupons.value = true
  try {
    couponTabsKey.value++
    ElMessage.success({ message: '优惠券已更新', duration: 1500 })
  } catch (e) {
    ElMessage.error('刷新失败')
  } finally {
    isRefreshingCoupons.value = false
  }
}

function handleUseCouponFromTabs(couponInfo) {
  console.log('使用优惠券，切换到咖啡下单:', couponInfo)
  router.push('/member/order')
  ElMessage.success({ message: '已切换到点单页面，请选择商品后使用优惠券', duration: 2000 })
}
</script>

<style scoped>
.coupons-view {
  animation: fadeIn 0.4s ease-out;
  max-width: 1000px;
  margin: 0 auto;
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

.coupons-container {
  max-width: 800px;
  margin: 0 auto;
}

.refresh-btn {
  margin-left: auto;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8B4513;
  transition: all 0.3s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(139, 69, 19, 0.1);
  transform: rotate(15deg);
}

.refresh-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.refresh-btn .spinning {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
