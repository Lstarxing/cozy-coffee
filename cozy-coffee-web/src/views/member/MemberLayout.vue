<template>
  <div class="member-layout" :class="themeClass">
    <!-- Left Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="brand-logo">COZY</h1>
      </div>

      <div class="user-profile">
        <div class="avatar-ring" @click="showAvatarModal = true">
          <img :src="userStore.userInfo?.avatar || '/images/default-avatar.png'" alt="Avatar" class="user-avatar">
          <div class="avatar-overlay">
            <span>更换</span>
          </div>
        </div>
        <div class="user-meta">
          <h2 class="user-name">{{ userStore.userInfo?.nickname || '访客' }}</h2>
          <span class="member-id">ID: {{ userStore.userInfo?.memberCode || '---' }}</span>
        </div>
      </div>

      <nav class="navigation">
        <router-link to="/member/center" class="nav-link" :class="{ active: isActiveTab('center') }">
          <span class="indicator"></span>
          <LayoutDashboard :size="20" class="nav-icon" />
          <span class="nav-text">会员中心</span>
        </router-link>

        <router-link to="/member/order" class="nav-link" :class="{ active: isActiveTab('order') }">
          <span class="indicator"></span>
          <Coffee :size="20" class="nav-icon" />
          <span class="nav-text">咖啡下单</span>
        </router-link>

        <router-link to="/member/mall" class="nav-link" :class="{ active: isActiveTab('mall') }">
          <span class="indicator"></span>
          <ShoppingBag :size="20" class="nav-icon" />
          <span class="nav-text">积分商城</span>
        </router-link>

        <!-- 可展开的历史订单菜单 -->
        <div class="nav-group">
          <a
href="#" class="nav-link nav-parent"
            :class="{ active: isOrdersExpanded || isActiveTab('orders/coffee') || isActiveTab('orders/redeem') }"
            @click.prevent="isOrdersExpanded = !isOrdersExpanded">
            <span class="indicator"></span>
            <ClipboardList :size="20" class="nav-icon" />
            <span class="nav-text">历史订单</span>
            <ChevronRight :size="16" class="expand-icon" :class="{ expanded: isOrdersExpanded }" />
          </a>
          <div class="nav-submenu" :class="{ expanded: isOrdersExpanded }">
            <router-link to="/member/orders/coffee" class="nav-link sub-link" :class="{ active: isActiveTab('orders/coffee') }">
              <span class="left-dot"></span>
              <span class="nav-text">咖啡订单</span>
            </router-link>
            <router-link to="/member/orders/redeem" class="nav-link sub-link" :class="{ active: isActiveTab('orders/redeem') }">
              <span class="left-dot"></span>
              <span class="nav-text">兑换订单</span>
            </router-link>
          </div>
        </div>

        <router-link to="/member/profile" class="nav-link" :class="{ active: isActiveTab('profile') }">
          <span class="indicator"></span>
          <User :size="20" class="nav-icon" />
          <span class="nav-text">个人信息</span>
        </router-link>

        <router-link to="/member/coupons" class="nav-link" :class="{ active: isActiveTab('coupons') }">
          <span class="indicator"></span>
          <Ticket :size="20" class="nav-icon" />
          <span class="nav-text">我的券包</span>
        </router-link>

        <router-link to="/member/benefits" class="nav-link" :class="{ active: isActiveTab('benefits') }">
          <span class="indicator"></span>
          <Crown :size="20" class="nav-icon" />
          <span class="nav-text">会员权益</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button class="footer-link" @click="router.push('/')">
          <Home :size="18" /> 返回首页
        </button>
        <button class="footer-link logout" @click="handleLogout">
          <LogOut :size="18" /> 退出登录
        </button>
      </div>
    </aside>

    <!-- Main Content Area -->
    <main class="main-content">
      <router-view />
    </main>

    <!-- 头像更换模态框 -->
    <div v-if="showAvatarModal" class="avatar-modal" @click.self="showAvatarModal = false">
      <div class="modal-content">
        <h3>更换头像</h3>
        <div class="avatar-preview-area">
          <img :src="avatarPreview || userStore.userInfo?.avatar || '/images/default-avatar.png'" class="preview-img" />
        </div>
        <input ref="avatarInput" type="file" accept="image/*" style="display: none" @change="handleAvatarChange" />
        <div class="modal-actions">
          <button class="select-btn" @click="$refs.avatarInput?.click()">选择图片</button>
          <button class="confirm-btn" :disabled="!avatarPreview" @click="saveAvatar">保存头像</button>
          <button class="cancel-btn" @click="showAvatarModal = false; avatarPreview = ''">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'
