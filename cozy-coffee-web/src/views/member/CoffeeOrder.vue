<template>
  <div class="coffee-order-view-wrapper">
    <CoffeeOrderView
      :user-info="userStore.userInfo"
      :points-multiplier="getConsumeMultiplier()"
      @order-created="handleCoffeeOrderCreated"
      @refresh-user="refreshUserInfo"
    />
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CoffeeOrderView from '@/components/CoffeeOrderView.vue'

const userStore = useUserStore()
const router = useRouter()

function getConsumeMultiplier() {
  const level = userStore.userLevel || 'basic'
  const map = { basic: 1, silver: 1.1, gold: 1.2, diamond: 1.3, black: 1.5 }
  if (level === 'black' && (userStore.userInfo?.monthlyAccelerateRemaining || 0) > 0) {
    return 1.7
  }
  return map[level] || 1
}

function handleCoffeeOrderCreated(order) {
  ElMessage.success(`订单创建成功！订单号：${order.orderNo}`)
  router.push('/member/orders/coffee')
}

async function refreshUserInfo() {
  await userStore.fetchUserInfo()
  await userStore.fetchMemberInfo()
}
</script>

<style scoped>
.coffee-order-view-wrapper {
  width: 100%;
}
</style>
