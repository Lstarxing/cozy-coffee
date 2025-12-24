document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    const tabBtns = document.querySelectorAll('.tab-btn');
    const togglePassword = document.getElementById('togglePassword');
    const passwordInput = document.getElementById('password');
    const verifyInput = document.getElementById('verifyCode');
    const rememberCheckbox = document.getElementById('remember');
    let isAdmin = false;

    // 标签切换
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            tabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            isAdmin = this.dataset.tab === 'admin';
        });
    });

    // 密码显示切换
    togglePassword.addEventListener('click', function() {
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        passwordInput.setAttribute('type', type);
        this.src = type === 'password' ? 
            'images/icons/eye-close.png' : 
            'images/icons/eye-open.png';
    });

    // 验证账号格式
    function validateAccount(account) {
        // 验证手机号
        const phoneRegex = /^1[3-9]\d{9}$/;
        // 验证邮箱
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        
        return phoneRegex.test(account) || emailRegex.test(account);
    }

    // 显示错误信息
    function showError(message) {
        alert(message); // 可以改为更友好的提示方式
    }

    // 处理登录
    async function handleLogin(e) {
        e.preventDefault();
        const username = document.getElementById('username').value.trim();
        const password = passwordInput.value;
        const verifyCode = verifyInput.value.trim();
        const remember = rememberCheckbox.checked;
        
        try {
            // 验证输入
            if (!username || !password || !verifyCode) {
                throw new Error('请填写完整信息');
            }
            
            if (!validateAccount(username)) {
                throw new Error('请输入正确的手机号或邮箱');
            }

            // 验证图形验证码
            const correctCode = document.getElementById('verifyCanvas').dataset.code;
            if (verifyCode.toLowerCase() !== correctCode.toLowerCase()) {
                throw new Error('验证码错误');
                drawCode();
                verifyInput.value = '';
                return;
            }
            
            // 发送登录请求
            const response = await fetch('/CozyCoffee/users', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    action: 'login',
                    loginName: username,
                    password: password
                })
            });
            
            const data = await response.json();
            if (data.status === 'success') {
                // 如果选择记住密码，保存登录信息
                if (remember) {
                    localStorage.setItem('rememberedUser', JSON.stringify({
                        username: username,
                        password: password
                    }));
                } else {
                    localStorage.removeItem('rememberedUser');
                }
                
                // 保存用户信息到localStorage
                localStorage.setItem('currentUser', JSON.stringify(data.data));
                
                // 跳转到会员中心
                window.location.href = 'member.html';
            } else {
                throw new Error(data.message || '登录失败');
            }
        } catch (error) {
            showError(error.message);
            console.error('登录错误:', error);
        }
    }

    // 检查记住的登录信息
    function checkRememberedLogin() {
        const rememberedUser = localStorage.getItem('rememberedUser');
        if (rememberedUser) {
            const userData = JSON.parse(rememberedUser);
            document.getElementById('username').value = userData.username;
            passwordInput.value = userData.password;
            rememberCheckbox.checked = true;
        }
    }

    // 绑定表单提交事件
    loginForm.addEventListener('submit', handleLogin);

    // 页面加载时检查记住的登录信息
    checkRememberedLogin();
    
    // 初始化验证码
    drawCode();
});

// 添加登录状态检查函数
function checkLoginStatus() {
    const currentUser = localStorage.getItem('currentUser');
    const userRole = localStorage.getItem('userRole');
    return {
        isLoggedIn: !!currentUser,
        isAdmin: userRole === 'admin',
        username: currentUser
    };
}

// 登录请求成功处理
function handleLoginResponse(data) {
    if (data.status === 'success') {
        window.location.href = 'member.html';
    } else {
        alert(data.message || '登录失败，请稍后重试');
    }
}

// 初始化图形验证码
function drawCode() {
    // 实现图形验证码生成逻辑
} 