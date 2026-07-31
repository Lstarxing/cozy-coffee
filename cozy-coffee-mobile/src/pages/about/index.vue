<!--
  About 页：复刻 Web 端品牌叙事页。
  Hero → 为什么开始 → 从土地到杯中 → 我们坚持的事 → 咖啡幕后 → 品牌宣言
-->
<template>
  <view class="about-page">
    <!-- Hero -->
    <view class="about-hero">
      <image src="/static/images/home/hero-season-soe.jpg" class="hero-media" mode="aspectFill" />
      <view class="hero-scrim" />
      <view class="hero-content">
        <text class="hero-kicker">ABOUT OUR JOURNEY</text>
        <text class="hero-title cozy-display">关于 Cozy Coffee</text>
        <text class="hero-lead">从一颗咖啡豆\n到一杯被认真对待的咖啡</text>
        <text class="hero-sub">我们记录产地 理解风味\n也分享每一次人与咖啡相遇</text>
      </view>
    </view>

    <!-- 为什么开始 -->
    <view class="section story">
      <view class="section-copy">
        <text class="eyebrow">OUR BEGINNING</text>
        <text class="section-title cozy-display">我们为什么开始</text>
        <text class="story-lead">咖啡不只是提神的饮品。</text>
        <text class="story-body">它来自遥远的土地，经过种植、处理、烘焙，最终成为人与人之间的一段时间。</text>
        <text class="story-body">Cozy Coffee 希望记录这些关于产地、风味和人的故事。</text>
      </view>
      <view class="story-media">
        <image src="/static/images/product-yunnan-soe.png" mode="aspectFill" class="story-image" />
      </view>
    </view>

    <!-- 从土地，到杯中 -->
    <view class="section journey">
      <view class="section-head">
        <text class="eyebrow">FROM ORIGIN TO CUP</text>
        <text class="section-title cozy-display">从土地，到杯中</text>
        <text class="section-lead">一颗咖啡豆，要走过很远的路，\n才能成为你面前这一杯。</text>
      </view>
      <view class="journey-rail">
        <view v-for="(step, i) in journeySteps" :key="step.en" class="journey-step">
          <text class="journey-num">{{ step.num }}</text>
          <view class="journey-body">
            <text class="journey-name">{{ step.zh }}</text>
            <text class="journey-en">{{ step.en }}</text>
            <text class="journey-desc">{{ step.desc }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 我们坚持的事情 -->
    <view class="section values">
      <view class="section-head section-head--center">
        <text class="eyebrow">WHAT WE BELIEVE</text>
        <text class="section-title cozy-display">我们坚持的事情</text>
        <text class="section-lead">不强调规模，只回应每一杯。</text>
      </view>
      <view class="values-list">
        <view v-for="(value, i) in values" :key="value.en" class="value-item">
          <text class="value-num">{{ value.num }}</text>
          <text class="value-en">{{ value.en }}</text>
          <text class="value-zh">{{ value.zh }}</text>
          <text class="value-desc">{{ value.desc }}</text>
        </view>
      </view>
    </view>

    <!-- 咖啡师与烘焙幕后 -->
    <view class="section behind">
      <view class="section-head">
        <text class="eyebrow">BEHIND THE COFFEE</text>
        <text class="section-title cozy-display">咖啡师与烘焙幕后</text>
        <text class="section-lead">每一次烘焙，都是对风味的一次重新理解。</text>
      </view>
      <view class="behind-grid">
        <view v-for="cell in behindCells" :key="cell.caption" class="behind-cell">
          <image :src="cell.image" mode="aspectFill" class="behind-image" />
          <text class="behind-caption">{{ cell.caption }}</text>
        </view>
      </view>
    </view>

    <!-- 品牌宣言 -->
    <view class="manifesto">
      <text class="eyebrow eyebrow--center">A QUIET PROMISE</text>
      <text class="manifesto-title cozy-display">一杯咖啡\n始于土地\n终于与你相遇</text>
      <text class="manifesto-sign">Cozy Coffee</text>
    </view>
  </view>
</template>

<script setup>
const journeySteps = [
  { num: '01', zh: '产地探索', en: 'Origin', desc: '走遍世界八大产区，理解每一片土地的风味语言。' },
  { num: '02', zh: '精选咖啡豆', en: 'Selection', desc: '从处理法到批次，只留下值得被记录的那一颗。' },
  { num: '03', zh: '杭州烘焙', en: 'Roasting', desc: '小批量、慢火候，寻找每一支豆子最合适的表达。' },
  { num: '04', zh: '门店萃取', en: 'Extraction', desc: '从研磨到注水，把风味完整地交到你的杯里。' },
  { num: '05', zh: '你的这一杯', en: 'Your Cup', desc: '一杯咖啡，连接土地与你的距离。' }
]

const values = [
  {
    num: '01', en: 'Origin', zh: '尊重每一片土地',
    desc: '从云南到埃塞俄比亚，记录产地、海拔与处理法，让风味被看清，而不是被盖住。'
  },
  {
    num: '02', en: 'Roasting', zh: '寻找最适合的表达',
    desc: '不是越深越好。每一次烘焙，都是对风味轮廓的一次重新理解。'
  },
  {
    num: '03', en: 'Experience', zh: '让咖啡成为日常的一部分',
    desc: '门店不只是取餐点。我们希望它是一个你愿意慢下来、坐一会儿的地方。'
  }
]

const behindCells = [
  { image: '/static/images/product-dirty.png', caption: '豆子 · 从生到熟的两次旅途' },
  { image: '/static/images/product-coldbrew.png', caption: '杯测 · 听风味的语言' },
  { image: '/static/images/product-ethiopia-yirgacheffe.png', caption: '烘焙 · 看见风味在变化' },
  { image: '/static/images/product-salted-caramel-latte.png', caption: '手冲 · 把水流写进风味' }
]
</script>

<style lang="scss" scoped>
.about-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding-bottom: 80rpx;
}

