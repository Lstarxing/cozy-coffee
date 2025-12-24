// 验证码按钮状态管理
let countdown = 60;
let timer = null;

// 发送验证码
async function sendVerificationCode(phone) {
    const sendButton = document.querySelector('.send-code-btn');
    
    try {
        // 检查手机号格式
        if (!/^1[3-9]\d{9}$/.test(phone)) {
            throw new Error('请输入正确的手机号码');
        }

        // 禁用按钮并开始倒计时
        sendButton.disabled = true;
        startCountdown(sendButton);

        // 发送请求到后端
        const response = await fetch('/api/send-code', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ phone })
        });

        const data = await response.json();
        
        if (!data.success) {
            throw new Error(data.message || '发送失败，请稍后重试');
        }

        showToast('验证码已发送，请注意查收');

    } catch (error) {
        showToast(error.message);
        // 发送失败时重置按钮状态
        resetButton(sendButton);
    }
}

// 验证验证码
async function verifyCode(phone, code) {
    try {
        // 检查验证码格式
        if (!/^\d{6}$/.test(code)) {
            throw new Error('请输入6位数字验证码');
        }

        // 发送验证请求到后端
        const response = await fetch('/api/verify-code', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ phone, code })
        });

        const data = await response.json();

        if (!data.success) {
            throw new Error(data.message || '验证失败，请重试');
        }

        return true;

    } catch (error) {
        showToast(error.message);
        return false;
    }
}

// 倒计时功能
function startCountdown(button) {
    countdown = 60;
    button.textContent = `${countdown}秒后重试`;
    
    timer = setInterval(() => {
        countdown--;
        if (countdown <= 0) {
            resetButton(button);
        } else {
            button.textContent = `${countdown}秒后重试`;
        }
    }, 1000);
}

// 重置按钮状态
function resetButton(button) {
    clearInterval(timer);
    button.disabled = false;
    button.textContent = '发送验证码';
}

// 提示消息
function showToast(message) {
    // 实现一个简单的提示框
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.remove();
    }, 3000);
}

document.addEventListener('DOMContentLoaded', function() {
    const canvas = document.getElementById('verifyCanvas');
    const ctx = canvas.getContext('2d');
    
    // 设置画布大小
    canvas.width = 100;
    canvas.height = 46;
    
    // 生成随机验证码
    function generateCode() {
        const chars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
        let code = '';
        for (let i = 0; i < 4; i++) {
            code += chars[Math.floor(Math.random() * chars.length)];
        }
        return code;
    }
    
    // 绘制验证码
    function drawCode() {
        // 清空画布
        ctx.fillStyle = '#f0f0f0';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        
        // 生成新验证码
        const code = generateCode();
        
        // 绘制文字
        ctx.font = 'bold 24px Arial';
        ctx.textBaseline = 'middle';
        
        // 随机颜色和位置绘制每个字符
        for (let i = 0; i < code.length; i++) {
            ctx.fillStyle = `rgb(${Math.random() * 100}, ${Math.random() * 100}, ${Math.random() * 100})`;
            ctx.save();
            
            // 随机旋转角度
            const x = 20 + i * 20;
            const y = canvas.height / 2;
            ctx.translate(x, y);
            ctx.rotate((Math.random() - 0.5) * 0.4);
            
            ctx.fillText(code[i], 0, 0);
            ctx.restore();
        }
        
        // 添加干扰线
        for (let i = 0; i < 3; i++) {
            ctx.beginPath();
            ctx.strokeStyle = `rgba(${Math.random() * 255}, ${Math.random() * 255}, ${Math.random() * 255}, 0.5)`;
            ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height);
            ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height);
            ctx.stroke();
        }
        
        // 添加干扰点
        for (let i = 0; i < 50; i++) {
            ctx.fillStyle = `rgba(${Math.random() * 255}, ${Math.random() * 255}, ${Math.random() * 255}, 0.5)`;
            ctx.fillRect(Math.random() * canvas.width, Math.random() * canvas.height, 2, 2);
        }
        
        // 保存验证码值用于验证
        canvas.dataset.code = code;
    }
    
    // 初始化验证码
    drawCode();
    
    // 点击刷新验证码
    canvas.addEventListener('click', drawCode);
    
    // 验证输入
    const loginForm = document.getElementById('loginForm');
    const verifyInput = document.getElementById('verifyCode');
    
    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const inputCode = verifyInput.value.trim();
        const correctCode = canvas.dataset.code;
        
        if (inputCode.toLowerCase() === correctCode.toLowerCase()) {
            // 验证码正确，继续提交
            console.log('验证码正确');
            // 这里添加登录逻辑
        } else {
            // 验证码错误
            alert('验证码错误，请重新输入');
            verifyInput.value = '';
            drawCode();
        }
    });

    // 添加密码显示/隐藏功能
    const togglePassword = document.getElementById('togglePassword');
    const passwordInput = document.getElementById('password');

    togglePassword.addEventListener('click', function() {
        // 切换密码显示状态
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        passwordInput.setAttribute('type', type);
        
        // 切换图标
        this.src = type === 'password' ? 
            'images/icons/eye-close.png' : 
            'images/icons/eye-open.png';
    });
}); 