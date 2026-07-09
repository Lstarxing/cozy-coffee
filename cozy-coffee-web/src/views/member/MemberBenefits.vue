<template>
  <div class="benefits-view">
    <header class="content-header">
      <h3>会员权益</h3>
    </header>

    <!-- Tabbed Member Privileges UI -->
    <div class="member-privileges-section">
      <div class="level-tabs">
        <button
          v-for="level in ['basic', 'silver', 'gold', 'diamond', 'black']"
          :key="level"
          class="level-tab-btn"
          :class="{ active: activeLevelTab === level, [level]: true }"
          @click="activeLevelTab = level"
        >
          {{ getLevelName(level) }}
        </button>
      </div>

      <div class="level-feature-view">
        <!-- Black Gold Special Card -->
        <div v-if="activeLevelTab === 'black'" class="feature-card black-gold-theme">
          <div class="card-visual-side">
            <div class="physical-card-3d">
              <div class="card-face">
                <div class="card-brand-mark"><Coffee :size="32" /></div>
                <div class="card-logo">COZY BLACK</div>
                <div class="card-number">8888 8888 8888 8888</div>
                <div class="card-member-name">MEMBER</div>
              </div>
            </div>
            <div class="ambient-glow"></div>
          </div>
          <div class="card-content-side">
            <h3 class="feature-header">The Ultimate Experience <br><span>尊享极致体验</span></h3>
            <div class="feature-list">
              <div class="f-item"><span class="f-icon">⚡</span> <div><strong>消费 1元 = 1.5 积分</strong><p>黑卡加速包前300元享 1.7x</p></div></div>
              <div class="f-item"><span class="f-icon">☕</span> <div><strong>每月：全通兑免单券x2 + 买一赠一券x5</strong><p>Monthly Premium Coupons</p></div></div>
              <div class="f-item"><span class="f-icon">🚚</span> <div><strong>无限次免配送费</strong><p>Unlimited Free Delivery</p></div></div>
              <div class="f-item"><span class="f-icon">🌟</span> <div><strong>新品免费试饮券</strong><p>New Product Trial</p></div></div>
              <div class="f-item"><span class="f-icon">🎂</span> <div><strong>生日：全通兑免单券+免费蛋糕券+888积分</strong><p>Birthday Ultimate Pack</p></div></div>
              <div class="f-item"><span class="f-icon">💎</span> <div><strong>积分兑换 8.5 折</strong><p>Premium Redemption Discount</p></div></div>
            </div>

            <div v-if="userStore.userLevel === 'black'" class="benefit-action-area">
              <button
                class="receive-btn"
                :disabled="!monthlyBenefitStatus.canClaim || monthlyBenefitStatus.claimed || isReceivingBenefit"
                @click="handleReceiveBenefit"
              >
                <span v-if="isReceivingBenefit">领取中...</span>
                <span v-else-if="monthlyBenefitStatus.claimed">本月权益已领取</span>
                <span v-else-if="monthlyBenefitStatus.canClaim">领取黑金专属权益包</span>
                <span v-else>本月暂无可领权益</span>
              </button>
              <p v-if="shouldShowUpgradeTip" class="upgrade-tip" style="font-size: 12px; color: #fbbf24; margin-top: 8px;">
                恭喜升级！您的黑金月度权益将在下月 1 日生效。
              </p>
            </div>
            <button v-else class="cta-btn secondary">查看升级路径</button>
          </div>
        </div>

        <!-- Other Levels -->
        <div v-else class="feature-card generic-theme" :class="activeLevelTab">
          <div class="card-visual-side">
            <div class="physical-card-3d" :class="activeLevelTab">
              <div class="card-face">
                <div class="card-brand-mark" :class="activeLevelTab"><Coffee :size="32" /></div>
                <div class="card-logo">COZY {{ activeLevelTab.toUpperCase() }}</div>
                <div class="card-number">8888 8888 8888 8888</div>
                <div class="card-member-name">MEMBER</div>
              </div>
            </div>
          </div>
          <div class="card-content-side">
            <h3 class="feature-header">{{ getLevelName(activeLevelTab) }} <br><span>等级权益</span></h3>
            <ul class="generic-benefit-list">
              <li v-for="(benefit, idx) in getLevelBenefits(activeLevelTab)" :key="idx">
                <component :is="benefit.icon" :size="18" class="b-icon" /> {{ benefit.text }}
              </li>
            </ul>

            <div v-if="userStore.userLevel === activeLevelTab" class="benefit-action-area">
              <button
                class="receive-btn"
                :class="activeLevelTab"
                :disabled="!monthlyBenefitStatus.canClaim || monthlyBenefitStatus.claimed || isReceivingBenefit"
                @click="handleReceiveBenefit"
              >
                <span v-if="isReceivingBenefit">领取中...</span>
                <span v-else-if="monthlyBenefitStatus.claimed">本月权益已领取</span>
                <span v-else-if="monthlyBenefitStatus.canClaim">领取月度权益礼包</span>
                <span v-else>本月暂无可领权益</span>
              </button>
              <p v-if="shouldShowUpgradeTip" class="upgrade-tip" style="font-size: 12px; color: #e6a23c; margin-top: 8px;">
                恭喜升级！您的{{ getLevelName(userStore.userLevel) }}月度权益将在下月 1 日生效。
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Member Progress Footer -->
    <div class="member-progress-footer">
      <div class="progress-info">
        <div class="current-status">
          <span class="status-icon">
            <span v-if="userStore.userLevel === 'basic'">☕</span>
            <span v-else-if="userStore.userLevel === 'silver'">🥈</span>
            <span v-else-if="userStore.userLevel === 'gold'">🏆</span>
            <span v-else-if="userStore.userLevel === 'diamond'">💎</span>
            <span v-else-if="userStore.userLevel === 'black'">👑</span>
          </span>
          <span class="status-text">{{ levelName }} {{ (userStore.userLevel || 'basic').toUpperCase() }}</span>
        </div>
        <div class="progress-numbers">
          <span class="current-exp">{{ levelProgress.current }}</span>
          <span class="total-exp">/ {{ levelProgress.target }} EXP</span>
          <component