/* ── Hero ── */
.about-hero {
  position: relative;
  height: 560rpx;
  overflow: hidden;
}
.hero-media {
  width: 100%;
  height: 100%;
}
.hero-scrim {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(26,16,10,.42) 0%, rgba(26,16,10,.12) 45%, rgba(26,16,10,.68) 100%);
}
.hero-content {
  position: absolute;
  bottom: 56rpx;
  left: 32rpx;
  right: 32rpx;
  color: #fff;
}
.hero-kicker {
  display: block;
  font-size: 20rpx;
  font-weight: 600;
  letter-spacing: .16em;
  opacity: .8;
}
.hero-title {
  display: block;
  margin-top: 14rpx;
  font-size: 48rpx;
}
.hero-lead {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: rgba(255,255,255,.92);
}
.hero-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: rgba(255,255,255,.72);
}

/* ── 通用 ── */
.section {
  padding: 72rpx 32rpx;
}
.eyebrow {
  display: block;
  font-size: 18rpx;
  font-weight: 700;
  letter-spacing: .16em;
  color: $cozy-muted;
}
.eyebrow--center {
  text-align: center;
}
.section-title {
  display: block;
  margin-top: 14rpx;
  font-size: 40rpx;
  color: $cozy-ink;
}
.section-lead {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: $cozy-muted;
}
.section-head {
  margin-bottom: 44rpx;
}
.section-head--center {
  text-align: center;
}

/* ── 为什么开始 ── */
.story {
  background: $cozy-bg;
}
.section-copy {
  margin-bottom: 40rpx;
}
.story-lead {
  display: block;
  margin-top: 22rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.story-body {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: $cozy-muted;
}
.story-media {
  overflow: hidden;
  border-radius: $cozy-radius-lg;
}
.story-image {
  width: 100%;
  height: 460rpx;
  background: $cozy-surface;
}

/* ── 从土地，到杯中 ── */
.journey {
  background: $cozy-surface;
}
.journey-step {
  display: flex;
  gap: 28rpx;
  padding: 28rpx 0;
  border-bottom: 1rpx solid $cozy-border;
}
.journey-step:last-child {
  border-bottom: 0;
}
.journey-num {
  flex: none;
  width: 64rpx;
  font-size: 22rpx;
  font-weight: 700;
  color: $cozy-primary;
  letter-spacing: .08em;
}
.journey-body {
  min-width: 0;
  flex: 1;
}
.journey-name {
  display: block;
  font-size: 28rpx;
  font-weight: 650;
  color: $cozy-ink;
}
.journey-en {
  display: block;
  margin-top: 4rpx;
  font-size: 18rpx;
  letter-spacing: .12em;
  color: $cozy-muted;
}
.journey-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  line-height: 1.7;
  color: $cozy-muted;
}

/* ── 我们坚持的事情 ── */
.values {
  background: $cozy-bg;
}
.value-item {
  margin-bottom: 36rpx;
  padding-bottom: 36rpx;
  border-bottom: 1rpx solid $cozy-border;
  text-align: center;
}
.value-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: 0;
}
.value-num {
  display: block;
  font-size: 20rpx;
  font-weight: 700;
  color: $cozy-primary;
  letter-spacing: .1em;
}
.value-en {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  font-weight: 700;
  color: $cozy-ink;
  letter-spacing: .06em;
}
.value-zh {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: $cozy-muted;
}
.value-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 22rpx;
  line-height: 1.7;
  color: $cozy-muted;
}

/* ── 咖啡幕后 ── */
.behind {
  background: $cozy-surface;
}
.behind-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}
.behind-cell {
  width: calc(50% - 10rpx);
  overflow: hidden;
  border-radius: $cozy-radius-md;
  background: $cozy-bg;
}
.behind-image {
  width: 100%;
  height: 300rpx;
}
.behind-caption {
  display: block;
  padding: 14rpx 16rpx 18rpx;
  font-size: 20rpx;
  color: $cozy-muted;
}

/* ── 品牌宣言 ── */
.manifesto {
  padding: 100rpx 32rpx;
  background: $cozy-surface-alt;
  text-align: center;
}
.manifesto .eyebrow {
  color: rgba(255,255,255,.5);
}
.manifesto-title {
  display: block;
  margin-top: 30rpx;
  font-size: 44rpx;
  line-height: 1.5;
  color: #fff;
}
.manifesto-sign {
  display: block;
  margin-top: 40rpx;
  font-size: 20rpx;
  letter-spacing: .2em;
  color: rgba(255,255,255,.6);
}
</style>
