<template>
  <div class="orders-view">
    <header class="content-header">
      <h3>兑换订单记录</h3>
    </header>

    <div v-if="redeemOrders.length > 0" class="orders-list">
      <div v-for="order in redeemOrders" :key="order.id" class="order-card">
        <div class="order-header">
          <span class="order-no">订单号: {{ order.orderNo }}</span>
          <span class="order-status" :class="order.status">{{ getStatusText(order.status) }}</span>
        </div>
        <div class="order-body">
          <img :src="getImageUrl(order.productImage, '/images/products/default.png')" class="order-img" />
          <div class="order-info">
            <p class="product-name">{{ order.productName }}</p>
            <p class="order-quantity">数量: {{ order.quantity || 1 }}</p>
            <p class="points-cost">消耗积分: {{ order.pointsCost }}</p>
            <p class="order-time">{{ formatDate(order.createdAt) }}</p>
          </div>
        </div>
        <div v-if="order.fulfillmentType !== 'VIRTUAL'" class="order-footer">
          <p class="receiver-info">收货人: {{ order.receiverName }} {{ order.receiverPhone }}</p>
          <p class="receiver-address">{{ order.receiverAddress }}</p>
        </div>
        <div v-else class="order-footer virtual">
          <p class="virtual-status">已自动发放至您的券包</p>
        </div>
      </div>
    </div>
    <div v-else class="no-data">
      <p>暂无兑换订单记录</p>
      <button class="go-mall-btn" @click="router.push('/member/mall')">去积分商城看看</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserRedemptions } from '@/api/mall'
import { getImageUrl } from '@/utils/image'

const router = useRouter()

const redeemOrders = ref([])

async function loadRedeemOrders() {
  try {
    const data = await getUserRedemptions()
    redeemOrders.value = data.data || []
  } catch (error) {
    console.error('Failed to load redeem orders:', error)
  }
}

function getStatusText(status) {
  const map = {
    pending: '待处理',
    processing: '处理中',
    shipped: '已发货',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadRedeemOrders()
})
</script>

<style scoped>
.orders-view {
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

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  transition: all 0.3s ease;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.06);
  border-color: #e6e6e6;
}

.order-header {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f9f9f9;
  font-size: 13px;
}

.order-no {
  color: #666;
}

.order-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.order-status.pending { background: #fff7ed; color: #d97706; }
.order-status.completed { background: #E6F1EB; color: #059669; }
.order-status.shipped { background: #e0f2fe; color: #0284c7; }
.order-status.cancelled { background: #fef2f2; color: #dc2626; }

.order-body {
  display: flex;
  gap: 16px;
  padding: 16px;
}

.order-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  background: #f5f5f5;
}

.order-info {
  flex: 1;
}

.order-info .product-name {
  font-weight: 500;
  margin-bottom: 6px;
}

.order-info .points-cost {
  color: #B8956B;
  font-size: 14px;
}

.order-info .order-time {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

.order-quantity {
  color: #888;
  font-size: 14px;
  margin: 4px 0;
}

.order-footer {
  margin-top: 15px;
  padding: 12px 16px;
  border-top: 1px solid #eee;
}

.receiver-info {
  font-weight: 500;
  margin: 0 0 5px 0;
}

.receiver-address {
  color: #888;
  font-size: 14px;
  margin: 0;
}

.order-footer.virtual {
  background: #f0fdf4;
}

.virtual-status {
  color: #059669;
  font-size: 13px;
  margin: 0;
}

.no-data {
  text-align: center;
  padding: 60px 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px dashed #e5e7eb;
  margin-top: 20px;
}

.no-data p {
  color: #9ca3af;
  font-size: 15px;
  margin-bottom: 20px;
}

.go-mall-btn {
  padding: 10px 24px;
  background: #d97706;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  margin-top: 16px;
  transition: background 0.2s;
}

.go-mall-btn:hover {
  background: #b45309;
}
</style>
