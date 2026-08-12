<!-- CozyJournal: 品牌编辑内容入口卡。Phase 1 静态数据，预留 API 扩展。 -->
<template>
  <view class="cozy-journal" @click="onOpen">
    <image :src="journal.image" mode="aspectFill" class="journal-image" />
    <view class="journal-copy">
      <text class="journal-kicker">COZY JOURNAL</text>
      <text class="journal-title cozy-display">{{ journal.title }}</text>
      <text class="journal-subtitle">{{ journal.subtitle }}</text>
      <text class="journal-link">探索 →</text>
    </view>
  </view>
</template>

<script setup>
import { imageUrl } from '@/config/image'

// Phase 1 static — Phase 2 replace with API call, keep shape identical
const journal = {
  title: '云南咖啡产地记录',
  subtitle: '5 年，我们寻找一杯更好的咖啡。',
  image: imageUrl('journal-yunnan.png'),
  link: '/pages/origins/index'
}

function onOpen() {
  uni.navigateTo({ url: journal.link })
}
</script>

<style lang="scss" scoped>
.cozy-journal {
  margin: 54rpx 28rpx 0;
  overflow: hidden;
  border-radius: $cozy-radius-lg;
  background: $cozy-surface-alt;
}

.journal-image {
  width: 100%;
  height: 300rpx;
  background: $cozy-surface;
}

.journal-copy {
  padding: 32rpx 30rpx 34rpx;
}

.journal-kicker {
  display: block;
  color: $cozy-muted;
  font-size: 17rpx;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.journal-title {
  display: block;
  margin-top: 14rpx;
  color: #fff;
  font-size: 35rpx;
}

.journal-subtitle {
  display: block;
  margin-top: 12rpx;
  color: $cozy-muted-on-dark;
  font-size: 22rpx;
  line-height: 1.55;
}

.journal-link {
  display: block;
  margin-top: 22rpx;
  color: #fff;
  font-size: 22rpx;
  font-weight: 650;
}
</style>
