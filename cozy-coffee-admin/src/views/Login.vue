<template>
  <div class="login-wrapper">
    <!-- 背景容器 -->
    <div class="background-container">
       <!-- 暂无背景图，使用CSS渐变 -->
    </div>

    <!-- 登录容器 -->
    <div class="login-container">
      <div class="login-form-container">
        <!-- Logo -->
        <div class="logo">
          <div class="logo-icon">
            <el-icon :size="48" color="#6F4E37"><CoffeeCup /></el-icon>
          </div>
          <h1 class="logo-text">COZY ADMIN</h1>
        </div>

        <!-- 登录表单 -->
        <form class="login-form" @submit.prevent="handleLogin">
          <h2>管理员登录</h2>
          
          <!-- 用户名输入框 -->
          <div class="input-group">
            <el-icon class="input-icon"><User /></el-icon>
            <input 
              v-model="form.username" 
              type="text" 
              placeholder="请输入管理员账号" 
              required
            >
          </div>

          <!-- 密码输入框 -->
          <div class="input-group">
            <el-icon class="input-icon"><Lock /></el-icon>
            <input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码" 
              required
            >
          </div>

          <!-- 记住密码 -->
          <div class="remember-forgot">
            <label class="remember">
              <input type="checkbox">
              <span>记住密码</span>
            </label>
          </div>

          <!-- 登录按钮 -->
          <button type="submit" class="login-btn" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>

          <div class="form-footer">
             <p>CozyCoffee Enterprise System</p>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '../stores/admin'
import { CoffeeCup, User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const adminStore = useAdminStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const result = await adminStore.login(form.username, form.password)
    
    if (result.success) {
      ElMessage.success('登录成功')
      try {
        await router.push('/') 
      } catch (err) {
        window.location.href = '/'
      }
    } else {
      ElMessage.error(result.message || '用户名或密码错误')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
:root {
    --primary-color: #7C5CFC;
    --text-color: #000000;
    --border-color: #E5E5E5;
    --background-color: #F8F9FD;
    --card-background: #FFFFFF;
    --coffee-brown: #6F4E37;
    --coffee-light: #A67C52;
}

.login-wrapper {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    z-index: 100;
    background: linear-gradient(to right, #ededed, #d4d4d4);
}

.login-container {
    display: flex;
    width: 450px;
    background: #fff;
    border-radius: 20px;
    overflow: hidden;
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
    padding: 40px 0;
}

.login-form-container {
    flex: 1;
    padding: 0 40px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.logo {
    margin-bottom: 30px;
    text-align: center;
}

.logo-icon {
    margin-bottom: 10px;
    display: flex;
    justify-content: center;
}

.logo-text {
    font-size: 24px;
    font-weight: 700;
    color: #1a1337;
    margin: 0;
    letter-spacing: 2px;
}

.login-form {
    width: 100%;
}

.login-form h2 {
    text-align: center;
    font-size: 20px;
    color: #666;
    margin-bottom: 30px;
    font-weight: 400;
}

.input-group {
    position: relative;
    margin-bottom: 20px;
}

.input-icon {
    position: absolute;
    right: 15px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 20px;
    color: #999;
}

.input-group input {
    width: 100%;
    padding: 12px 45px 12px 15px;
    border: 1px solid #E5E5E5;
    border-radius: 8px;
    font-size: 16px;
    transition: all 0.3s ease;
    box-sizing: border-box; /* Important for padding */
}

.input-group input:focus {
    border-color: #000;
    outline: none;
}

.remember-forgot {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
}

.remember {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #666;
    font-size: 14px;
    cursor: pointer;
}

.login-btn {
    width: 100%;
    padding: 12px;
    background: #000000;
    color: white;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.3s ease;
}

.login-btn:hover {
    background: #333;
}

.login-btn:disabled {
    background: #ccc;
    cursor: not-allowed;
}

.form-footer {
    text-align: center;
    margin-top: 30px;
}

.form-footer p {
    color: #999;
    font-size: 12px;
}
</style>
