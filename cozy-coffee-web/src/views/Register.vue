<template>
  <div class="login-wrapper">
    <div class="login-container">
      <!-- 只有右侧注册区域，将居中显示 -->
      <div class="login-form-container">
        <form @submit.prevent="handleRegister" class="login-form">
          <h2>注册</h2>

          <!-- 用户名/手机号/邮箱 -->
          <div class="input-group">
            <img src="/images/icons/user.png" alt="用户" class="input-icon">
            <input 
              v-model="form.username" 
              type="text" 
              placeholder="请输入手机号码或邮箱账号" 
              required
            >
          </div>

          <!-- 密码 -->
          <div class="input-group">
            <img src="/images/icons/password.png" alt="密码" class="input-icon">
            <input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码 (至少6位)" 
              required
              minlength="6"
            >
          </div>

          <!-- 确认密码 -->
          <div class="input-group">
            <img src="/images/icons/password.png" alt="密码" class="input-icon">
            <input 
              v-model="form.confirmPassword" 
              type="password" 
              placeholder="请再次确认密码" 
              required
            >
          </div>

          <!-- 邀请码（可选） -->
          <div class="input-group invite-code-group">
            <img src="/images/icons/user.png" alt="邀请码" class="input-icon">
            <input 
              v-model="form.inviterCode" 
              type="text" 
              placeholder="邀请码（选填，可获得额外积分）"
              maxlength="8"
              @input="form.inviterCode = form.inviterCode.toUpperCase()"
            >
          </div>
          <p class="invite-hint" v-if="form.inviterCode">
            填写好友邀请码，双方各得积分奖励 🎁
          </p>

          <!-- 协议 -->
          <div class="agreement-checkbox">
            <input type="checkbox" id="agreement" required>
            <span>
              我已阅读并同意
              <a href="#" class="terms-link">《用户协议》</a>
            </span>
          </div>

          <!-- 注册按钮 -->
          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '注册中...' : '注册' }}
          </button>

          <!-- 底部链接 -->
          <div class="form-footer">
            <p>已有账号？<router-link to="/login">立即登录</router-link></p>
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

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  inviterCode: ''  // 可选邀请码
})

const handleRegister = async () => {
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }

  if (form.password.length < 6) {
    ElMessage.warning('密码长度至少6位')
    return
  }

  loading.value = true
  try {
    // 调用后端注册API
    const response = await fetch('http://localhost:8080/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: form.username,
        password: form.password,
        inviterCode: form.inviterCode || undefined  // 可选邀请码
      })
    })
    
    const data = await response.json()
    
    if (data.success) {
      const successMsg = form.inviterCode 
        ? '注册成功！邀请码奖励已发放，请登录' 
        : '注册成功，请登录'
      ElMessage.success(successMsg)
      router.push('/login')
    } else {
      ElMessage.error(data.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error('注册失败: ' + error.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import '@/assets/styles/login_style.css';

.login-wrapper {
  z-index: 100;
  pointer-events: none;
}

.login-container {
  pointer-events: auto;
}

/* 邀请码输入框样式 */
.invite-code-group {
  position: relative;
}

.invite-code-group input {
  text-transform: uppercase;
  letter-spacing: 2px;
  font-family: 'Courier New', monospace;
}

.optional-tag {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.invite-hint {
  text-align: center;
  color: #667eea;
  font-size: 12px;
  margin: -8px 0 12px 0;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
</style>
