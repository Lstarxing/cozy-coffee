<template>
  <div class="home-container">
    <!-- 首页部分 (Hero Section) -->
    <section id="home" class="hero-section">
      <div class="hero-slider">
        <!-- 左右箭头 -->
        <div class="slider-arrows">
          <button class="arrow prev-arrow" @click="prevSlide">&#10094;</button>
          <button class="arrow next-arrow" @click="nextSlide">&#10095;</button>
        </div>
        
        <!-- 幻灯片 -->
        <div 
          v-for="(slide, index) in slides" 
          :key="index"
          class="slide" 
          :class="{ active: currentSlide === index }"
        >
          <img :src="slide" :alt="'咖啡店图片' + (index + 1)">
        </div>

        <!-- 轮播点 -->
        <div class="slider-dots">
          <span 
            v-for="(slide, index) in slides" 
            :key="'dot-' + index"
            class="dot" 
            :class="{ active: currentSlide === index }"
            @click="goToSlide(index)"
          ></span>
        </div>
      </div>
      
      <div class="hero-content">
        <h1>欢迎来到咖熙咖啡</h1>
        <p>每一杯都是手心的温度，每一刻都是生活的馈赠</p>
        <a href="#menu" class="cta-button">查看更多</a>
      </div>
    </section>

    <!-- 品牌特色部分 -->
    <section id="features" class="features-section">
      <div class="feature-block bean-source" id="bean-source">
        <div class="feature-content">
          <h2 class="animate-title">臻选产地 | Premium Origins</h2>
          <div class="origin-cards">
            <div class="origin-card" v-for="origin in origins" :key="origin.name">
              <div class="card-image">
                <img :src="origin.image" :alt="origin.name">
                <div class="image-overlay"></div>
              </div>
              <div class="card-content">
                <h3>{{ origin.name }}</h3>
                <p class="altitude">{{ origin.altitude }}</p>
                <p class="flavor">{{ origin.flavor }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>    
    </section>

    <!-- 获奖海报 -->
    <section class="prize-section">
      <div class="prize-banner">
        <img src="/images/banner_prize.png" alt="咖熙咖啡连续5年获得IIAC金奖">
      </div>
    </section>

    <!-- 主体菜单区域 -->
    <section id="menu" class="menu-section">
      <div class="menu-container">
        <h1 class="section-title">精品推荐 | Best Sellers</h1>
        <p class="section-subtitle" style="text-align: center;">一杯咖啡，一种心情</p>
        <div class="menu-grid"> 
          <div class="menu-item" v-for="item in menuItems" :key="item.name">
            <img :src="item.image" :alt="item.name">
            <div class="menu-item-content">
              <h3>{{ item.name }}</h3>
              <p>{{ item.desc }}</p>
              <span class="price">{{ item.price }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 会员计划部分 -->
    <section id="membership" class="membership-section">
      <div class="membership-container">
        <h1 class="section-title">会员计划 | Membership Plan</h1>
        <p class="section-subtitle" style="text-align: center;">加入 Cozy Cafe 会员，开启您的专属咖啡之旅</p>
        
        <!-- 会员权益概览 -->
        <div class="membership-intro">
          <div class="intro-card" v-for="intro in intros" :key="intro.title">
            <div class="intro-icon">{{ intro.icon }}</div>
            <h3>{{ intro.title }}</h3>
            <p>{{ intro.desc }}</p>
          </div>
        </div>

        <!-- 会员等级体系 -->
        <div class="membership-levels">
          <h2>会员成长体系</h2>
          <div class="level-cards">
            <div class="level-card" v-for="level in levels" :key="level.class">
              <div class="level-header" :class="level.class">
                <h3>{{ level.name }}</h3>
                <p>{{ level.desc }}</p>
              </div>
              <div class="level-benefits">
                <ul>
                  <li v-for="(benefit, idx) in level.benefits" :key="idx">{{ benefit }}</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <!-- 加入按钮 -->
        <div class="join-now">
          <router-link to="/register" class="join-button">立即加入</router-link>
          <p class="terms">*积分有效期为24个月</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

// --- Slider Logic ---
const currentSlide = ref(0)
const slides = [
  '/images/slide1.png',
  '/images/slide2.png',
  '/images/slide3.png',
  '/images/slide4.png',
  '/images/slide5.png'
]
let sliderInterval = null

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % slides.length
}

const prevSlide = () => {
  currentSlide.value = (currentSlide.value - 1 + slides.length) % slides.length
}

const goToSlide = (index) => {
  currentSlide.value = index
}

// --- Data ---
const origins = [
  {
    name: '巴西圣保罗',
    altitude: '海拔1200米以上黄波旁',
    flavor: '醇厚果香，坚果巧克力风味',
    image: '/images/origins/brazil.webp'
  },
  {
    name: '埃塞俄比亚耶加雪菲',
    altitude: '海拔1800-2200米',
    flavor: '焦糖甜感，柑橘般明亮风味',
    image: '/images/origins/ethiopia.avif'
  },
  {
    name: '哥伦比亚安第斯',
    altitude: '海拔1700-2000米',
    flavor: '花香馥郁，莓果般明亮酸甜',
    image: '/images/origins/colombia.png'
  }
]

const menuItems = [
  { name: '美式咖啡', desc: '优选上等阿拉比卡豆萃取美式', price: '¥18', image: '/images/cafe1.png' },
  { name: '原味拿铁', desc: '丝滑浓醇的拿铁', price: '¥25', image: '/images/cafe2.jpg' },
  { name: '卡布奇诺', desc: '完美比例的卡布奇诺', price: '¥30', image: '/images/cafe3.jpg' },
  { name: '摩卡咖啡', desc: '香浓可可与咖啡的邂逅', price: '¥30', image: '/images/cafe4.jpg' },
  { name: '抹茶拿铁', desc: '抹茶浓郁的拿铁', price: '¥30', image: '/images/cafe5.jpg' },
  { name: '生椰拿铁', desc: '醇厚椰香与咖啡的完美融合', price: '¥28', image: '/images/cafe6.jpg' }
]

const intros = [
  { icon: '🎁', title: '零门槛加入', desc: '注册即享20积分好礼' },
  { icon: '💫', title: '多重积分奖励', desc: '消费、推荐好友多渠道赚积分' },
  { icon: '🏆', title: '专属特权礼遇', desc: '等级升级尊享多重权益' }
]

const levels = [
  {
    class: 'basic',
    name: '基础会员',
    desc: '开启您的咖啡之旅',
    benefits: ['积分倍率1元=1积分', '生日当月双倍积分', '每日签到享积分', '积分商城基础兑换资格']
  },
  {
    class: 'silver',
    name: '白银会员',
    desc: '累计1000积分',
    benefits: ['积分倍率1元=1.2积分', '生日享免费中杯饮品', '每月2张免配送费券', '积分商城中级兑换资格']
  },
  {
    class: 'gold',
    name: '黄金会员',
    desc: '累计3000积分',
    benefits: ['积分倍率1元=1.5积分', '生日享免费大杯饮品+甜品', '新品品鉴会优先邀请', '会员专属休息区使用权', '积分商城高级兑换资格']
  },
  {
    class: 'black',
    name: '黑金会员',
    desc: '累计8000积分',
    benefits: ['积分倍率1元=2积分', '生日享全年任意饮品免单券+200积分', '专属实体黑金卡', '咖啡师上门服务', '无限次免配送费', '年度品牌活动VIP邀请', '积分商城黑金专属兑换资格']
  }
]

// --- Lifecycle ---
onMounted(() => {
  sliderInterval = setInterval(nextSlide, 5000)
})

onUnmounted(() => {
  if (sliderInterval) clearInterval(sliderInterval)
})
</script>

<style scoped>
/* 避免样式污染，部分特定样式可在此覆盖 */
.home-container {
  width: 100%;
}
</style>
