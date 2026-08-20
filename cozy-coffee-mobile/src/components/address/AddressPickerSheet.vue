<!--
  选择收货地址抽屉 - 复用 menu 页抽屉样式（底部弹出，地址列表 + 选中态 + 编辑/新增）
  用于确认订单/兑换确认等页面配送地址选择；不跳转地址页，抽屉内直接选
-->
<template>
  <view v-if="visible" class="address-picker-mask" @click="emit('close')">
    <view class="picker-mask" />
    <view class="picker-content" @click.stop>
      <view class="picker-header">
        <text class="picker-title">请选择收货地址</text>
        <text class="picker-close" @click="emit('close')">×</text>
      </view>
      <scroll-view scroll-y class="picker-list">
        <view v-if="loading" class="picker-loading">加载中…</view>
        <view
          v-for="addr in addressList"
          :key="addr.id"
          class="picker-row"
          @click="pickAddress(addr)"
        >
          <view class="picker-check">
            <CozyIcon v-if="isSelected(addr)" name="check" :size="16" color="#753A22" />
          </view>
          <view class="picker-copy">
            <view class="picker-addr-line">
              <text v-if="addr.label" class="picker-label">{{ labelText(addr.label) }}</text>
              <text class="picker-addr">{{ addressText(addr) }}</text>
            </view>
            <view class="picker-contact-line">
              <text class="picker-name">{{ contactNameText(addr) }}</text>
              <text class="picker-phone">{{ maskPhone(addr.phone) }}</text>
            </view>
          </view>
          <view class="picker-edit" @click.stop="editAddress(addr)"><CozyIcon name="pencil" :size="16" color="#756A63" /></view>
        </view>
        <view v-if="!loading && addressList.length === 0" class="picker-loading">暂无收货地址，请先新增</view>
      </scroll-view>
      <view class="picker-add" @click="addAddress">
        <CozyIcon name="plus" :size="18" color="#753A22" />
        <text>新增收货地址</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { get } from '@/api/request'
import CozyIcon from '@/components/CozyIcon.vue'
import { addressText, contactNameText, labelText, maskPhone, normalizeAddress } from '@/utils/address'

const props = defineProps({
  visible: { type: Boolean, default: false },
  selectedId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close', 'select'])

const addressList = ref([])
const loading = ref(false)

watch(() => props.visible, (visible) => {
  if (visible) loadAddresses()
})

async function loadAddresses() {
  loading.value = true
  try {
    const res = await get('/member/addresses')
    if (res.code === 200 && Array.isArray(res.data)) {
      addressList.value = res.data.map(normalizeAddress)
    }
  } catch (_) { /* 加载失败保持空列表，可点新增 */ } finally {
    loading.value = false
  }
}

function isSelected(addr) {
  return props.selectedId != null && String(addr.id) === String(props.selectedId)
}
function pickAddress(addr) {
  emit('select', addr)
}
function editAddress(addr) {
  uni.setStorageSync('cozy_edit_address', addr)
  uni.navigateTo({ url: `/pages/address/edit?id=${addr.id}` })
}
function addAddress() {
  uni.navigateTo({ url: '/pages/address/edit' })
}
</script>

<style lang="scss" scoped>
.address-picker-mask { position: fixed; inset: 0; z-index: 999; }
.picker-mask { position: absolute; inset: 0; background: rgba(44,30,24,.42); }
.picker-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  max-height: 66vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
}
.picker-header {
  padding: 32rpx 40rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.picker-title { font-family: $font-display; font-size: 36rpx; font-weight: 600; color: $cozy-ink; }
.picker-close { font-size: 40rpx; color: $cozy-muted; padding: 4rpx 8rpx; }
.picker-list {
  flex: 1 1 auto;
  min-height: 38vh;
  max-height: 48vh;
  padding: 0 40rpx 8rpx;
  box-sizing: border-box;
}
.picker-loading { padding: 48rpx 0; text-align: center; font-size: 24rpx; color: $cozy-muted; }
.picker-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: 24rpx;
  padding: 30rpx 28rpx;
  border: 1rpx solid $cozy-border;
  border-radius: 16rpx;
  background: $bg-white;
}
.picker-check { flex: none; width: 40rpx; display: flex; align-items: center; justify-content: center; }
.picker-copy { flex: 1; min-width: 0; }
.picker-addr-line { display: flex; align-items: center; gap: 10rpx; }
.picker-label {
  flex: none;
  padding: 0 10rpx;
  border: 1rpx solid $cozy-primary;
  border-radius: 4rpx;
  color: $cozy-primary;
  font-size: 18rpx;
  line-height: 1.6;
}
.picker-addr {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: $cozy-ink;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.picker-contact-line { display: flex; align-items: center; gap: 12rpx; margin-top: 12rpx; }
.picker-name { font-size: 22rpx; font-weight: 600; color: $cozy-ink; }
.picker-phone { font-size: 22rpx; color: $cozy-muted; }
.picker-edit { flex: none; padding: 8rpx; }
.picker-add {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin: 16rpx 40rpx 32rpx;
  height: 84rpx;
  border: 1rpx solid $cozy-border;
  border-radius: 12rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-primary;
}
.picker-add:active { background: $cozy-surface; }
</style>