:is="levelProgress.nextLevelIcon" :size="24" :fill="levelProgress.nextLevelColor || 'currentColor'" fill-opacity="0.2" class="m-icon"
            :style="{ color: levelProgress.nextLevelColor || '#999', marginLeft: '8px' }" />
        </div>
      </div>
      <div class="progress-bar-container">
        <div class="progress-track">
          <div class="progress-fill" :class="userStore.userLevel || 'basic'" :style="{ width: levelProgress.percentage + '%' }"></div>
        </div>
      </div>
      <div class="progress-motivation">
        <span v-if="!levelProgress.isMax">再积 <strong>{{ levelProgress.remaining }}</strong> EXP {{ levelProgress.benefitText }}</span>
        <span v-else>🎉 {{ levelProgress.benefitText }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, markRaw } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Coffee, Coins, CalendarCheck, Gift, Zap, Ticket, Truck, Crown, Gem, Medal, Trophy } from 'lucide-vue-next'
import { getMemberBenefits, receiveMonthlyBenefit } from '@/api/member'

const userStore = useUserStore()

const activeLevelTab = ref(userStore.userLevel || 'basic')
const isReceivingBenefit = ref(false)
const monthlyBenefitStatus = ref({ claimed: false, canClaim: false, benefitName: '', claimedLevel: null, currentLevel: null })

const levelOrder = { basic: 0, silver: 1, gold: 2, diamond: 3, black: 4 }

const shouldShowUpgradeTip = computed(() => {
  if (!monthlyBenefitStatus.value.claimed) return false
  const current = monthlyBenefitStatus.value.currentLevel
  const claimed = monthlyBenefitStatus.value.claimedLevel
  if (!current || !claimed) return false
  return (levelOrder[current] || 0) > (levelOrder[claimed] || 0)
})

watch(() => userStore.userLevel, (newVal) => {
  if (newVal) activeLevelTab.value = newVal
}, { immediate: true })

const levelName = computed(() => {
  const lvl = userStore.userLevel || 'basic'
  const map = { basic: '基础会员', silver: '白银会员', gold: '黄金会员', diamond: '钻石会员', black: '黑金会员' }
  return map[lvl] || '基础会员'
})

