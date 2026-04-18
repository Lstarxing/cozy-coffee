<template>
  <el-container class="admin-layout">
    <!-- Sidebar -->
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <el-icon :size="24" color="#8B5E3C"><CoffeeCup /></el-icon>
        <h1>COZY ADMIN</h1>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
        text-color="#9CA3AF"
        active-text-color="#FFFFFF"
        background-color="#111827"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>控制台</span>
        </el-menu-item>
        
        <el-menu-item index="/orders">
          <el-icon><List /></el-icon>
          <span>咖啡订单</span>
        </el-menu-item>
        
        <el-menu-item index="/redemptions">
          <el-icon><Present /></el-icon>
          <span>兑换管理</span>
        </el-menu-item>

        <el-sub-menu index="/products">
          <template #title>
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </template>
          <el-menu-item index="/products/coffee">咖啡菜单</el-menu-item>
          <el-menu-item index="/products/points">积分商品</el-menu-item>
        </el-sub-menu>
        
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>

      <!-- Sidebar Footer (User Profile & Logout) -->
      <div class="sidebar-footer">
        <div class="user-info">
          <el-avatar :size="36" class="user-avatar">{{ adminInitial }}</el-avatar>
          <div class="user-text">
            <div class="username">{{ adminStore.adminInfo?.username || 'Admin' }}</div>
            <div class="role-badge">管理员</div>
          </div>
        </div>
        <el-tooltip content="退出登录" placement="top">
          <el-button link class="logout-btn" @click="handleLogout">
            <el-icon :size="18"><SwitchButton /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </el-aside>

    <!-- Main Content -->
    <el-container class="is-vertical main-container">
      <el-main class="page-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '../stores/admin'
import { 
  Odometer, List, Present, Goods, User, 
  CoffeeCup, SwitchButton 
} from '@element-plus/icons-vue' // Removed Bell, CaretBottom

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

adminStore.init()

const activeMenu = computed(() => route.path)
const adminInitial = computed(() => (adminStore.adminInfo?.username?.[0] || 'A').toUpperCase())

const handleLogout = async () => {
  await adminStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100vh; /* Fixed height for the whole layout */
  width: 100vw;
  background-color: #F9FAFB;
  overflow: hidden; /* Prevent body scroll */
}

.sidebar {
  background-color: #111827;
  border-right: 1px solid #1F2937;
  display: flex;
  flex-direction: column;
  height: 100%; /* Full height */
  flex-shrink: 0;
  
  .logo {
    height: 64px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 24px;
    background-color: #111827;
    border-bottom: 1px solid #1F2937;
    
    h1 {
      color: #F3F4F6;
      font-size: 16px;
      font-weight: 700;
      letter-spacing: 0.05em;
      margin: 0;
    }
  }
  
  .sidebar-menu {
    border-right: none;
    padding-top: 16px;
    flex: 1; /* Takes available space */
    overflow-y: auto; /* Internal scroll for menu */
    
    :deep(.el-menu-item), :deep(.el-sub-menu__title) {
      margin: 4px 12px;
      border-radius: 6px;
      height: 44px;
      line-height: 44px;
      
      &.is-active {
        background-color: #374151 !important;
        font-weight: 600;
      }
      
      &:hover {
        background-color: #1F2937 !important;
      }
    }
    
    // Scrollbar styling for webkit
    &::-webkit-scrollbar {
      width: 4px;
    }
    &::-webkit-scrollbar-thumb {
      background: #374151;
      border-radius: 2px;
    }
  }

  .sidebar-footer {
    flex-shrink: 0;
    height: 72px;
    border-top: 1px solid #1F2937;
    padding: 0 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background-color: #0F1522;

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .user-avatar {
      background: #374151;
      color: #D1D5DB;
      font-weight: 600;
      font-size: 14px;
      border: 1px solid #4B5563;
    }

    .user-text {
      display: flex;
      flex-direction: column;
      
      .username {
        color: #F3F4F6;
        font-size: 14px;
        font-weight: 500;
        max-width: 90px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      
      .role-badge {
        color: #9CA3AF;
        font-size: 11px;
        line-height: 1.2;
      }
    }

    .logout-btn {
      color: #9CA3AF;
      padding: 8px;
      border-radius: 6px;
      transition: all 0.2s;
      
      &:hover {
        background-color: #1F2937;
        color: #F87171; // Red for logout hover
      }
    }
  }
}

.main-container {
  height: 100%;
  overflow: hidden;
  background-color: #F9FAFB;
}

.page-content {
  padding: 24px 32px;
  overflow-y: auto; /* Content scrolls here */
  height: 100vh; /* Full height since header is gone */
}
</style>
