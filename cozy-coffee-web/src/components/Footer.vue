<template>
  <footer class="footer">
    <div class="footer-container">
      <!-- 左侧联系信息 -->
      <div class="footer-left">
        <div class="contact-info">
          <div class="contact-item">
            <img src="/images/email.png" alt="email">
            <p>联系我们：3187979459@qq.com</p>
          </div>
          <div class="contact-item">
            <img src="/images/phone.png" alt="phone">
            <p>服务热线：400-888-8888</p>
          </div>
        </div>
      </div>

      <!-- 中间社交媒体 -->
      <div class="footer-middle">
        <div class="social-links">
          <div class="social-item">
            <img src="/images/icons/xhs.png" alt="小红书">
            <a href="https://www.xiaohongshu.com/explore" target="_blank" class="social-link">小红书</a>
          </div>
          <div class="social-item">
            <img src="/images/icons/wechat.png" alt="微信">
            <a href="https://wx.qq.com/" target="_blank" class="social-link">公众号</a>
          </div>
        </div>
      </div>

      <!-- 右侧版权信息 -->
      <div class="footer-right">
        <p class="copyright">Copyright © {{ currentYear }} 咖熙咖啡版权所有</p>
        <p class="rights">All rights reserved</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'
const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
/* 样式依赖全局style.css */
</style>
