<template>
  <div class="about-page">
    <!-- 头部横幅 -->
    <section class="about-banner">
        <div class="banner-inner">
            <img src="/images/banner_about.png" alt="banner">
        </div>
    </section>

    <!-- 品牌使命 -->
    <section class="brand-mission">
        <div class="mission-content">
            <h2>品牌使命</h2>
            <p>咖熙咖啡总部位于杭州，是中国口碑最好的连锁咖啡品牌。咖熙咖啡以"创造温馨时刻，激发美好生活热望"为使命，充分利用移动互联网和大数据技术的新零售模式，与各领域优质供应商深度合作，打造高品质的消费体验。</p>
            <p>为顾客创造温馨运时刻，以"创造世界级咖啡品牌，让咖熙成为人们日常生活的一部分"为愿景，围绕"以善为先，求真务实，品质至上，持续创新，非我莫属，互信共赢"核心价值观，咖熙咖啡正在通过产品和服务，努力渗透日常生活每一处，传递美好生活的理念，激发对美好生活的热切期盼。</p>
        </div>
    </section>

    <!-- 品牌发展 -->
    <section class="brand-development">
        <div class="development-content">
            <h2>品牌发展</h2>
            <p>咖熙咖啡自成立以来，始终致力于成为中国领先的咖啡品牌。2024年，我们在全国范围内开设了超过1000家门店，为顾客提供高品质、高性价比的咖啡产品和服务。2024年，我们计划在上海开设首个旗舰店，进一步提升品牌影响力。</p>
            <p>咖熙咖啡在国际咖啡品鉴大赛中屡获殊荣，其中"SOE耶加雪菲"多次荣获金奖，彰显了我们在咖啡品质上的卓越追求。2022年，我们在云南建立了首个咖啡豆烘焙工厂，采用国际先进技术，年烘焙产能达到1万吨，成为国内领先的智能化烘焙基地。</p>
        </div>
    </section>

    <!-- 品牌愿景 -->
    <section class="brand-vision" ref="visionSection">
        <div class="vision-content">
            <h2>品牌愿景</h2>
            <div class="vision-text-container">
                <p>2025年，我们将在云南开设首个咖啡鲜果处理厂，采用国际先进的鲜果处理技术，年处理量可达3000吨，进一步优化我们的高品质供应链。我们将继续依托云南的天然优势，打造年产能2万吨的自加工烘焙供应网络，以更快速、精准地响应消费者需求，为全国门店提供更高品质的咖啡豆。</p>
                <p>咖熙咖啡始终致力于深度整合咖啡垂直供应链，以创新的生产力推动行业品质升级，引领行业高质量可持续发展的新趋势。我们的愿景是成为全球咖啡行业的标杆，为消费者带来无与伦比的咖啡体验。</p>
            </div>
            <div class="vision-cards">
                <div class="vision-item">
                    <h3>2024年已有</h3>
                    <div class="counter-wrapper">
                        <span class="counter" data-val="1000">0</span>
                        <span>+</span>
                    </div>
                    <p>家连锁门店</p>
                </div>
                <div class="vision-item">
                    <h3>2025年目标</h3>
                    <div class="counter-wrapper">
                        <span class="counter" data-val="2500">0</span>
                        <span>+</span>
                    </div>
                    <p>全国门店数量</p>
                </div>
                <div class="vision-item">
                    <h3>品质服务</h3>
                    <div class="counter-wrapper">
                        <span class="counter" data-val="95">0</span>
                        <span>%</span>
                    </div>
                    <p>顾客好评率</p>
                </div>
            </div>
        </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, onUnmounted } from 'vue'

const visionSection = ref(null)
let observer = null
let animationStarted = false

// 数字增长动画逻辑
const startCounting = () => {
  if (animationStarted) return
  animationStarted = true

  const counters = document.querySelectorAll('.counter')
  counters.forEach(counter => {
    let startVal = 0
    let endVal = parseInt(counter.getAttribute('data-val'))
    let duration = 2000 // 2秒
    let increment = endVal / (duration / 16) // 60fps

    let timer = setInterval(() => {
      startVal += increment
      if (startVal >= endVal) {
        counter.textContent = endVal
        clearInterval(timer)
      } else {
        counter.textContent = Math.floor(startVal)
      }
    }, 16)
  })
}

// 使用 IntersectionObserver 监听滚动
onMounted(() => {
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        startCounting()
        observer.disconnect() // 只触发一次
      }
    })
  }, { threshold: 0.3 })

  if (visionSection.value) {
    observer.observe(visionSection.value)
  }
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})
</script>

<style scoped>
@import '@/assets/styles/about_us_style.css';

/* 修复一些未生效的全局重置 */
.about-page {
  width: 100%;
}
</style>
