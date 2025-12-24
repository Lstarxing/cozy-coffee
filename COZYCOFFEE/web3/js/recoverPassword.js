document.addEventListener('DOMContentLoaded', function() {
    const recoverForm = document.getElementById('recoverForm');
    const passwordInput = document.getElementById('newPassword');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const phoneInput = document.getElementById('phoneInput');
    const emailInput = document.getElementById('emailInput');
    const phoneField = document.getElementById('phone');
    const emailField = document.getElementById('email');

    function checkPasswordStrength(password) {
        let strength = 0;
        let status = '';
    
        // 检查长度
        if (password.length >= 8) strength += 1;
        // 检查是否包含数字
        if (/\d/.test(password)) strength += 1;
        // 检查是否包含小写字母
        if (/[a-z]/.test(password)) strength += 1;
        // 检查是否包含大写字母
        if (/[A-Z]/.test(password)) strength += 1;
        // 检查是否包含特殊字符
        if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) strength += 1;
    
        // 确定密码强度级别
        if (password.length === 0) {
            status = '';
        } else if (strength <= 2) {
            status = 'weak';
        } else if (strength === 3) {
            status = 'medium';
        } else {
            status = 'strong';
        }
    
        return {
            score: strength,
            level: status
        };
    }
    
    // 注册方式切换处理
    document.querySelectorAll('.type-btn').forEach(button => {
        button.addEventListener('click', function() {
            const type = this.getAttribute('data-type');
            document.querySelectorAll('.type-btn').forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');

            if (type === 'phone') {
                phoneInput.style.display = 'block';
                emailInput.style.display = 'none';
                phoneField.required = true;
                emailField.required = false;
                emailField.value = '';
            } else {
                phoneInput.style.display = 'none';
                emailInput.style.display = 'block';
                phoneField.required = false;
                emailField.required = true;
                phoneField.value = '';
            }
        });
    });

    // 密码显示/隐藏切换
    [document.getElementById('togglePassword'), document.getElementById('toggleConfirmPassword')].forEach(toggle => {
        toggle.addEventListener('click', function() {
            const input = this.id === 'togglePassword' ? passwordInput : confirmPasswordInput;
            const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
            input.setAttribute('type', type);
            this.src = type === 'password' ? 'images/icons/eye-close.png' : 'images/icons/eye-open.png';
        });
    });

    // 验证码按钮事件绑定
    document.querySelectorAll('.send-code').forEach(button => {
        button.addEventListener('click', handleSendCode);
    });

    // 密码强度实时检查
    passwordInput.addEventListener('input', function() {
        const strength = checkPasswordStrength(this.value);
        const strengthBar = document.querySelector('.strength-bar');
        const strengthText = document.querySelector('.strength-text');
        
        strengthBar.style.width = `${(strength.score / 5) * 100}%`;
        strengthBar.className = 'strength-bar';
        
        switch (strength.level) {
            case 'weak':
                strengthBar.classList.add('weak');
                strengthText.textContent = '弱';
                strengthText.style.color = '#ff4d4f';
                break;
            case 'medium':
                strengthBar.classList.add('medium');
                strengthText.textContent = '中';
                strengthText.style.color = '#faad14';
                break;
            case 'strong':
                strengthBar.classList.add('strong');
                strengthText.textContent = '强';
                strengthText.style.color = '#52c41a';
                break;
            default:
                strengthText.textContent = '';
                break;
        }
        
        document.querySelector('.password-strength').style.display = this.value ? 'block' : 'none';
    });

    confirmPasswordInput.addEventListener('input', checkPasswordMatch);

    // 检查密码一致性
    function checkPasswordMatch() {
        const matchDiv = document.querySelector('.password-match');
        const matchText = matchDiv.querySelector('.match-text');
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        if (confirmPassword) {
            matchDiv.style.display = 'block';
            if (password === confirmPassword) {
                matchText.textContent = '密码一致';
                matchText.className = 'match-text match';
            } else {
                matchText.textContent = '密码不一致';
                matchText.className = 'match-text not-match';
            }
        } else {
            matchDiv.style.display = 'none';
        }
    }

    // 验证码输入限制
    document.getElementById('verifyCode').addEventListener('input', function() {
        this.value = this.value.replace(/\D/g, '').slice(0, 6);
    });

    // 处理发送验证码
    async function handleSendCode(e) {
        const button = e.target;
        const type = button.closest('.input-group').id === 'phoneInput' ? 'phone' : 'email';
        const input = document.getElementById(type);

        if (!validateInput(input)) return;

        try {
            const response = await fetch('/CozyCoffee/users', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    action: 'sendVerifyCode',
                    operation: 'recoverPassword',
                    type: type,
                    target: input.value
                })
            });

            const data = await response.json();
            if (data.status === 'success') {
                startCountdown(button);
                alert('验证码已发送，请查收');
            } else {
                alert(data.message || '发送验证码失败，请稍后重试');
            }
        } catch (error) {
            console.error('发送验证码失败:', error);
            alert('发送验证码失败，请检查网络连接后重试');
        }
    }

    // 验证输入格式
    function validateInput(input) {
        if (!input.value) {
            alert('请输入' + (input.type === 'tel' ? '手机号' : '邮箱'));
            return false;
        }

        if (input.type === 'tel') {
            const phoneRegex = /^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\d{8}$/;
            if (!phoneRegex.test(input.value)) {
                alert('请输入正确的手机号');
                return false;
            }
        } else if (input.type === 'email') {
            const emailRegex = /^[A-Za-z0-9\u4e00-\u9fa5]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
            if (!emailRegex.test(input.value)) {
                alert('请输入正确的邮箱地址');
                return false;
            }
        }
        return true;
    }

    // 验证码倒计时
    function startCountdown(button) {
        let seconds = 60;
        button.disabled = true;
        const originalText = button.textContent;
        button.textContent = `${seconds}秒后重试`;

        const timer = setInterval(() => {
            seconds--;
            if (seconds <= 0) {
                clearInterval(timer);
                button.disabled = false;
                button.textContent = originalText;
                document.getElementById('verifyCode').value = '';
                alert('验证码已过期，请重新获取');
            } else {
                button.textContent = `${seconds}秒后重试`;
            }
        }, 1000);
    }

    // 表单提交处理
    recoverForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        const type = document.querySelector('.type-btn.active').getAttribute('data-type');
        const identifier = document.getElementById(type === 'phone' ? 'phone' : 'email').value.trim();
        const verifyCode = document.getElementById('verifyCode').value.trim();
        const newPassword = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        // 表单验证
        if (!validateInput(document.getElementById(type === 'phone' ? 'phone' : 'email'))) return;
        
        if (!/^\d{6}$/.test(verifyCode)) {
            alert('请输入6位数字验证码');
            return;
        }

        const passwordStrength = checkPasswordStrength(newPassword);
        if (passwordStrength.level === 'weak') {
            alert('密码强度太弱，请至少包含以下条件中的3项：\n' +
                  '- 8位及以上字符\n' +
                  '- 包含数字\n' +
                  '- 包含小写字母\n' +
                  '- 包含大写字母\n' +
                  '- 包含特殊字符');
            return;
        }

        if (newPassword !== confirmPassword) {
            alert('两次输入的密码不一致');
            return;
        }

        try {
            const response = await fetch('/CozyCoffee/users', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    action: 'recoverPassword',
                    type: type,
                    identifier: identifier,
                    verifyCode: verifyCode,
                    newPassword: newPassword,
                    operation: 'recoverPassword'
                })
            });

            const data = await response.json();
            if (data.status === 'success') {
                alert('密码重置成功！');
                window.location.href = 'login.html';
            } else {
                alert(data.message || '密码重置失败，请稍后重试');
            }
        } catch (error) {
            console.error('密码重置失败:', error);
            alert('服务器错误，请稍后重试');
        }
    });
});