const levelProgress = computed(() => {
  const currentExp = userStore.userInfo?.expTotal || 0
  let target = 500
  let benefitText = '解锁 [积分兑换9.8折]'
  let nextLevelIcon = markRaw(Medal)
  let nextLevelColor = '#B0BEC5'

  if (currentExp < 500) {
    target = 500; benefitText = '解锁 [积分兑换9.8折] 与 [生日月免单]'
    nextLevelIcon = markRaw(Medal); nextLevelColor = '#B0BEC5'
  } else if (currentExp < 1500) {
    target = 1500; benefitText = '解锁 [1.2倍积分] 与 [免配送权益]'
    nextLevelIcon = markRaw(Trophy); nextLevelColor = '#FFB300'
  } else if (currentExp < 4000) {
    target = 4000; benefitText = '解锁 [每月2次免配送] 与 [生日大礼包]'
    nextLevelIcon = markRaw(Gem); nextLevelColor = '#039BE5'
  } else if (currentExp < 9000) {
    target = 9000; benefitText = '解锁 [黑金加速包] 与 [线下品鉴权]'
    nextLevelIcon = markRaw(Crown); nextLevelColor = '#333333'
  } else {
    target = 99999; benefitText = '您已尊享最高等级权益'
    nextLevelIcon = null; nextLevelColor = '#FFD700'
  }

  const percentage = target === 0 ? 0 : Math.min(100, (currentExp / target) * 100)
  return {
    current: currentExp, target, percentage,
    nextLevelName: '', benefitText,
    remaining: Math.max(0, target - currentExp),
    isMax: currentExp >= 9000,
    nextLevelIcon, nextLevelColor
  }
})

function getLevelName(lvl) {
  const map = { basic: '基础会员', silver: '白银会员', gold: '黄金会员', diamond: '钻石会员', black: '黑金会员' }
  return map[lvl] || '会员'
}

function getLevelBenefits(lvl) {
  if (lvl === 'basic') return [
    { icon: markRaw(Coins), text: '消费 1元 = 1.0 积分' },
    { icon: markRaw(CalendarCheck), text: '每日签到 +2 积分，连续 7 天送优惠券' },
    { icon: markRaw(Coffee), text: '每月可领：免费加浓缩券 x1' },
    { icon: markRaw(Gift), text: '生日：单饮品 5 折券' },
    { icon: markRaw(Zap), text: '周三会员日 1.5x 积分' }
  ]
  if (lvl === 'silver') return [
    { icon: markRaw(Coins), text: '消费 1元 = 1.1 积分' },
    { icon: markRaw(Ticket), text: '积分兑换 9.8 折' },
    { icon: markRaw(Truck), text: '每月可领：配送费抵扣券x1 + 加浓缩券x2' },
    { icon: markRaw(Gift), text: '生日：买一赠一券 x1' },
    { icon: markRaw(Zap), text: '周三会员日 1.6x 积分' }
  ]
  if (lvl === 'gold') return [
    { icon: markRaw(Coins), text: '消费 1元 = 1.2 积分' },
    { icon: markRaw(Ticket), text: '积分兑换 9.5 折' },
    { icon: markRaw(Gift), text: '每月可领：买一赠一券x1 + 8.8折券x2 + 配送费抵扣x2' },
    { icon: markRaw(Gift), text: '生日：标准饮品免单券' },
    { icon: markRaw(Zap), text: '周三会员日 1.7x 积分' }
  ]
  if (lvl === 'diamond') return [
    { icon: markRaw(Coins), text: '消费 1元 = 1.3 积分' },
    { icon: markRaw(Ticket), text: '积分兑换 9.0 折' },
    { icon: markRaw(Gift), text: '每月可领：优选饮品免单券x1 + 买一赠一券x2 + 配送费抵扣券x5 + 新品5折券' },
    { icon: markRaw(Gift), text: '生日：优选饮品免单券 + 烘培甜品 5 折券' },
    { icon: markRaw(Zap), text: '周三会员日 1.8x 积分' }
  ]
  return []
}

