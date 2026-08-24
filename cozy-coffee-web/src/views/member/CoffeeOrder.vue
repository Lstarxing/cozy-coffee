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

// 积分倍率走后端 MemberDTO.pointsRate（单一事实源），本地不再维护倍率表
function getConsumeMultiplier() {
  return Number(userStore.userInfo?.pointsRate) || 1
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
