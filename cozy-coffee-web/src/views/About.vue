<template>
  <main class="about-page">
    <!-- Hero -->
    <section class="about-hero" aria-labelledby="about-hero-title">
      <picture class="about-hero__media">
        <img
          src="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/about/hero-about-20260716.png"
          alt="Cozy Coffee — 从一颗咖啡豆到一杯被认真对待的咖啡"
          fetchpriority="high"
          width="4096"
          height="1056"
          @error="handleImageError"
        >
      </picture>
      <div class="about-hero__scrim" aria-hidden="true"></div>
      <div class="warm-shell about-hero__content">
        <p class="about-hero__kicker">ABOUT OUR JOURNEY</p>
        <h1 id="about-hero-title">
          关于<br>Cozy Coffee
        </h1>
        <p class="about-hero__lead">
          从一颗咖啡豆<br>
          到一杯被认真对待的咖啡
        </p>
        <p class="about-hero__sub">
          我们记录产地 理解风味<br>
          也分享每一次人与咖啡相遇
        </p>
      </div>
    </section>

    <!-- 我们的故事 —— 为什么开始 -->
    <section class="about-story" aria-labelledby="story-title">
      <div class="warm-shell about-story__grid">
        <div class="about-story__text">
          <p class="about-eyebrow">OUR BEGINNING</p>
          <h2 id="story-title">我们为什么开始</h2>
          <p class="about-story__lead">
            咖啡不只是提神的饮品。
          </p>
          <p>
            它来自遥远的土地，<br>
            经过种植、处理、烘焙，<br>
            最终成为人与人之间的一段时间。
          </p>
          <p>
            Cozy Coffee 希望记录这些<br>
            关于产地、风味和人的故事。
          </p>
        </div>
        <figure class="about-story__media image-frame">
          <img
            src="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/about/editorial-store.png"
            alt="Cozy Coffee 门店环境"
            loading="lazy"
            width="896"
            height="1200"
            @error="handleImageError"
          >
        </figure>
      </div>
    </section>

    <!-- 从土地，到杯中 -->
    <section class="about-journey" aria-labelledby="journey-title" ref="journeySection">
      <div class="warm-shell about-journey__inner">
        <header class="about-section-head">
          <p class="about-eyebrow">FROM ORIGIN TO CUP</p>
          <h2 id="journey-title">从土地，到杯中</h2>
          <p class="about-section-head__lead">
            一颗咖啡豆，要走过很远的路，<br>
            才能成为你面前这一杯。
          </p>
        </header>
        <ol class="about-journey__rail">
          <li class="about-journey__step">
            <span class="about-journey__num">01</span>
            <h3>产地探索</h3>
            <p class="about-journey__en">Origin</p>
            <p>走遍世界八大产区，<br>理解每一片土地的风味语言。</p>
          </li>
          <li class="about-journey__step">
            <span class="about-journey__num">02</span>
            <h3>精选咖啡豆</h3>
            <p class="about-journey__en">Selection</p>
            <p>从处理法到批次，<br>只留下值得被记录的那一颗。</p>
          </li>
          <li class="about-journey__step">
            <span class="about-journey__num">03</span>
            <h3>杭州烘焙</h3>
            <p class="about-journey__en">Roasting</p>
            <p>小批量、慢火候，<br>寻找每一支豆子最合适的表达。</p>
          </li>
          <li class="about-journey__step">
            <span class="about-journey__num">04</span>
            <h3>门店萃取</h3>
            <p class="about-journey__en">Extraction</p>
            <p>从研磨到注水，<br>把风味完整地交到你的杯里。</p>
          </li>
          <li class="about-journey__step about-journey__step--last">
            <span class="about-journey__num">05</span>
            <h3>你的这一杯</h3>
            <p class="about-journey__en">Your Cup</p>
            <p>一杯咖啡，<br>连接土地与你的距离。</p>
          </li>
        </ol>
      </div>
    </section>

    <!-- 我们坚持的三件事 -->
    <section class="about-values" aria-labelledby="values-title">
      <div class="warm-shell about-values__inner">
        <header class="about-section-head about-section-head--center">
          <p class="about-eyebrow">WHAT WE BELIEVE</p>
          <h2 id="values-title">我们坚持的事情</h2>
          <p class="about-section-head__lead">
            不强调规模，只回应每一杯。
          </p>
        </header>
        <ol class="about-values__list">
          <li class="about-value">
            <span class="about-value__num">01</span>
            <h3>Origin</h3>
            <p class="about-value__zh">尊重每一片土地</p>
            <p class="about-value__desc">
              从云南到埃塞俄比亚，记录产地、海拔与处理法，
              让风味被看清，而不是被盖住。
            </p>
          </li>
          <li class="about-value">
            <span class="about-value__num">02</span>
            <h3>Roasting</h3>
            <p class="about-value__zh">寻找最适合的表达</p>
            <p class="about-value__desc">
              不是越深越好。每一次烘焙，
              都是对风味轮廓的一次重新理解。
            </p>
          </li>
          <li class="about-value">
            <span class="about-value__num">03</span>
            <h3>Experience</h3>
            <p class="about-value__zh">让咖啡成为日常的一部分</p>
            <p class="about-value__desc">
              门店不只是取餐点。我们希望它是一个
              你愿意慢下来、坐一会儿的地方。
            </p>
          </li>
        </ol>
      </div>
    </section>

    <!-- Behind The Coffee -->
    <section class="about-behind" aria-labelledby="behind-title">
      <div class="warm-shell about-behind__inner">
        <header class="about-section-head">
          <p class="about-eyebrow">BEHIND THE COFFEE</p>
          <h2 id="behind-title">咖啡师与烘焙幕后</h2>
          <p class="about-section-head__lead">
            每一次烘焙，都是对风味的一次重新理解。
          </p>
        </header>
        <div class="about-behind__grid">
          <figure class="about-behind__cell image-frame">
            <img
              src="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/about/editorial-beans-comparison.png"
              alt="生豆与熟豆对比"
              loading="lazy"
              width="896"
              height="1200"
              @error="handleImageError"
            >
            <figcaption>豆子 · 从生到熟的两次旅途</figcaption>
          </figure>
          <figure class="about-behind__cell image-frame">
            <img
              src="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/about/editorial-cupping.png"
              alt="杯测桌上的一排咖啡样品"
              loading="lazy"
              width="896"
              height="1200"
              @error="handleImageError"
            >
            <figcaption>杯测 · 听风味的语言</figcaption>
          </figure>
          <figure class="about-behind__cell image-frame">
            <img
              src="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/about/editorial-roastery.png"
              alt="烘焙师观察烘焙机中的咖啡豆"
              loading="lazy"
              width="896"
              height="1200"
              @error="handleImageError"
            >
            <figcaption>烘焙 · 看见风味在变化</figcaption>
          </figure>
          <figure class="about-behind__cell image-frame">
            <img
              src="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/about/editorial-pour-over.png"
              alt="咖啡师手冲过程"
              loading="lazy"
              width="896"
              height="1200"
              @error="handleImageError"
            >
            <figcaption>手冲 · 把水流写进风味</figcaption>
          </figure>
        </div>
      </div>
    </section>

    <!-- 品牌宣言 -->
    <section class="about-manifesto" aria-labelledby="manifesto-title">
      <div class="warm-shell about-manifesto__inner">
        <p class="about-eyebrow about-eyebrow--center">A QUIET PROMISE</p>
        <h2 id="manifesto-title">
          一杯咖啡<br>
          始于土地<br>
          终于与你相遇
        </h2>
        <p class="about-manifesto__sign">Cozy Coffee</p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref, onUnmounted } from 'vue'

const journeySection = ref(null)

const handleImageError = (e) => {
  const img = e.currentTarget
  if (img) img.classList.add('image-fallback')
}

// 滚动进入流程节点时添加进入动画（轻量，不影响内容）
let observer = null

onMounted(() => {
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible')
      }
    })
  }, { threshold: 0.2 })

  document.querySelectorAll('.about-journey__step, .about-value, .about-behind__cell').forEach(el => {
    observer.observe(el)
  })
})

onUnmounted(() => {
  if (observer) observer.disconnect()
})
</script>

<style scoped>
@import '@/assets/styles/about_us_style.css';

.about-page {
  width: 100%;
}
</style>