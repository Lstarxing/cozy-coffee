<!--
  地址管理页 - 对齐 prototype/address.html：pin 卡 + 绿色默认徽标 + 脱敏 + 图标操作 + 独立编辑页（标题由原生导航栏提供）
-->
<template>
  <view class="address-page">
    <!-- 地址列表 -->
    <view class="address-list" v-if="addresses.length > 0">
      <view class="address-card" :class="{ default: item.isDefault }" v-for="item in addresses" :key="item.id">
        <view class="addr-main" @click="selectAddress(item)">
          <view class="addr-pin"><CozyIcon name="pin" :size="24" color="#753A22" /></view>
          <view class="addr-copy">
            <text class="addr-region">{{ item.region }}</text>
            <text class="addr-detail">{{ item.detail }}</text>
          </view>
          <text v-if="item.isDefault" class="default-badge">默认</text>
        </view>
        <view class="addr-receiver">
          <text class="receiver-name">{{ maskName(item.name) }}</text>
          <text class="receiver-honorific">{{ genderSuffix(item.gender) }}</text>
          <text class="receiver-phone">{{ maskPhone(item.phone) }}</text>
        </view>
        <view class="addr-actions">
          <view class="act" @click="setDefault(item)">
            <CozyIcon :name="item.isDefault ? 'star-filled' : 'star'" :size="22" :color="item.isDefault ? '#753A22' : '#756A63'" />
          </view>
          <view class="act" @click="editAddress(item)">
            <CozyIcon name="pencil" :size="22" color="#756A63" />
          </view>
          <view class="act" @click="deleteAddress(item)">
            <CozyIcon name="trash" :size="22" color="#756A63" />
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else>
      <view class="empty-mark"><CozyIcon name="pin" :size="30" color="#753A22" /></view>
      <text class="empty-text">暂无收货地址</text>
      <text class="empty-hint">添加地址后，兑换实物礼品可配送上门</text>
    </view>

    <!-- 新增地址入口 -->
    <view class="add-entry" @click="addAddress">
      <view class="add-plus"><CozyIcon name="plus" :size="24" color="#753A22" /></view>
      <text class="add-text">新增收货地址</text>
      <text class="add-arrow">›</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get, post, del, put } from '@/api/request'
import CozyIcon from '@/components/CozyIcon.vue'

const getAddressList = () => get('/member/addresses')
const deleteAddressApi = (id) => del(`/member/addresses/${id}`)
const setDefaultAddressApi = (id) => put(`/member/addresses/${id}/default`)

const addresses = ref([])

onShow(() => {
  loadAddresses()
})

const loadAddresses = async () => {
  try {
    const res = await getAddressList()
    if (res.code === 200 && res.data) {
      addresses.value = res.data.map(item => ({
        ...item,
        name: item.receiverName,
        gender: item.gender || 'MALE',
        phone: item.receiverPhone,
        region: [item.province, item.city, item.district].filter(Boolean).join(' '),
        detail: item.detailAddress,
        isDefault: item.isDefault
      }))
    }
  } catch (e) {
    console.error('加载地址失败', e)
  }
}

const addAddress = () => uni.navigateTo({ url: '/pages/address/edit' })
const editAddress = (item) => {
  uni.setStorageSync('cozy_edit_address', item)
  uni.navigateTo({ url: `/pages/address/edit?id=${item.id}` })
}

const setDefault = async (item) => {
  if (item.isDefault) return
  try {
    const res = await setDefaultAddressApi(item.id)
    if (res.code === 200) {
      uni.showToast({ title: '已设为默认', icon: 'success' })
      loadAddresses()
    }
  } catch (e) {
    console.error('设置默认地址失败', e)
  }
}

const deleteAddress = (item) => {
  uni.showModal({
    title: '删除地址',
    content: '确定要删除这个地址吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const delRes = await deleteAddressApi(item.id)
        if (delRes.code === 200) {
          uni.showToast({ title: '已删除', icon: 'success' })
          loadAddresses()
        }
      } catch (e) {
        console.error('删除地址失败', e)
      }
    }
  })
}

const maskName = (name) => {
  const n = String(name || '')
  return n.length > 1 ? n[0] + '*' + n.slice(-1) : n
}
const genderSuffix = (gender) => String(gender || '').toUpperCase() === 'FEMALE' ? '女士' : '先生'
const maskPhone = (phone) => String(phone || '').replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')

const selectAddress = (item) => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    const prevPage = pages[pages.length - 2]
    if (prevPage.route === 'pages/order/confirm') {
      uni.$emit('addressSelected', item)
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.address-page {
  min-height: 100vh;
  padding: 24rpx 32rpx 120rpx;
  background: $cozy-bg;
}

/* ── 地址列表 ── */
.address-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}
.address-card {
  border-radius: 28rpx;
  background: $bg-white;
  border: 1rpx solid $cozy-border;
  padding: 32rpx 36rpx 28rpx;
}
.address-card.default { border-color: #E3CDB6; }

.addr-main {
  display: flex;
  align-items: flex-start;
  gap: 24rpx;
}
.addr-pin {
  flex: none;
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $cozy-primary;
  margin-top: 2rpx;
}
.addr-copy { flex: 1; min-width: 0; }
.addr-region {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $cozy-ink;
  line-height: 1.4;
}
.addr-detail {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $cozy-muted;
  line-height: 1.5;
}
.default-badge {
  flex: none;
  margin-left: 16rpx;
  padding: 6rpx 18rpx;
  border-radius: 12rpx;
  background: $cozy-primary-soft;
  color: $cozy-primary;
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: .04em;
}

.addr-receiver {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid $cozy-border;
}
.receiver-name {
  font-size: 26rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.receiver-honorific {
  font-size: 24rpx;
  color: $cozy-muted;
}
.receiver-phone {
  font-size: 24rpx;
  color: $cozy-muted;
}

/* 操作：右侧图标按钮 */
.addr-actions {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid $cozy-border;
  justify-content: flex-end;
}
.act {
  width: 68rpx;
  height: 68rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $cozy-muted;

  &:active { opacity: .5; }
}

/* ── 空状态 ── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0 80rpx;
}
.empty-mark {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  background: $cozy-surface;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $cozy-primary;
}
.empty-text {
  margin-top: 32rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.empty-hint {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: $cozy-muted;
}

/* ── 新增地址入口（icon 行） ── */
.add-entry {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: 36rpx;
  padding: 32rpx 36rpx;
  border-radius: 28rpx;
  background: $bg-white;
  border: 1rpx solid $cozy-border;

  &:active { opacity: .75; }
}
.add-plus {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $cozy-primary;
}
.add-text {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.add-arrow {
  font-size: 36rpx;
  color: $cozy-placeholder;
  line-height: 1;
}
</style>
