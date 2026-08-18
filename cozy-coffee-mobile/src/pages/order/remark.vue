<!--
  订单备注页 - 自动聚焦键盘 + 限 50 字 + 快捷输入 + 底部提交，写入 checkoutStore.remark
-->
<template>
  <view class="remark-page">
    <!-- 备注输入框（自动弹键盘） -->
    <view class="remark-box">
      <textarea
        v-model="remark"
        :focus="focused"
        :maxlength="50"
        :cursor-spacing="48"
        placeholder="给这杯咖啡一点备注，如少冰、去糖…"
        placeholder-class="remark-placeholder"
        class="remark-input"
        @blur="focused = false"
      />
      <text class="char-count">{{ remark.length }}/50</text>
    </view>

    <!-- 快捷输入 -->
    <view class="quick-section">
      <text class="quick-title">快捷输入</text>
      <view class="quick-list">
        <view
          v-for="q in QUICK"
          :key="q"
          class="quick-chip"
          :class="{ on: has(q) }"
          @click="toggle(q)"
        >{{ q }}</view>
      </view>
    </view>

    <!-- 底部提交 -->
    <view class="submit-bar safe-area-bottom">
      <view class="submit-btn" :class="{ disabled: !remark.trim() }" @click="save">保存备注</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useCheckoutStore } from '@/stores/checkout'

const QUICK = ['少冰', '去冰', '多冰', '少糖', '无糖', '不要杯套', '打包带走']

const checkoutStore = useCheckoutStore()
const focused = ref(true)
const remark = ref('')

onLoad(() => {
  remark.value = checkoutStore.remark || ''
})

function has(q) {
  return remark.value.split(/[\s，,。]+/).filter(Boolean).includes(q)
}

function toggle(q) {
  const tokens = remark.value.split(/[\s，,。]+/).filter(Boolean)
  if (tokens.includes(q)) {
    remark.value = tokens.filter(t => t !== q).join(' ')
  } else {
    const next = [...tokens, q].join(' ')
    if (next.length <= 50) remark.value = next
  }
}

function save() {
  checkoutStore.remark = remark.value.trim()
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.remark-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding: 32rpx 32rpx 200rpx;
}

/* ── 输入框 ── */
.remark-box {
  position: relative;
  background: $bg-white;
  border-radius: 28rpx;
  padding: 36rpx 40rpx 64rpx;
}
.remark-input {
  width: 100%;
  height: 220rpx;
  font-size: 30rpx;
  line-height: 1.6;
  color: $cozy-ink;
}
.remark-placeholder { color: $cozy-placeholder; }
.char-count {
  position: absolute;
  right: 36rpx;
  bottom: 24rpx;
  font-size: 22rpx;
  color: $cozy-placeholder;
}

/* ── 快捷输入 ── */
.quick-section { margin-top: 40rpx; }
.quick-title {
  display: block;
  font-size: 24rpx;
  font-weight: 650;
  color: $cozy-muted;
  letter-spacing: .04em;
}
.quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-top: 24rpx;
}
.quick-chip {
  padding: 16rpx 36rpx;
  border-radius: 999rpx;
  border: 1rpx solid $cozy-border;
  background: $bg-white;
  color: $cozy-ink;
  font-size: 26rpx;

  &.on {
    background: #F1E4DA;
    border-color: $cozy-primary;
    color: $cozy-primary;
    font-weight: 650;
  }
}

/* ── 底部提交 ── */
.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 32rpx;
  border-top: 1rpx solid $cozy-border;
  background: #fff;
}
.submit-btn {
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: $cozy-ink;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;

  &.disabled { opacity: .4; }
}
</style>
