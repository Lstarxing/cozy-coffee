<!-- 会员信息卡片 + 三栏统计条 -->
<template>
  <!-- Hero Card -->
  <div class="digital-card premium-hero-card" :class="[userLevel || 'base', { dormant: userInfo?.memberStatus === 'DORMANT' }]">
    <div class="card-layer texture"></div>
    <div class="card-layer shine-effect"></div>
    <div v-if="userLevel === 'diamond'" class="card-layer holographic-overlay"></div>
    <div class="card-layer pattern-overlay"></div>

    <div class="hero-content-grid">
      <div class="brand-area">
        <div class="logo-circle">
          <svg class="start-logo" viewBox="0 0 24 24" fill="currentColor">
            <path d="M18.5,8H19C20.66,8 22,9.34 22,11V13C22,14.66 20.66,16 19,16H18.28C17.76,18.29 15.63,20 13,20H7C4.24,20 2,17.76 2,15V8H18.5ZM19,10H18V14H19C19.55,14 20,13.55 20,13V11C20,10.45 19.55,10 19,10ZM7,3H9V6H7V3ZM11,3H13V6H11V3ZM15,3H17V6H15V3Z"/>
          </svg>
        </div>
        <span class="brand-text">CozyCoffee</span>
      </div>

      <div class="points-area">
        <span class="caption">CURRENT POINTS</span>
        <span class="points-val">{{ userInfo?.currentPoints || 0 }}</span>
      </div>

      <div class="tier-emblem-area">
        <div class="emblem-3d-wrapper">
          <div v-if="userLevel === 'black'" class="emblem-shape crown">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M5 16L3 5L8.5 10L12 4L15.5 10L21 5L19 16H5M19 19C19 19.55 18.55 20 18 20H6C5.45 20 5 19.55 5 19V18H19V19Z" />
            </svg>
          </div>
          <div v-else-if="userLevel === 'diamond'" class="emblem-shape diamond">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,12l-7,10l-7,-10l3.5,-8h7l3.5,8z M12,3.5L8.5,8h7L12,3.5z"/>
            </svg>
          </div>
          <div v-else-if="['gold','silver'].includes(userLevel)" class="emblem-shape medal">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
            </svg>
          </div>
          <div v-else class="emblem-shape bean">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,2 C17.5,2 22,6.5 22,12 C22,17.5 17.5,22 12,22 C6.5,22 2,17.5 2,12 C2,6.5 6.5,2 12,2 Z" />
              <path d="M12,5 C14,5 16,8 16,11 C16,15 13,18 12,18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="shine-overlay"></div>
        </div>
        <div class="tier-text">{{ levelName }}</div>
      </div>

      <div class="footer-area">
        <span class="member-code">ID: {{ userInfo?.memberCode }}</span>
        <span v-if="userInfo?.levelExpireDate" class="expiry">EXP: {{ userInfo.levelExpireDate }}</span>
      </div>
    </div>
  </div>

  <!-- Stats Bar -->
  <div class="stats-premium-bar">
    <div class="stat-item">
      <div class="stat-icon-bg"><TrendingUp :size="25" /></div>
      <div class="stat-text">
        <span class="val">{{ userInfo?.expTotal || 0 }}</span>
        <span class="lbl">成长值</span>
      </div>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <div class="stat-icon-bg"><CalendarCheck :size="25" /></div>
      <div class="stat-text">
        <span class="val">{{ userInfo?.signInDays || 0 }} <small>天</small></span>
        <span class="lbl">连续签到</span>
      </div>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <div class="stat-icon-bg"><Target :size="25" /></div>
      <div class="stat-text">
        <span class="val highlight">{{ Math.max(0, nextLevelPoints - (userInfo?.expTotal || 0)) }}</span>
        <span class="lbl">距下一级</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { TrendingUp, CalendarCheck, Target } from 'lucide-vue-next'

const props = defineProps({
  userInfo: Object,
  userLevel: String,
  nextLevelPoints: Number
})

const levelName = computed(() => {
  const lvl = props.userLevel || 'basic'
  const map = { basic: '基础会员', silver: '白银会员', gold: '黄金会员', diamond: '钻石会员', black: '黑金会员' }
  return map[lvl] || '基础会员'
})
</script>

<style scoped>
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes hologram { 0% { filter: hue-rotate(0deg); } 100% { filter: hue-rotate(360deg); } }