import { updateProfile } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { LayoutDashboard, Coffee, ClipboardList, ChevronRight, ShoppingBag, User, Ticket, Crown, Home, LogOut } from 'lucide-vue-next'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const isOrdersExpanded = ref(false)

const showAvatarModal = ref(false)
const avatarPreview = ref('')
const avatarInput = ref(null)

const themeClass = computed(() => {
  return `theme-${userStore.userLevel || 'basic'}`
})

function isActiveTab(segment) {
  return route.path.startsWith(`/member/${segment}`)
}

function handleAvatarChange(event) {
  const file = event.target.files[0]
  if (file) {
    if (file.size > 2 * 1024 * 1024) {
      window.alert('图片大小不能超过2MB')
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      avatarPreview.value = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

async function saveAvatar() {
  if (!avatarPreview.value) {
    return
  }
  try {
    await updateProfile({ avatar: avatarPreview.value })
    userStore.userInfo.avatar = avatarPreview.value
    showAvatarModal.value = false
    avatarPreview.value = ''
    ElMessage.success('头像更新成功')
  } catch (error) {
    ElMessage.error('头像保存失败: ' + (error.message || '请稍后重试'))
  }
}

async function handleLogout() {
  const ok = await userStore.logout()
  if (ok) {
    router.push('/')
    return
  }
  ElMessage.error('退出失败，请检查后端服务或网络后重试')
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  userStore.fetchMemberInfo()
})
</script>

<style scoped>
.theme-basic {
  --primary-color: #C69C6D;
  --primary-light: #FDF8F3;
  --primary-dark: #8D6E63;
  --accent-color: #E6B07A;
  --card-bg-gradient: linear-gradient(135deg, #1f1f1f, #2c2c2c);
  --highlight-text: #C69C6D;
  --button-hover: #b08d55;
}

.theme-silver {
  --primary-color: #90A4AE;
  --primary-light: #ECEFF1;
  --primary-dark: #546E7A;
  --accent-color: #CFD8DC;
  --card-bg-gradient: linear-gradient(135deg, #78909C, #B0BEC5);
  --highlight-text: #546E7A;
  --button-hover: #78909C;
}

.theme-gold {
  --primary-color: #D4AF37;
  --primary-light: #FFF8E1;
  --primary-dark: #A68B29;
  --accent-color: #FFD700;
  --card-bg-gradient: linear-gradient(135deg, #D4AF37, #FDD835);
  --highlight-text: #D4AF37;
  --button-hover: #c49f2b;
}

.theme-diamond {
  --primary-color: #64B5F6;
  --primary-light: #E3F2FD;
  --primary-dark: #1976D2;
  --accent-color: #64B5F6;
  --card-bg-gradient: linear-gradient(135deg, #42A5F5, #90CAF9);
  --highlight-text: #1976D2;
  --button-hover: #1E88E5;
}

.theme-black {
  --primary-color: #212121;
  --primary-light: #FAFAFA;
  --primary-dark: #000000;
  --accent-color: #C69C6D;
  --card-bg-gradient: linear-gradient(135deg, #000000, #2c2c2c);
  --highlight-text: #C69C6D;
  --button-hover: #333333;
}

.member-layout {
  display: flex;
  min-height: 100vh;
  background-color: #F8F5F2;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  color: #333;
  user-select: none;
}

.modern-input,
.invite-input,
input,
textarea {
  user-select: text !important;
}

.sidebar {
  width: 280px;
  background: #FFFFFF;
  display: flex;
  flex-direction: column;
  padding: 40px 20px 14px 20px;
  box-shadow: 1px 0 20px rgba(0, 0, 0, 0.03);
  z-index: 100;
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.sidebar::-webkit-scrollbar {
  display: none;
}

.brand-logo {
  font-size: 24px;
  letter-spacing: 4px;
  color: #333;
  margin: 0 0 30px 0;
  font-weight: 800;
}

.user-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 35px;
}

.avatar-ring {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  padding: 3px;
  border: 1px solid #E0E0E0;
  margin-bottom: 20px;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.user-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.user-name {
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #2C2C2C;
}

.member-id {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
  letter-spacing: 1px;
}

.navigation {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.nav-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: #888;
  padding: 10px 0;
  transition: all 0.3s ease;
  position: relative;
  gap: 10px;
}

.nav-link:hover,
.nav-link.active {
  color: #C69C6D;
}

.indicator {
  width: 0;
  height: 2px;
  background: #C69C6D;
  margin-right: 0;
  transition: all 0.3s ease;
}

.nav-link.active .indicator {
  width: 20px;
  margin-right: 8px;
}

.nav-text {
  font-size: 15px;
  font-weight: 500;
}

.nav-icon {
  color: #8D6E63;
  transition: color 0.3s ease;
  flex-shrink: 0;
}

.nav-link.active .nav-icon {
  color: #5D4037;
}

.sidebar-footer {
  align-items: flex-start;
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-top: auto;
  border-top: 1px solid #F0F0F0;
  padding-top: 30px;
}

.footer-link {
  display: flex !important;
  align-items: center;
  gap: 8px;
  justify-content: center;
  background: none;
  border: none;
  text-align: left;
  color: #999;
  cursor: pointer;
  font-size: 13px;
  transition: color 0.2s;
  padding: 0;
}

.footer-link:hover {
  color: #333;
}

.main-content {
  flex: 1;
  padding: 30px 60px;
  overflow-y: auto;
  margin-left: 280px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.main-content::-webkit-scrollbar {
  display: none;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 50%;
}

.avatar-overlay span {
  color: white;
  font-size: 12px;
}

.avatar-ring:hover .avatar-overlay {
  opacity: 1;
}

.nav-group {
  width: 100%;
}

.nav-parent {
  display: flex;
  align-items: center;
  position: relative;
}

.expand-icon {
  margin-left: auto;
  font-size: 16px;
  transition: transform 0.2s;
  color: #888;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.nav-submenu {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
  padding-left: 16px;
}

.nav-submenu.expanded {
  max-height: 120px;
}

.nav-link.sub-link {
  padding: 10px 16px 10px 24px;
  font-size: 14px;
}

.nav-link.sub-link .nav-text {
  font-size: 14px;
}

.left-dot {
  width: 4px;
  height: 4px;
  background: #888;
  border-radius: 50%;
  margin-right: 12px;
  transition: background 0.3s;
}

.nav-link.sub-link.active .left-dot {
  background: var(--primary-color, #C69C6D);
  transform: scale(1.2);
}

.avatar-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 20px;
  padding: 32px;
  width: 90%;
  max-width: 480px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin: 0 0 24px;
  text-align: center;
  font-size: 20px;
}

.avatar-preview-area {
  text-align: center;
  padding: 30px 0;
}

.preview-img {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #eee;
}

.select-btn {
  background: #666;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
  margin-right: 10px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.modal-actions .cancel-btn {
  padding: 12px 32px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 30px;
  cursor: pointer;
}

.modal-actions .confirm-btn {
  padding: 12px 32px;
  background: #B8956B;
  color: white;
  border: none;
  border-radius: 30px;
  cursor: pointer;
}

.modal-actions .confirm-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