async function checkMonthlyBenefitStatus() {
  if (!userStore.isLoggedIn) return
  try {
    const data = await getMemberBenefits()
    monthlyBenefitStatus.value = data.data
  } catch (e) {
    console.warn('Failed to check benefit status', e)
  }
}

async function handleReceiveBenefit() {
  if (isReceivingBenefit.value) return
  isReceivingBenefit.value = true
  try {
    await receiveMonthlyBenefit()
    ElMessage.success('权益已发放至您的账户')
    checkMonthlyBenefitStatus()
  } catch (e) {
    ElMessage.error(e.message || '领取失败，请稍后重试')
  } finally {
    isReceivingBenefit.value = false
  }
}

onMounted(() => {
  checkMonthlyBenefitStatus()
})
</script>

<style scoped>
.benefits-view {
  animation: fadeIn 0.4s ease-out;
  max-width: 1200px;
  margin: 0 auto;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
}

.content-header h3 {
  font-size: 28px;
  font-weight: 300;
  color: #1a1a1a;
  margin: 0;
}

.member-privileges-section { margin-top: 20px; }

.level-tabs {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 0;
}

.level-tab-btn {
  background: transparent;
  border: none;
  padding: 12px 24px;
  cursor: pointer;
  color: #9E9E9E;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.3s;
}

