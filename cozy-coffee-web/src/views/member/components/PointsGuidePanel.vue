<!-- 积分指南面板 + 积分明细弹窗 -->
<template>
  <div class="points-guide-section">
    <div class="section-title">
      <h4>积分获取</h4>
      <div class="header-right-action">
        <span class="subtitle">完成任务积攒成长值</span>
        <button class="link-btn-small" @click="showDetailModal = true">明细 &gt;</button>
      </div>
    </div>

    <div class="points-channels" style="gap: 20px;">
      <div class="channel-card" :class="{ done: isSignedToday }">
        <div class="channel-icon-bg"><i class="icon-calendar"></i></div>
        <div class="channel-info">
          <span class="channel-name">每日签到</span>
          <span class="channel-desc">连续签到额外奖励</span>
        </div>
        <div class="channel-points simple-row">
          <span>+{{ getSigninPointsByLevel() }} 积分/天</span>
        </div>
        <span v-if="isSignedToday" class="status-check"><Check :size="20" /></span>
      </div>

      <div class="channel-card" :class="{ done: profileComplete }">
        <div class="channel-icon-bg"><i class="icon-edit"></i></div>
        <div class="channel-info">
          <span class="channel-name">完善资料</span>
          <span class="channel-desc">填写手机号和邮箱</span>
        </div>
        <div class="channel-points simple-row">
          <span>+20 积分</span>
        </div>
        <span v-if="profileComplete" class="status-check"><Check :size="20" /></span>
        <button v-else class="go-btn" @click="$emit('navigate', '/member/profile')">去完成</button>
      </div>

      <div class="channel-card">
        <div class="channel-icon-bg"><i class="icon-coffee"></i></div>
        <div class="channel-info">
          <span class="channel-name">消费赚积分</span>
        </div>
        <div class="channel-points simple-row">
          <span>1元={{ getConsumeMultiplier() }} 积分</span>
        </div>
        <button class="go-btn consume" @click="$emit('navigate', '/member/order')">去下单</button>
      </div>
    </div>

    <div v-if="userLevel !== 'black'" class="level-tip">
      <div class="tip-content">
        <span class="tip-icon"><Rocket :size="18" /></span>
        <span class="tip-text">
          升级到 <strong>{{ nextLevelName }}</strong> 还需 <strong>{{ Math.max(0, nextLevelPoints - (userInfo?.expTotal || 0)) }}</strong> EXP
        </span>
      </div>
      <button class="view-benefits-text-btn" @click="$emit('navigate', '/member/benefits')">查看权益</button>
    </div>

    <div class="promo-banner">
      <img src="/images/banner-promo.png" alt="Promo" />
    </div>

    <PointsDetailModal v-model:visible="showDetailModal" :user-info="userInfo" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Check, Rocket } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import PointsDetailModal from './PointsDetailModal.vue'

defineProps({
  userInfo: Object,
  userLevel: String,
  isSignedToday: Boolean,
  profileComplete: Boolean,
  nextLevelPoints: Number
})

defineEmits(['navigate'])

const userStore = useUserStore()
const showDetailModal = ref(false)

function getSigninPointsByLevel() { return 2 }

function getConsumeMultiplier() {
  const level = userStore.userLevel || 'basic'
  const map = { basic: 1, silver: 1.1, gold: 1.2, diamond: 1.3, black: 1.5 }
  if (level === 'black' && (userStore.userInfo?.monthlyAccelerateRemaining || 0) > 0) return 1.7
  return map[level] || 1
}

const nextLevelName = (() => {
  const level = userStore.userLevel || 'basic'
  const map = { basic: '白银会员', silver: '黄金会员', gold: '钻石会员', diamond: '黑金会员', black: '黑金会员' }
  return map[level] || '白银会员'
})()
</script>

<style scoped>
.points-guide-section { background: #fff; border-radius: 24px; padding: 24px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); height: 100%; display: flex; flex-direction: column; }
.section-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.section-title h4 { font-size: 18px; font-weight: 700; color: #1f2937; letter-spacing: -0.5px; margin: 0; }
.section-title .subtitle { font-size: 13px; color: #9ca3af; font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px; }
.header-right-action { display: flex; align-items: center; gap: 12px; }
.link-btn-small { background: none; border: none; font-size: 12px; color: #8D6E63; cursor: pointer; padding: 0; font-weight: 600; }
.link-btn-small:hover { text-decoration: underline; color: #5D4037; }
.promo-banner { margin-top: auto; padding-top: 24px; width: 100%; }
.promo-banner img { width: 100%; border-radius: 16px; display: block; object-fit: cover; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

.channel-card { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 16px; border: 1px solid #F5F5F5; border-radius: 16px; background: #FAFAFA; transition: all 0.2s; }
.channel-info { flex: 1; display: flex; flex-direction: column; }
.channel-points.simple-row span { font-size: 14px; color: #C69C6D; font-weight: 400; white-space: nowrap; }
.channel-card:hover { background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.04); transform: translateY(-2px); border-color: #EFEBE9; }
.channel-card.done { background: #f8fff8; border-color: #4CAF50; }
.channel-name { display: block; font-weight: 600; color: #2C1810; font-size: 14px; }
.channel-desc { display: block; font-size: 12px; color: #888; margin-top: 2px; }
.go-btn { background: none; border: none; font-size: 13px; font-weight: 600; color: #8D6E63; cursor: pointer; padding: 6px 12px; border-radius: 8px; transition: all 0.2s; white-space: nowrap; }
.go-btn:hover { background: #EFEBE9; color: #5D4037; }
.status-check { color: #D97706; display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; background: #FEF3C7; border-radius: 50%; }
.level-tip { display: flex; align-items: center; gap: 10px; margin-top: 16px; padding: 12px 16px; background: white; border-radius: 10px; border: 1px dashed #C69C6D; }
.tip-content { display: flex; align-items: center; gap: 8px; flex: 1; }
.view-benefits-text-btn { background: none; border: none; color: #C69C6D; font-weight: 600; cursor: pointer; font-size: 12px; }
</style>
