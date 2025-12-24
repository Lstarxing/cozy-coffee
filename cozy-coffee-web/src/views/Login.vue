<template>
  <div class="login-wrapper">
    <!-- 背景容器 -->
    <div class="login-container">
      <!-- 只有右侧登录区域，将居中显示 -->
      <div class="login-form-container">
        <!-- Logo -->
        <div class="logo">
          <img src="/images/cozycafe_logo.png" alt="Logo">
        </div>

        <!-- 登录表单 -->
        <form @submit.prevent="handleLogin" class="login-form">
          <h2>登录</h2>
          
          <!-- 用户名输入框 -->
          <div class="input-group">
            <img src="/images/icons/user.png" alt="用户" class="input-icon">
            <input 
              v-model="loginForm.username" 
              type="text" 
              placeholder="请输入手机号码或邮箱账号" 
              required
            >
          </div>

          <!-- 密码输入框 -->
          <div class="input-group">
            <img src="/images/icons/password.png" alt="密码" class="input-icon">
            <input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码" 
              required
            >
          </div>

          <!-- 记住密码和忘记密码 -->
          <div class="remember-forgot">
            <label class="remember">
              <input type="checkbox" v-model="loginForm.remember">
              <span>记住密码</span>
            </label>
            <div class="forgot-links">
              <a href="#" class="forgot-password">忘记密码？</a>
              <!-- 如果需要可以加隐私协议链接 -->
            </div>
          </div>

          <!-- 登录按钮 -->
          <button type="submit" class="login-btn" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>

          <!-- 注册链接 -->
          <div class="register-link">
            还没有账号？<router-link to="/register">立即注册</router-link>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    // 1. 调用后端登录API
    const loginResponse = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: loginForm.username,
        password: loginForm.password
      })
    })
    
    const loginData = await loginResponse.json()
    
    if (!loginData.success) {
      ElMessage.error(loginData.message || '登录失败')
      return
    }
    
    const token = loginData.data?.token || loginData.token
    
    // 2. 获取用户个人信息
    const userResponse = await fetch('http://localhost:8080/api/auth/userinfo', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    const userData = await userResponse.json()
    
    // 3. 获取会员信息
    const memberResponse = await fetch('http://localhost:8080/api/member/info', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    const memberData = await memberResponse.json()
    
    // 4. 合并用户信息和会员信息，保存到store
    userStore.login({
      // 用户个人信息
      id: userData.data?.id,
      username: loginForm.username,
      nickname: userData.data?.nickname || loginForm.username,
      avatar: userData.data?.avatar,
      memberCode: userData.data?.memberCode,
      phone: userData.data?.phone,
      email: userData.data?.email,
      // 会员信息
      memberLevel: memberData.data?.memberLevel || 'basic',
      totalPoints: memberData.data?.totalPoints || 0,
      currentPoints: memberData.data?.currentPoints || 0,
      consecutiveSignDays: memberData.data?.consecutiveSignDays || 0,
      lastSigninDate: memberData.data?.lastSigninDate || null
    }, token)
    
    ElMessage.success('登录成功')
    router.push('/member')
  } catch (error) {
    ElMessage.error('登录失败: ' + error.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import '@/assets/styles/login_style.css';

/* 登录页特有的背景修正 */
.login-wrapper {
  z-index: 100; /* 低于导航栏的 1000，确保导航栏可见 */
  pointer-events: none;
}

.login-container {
  pointer-events: auto;
}
</style>