.level-tab-btn:hover { color: #666; }
.level-tab-btn.active { color: #333; border-bottom-color: #333; }
.level-tab-btn.active.silver { color: #757575; border-bottom-color: #BDBDBD; }
.level-tab-btn.active.gold { color: #D4AF37; border-bottom-color: #D4AF37; }
.level-tab-btn.active.diamond { color: #1565C0; border-bottom-color: #1565C0; }
.level-tab-btn.active.black { color: #212121; border-bottom-color: #000; }

.level-feature-view { animation: fadeIn 0.4s ease; }

.feature-card {
  display: flex;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0,0,0,0.08);
  min-height: 380px;
  transition: all 0.3s ease;
}

.feature-card.black-gold-theme {
  background: #000;
  color: #FFD700;
  box-shadow: 0 30px 60px rgba(0,0,0,0.4);
}

.black-gold-theme .card-visual-side {
  width: 45%;
  background: radial-gradient(circle at center, #2c2c2c 0%, #000 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.black-gold-theme .ambient-glow {
  position: absolute;
  width: 200px; height: 200px;
  background: rgba(255, 215, 0, 0.15);
  filter: blur(60px);
  border-radius: 50%;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
}

.physical-card-3d {
  width: 280px; height: 176px;
  background: linear-gradient(135deg, #1a1a1a, #000);
  border-radius: 12px;
  border: 1px solid rgba(255,215,0,0.3);
  position: relative;
  transform: perspective(1000px) rotateY(-15deg) rotateX(8deg);
  box-shadow: -15px 15px 30px rgba(0,0,0,0.5), inset 0 0 20px rgba(0,0,0,0.8);
  transition: transform 0.5s ease;
}

.feature-card:hover .physical-card-3d {
  transform: perspective(1000px) rotateY(-5deg) rotateX(2deg) scale(1.05);
}

.card-face {
  padding: 20px; height: 100%;
  display: flex; flex-direction: column; justify-content: space-between;
  background-image: linear-gradient(45deg, rgba(255,255,255,0.05) 25%, transparent 25%, transparent 50%, rgba(255,255,255,0.05) 50%, rgba(255,255,255,0.05) 75%, transparent 75%, transparent);
  background-size: 20px 20px;
}

.card-logo {
  font-family: serif;
  font-weight: 900;
  letter-spacing: 2px;
  font-size: 18px;
  color: #FFD700;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  margin-left: auto; margin-top: -30px;
}

.card-member-name { font-size: 10px; color: rgba(255,255,255,0.5); letter-spacing: 1px; }
.card-number { font-family: monospace; letter-spacing: 2px; color: #fff; opacity: 0.9; text-shadow: 0 1px 2px #000; }

.card-brand-mark {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  color: #D4AF37; opacity: 0.9;
}

.black-gold-theme .card-content-side {
  flex: 1;
  padding: 40px;
  display: flex; flex-direction: column; justify-content: center;
  background: linear-gradient(to right, #080808, #111);
}

.feature-header {
  font-family: serif;
  font-size: 28px;
  color: #fff;
  margin-bottom: 30px;
  line-height: 1.2;
}

.feature-header span { font-size: 16px; color: #D4AF37; display: block; margin-top: 8px; font-family: sans-serif; opacity: 0.9; }

.feature-list {
  display: grid; grid-template-columns: 1fr 1fr; gap: 20px;
  margin-bottom: 40px;
}

.f-item { display: flex; align-items: flex-start; }
.f-icon { font-size: 24px; margin-right: 12px; filter: drop-shadow(0 0 5px rgba(255,215,0,0.3)); }
.f-item div strong { color: #fff; font-size: 14px; display: block; margin-bottom: 2px; }
.f-item div p { color: #666; font-size: 12px; margin: 0; }

.cta-btn {
  align-self: flex-start;
  padding: 12px 32px;
  background: linear-gradient(135deg, #FFD700, #B8860B);
  border: none;
  font-weight: 700;
  color: #000;
  border-radius: 30px;
  cursor: pointer;
  box-shadow: 0 5px 15px rgba(184, 134, 11, 0.3);
  transition: all 0.3s;
}

.cta-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(184, 134, 11, 0.5); }

.generic-theme { border: 1px solid #eee; background: #fff; }

.generic-theme .card-visual-side {
  width: 40%;
  display: flex; justify-content: center; align-items: center;
  background: #fafafa;
}

.generic-theme .card-content-side { flex: 1; padding: 40px; }

.generic-benefit-list { list-style: none; padding: 0; }
.generic-benefit-list li { margin-bottom: 12px; display: flex; align-items: center; color: #555; }
.b-icon { margin-right: 6px; font-size: 14px; vertical-align: middle; }

/* 3D Card Themes */
.physical-card-3d.basic { background: #A1887F; color: #5D4037; }
.physical-card-3d.basic .card-face { background-image: none; }
.physical-card-3d.basic .card-logo, .physical-card-3d.basic .card-number { color: #3E2723; text-shadow: 0 1px 0 rgba(255,255,255,0.2); }
.physical-card-3d.basic .card-member-name { color: rgba(62, 39, 35, 0.6); }
.physical-card-3d.basic .card-brand-mark { color: #3E2723; opacity: 0.5; }

.physical-card-3d.silver { background: linear-gradient(135deg, #E0E0E0 0%, #BDBDBD 40%, #FFFFFF 50%, #BDBDBD 60%, #9E9E9E 100%); }
.physical-card-3d.silver .card-face { background-image: none; }
.physical-card-3d.silver .card-logo, .physical-card-3d.silver .card-number { color: #424242; text-shadow: 0 1px 1px rgba(255,255,255,0.8); }
.physical-card-3d.silver .card-member-name { color: #616161; }
.physical-card-3d.silver .card-brand-mark { color: #fff; text-shadow: 0 1px 2px rgba(0,0,0,0.3); }

.physical-card-3d.gold { background: linear-gradient(135deg, #FFECB3 0%, #FFC107 40%, #FF8F00 100%); }
.physical-card-3d.gold .card-face { box-shadow: inset 0 0 30px rgba(255, 215, 0, 0.3); }
.physical-card-3d.gold .card-logo, .physical-card-3d.gold .card-number { color: #795548; text-shadow: 0 1px 1px rgba(255,255,255,0.5); }
.physical-card-3d.gold .card-member-name { color: #8D6E63; }
.physical-card-3d.gold .card-brand-mark { color: #BF360C; }

.physical-card-3d.diamond { background: linear-gradient(135deg, #01579B 0%, #0288D1 50%, #29B6F6 100%); border: 1px solid rgba(255,255,255,0.5); }
.physical-card-3d.diamond .card-face { background: linear-gradient(135deg, rgba(255,255,255,0.1), transparent 40%, rgba(255,255,255,0.05)); }
.physical-card-3d.diamond .card-brand-mark { color: #E1F5FE; filter: drop-shadow(0 0 8px rgba(255,255,255,0.8)); }
.physical-card-3d.diamond .card-logo, .physical-card-3d.diamond .card-number { color: #E1F5FE; text-shadow: 0 0 10px rgba(255,255,255,0.5); }
.physical-card-3d.diamond .card-member-name { color: #81D4FA; }
.physical-card-3d.diamond .card-brand-mark { color: #fff; }

/* Feature card backgrounds */
.feature-card.basic .card-visual-side { background: #EFEBE9; }
.feature-card.basic .card-content-side { background: #FAF8F6; }
.feature-card.basic .feature-header { color: #5D4037; }
.feature-card.basic .feature-header span { color: #8D6E63; }
.feature-card.basic .generic-benefit-list li { color: #5D4037; }

.feature-card.silver .card-visual-side { background: #ECEFF1; }
.feature-card.silver .card-content-side { background: #F9FAFB; }
.feature-card.silver .feature-header { color: #37474F; }
.feature-card.silver .feature-header span { color: #607D8B; }
.feature-card.silver .generic-benefit-list li { color: #455A64; }

.feature-card.gold .card-visual-side { background: #FFF8E1; }
.feature-card.gold .card-content-side { background: #FFFCF2; }
.feature-card.gold .feature-header { color: #795548; }
.feature-card.gold .feature-header span { color: #A1887F; }
.feature-card.gold .generic-benefit-list li { color: #5D4037; }

.feature-card.diamond .card-visual-side { background: #E3F2FD; }
.feature-card.diamond .card-content-side { background: #F1F8FF; }
.feature-card.diamond .feature-header { color: #01579B; }
.feature-card.diamond .feature-header span { color: #0288D1; }
.feature-card.diamond .generic-benefit-list li { color: #0277BD; }

.card-visual-side, .card-content-side { transition: background 0.3s ease; }

.benefit-action-area { margin-top: 20px; width: 100%; }

.receive-btn {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  border: none;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: white;
  box-shadow: 0 4px 15px rgba(198, 156, 109, 0.3);
}

.receive-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(198, 156, 109, 0.4); }
.receive-btn:disabled { background: #E0E0E0; color: #9E9E9E; cursor: not-allowed; box-shadow: none; transform: none; }
.receive-btn.silver { background: linear-gradient(135deg, #ECEFF1, #CFD8DC); color: #546E7A; }
.receive-btn.diamond { background: linear-gradient(135deg, #64B5F6, #1976D2); color: white; }

.member-progress-footer {
  width: 100%;
  margin-top: 30px;
  background: #FFFFFF;
  border-radius: 16px;
  padding: 30px 40px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
  border: 1px solid rgba(0,0,0,0.04);
  display: flex;
  flex-direction: column;
}

.progress-info { display: flex; justify-content: space-between; align-items: center; width: 100%; margin-bottom: 20px; }
.current-status { display: flex; align-items: center; gap: 12px; }
.status-text { font-size: 18px; font-weight: 800; color: #333; }
.status-icon { font-size: 24px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1)); }
.progress-numbers { display: flex; align-items: baseline; gap: 8px; }
.current-exp { font-size: 32px; font-weight: 800; color: #222; font-family: 'Inter', sans-serif; letter-spacing: -1px; }
.total-exp { font-size: 15px; color: #999; font-weight: 500; }
.progress-numbers .m-icon { margin-left: 8px; color: #FFD700; transform: translateY(2px); }

.progress-bar-container { position: relative; height: 16px; width: 100%; }
.progress-track { width: 100%; height: 100%; background: #F5F5F5; border-radius: 100px; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 100px; box-shadow: 0 4px 10px rgba(0,0,0,0.1); transition: width 1s ease-out; }
.progress-fill.basic { background: linear-gradient(90deg, #D7CCC8, #A1887F); }
.progress-fill.silver { background: linear-gradient(90deg, #ECEFF1, #B0BEC5); }
.progress-fill.gold { background: linear-gradient(90deg, #FFF176, #FFB300); }
.progress-fill.diamond { background: linear-gradient(90deg, #64B5F6, #1976D2); }
.progress-fill.black { background: linear-gradient(90deg, #757575, #212121); }

.progress-motivation { margin-top: 16px; text-align: center; font-size: 13px; color: #9E9E9E; }
.progress-motivation strong { color: #555; font-weight: 600; margin: 0 2px; }
</style>
