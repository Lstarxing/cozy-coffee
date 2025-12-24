<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <h1>COZY</h1>
        <span>管理后台</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1a1a2e"
        text-color="#a0a0a0"
        active-text-color="#fff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>控制台</span>
        </el-menu-item>
        
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        
        <el-menu-item index="/orders">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        
        <el-menu-item index="/redemptions">
          <el-icon><Present /></el-icon>
          <span>兑换管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="admin-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="admin-name">{{ adminStore.adminInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '../stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

adminStore.init()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '控制台')

const handleCommand = (command) => {
  if (command === 'logout') {
    adminStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.sidebar {
  background: #1a1a2e;
  color: #fff;
  overflow-y: auto;
}

.logo {
  padding: 24px 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h1 {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 3px;
  color: #fff;
  margin: 0;
}

.logo span {
  font-size: 12px;
  color: #888;
}

.el-menu {
  border-right: none;
}

.el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 4px 8px;
  border-radius: 8px;
}

.el-menu-item.is-active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.admin-name {
  font-size: 14px;
  color: #666;
}

.main-content {
  background: #f5f7fa;
  padding: 24px;
}
</style>
