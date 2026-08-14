<!-- 产区探索：世界地图 + 8 产区详情。 -->
<template>
  <view class="origins-page">
    <scroll-view scroll-y class="origins-scroll">
      <!-- 世界产区地图 -->
      <view class="map-section">
        <image
          :src="imageUrl('origin-map.png')"
          mode="widthFix"
          class="map-image"
        />
        <view class="map-label">世界产区汇聚杭州</view>
      </view>

      <!-- 产区详情卡片 -->
      <view class="origins-list">
        <block v-for="(o, i) in origins" :key="o.id">
          <view v-if="i > 0" class="card-rule" />
          <view class="origin-card">
            <text class="card-counter">{{ pad(i + 1) }} / 08 · {{ o.englishName }}</text>
            <text class="card-name cozy-display">{{ o.name }}</text>
            <text class="card-region">{{ o.region }}</text>
            <view class="section-rule" />
            <text class="card-story">{{ o.story }}</text>

            <view class="facts-rule" />
            <view class="card-facts">
              <view class="fact">
                <text class="fact-label">海拔</text>
                <text class="fact-value">{{ o.altitude }}</text>
              </view>
              <view class="fact">
                <text class="fact-label">处理法</text>
                <text class="fact-value">{{ o.process }}</text>
              </view>
              <view class="fact">
                <text class="fact-label">品种</text>
                <text class="fact-value">{{ o.varieties.join(' · ') }}</text>
              </view>
              <view class="fact fact-role">
                <text class="fact-label">风味角色</text>
                <text class="fact-value">{{ o.role }}</text>
              </view>
            </view>

            <view class="card-flavors">
              <text v-for="f in o.flavors" :key="f" class="flavor-tag">{{ f }}</text>
            </view>

            <text class="card-route">{{ o.name }} → 杭州烘焙中心</text>
          </view>
        </block>

        <view class="card-rule card-rule--hz" />
        <!-- 杭州收束 -->
        <view class="origin-card card-summary">
          <text class="card-counter">09 / 09 · HANGZHOU</text>
          <text class="card-name cozy-display">杭州</text>
          <text class="card-region">Cozy Coffee Roastery</text>
          <text class="card-story">世界八处产区，最终在杭州完成属于 Cozy Coffee 的风味语言。</text>
          <view class="card-roastery">
            <text v-for="s in ['Roasting', 'Blending', 'Cup']" :key="s" class="roastery-step">{{ s }}</text>
          </view>
        </view>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>
  </view>
</template>

<script setup>
import { imageUrl } from '@/config/image'
import { COFFEE_ORIGINS } from '@/data/coffeeOrigins'

const origins = COFFEE_ORIGINS

function pad(n) { return String(n).padStart(2, '0') }
</script>

<style lang="scss" scoped>
.origins-page {
  min-height: 100vh;
  background: $cozy-bg;
}

.origins-scroll {
  height: 100vh;
}

.map-section { background: $cozy-surface; }
.map-image { display: block; width: 100%; }
.map-label { padding: 16rpx 28rpx 20rpx; color: $cozy-muted; font-size: 24rpx; text-align: center; }

.origins-list { padding: 0 28rpx; }
.origin-card { padding: 32rpx 0 56rpx; }

// short decorative rules between cards — editorial rhythm
.card-rule { width: 80rpx; height: 3rpx; background: $cozy-border; }
.card-rule--hz { width: 120rpx; }

.card-summary { padding-bottom: 0; }

.card-counter { color: $cozy-muted; font-size: 24rpx; letter-spacing: 0.06em; margin-bottom: 20rpx; }
.card-name { display: block; color: $cozy-ink; font-size: 48rpx; line-height: 1.12; }
.card-region { display: block; margin-top: 12rpx; color: $cozy-primary; font-size: 26rpx; font-weight: 650; }
.section-rule { width: 48rpx; height: 2rpx; margin-top: 24rpx; background: $cozy-border; }
.card-story { display: block; margin-top: 28rpx; color: $cozy-muted; font-size: 28rpx; line-height: 1.75; }

.facts-rule { width: 56rpx; height: 2rpx; margin-top: 32rpx; background: $cozy-border; }
.card-facts { display: grid; grid-template-columns: 1fr 1fr; gap: 20rpx 24rpx; margin-top: 32rpx; }
.fact-label { display: block; color: $cozy-muted; font-size: 22rpx; }
.fact-value { display: block; margin-top: 8rpx; color: $cozy-ink; font-size: 26rpx; }
.fact-role .fact-value { color: $cozy-primary; font-weight: 700; }

.card-flavors { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 28rpx; }
.flavor-tag { padding: 12rpx 22rpx; border-radius: 999rpx; background: $cozy-surface; color: $cozy-ink; font-size: 24rpx; }

.card-route { display: block; margin-top: 28rpx; color: $cozy-primary; font-size: 24rpx; font-weight: 600; }

.card-roastery { display: flex; gap: 12rpx 28rpx; margin-top: 32rpx; }
.roastery-step { color: $cozy-ink; font-size: 26rpx; font-weight: 650; }
.roastery-step:not(:last-child)::after { content: ' →'; color: $cozy-muted; margin-left: 28rpx; }

.bottom-spacer { height: 100rpx; }
</style>
