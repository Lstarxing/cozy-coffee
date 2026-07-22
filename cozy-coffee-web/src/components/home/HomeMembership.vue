<template>
  <section id="membership" class="warm-membership" aria-labelledby="membership-title">
    <div class="warm-shell membership-shell">
      <template v-if="membershipState === 'auth-resolving'">
        <div class="membership-skeleton" aria-label="正在确认登录状态" aria-busy="true">
          <span></span><span></span><span></span>
        </div>
      </template>

      <template v-else>
        <header class="membership-header">
          <p class="membership-header__kicker">MEMBERSHIP JOURNEY</p>
          <h2 id="membership-title">这一杯，也在回馈下一杯</h2>
          <p>消费 · 积累 · 成长 · 解锁更多专属体验</p>
        </header>

        <div v-if="membershipState === 'member-loading'" class="membership-skeleton membership-skeleton--content" aria-label="正在读取会员数据" aria-busy="true">
          <span></span><span></span><span></span>
        </div>

        <div v-else-if="membershipState === 'member-failed'" class="membership-error" role="status">
          <h3>暂时没有读到你的会员数据</h3>
          <p>网络恢复后可以在这里继续查看积分进度。</p>
          <div class="membership-actions">
            <button class="warm-button warm-button--inverse" type="button" @click="loadMemberData">重试读取</button>
            <details class="benefits-details benefits-details--compact">
              <summary>查看示例权益</summary>
              <p>普通日消费 ¥32，白银会员可获得 35 积分；周五会员日额外 +0.5×。</p>
            </details>
          </div>
        </div>

        <template v-else>
          <!-- 等级 + 成长进度 - 居中单列 -->
          <div class="membership-progress-section">
            <p class="membership-progress__kicker">CURRENT MEMBERSHIP</p>
            <p class="membership-progress__level-en">{{ levelProgress.currentLevel.name.split(' ')[1] }}</p>
            <p class="membership-progress__level-cn">{{ levelProgress.currentLevel.name.split(' ')[0] }}会员</p>
            <p class="membership-progress__exp-val">{{ membershipState === 'anonymous' ? 850 : levelProgress.current }}<span class="membership-progress__exp-unit"> EXP</span></p>

            <div v-if="!levelProgress.isMax" class="membership-progress__line">
              <span class="membership-progress__line-dot" :style="{ left: levelProgress.percentage + '%' }"></span>
            </div>

            <p v-if="!levelProgress.isMax" class="membership-progress__next">
              <span class="membership-progress__next-en">{{ levelProgress.nextLevelName }}</span>
              <span class="membership-progress__next-cn">距离下一阶段，还有 {{ levelProgress.remaining }} EXP</span>
            </p>
            <p v-else class="membership-progress__max">COZY BLACK MEMBER · 品牌大使</p>
          </div>

          <div class="membership-hairline"></div>

          <!-- 消费 & 兑换示例 - 双栏 -->
          <div class="membership-activity">

            <div class="membership-activity__grid">
              <!-- 01: 消费得积分 -->
              <div class="membership-activity__col">
                <p class="membership-activity__header">
                  RECENT MOMENT<span class="membership-activity__header-dot"> · </span><span class="membership-activity__seq">01</span>
                </p>
                <div class="membership-activity__row">
                  <span class="membership-activity__name">手冲咖啡</span>
                  <span class="membership-activity__price">¥35</span>
                </div>
                <p class="membership-activity__earned-kicker">EARNED</p>
                <div class="membership-activity__earned-row">
                  <span class="membership-activity__earned-num">35</span>
                  <span class="membership-activity__earned-unit">EXP</span>
                  <span class="membership-activity__earned-plus">+</span>
                  <span class="membership-activity__earned-num">{{ earnedPoints }}</span>
                  <span class="membership-activity__earned-unit">POINTS</span>
                </div>
                <p class="membership-activity__bonus-note">{{ levelLabel }} {{ currentRate }}× 倍率</p>
                <p class="membership-activity__col-narrative">"因为一次咖啡选择，积累下一次相遇"</p>
              </div>

              <!-- 02: 积分兑换 -->
              <div class="membership-activity__col">
                <p class="membership-activity__header">
                  RECENT MOMENT<span class="membership-activity__header-dot"> · </span><span class="membership-activity__seq">02</span>
                </p>
                <div class="membership-activity__row">
                  <span class="membership-activity__name">拿铁兑换券</span>
                  <span>                    <span class="membership-activity__price membership-activity__price--strike">150</span></span>
                </div>
                <p class="membership-activity__earned-kicker">REDEEM</p>
                <div class="membership-activity__earned-row">
                  <span class="membership-activity__earned-num">{{ rewardTarget }}</span>
                  <span class="membership-activity__earned-unit">POINTS</span>
                </div>
                <p class="membership-activity__bonus-note">{{ levelLabel }} {{ currentDiscount }}兑换</p>
                <p class="membership-activity__col-narrative">"用积分换一杯心仪，也是给自己的犒赏"</p>
              </div>
            </div>
          </div>

          <div class="membership-hairline"></div>

          <!-- 权益亮点 - 单列编号纵向 -->
          <div class="membership-benefits">
            <p class="membership-benefits__kicker">MEMBERSHIP JOURNEY</p>

            <article class="benefit-item">
              <span class="benefit-item__seq">01</span>
              <div class="benefit-item__body">
                <p class="benefit-item__en">MONTHLY BENEFIT</p>
                <p class="benefit-item__cn">月度权益</p>
                <p class="benefit-item__desc">每个月，一份属于会员的日常心意</p>
              </div>
            </article>

            <article class="benefit-item">
              <span class="benefit-item__seq">02</span>
              <div class="benefit-item__body">
                <p class="benefit-item__en">BIRTHDAY REWARD</p>
                <p class="benefit-item__cn">生日礼遇</p>
                <p class="benefit-item__desc">生日这个月，有一杯咖啡算我们的</p>
              </div>
            </article>

            <article class="benefit-item">
              <span class="benefit-item__seq">03</span>
              <div class="benefit-item__body">
                <p class="benefit-item__en">MEMBERSHIP JOURNEY</p>
                <p class="benefit-item__cn">等级成长</p>
                <p class="benefit-item__desc">每一次停留，都让下一次体验更丰富</p>
              </div>
            </article>
          </div>

          <div class="membership-hairline"></div>

          <!-- 完整等级对比表 -->
          <details class="benefits-details">
            <summary>Explore Membership Levels -></summary>
            <div v-if="!isMobile" class="benefits-table-wrap">
              <table aria-label="会员等级速览">
                <thead>
                  <tr><th scope="col">等级</th><th scope="col">EXP门槛</th><th scope="col">积分</th><th scope="col">会员日</th><th scope="col">折扣</th><th scope="col">每月权益</th></tr>
                </thead>
                <tbody>
                  <tr v-for="level in levels" :key="level.key" :class="{ 'is-current': isCurrentLevel(level.key) }">
                    <td>{{ level.name }} <span v-if="isCurrentLevel(level.key)" class="current-marker">当前</span></td>
                    <td>{{ level.threshold }}</td><td>{{ level.rate }}</td><td>{{ level.cozyDay }}</td><td>{{ level.discount }}</td><td>{{ level.benefit }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="benefits-list" aria-label="会员等级速览">
              <dl v-for="level in levels" :key="level.key" :class="{ 'is-current': isCurrentLevel(level.key) }">
                <div class="benefits-list__title"><dt>等级</dt><dd>{{ level.name }} <span v-if="isCurrentLevel(level.key)" class="current-marker">当前</span></dd></div>
                <div><dt>EXP门槛</dt><dd>{{ level.threshold }}</dd></div>
                <div><dt>积分倍率</dt><dd>{{ level.rate }}</dd></div>
                <div><dt>会员日</dt><dd>{{ level.cozyDay }}</dd></div>
                <div><dt>兑换折扣</dt><dd>{{ level.discount }}</dd></div>
                <div><dt>每月权益</dt><dd>{{ level.benefit }}</dd></div>
              </dl>
            </div>
          </details>

          <div class="membership-hairline"></div>

          <div class="membership-journey__cta">
            <router-link
              class="membership-journey__cta-link"
              :to="membershipState === 'anonymous' ? '/register' : '/member'"
            >
              {{ membershipState === 'anonymous' ? '开始你的咖啡旅程 ->' : '进入会员中心 →' }}
            </router-link>
          </div>
        </template>
      </template>
    </div>
  </section>
</template>

<script setup>
import { useHomeMembership } from '@/composables/useHomeMembership'

defineProps({
  isMobile: { type: Boolean, default: false }
})

const {
  membershipState,
  levels,
  levelProgress,
  earnedPoints,
  levelLabel,
  currentRate,
  rewardTarget,
  currentDiscount,
  isCurrentLevel,
  loadMemberData
} = useHomeMembership()
</script>

<style scoped>
.warm-button {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  font: inherit;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  border-radius: 10px;
  padding: 11px 22px;
}

.warm-button--inverse {
  color: var(--cozy-cta-alt-text);
  background: var(--cozy-cta-alt-bg);
}

.warm-button--inverse:hover { filter: brightness(0.94); }

.warm-membership h2 { font-size: clamp(1.75rem, 3vw, 2.4rem); line-height: 1.2; font-weight: 500; }
.membership-header__kicker { margin: 0 0 8px; font-size: 11px; font-weight: 500; letter-spacing: .18em; text-transform: uppercase; color: var(--cozy-muted-on-alt); }
.membership-header p { max-width: 34em; margin: 12px 0 0; color: var(--cozy-muted-on-alt); font-size: 15px; line-height: 1.7; }

.membership-hairline { width: 100%; height: 1px; margin-block: clamp(28px, 3vw, 40px); background: rgba(255,255,255,.1); }

/* ── 等级 + 成长进度 - 居中单列 ── */
.membership-progress-section { text-align: center; padding-block: clamp(48px, 6vw, 72px); }
.membership-progress__kicker { margin: 0 0 32px; font-size: 11px; font-weight: 500; letter-spacing: .14em; text-transform: uppercase; color: var(--cozy-muted-on-alt); }
.membership-progress__level-en { margin: 0; font-family: var(--font-display); font-size: clamp(48px, 6.5vw, 72px); font-weight: 500; line-height: 1; color: var(--cozy-on-surface-alt); }
.membership-progress__level-cn { margin: 8px 0 0; font-size: 14px; color: var(--cozy-muted-on-alt); }
.membership-progress__exp-val { margin: 24px 0 0; font-family: var(--font-display); font-size: 32px; font-weight: 500; color: var(--cozy-on-surface-alt); }
.membership-progress__exp-unit { font-size: 13px; letter-spacing: .06em; color: var(--cozy-muted-on-alt); }

/* 线与圆点公用 top:50% 容器，保证圆心必定在线上 */
.membership-progress__line { position: relative; height: 12px; margin: clamp(32px, 5vw, 48px) auto 0; max-width: 360px; background: linear-gradient(rgba(255,255,255,.20), rgba(255,255,255,.20)) no-repeat center / 100% 1px; }
.membership-progress__line-dot { position: absolute; top: 50%; left: 0; transform: translate(-50%, -50%); width: 6px; height: 6px; border-radius: 50%; background: var(--cozy-on-surface-alt); transition: left 1s ease-out; }

.membership-progress__next { margin-top: 20px; text-align: center; }
.membership-progress__next-en { display: block; font-family: var(--font-display); font-size: clamp(1.3rem, 1.8vw, 1.6rem); color: var(--cozy-on-surface-alt); opacity: .45; }
.membership-progress__next-cn { display: block; margin-top: 6px; font-size: 12px; color: var(--cozy-muted-on-alt); }
.membership-progress__max { margin-top: 32px; font-family: var(--font-display); font-size: 1rem; letter-spacing: .04em; color: var(--cozy-on-surface-alt); }

/* ── 消费 & 兑换示例（Editorial 双栏） ── */
.membership-activity { padding-block: clamp(28px, 3vw, 40px); }

/* Group 1: Header — inline with dot separator */
.membership-activity__header { margin: 0 0 24px; font-size: 11px; font-weight: 600; letter-spacing: .15em; text-transform: uppercase; color: #8C7A6B; }
.membership-activity__header-dot { font-weight: 400; color: var(--cozy-muted-on-alt); }
.membership-activity__seq { font-family: var(--font-display); font-size: 13px; font-weight: 400; letter-spacing: 0; text-transform: none; color: var(--cozy-muted-on-alt); }

/* Two-column grid */
.membership-activity__grid { display: grid; grid-template-columns: 1fr 1fr; gap: clamp(24px, 4vw, 48px); }
.membership-activity__col { min-width: 0; }

/* Group 2: Product row + border */
.membership-activity__row { display: flex; align-items: baseline; justify-content: space-between; padding-bottom: 14px; border-bottom: 1px solid rgba(255,255,255,.08); }
.membership-activity__name { font-size: clamp(1.1rem, 1.4vw, 1.3rem); font-weight: 600; color: var(--cozy-on-surface-alt); letter-spacing: .02em; }
.membership-activity__price { font-family: var(--font-display); font-size: 15px; color: var(--cozy-muted-on-alt); }
.membership-activity__price--strike { text-decoration: line-through; opacity: .45; margin-right: 6px; }

/* Group 3: Earned / Redeem block — tight grouping */
.membership-activity__earned-kicker { margin: 24px 0 0; font-size: 10px; font-weight: 600; letter-spacing: .15em; text-transform: uppercase; color: #8C7A6B; }
.membership-activity__earned-row { display: flex; align-items: baseline; gap: 8px; margin-top: 6px; }
.membership-activity__earned-num { font-family: var(--font-display); font-size: clamp(1.75rem, 2.4vw, 2.25rem); font-weight: 700; color: var(--cozy-on-surface-alt); line-height: 1; }
.membership-activity__earned-unit { font-size: 14px; font-weight: 600; letter-spacing: .08em; color: var(--cozy-muted-on-alt); }
.membership-activity__earned-plus { margin: 0 4px; font-size: 18px; font-weight: 300; color: var(--cozy-muted-on-alt); }
.membership-activity__bonus-note { margin-top: 10px; font-size: 12px; color: rgba(255,255,255,.45); }
.membership-activity__col-narrative { margin-top: 18px; font-family: var(--font-display); font-size: 12px; letter-spacing: .02em; color: var(--cozy-muted-on-alt); line-height: 2; }

/* ── 权益亮点 - 单列编号纵向 ── */
.membership-benefits { max-width: 760px; margin: 0 auto; padding-block: clamp(28px, 3vw, 40px); }
.membership-benefits__kicker { margin: 0 0 clamp(28px, 3vw, 40px); font-size: 11px; font-weight: 500; letter-spacing: .14em; text-transform: uppercase; color: var(--cozy-muted-on-alt); }
.benefit-item { display: grid; grid-template-columns: 80px 1fr; gap: 20px; padding: clamp(36px, 4vw, 48px) 0; border-bottom: 1px solid rgba(255,255,255,.08); }
.benefit-item:last-child { border-bottom: 0; }
.benefit-item__seq { font-family: var(--font-display); font-size: 18px; color: var(--cozy-muted-on-alt); padding-top: 2px; }
.benefit-item__en { margin: 0; font-size: 12px; font-weight: 500; letter-spacing: .14em; color: var(--cozy-on-surface-alt); }
.benefit-item__cn { margin: 8px 0 0; font-family: var(--font-display); font-size: clamp(1.1rem, 1.4vw, 1.25rem); color: var(--cozy-on-surface-alt); }
.benefit-item__desc { margin: 12px 0 0; font-family: var(--font-display); font-style: italic; font-size: 14px; color: var(--cozy-muted-on-alt); line-height: 1.7; }

/* ── CTA ── */
.membership-journey__cta { display: flex; justify-content: center; margin-top: clamp(20px, 2.6vw, 32px); }
.membership-journey__cta-link { padding: 14px 36px; border: 1px solid rgba(255,255,255,.18); font-size: 15px; font-weight: 500; color: var(--cozy-on-surface-alt); text-decoration: none; transition: border-color .22s ease, background .22s ease; }
.membership-journey__cta-link:hover { border-color: rgba(255,255,255,.4); background: rgba(255,255,255,.06); }

.warm-membership { min-height: 620px; padding-block: 128px; color: var(--cozy-on-surface-alt); background: oklch(0.22 0.025 42); }
.membership-shell { min-height: 360px; }

.warm-membership :where(a, button, summary):focus-visible { outline-color: var(--cozy-on-surface-alt); }

.benefits-details { margin-top: 48px; border-top: 1px solid var(--cozy-border-on-alt); }
.benefits-details summary { min-height: 52px; display: inline-flex; align-items: center; color: var(--cozy-on-surface-alt); font-weight: 600; cursor: pointer; }
.benefits-details--compact { margin: 0; border: 0; }
.benefits-details--compact p { max-width: 44em; color: var(--cozy-muted-on-alt); line-height: 1.75; }

.benefits-table-wrap { overflow-x: auto; }
.benefits-details table { width: 100%; border-collapse: collapse; text-align: left; }
.benefits-details caption { padding: 16px 0; text-align: left; font-weight: 600; }
.benefits-details th,
.benefits-details td { padding: 16px 12px; border-bottom: 1px solid var(--cozy-border-on-alt); line-height: 1.55; }
.benefits-details th { color: var(--cozy-muted-on-alt); font-size: 14px; font-weight: 500; }
.benefits-details tr.is-current td,
.benefits-list dl.is-current { color: var(--cozy-accent-on-alt); }
.current-marker { margin-left: 6px; color: var(--cozy-accent-on-alt); font-size: 12px; font-weight: 700; }
.benefits-list { display: grid; gap: 16px; padding-top: 16px; }
.benefits-list dl { margin: 0; padding-block: 16px; border-bottom: 1px solid var(--cozy-border-on-alt); }
.benefits-list dl > div { display: grid; grid-template-columns: 96px 1fr; gap: 16px; padding-block: 6px; }
.benefits-list dt { color: var(--cozy-muted-on-alt); }
.benefits-list dd { margin: 0; line-height: 1.55; }
.benefits-list__title dd { font-weight: 700; }
.membership-error { padding-block: 64px; }
.membership-error h3 { margin: 0; font-size: 1.5rem; }
.membership-error > p { color: var(--cozy-muted-on-alt); margin-top: 12px; }
.membership-skeleton { min-height: 620px; display: grid; align-content: center; gap: 20px; }
.membership-skeleton span { display: block; height: 22px; max-width: 620px; border-radius: 6px; background: rgba(255,255,255,.11); animation: warm-pulse 1.4s ease-in-out infinite alternate; }
.membership-skeleton span:nth-child(1) { width: 52%; height: 54px; }
.membership-skeleton span:nth-child(2) { width: 76%; }
.membership-skeleton span:nth-child(3) { width: 64%; }
.membership-skeleton--content { min-height: 280px; }

@keyframes warm-pulse { from { opacity: .45; } to { opacity: 1; } }

@media (max-width: 960px) {
  .warm-membership { padding-block: 96px; }
}

@media (max-width: 760px) {
  .membership-activity__grid { grid-template-columns: 1fr; gap: 24px; }
}

@media (max-width: 560px) {
  .warm-membership { padding-block: 64px; }
}
</style>
