<template>
  <footer class="site-footer">
    <div class="site-footer__inner">
      <div class="site-footer__contact" aria-label="联系方式">
        <a href="mailto:3187979459@qq.com" class="site-footer__item">
          <img src="/images/email.png" alt="" width="24" height="24">
          <span>3187979459@qq.com</span>
        </a>
        <a href="tel:4008888888" class="site-footer__item">
          <img src="/images/phone.png" alt="" width="24" height="24">
          <span>400-888-8888</span>
        </a>
      </div>

      <div class="site-footer__social" aria-label="社交媒体">
        <a href="https://www.xiaohongshu.com/explore" target="_blank" rel="noopener noreferrer" class="site-footer__item">
          <img src="/images/icons/xhs.png" alt="" width="28" height="28">
          <span>小红书</span>
        </a>
        <a href="https://wx.qq.com/" target="_blank" rel="noopener noreferrer" class="site-footer__item">
          <img src="/images/icons/wechat.png" alt="" width="28" height="28">
          <span>公众号</span>
        </a>
      </div>

      <div class="site-footer__legal">
        <p>Copyright © {{ currentYear }} 咖熙咖啡版权所有</p>
        <p>All rights reserved</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.site-footer {
  color: var(--cozy-on-surface-alt);
  background: var(--cozy-surface-alt);
  border-top: 1px solid var(--cozy-border-on-alt);
  font-family: var(--font-sans);
}

.site-footer__inner {
  width: min(1180px, calc(100% - 48px));
  min-height: 176px;
  margin-inline: auto;
  padding-block: 40px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 48px;
}

.site-footer__contact { display: flex; flex-direction: column; align-items: flex-start; gap: 12px; }
.site-footer__social { display: flex; align-items: center; justify-content: center; gap: 28px; }
.site-footer__item { min-height: 44px; display: inline-flex; align-items: center; gap: 10px; color: var(--cozy-on-surface-alt); text-decoration: none; font-size: 15px; }
.site-footer__item img { flex: 0 0 auto; object-fit: contain; }
.site-footer__item:hover { text-decoration: underline; text-underline-offset: 5px; }
.site-footer__legal { text-align: right; }
.site-footer__legal p { margin: 0; line-height: 1.65; }
.site-footer__legal p + p { color: var(--cozy-muted-on-alt); font-size: 14px; }
.site-footer :where(a):focus-visible { outline: 3px solid var(--cozy-on-surface-alt); outline-offset: 4px; }

@media (max-width: 820px) {
  .site-footer__inner { grid-template-columns: 1fr 1fr; gap: 28px 40px; }
  .site-footer__social { justify-content: flex-end; }
  .site-footer__legal { grid-column: 1 / -1; padding-top: 24px; border-top: 1px solid var(--cozy-border-on-alt); text-align: left; }
}

@media (max-width: 560px) {
  .site-footer__inner { width: min(100% - 32px, 520px); grid-template-columns: 1fr; padding-block: 48px; }
  .site-footer__social { justify-content: flex-start; }
  .site-footer__legal { grid-column: auto; }
}
</style>
