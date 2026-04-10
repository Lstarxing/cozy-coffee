<template>
  <nav class="navbar">
    <div class="nav-container">
      <div class="nav-logo">
        <router-link to="/">
          <img src="/images/cozycafe_logo.png" alt="Logo">
        </router-link>
      </div>
      <div class="nav-links">
        <router-link to="/" class="active">首页</router-link>
        <a href="/#menu">菜单</a>
        <a href="/#membership">加入我们</a>
        <router-link to="/about">关于我们</router-link>
      </div>
    </div>
    
    <div class="nav-buttons">
      <!-- 未登录状态 -->
      <template v-if="!userStore.isLoggedIn">
        <router-link to="/login" class="nav-btn login"><strong>登录</strong></router-link>
        <router-link to="/register" class="nav-btn register"><strong>注册</strong></router-link>
      </template>
      
      <!-- 已登录状态 -->
      <template v-else>
        <router-link to="/member" class="nav-btn member">
          <span class="user-greeting">{{ userStore.userInfo?.nickname || '会员中心' }}</span>
        </router-link>
        <button @click="handleLogout" class="nav-btn logout">退出</button>
      </template>
    </div>
  </nav>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const handleLogout = async () => {
  await userStore.logout()
  router.push('/')
}
</script>

<style scoped>
.user-greeting {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-btn.member {
  background: linear-gradient(135deg, #8B4513, #A0522D);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  text-decoration: none;
  font-size: 14px;
}

.nav-btn.logout {
  background: transparent;
  border: 1px solid #8B4513;
  color: #8B4513;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  margin-left: 10px;
}

.nav-btn.logout:hover {
  background: #8B4513;
  color: white;
}
</style>