.digital-card.premium-hero-card { position: relative; width: 100%; min-height: 240px; border-radius: 24px; overflow: hidden; box-shadow: 0 20px 40px -10px rgba(0,0,0,0.3); font-family: 'Inter', sans-serif; color: #3E2723; margin-bottom: 24px; }
.hero-content-grid { position: relative; z-index: 10; height: 100%; padding: 32px; display: grid; grid-template-areas: "brand emblem" "points emblem" "footer emblem"; grid-template-columns: 1fr auto; grid-template-rows: auto 1fr auto; gap: 16px; }
.card-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; z-index: 1; }
.brand-area { grid-area: brand; display: flex; align-items: center; gap: 12px; }
.logo-circle { width: auto; height: auto; background: none !important; border-radius: 0; box-shadow: none !important; padding: 0; display: flex; align-items: center; justify-content: center; color: inherit; }
.start-logo { width: 28px; height: 28px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1)); }
.brand-text { font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 800; letter-spacing: 3px; text-transform: uppercase; }
.points-area { grid-area: points; display: flex; flex-direction: column; justify-content: center; }
.points-area .caption { font-size: 11px; letter-spacing: 2px; opacity: 0.7; margin-bottom: 4px; font-weight: 600; }
.points-area .points-val { font-family: 'Playfair Display', serif; font-size: 56px; line-height: 1; font-weight: 700; }
.footer-area { grid-area: footer; display: flex; align-items: flex-end; gap: 16px; font-size: 12px; opacity: 0.8; font-family: monospace; }
.tier-emblem-area { grid-area: emblem; display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 100px; }
.emblem-3d-wrapper { width: 80px; height: 80px; position: relative; display: flex; align-items: center; justify-content: center; }
.emblem-shape { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; filter: drop-shadow(0 10px 20px rgba(0,0,0,0.3)); }
.emblem-shape svg { width: 64px; height: 64px; }
.tier-text { margin-top: 12px; font-weight: 700; font-size: 14px; letter-spacing: 1px; text-transform: uppercase; }

.premium-hero-card.base { background: #F5F0E6; color: #5D4037; }
.premium-hero-card.base .card-layer.texture { background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.1'/%3E%3C/svg%3E"); opacity: 0.4; }
.premium-hero-card.silver { background: linear-gradient(135deg, #E0E0E0 0%, #BDBDBD 100%); color: #424242; }
.premium-hero-card.gold { background: linear-gradient(135deg, #F3E5AB 0%, #D4AF37 100%); color: #4E342E; }
.premium-hero-card.diamond { background: linear-gradient(135deg, #CFD8DC 0%, #ECEFF1 50%, #B0BEC5 100%); color: #0D47A1; overflow: hidden; }
.premium-hero-card.diamond .card-layer.holographic-overlay { background: linear-gradient(45deg, rgba(33,150,243,0.1), rgba(0,229,255,0.1), rgba(101,31,255,0.1)); mix-blend-mode: color-dodge; animation: hologram 6s infinite linear; }
.premium-hero-card.black { background: #121212; color: #FFD700; }
.premium-hero-card.black .brand-text, .premium-hero-card.black .points-val, .premium-hero-card.black .tier-text { background: linear-gradient(135deg, #FFD700 0%, #FDB931 100%); background-clip: text; -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.premium-hero-card.dormant { background: linear-gradient(135deg, #2a2a2a, #3a3a3a, #2a2a2a) !important; border: 1px solid rgba(212, 175, 55, 0.3) !important; color: #888 !important; filter: saturate(0.5) brightness(0.8); }

.stats-premium-bar { display: grid; grid-template-columns: 1fr auto 1fr auto 1fr; align-items: center; background: #fff; padding: 24px; border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); margin: 24px 0; border: 1px solid rgba(0,0,0,0.03); }
.stat-item { display: flex; align-items: center; gap: 16px; justify-content: center; }
.stat-icon-bg { width: 48px; height: 48px; border-radius: 12px; background: #F9F9F9; display: flex; align-items: center; justify-content: center; color: #8D6E63; }
.stat-text { display: flex; flex-direction: column; }
.stat-text .val { font-size: 20px; font-weight: 700; color: #3E2723; }
.stat-text .lbl { font-size: 12px; color: #9E9E9E; margin-top: 2px; }
.stat-divider { width: 1px; height: 40px; background: #EEEEEE; }
</style>
