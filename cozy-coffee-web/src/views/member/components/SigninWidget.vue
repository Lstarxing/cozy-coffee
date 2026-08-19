<!-- 每日签到组件 -->
<template>
  <div class="signin-premium-widget">
    <div class="widget-header">
      <div>
        <h4>每日签到</h4>
        <span class="sub">连续签到 7 天可领惊喜礼包</span>
      </div>
      <button class="signin-action-btn" :disabled="isSignedToday" @click="handleSignIn">
        {{ isSignedToday ? '今日已签' : '签到领豆' }}
      </button>
    </div>

    <div class="bean-track-wrapper">
      <div class="track-line-base">
        <div class="track-line-fill" :style="{ width: Math.max(0, (currentSignInCycleDay - 1) / 6 * 100) + '%' }"></div>
      </div>
      <div class="bean-steps">
        <div v-for="day in 7" :key="day" class="bean-step"
          :class="{ 'is-active': day <= currentSignInCycleDay, 'is-today': day === currentSignInCycleDay }">
          <div class="bean-icon-box" :class="{ 'is-gift': day === 7 }">
            <template v-if="day === 7">
              <div class="gift-box-3d">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="gift-svg">
                  <polyline points="20 12 20 22 4 22 4 12"></polyline>
                  <rect x="2" y="7" width="20" height="5"></rect>
                  <line x1="12" y1="22" x2="12" y2="7"></line>
                  <path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"></path>
                  <path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"></path>
                </svg>
              </div>
              <div v-if="day <= currentSignInCycleDay" class="gift-glow"></div>
            </template>
            <template v-else>
              <svg class="bean-svg" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,2 C6.5,2 2,6.5 2,12 C2,17.5 6.5,22 12,22 C17.5,22 22,17.5 22,12 C2,6.5 17.5,2 12,2 Z M12.5,5 C12.5,5 14,8 14,12 C14,16 12.5,19 12.5,19 C11,19 8,16 8,12 C8,8 11,5 12.5,5 Z" />
              </svg>
            </template>
          </div>
          <span class="step-label">{{ day === 7 ? '礼包' : '+2' }}</span>
        </div>
      </div>
    </div>

    <div v-if="currentSignInCycleDay < 7" class="widget-footer-info">
      <span class="info-icon">✨</span>
      <span>再签 <strong>{{ 7 - currentSignInCycleDay }}</strong> 天领满35-10券</span>
    </div>
    <div v-else class="widget-footer-info success">
      <span class="info-icon">🎁</span>
      <span>连续签到7天！礼包已到账</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { signIn } from '@/api/member'

const props = defineProps({
  userInfo: Object,
  isSignedToday: Boolean
})

const emit = defineEmits(['signin-success'])

const userStore = useUserStore()

const effectiveSignInDays = computed(() => {
  const info = props.userInfo
  if (!info || !info.lastSigninDate) return 0
  const days = info.signInDays || 0
  if (days === 0) return 0
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const lastSignDate = new Date(info.lastSigninDate); lastSignDate.setHours(0, 0, 0, 0)
  const diffTime = Math.abs(today - lastSignDate)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  if (diffDays > 1) return 0
  return days
})

const currentSignInCycleDay = computed(() => {
  const days = effectiveSignInDays.value
  if (days === 0) return 0
  return days % 7 === 0 ? 7 : days % 7
})

function getSigninPointsByLevel() { return 2 }

async function handleSignIn() {
  try {
    if (!userStore.token) return
    const data = await signIn()
    const result = data.data
    ElMessage.success(result.message || `签到成功！积分+${result.pointsEarned}`)
    emit('signin-success', result)
  } catch (error) {
    ElMessage.error(error.message || '签到失败')
  }
}
</script>

<style scoped>
.signin-premium-widget { background: #fff; border-radius: 20px; padding: 24px; box-shadow: 0 8px 30px rgba(0,0,0,0.04); border: 1px solid rgba(0,0,0,0.02); margin-bottom: 24px; }
.widget-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; }
.widget-header h4 { font-size: 18px; font-weight: 700; color: #3E2723; margin-bottom: 4px; }
.widget-header .sub { font-size: 13px; color: #8D6E63; }
.signin-action-btn { background: #3E2723; color: #FBEEA8; border: none; padding: 8px 24px; border-radius: 20px; font-size: 13px; font-weight: 600; cursor: pointer; }
.signin-action-btn:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(62, 39, 35, 0.3); }
.signin-action-btn:disabled { background: #E0E0E0; color: #9E9E9E; cursor: not-allowed; transform: none; box-shadow: none; }
.bean-track-wrapper { position: relative; padding: 20px 0; }
.track-line-base { position: absolute; top: 36px; left: 20px; right: 20px; height: 4px; background: #F5F5F5; border-radius: 2px; transform: translateY(-50%); z-index: 1; }
.track-line-fill { height: 100%; background: #C69C6D; border-radius: 2px; transition: width 0.5s ease; }
.bean-steps { display: flex; justify-content: space-between; position: relative; z-index: 2; }
.bean-step { display: flex; flex-direction: column; align-items: center; gap: 8px; width: 40px; }
.bean-icon-box { width: 32px; height: 32px; background: #EFEBE9; border-radius: 50%; border: 2px solid #fff; display: flex; align-items: center; justify-content: center; color: #BCAAA4; transition: all 0.3s; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.bean-step.is-active .bean-icon-box { background: #5D4037; color: #FBEEA8; transform: scale(1.1); }
.bean-step.is-today .bean-icon-box { box-shadow: 0 0 0 4px rgba(198, 156, 109, 0.3); }
.bean-svg { width: 18px; height: 18px; }
.bean-icon-box.is-gift { width: 48px; height: 48px; border-radius: 16px; background: linear-gradient(135deg, #8D6E63 0%, #5D4037 100%); color: #FBEEA8; margin-top: -8px; display: flex; align-items: center; justify-content: center; }
.gift-svg { width: 24px; height: 24px; }
.step-label { font-size: 11px; color: #9E9E9E; font-weight: 500; }
.bean-step.is-active .step-label { color: #5D4037; font-weight: 700; }
.widget-footer-info { display: flex; align-items: center; gap: 8px; margin-top: 16px; padding: 12px 16px; background: #f9f9f9; border-radius: 8px; font-size: 14px; color: #555; }
.widget-footer-info.success { background: #F3E9DF; color: #753A22; }
</style>
