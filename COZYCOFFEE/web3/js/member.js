document.addEventListener('DOMContentLoaded', function() {
    // 加载用户信息
    // loadUserInfo(); // 暂时注释掉，开发完成后取消注释

    // 测试用户数据
    const testUsers = [
        {
            id: 1001,
            nickname: "基础会员",
            totalPoints: 500,    // 累计积分
            availablePoints: 500, // 积分余额
            email: "basic@example.com",
            phoneNumber: "13800138001",
            avatar: "images/default-avatar.png",
            invitationCode: "BASIC001",
            level: "basic",
            signInDays: 2,
            makeupCards: 1,
            signInHistory: [1, 2], // 连续签到第1、2天
            lastSignIn: new Date(new Date().setDate(new Date().getDate() - 1)).toISOString().split('T')[0],
            addresses: []
        },
        {
            id: 1002,
            nickname: "白银会员",
            totalPoints: 1500,
            availablePoints: 1500,
            email: "silver@example.com",
            phoneNumber: "13800138002",
            avatar: "images/default-avatar.png",
            invitationCode: "SILVER001",
            level: "silver",
            signInDays: 4,
            makeupCards: 1,
            signInHistory: [1, 2, 3, 4], // 连续签到第1-4天
            lastSignIn: new Date(new Date().setDate(new Date().getDate() - 1)).toISOString().split('T')[0],
            addresses: []
        },
        {
            id: 1003,
            nickname: "黄金会员",
            totalPoints: 4500,
            availablePoints: 4500,
            email: "gold@example.com",
            phoneNumber: "13800138003",
            avatar: "images/default-avatar.png",
            invitationCode: "GOLD001",
            level: "gold",
            signInDays: 7,
            makeupCards: 1,
            signInHistory: [1, 2, 3, 4, 5, 6, 7], // 连续签到第1-7天
            lastSignIn: new Date(new Date().setDate(new Date().getDate() - 1)).toISOString().split('T')[0],
            addresses: []
        },
        {
            id: 1004,
            nickname: "黑金会员",
            totalPoints: 12000,  // 累计积分可以超过8000
            availablePoints: 9000,
            email: "black@example.com",
            phoneNumber: "13800138004",
            avatar: "images/default-avatar.png",
            invitationCode: "BLACK001",
            level: "black",
            signInDays: 3,
            makeupCards: 1,
            lastSignIn: new Date(new Date().setDate(new Date().getDate() - 1)).toISOString().split('T')[0],
            addresses: []
        }
    ];

    // 将变量声明移到全局作用域
    window.currentUserIndex = 0;
    window.mockUserData = testUsers[window.currentUserIndex];
    window.testUsers = testUsers;

    // 添加测试用户选择器
    addTestUserSelector();
    
    // 初始化签到功能
    initSignIn();
    
    // 初始化会员权益切换
    initBenefitsToggle();
    
    // 添加会员制度导航
    addMembershipRulesNav();

    // 导航切换
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();
            document.querySelectorAll('.nav-item').forEach(nav => nav.classList.remove('active'));
            this.classList.add('active');
            
            // 显示对应的内容区域
            const targetId = this.getAttribute('href').substring(1);
            document.querySelectorAll('.content-section').forEach(section => {
                section.style.display = 'none';
            });
            document.getElementById(targetId).style.display = 'block';
        });
    });

    // 个人信息表单提交
    const personalInfoForm = document.getElementById('personalInfoForm');
    if (personalInfoForm) {
        personalInfoForm.addEventListener('submit', handlePersonalInfoSubmit);
    }

    // 头像上传
    const avatarInput = document.getElementById('avatarInput');
    if (avatarInput) {
        avatarInput.addEventListener('change', handleAvatarUpload);
    }

    // 添加地址按钮
    const addAddressBtn = document.getElementById('addAddressBtn');
    if (addAddressBtn) {
        addAddressBtn.addEventListener('click', showAddAddressModal);
    }

    // 积分商城功能
    updatePointsDisplay();

    // 为所有兑换按钮添加点击事件
    document.querySelectorAll('.exchange-btn').forEach(btn => {
        btn.addEventListener('click', handleExchange);
    });
    
    // 初始化用户界面
    updateUserInterface(window.mockUserData);

    // 为测试用户添加地址数据
    window.testUsers.forEach(user => {
        if (!user.addresses) {
            user.addresses = [{
                name: user.nickname,
                phone: user.phoneNumber,
                province: '广东省',
                city: '深圳市',
                district: '南山区',
                address: '科技园路1号',
                isDefault: true
            }];
        }
    });
    
    // 初始化地址模态框关闭按钮
    const closeModalBtns = document.querySelectorAll('.close-modal');
    closeModalBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const modalId = this.getAttribute('data-modal');
            if (modalId) {
                closeModal(modalId);
            } else {
                closeAddressModal();
            }
        });
    });
    
    // 初始化地址表单提交
    const addressForm = document.getElementById('addressForm');
    if (addressForm) {
        addressForm.onsubmit = saveAddress;
    }
    
    // 初始化地址列表
    updateAddressList();
    
    // 初始化省市区选择器
    initRegionSelector();
    
    // 初始化修改邮箱和手机号按钮
    const changeEmailBtn = document.getElementById('changeEmailBtn');
    if (changeEmailBtn) {
        changeEmailBtn.addEventListener('click', showEmailModal);
    }
    
    const changePhoneBtn = document.getElementById('changePhoneBtn');
    if (changePhoneBtn) {
        changePhoneBtn.addEventListener('click', showPhoneModal);
    }

    // 初始化生日选择器
    initBirthdaySelector();

    // 初始化用户数据
    initUserData(window.mockUserData);

    // 添加兑换记录跳转功能
    const exchangeHistoryLink = document.querySelector('.exchange-history a');
    if (exchangeHistoryLink) {
        exchangeHistoryLink.addEventListener('click', function(e) {
            e.preventDefault();
            // 切换到积分明细标签
            const pointsHistoryTab = document.querySelector('a[href="#points-history"]');
            if (pointsHistoryTab) {
                pointsHistoryTab.click();
            }
        });
    }

    // 初始化积分商城
    initPointsMall();
});

// 添加测试用户选择器
function addTestUserSelector() {
    const container = document.createElement('div');
    container.className = 'test-user-selector';
    container.innerHTML = `
        <h3>测试用户选择</h3>
        <div class="user-buttons">
            <button data-index="0">基础会员</button>
            <button data-index="1">白银会员</button>
            <button data-index="2">黄金会员</button>
            <button data-index="3">黑金会员</button>
        </div>
    `;
    
    document.querySelector('.member-container').appendChild(container);
    
    // 添加样式
    const style = document.createElement('style');
    style.textContent = `
        .test-user-selector {
            position: fixed;
            bottom: 20px;
            right: 20px;
            background: white;
            padding: 15px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.2);
            z-index: 1000;
        }
        .test-user-selector h3 {
            margin-top: 0;
            margin-bottom: 10px;
            font-size: 14px;
        }
        .user-buttons {
            display: flex;
            gap: 5px;
        }
        .user-buttons button {
            padding: 5px 10px;
            background: #f0f0f0;
            border: 1px solid #ddd;
            border-radius: 5px;
            cursor: pointer;
        }
        .user-buttons button:hover {
            background: #e0e0e0;
        }
    `;
    document.head.appendChild(style);
    
    // 添加点击事件
    container.querySelectorAll('button').forEach(btn => {
        btn.addEventListener('click', function() {
            const index = parseInt(this.getAttribute('data-index'));
            window.currentUserIndex = index;
            window.mockUserData = window.testUsers[index];
            updateUserInterface(window.mockUserData);
        });
    });
}

// 初始化签到功能
function initSignIn() {
    const signInButton = document.getElementById('signInButton');
    const makeupSignBtn = document.getElementById('makeupSignBtn');
    const days = document.querySelectorAll('.day');
    const signInDaysElement = document.getElementById('signInDays');
    const makeupCardsElement = document.getElementById('makeupCards');
    
    // 更新签到状态
    updateSignInStatus();
    
    // 添加签到连线
    drawSignInProgressLines();
    
    // 签到按钮点击事件
    signInButton.addEventListener('click', function() {
        // 检查今天是否已经签到
        const today = new Date().toISOString().split('T')[0];
        if (window.mockUserData.lastSignIn === today) {
            alert('今天已经签到过了');
            return;
        }
        
        // 更新签到数据
        window.mockUserData.lastSignIn = today;
        window.mockUserData.signInDays += 1;
        
        // 如果连续签到7天，重置为1
        if (window.mockUserData.signInDays > 7) {
            window.mockUserData.signInDays = 1;
        }
        
        // 根据连续签到天数给予积分奖励
        const pointsReward = getSignInPointsReward(window.mockUserData.signInDays, window.mockUserData.level);
        window.mockUserData.availablePoints += pointsReward;
        
        // 更新界面
        updateSignInStatus();
        updatePointsDisplay();
        drawSignInProgressLines();
        
        // 禁用签到按钮
        signInButton.disabled = true;
        signInButton.textContent = '今日已签到';
        
        // 显示奖励提示
        alert(`签到成功！获得${pointsReward}积分奖励`);
    });
    
    // 补签按钮点击事件
    makeupSignBtn.addEventListener('click', function() {
        if (window.mockUserData.makeupCards <= 0) {
            alert('没有补签卡了');
            return;
        }
        
        // 找到第一个未签到的日期
        let dayToMakeup = null;
        days.forEach(day => {
            if (!day.classList.contains('signed') && !dayToMakeup) {
                dayToMakeup = day;
            }
        });
        
        if (!dayToMakeup) {
            alert('没有需要补签的日期');
            return;
        }
        
        // 更新签到数据
        window.mockUserData.signInDays += 1;
        window.mockUserData.makeupCards -= 1;
        
        // 如果连续签到7天，重置为1
        if (window.mockUserData.signInDays > 7) {
            window.mockUserData.signInDays = 1;
        }
        
        // 根据连续签到天数给予积分奖励
        const pointsReward = getSignInPointsReward(window.mockUserData.signInDays, window.mockUserData.level);
        window.mockUserData.availablePoints += pointsReward;
        
        // 更新界面
        updateSignInStatus();
        updatePointsDisplay();
        drawSignInProgressLines();
        
        // 显示奖励提示
        alert(`补签成功！获得${pointsReward}积分奖励`);
    });
}

// 更新签到状态
function updateSignInStatus() {
    const signInDaysElement = document.getElementById('signInDays');
    const makeupCardsElement = document.getElementById('makeupCards');
    const signInButton = document.getElementById('signInButton');
    const makeupSignBtn = document.getElementById('makeupSignBtn');
    const days = document.querySelectorAll('.day');
    const today = new Date().toISOString().split('T')[0];
    
    // 更新连续签到天数显示
    if (signInDaysElement) {
        signInDaysElement.textContent = window.mockUserData.signInDays;
    }
    
    // 更新补签卡数量显示
    if (makeupCardsElement) {
        makeupCardsElement.textContent = window.mockUserData.makeupCards;
    }
    
    // 更新签到按钮状态
    if (signInButton) {
        if (window.mockUserData.lastSignIn === today) {
            signInButton.disabled = true;
            signInButton.textContent = '今日已签到';
        } else {
            signInButton.disabled = false;
            signInButton.textContent = '今日签到';
        }
    }
    
    // 更新补签按钮状态
    if (makeupSignBtn) {
        makeupSignBtn.disabled = window.mockUserData.makeupCards <= 0;
    }
    
    // 更新签到日历
    if (days) {
        days.forEach((day, index) => {
            if (index < window.mockUserData.signInDays) {
                day.classList.add('signed');
            } else {
                day.classList.remove('signed');
            }
        });
    }
}

// 绘制签到进度连线
function drawSignInProgressLines() {
    // 清除现有连线
    const existingLines = document.querySelectorAll('.sign-in-progress-line');
    existingLines.forEach(line => line.remove());
    
    const calendarContainer = document.querySelector('.sign-in-calendar');
    const days = document.querySelectorAll('.day');
    const signedDays = window.mockUserData.signInDays;
    
    // 如果没有找到元素，直接返回
    if (!calendarContainer || !days.length) return;
    
    // 为每两个相邻的日期之间添加连线
    for (let i = 0; i < days.length - 1; i++) {
        const day1 = days[i];
        const day2 = days[i + 1];
        
        // 获取两个日期的位置
        const rect1 = day1.getBoundingClientRect();
        const rect2 = day2.getBoundingClientRect();
        const calendarRect = calendarContainer.getBoundingClientRect();
        
        // 计算连线的位置和宽度
        const x1 = rect1.left + rect1.width / 2 - calendarRect.left;
        const x2 = rect2.left + rect2.width / 2 - calendarRect.left;
        const width = x2 - x1;
        
        // 创建连线元素
        const line = document.createElement('div');
        line.className = 'sign-in-progress-line';
        
        // 根据签到状态设置连线样式
        if (i < signedDays - 1) {
            line.classList.add('completed');
        } else {
            line.classList.add('pending');
        }
        
        // 设置连线位置和宽度
        line.style.left = `${x1}px`;
        line.style.width = `${width}px`;
        
        // 添加到日历容器
        calendarContainer.appendChild(line);
    }
}

// 根据连续签到天数和会员等级计算积分奖励
function getSignInPointsReward(day, level) {
    // 基础积分奖励
    let points = 0;
    switch(day) {
        case 1: points = 5; break;
        case 2: points = 5; break;
        case 3: points = 10; break;
        case 4: points = 15; break;
        case 5: points = 20; break;
        case 6: points = 25; break;
        case 7: points = 30; break;
    }
    
    // 会员等级加成
    if (level === 'gold') {
        points = Math.floor(points * 1.2); // 黄金会员+20%
    } else if (level === 'black') {
        points = Math.floor(points * 1.5); // 黑金会员+50%
    }
    
    return points;
}

// 初始化会员权益切换
function initBenefitsToggle() {
    const levelBtns = document.querySelectorAll('.level-btn');
    
    if (levelBtns.length === 0) return;
    
    levelBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const level = this.getAttribute('data-level');
            
            // 更新按钮状态
            levelBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            
            // 隐藏所有权益
            document.querySelectorAll('.benefit-item').forEach(item => {
                item.style.display = 'none';
            });
            
            // 显示对应等级的权益
            document.querySelectorAll(`.benefit-${level}`).forEach(item => {
                item.style.display = 'block';
            });
        });
    });
}

// 添加会员制度导航
function addMembershipRulesNav() {
    const sideNav = document.querySelector('.side-nav');
    if (sideNav) {
        const rulesNav = document.createElement('a');
        rulesNav.href = "#membership-rules";
        rulesNav.className = "nav-item";
        rulesNav.innerHTML = '<i class="icon-rules"></i>会员制度';
        sideNav.appendChild(rulesNav);
        
        // 添加点击事件
        rulesNav.addEventListener('click', function(e) {
            e.preventDefault();
            document.querySelectorAll('.nav-item').forEach(nav => nav.classList.remove('active'));
            this.classList.add('active');
            
            document.querySelectorAll('.content-section').forEach(section => {
                section.style.display = 'none';
            });
            document.getElementById('membership-rules').style.display = 'block';
        });
    }
    
    // 添加查看更多按钮
    const benefitsSection = document.querySelector('.member-benefits');
    if (benefitsSection) {
        const viewMoreDiv = document.createElement('div');
        viewMoreDiv.className = 'view-more';
        viewMoreDiv.innerHTML = '<a href="#membership-rules" class="view-rules-btn">查看会员制度详情</a>';
        benefitsSection.appendChild(viewMoreDiv);
        
        // 添加点击事件
        viewMoreDiv.querySelector('a').addEventListener('click', function(e) {
            e.preventDefault();
            document.querySelectorAll('.nav-item').forEach(nav => nav.classList.remove('active'));
            document.querySelector('a[href="#membership-rules"]').classList.add('active');
            
            document.querySelectorAll('.content-section').forEach(section => {
                section.style.display = 'none';
            });
            document.getElementById('membership-rules').style.display = 'block';
        });
    }
}

// 加载用户信息
async function loadUserInfo() {
    try {
        const response = await fetch('/CozyCoffee/users', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: 'action=getUserInfo',
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('未登录或会话已过期');
        }

        const data = await response.json();
        if (data.status === 'success') {
            updateUserInterface(data.data);
        } else {
            throw new Error(data.message || '获取用户信息失败');
        }
    } catch (error) {
        console.error('加载用户信息失败:', error);
        // 重定向到登录页
        window.location.href = 'login.html';
    }
}

// 更新用户界面
function updateUserInterface(userData) {
    // 更新顶部导航栏用户信息
    const userAvatar = document.querySelector('.user-avatar');
    const userNickname = document.querySelector('.user-info .user-nickname');
    
    if (userAvatar) userAvatar.src = userData.avatar || 'images/default-avatar.png';
    if (userNickname) userNickname.textContent = userData.nickname || '未设置昵称';
    
    // 更新会员卡信息
    const cardAvatar = document.querySelector('.card-avatar');
    const cardNickname = document.querySelector('.card-info .user-nickname');
    const memberLevel = document.querySelector('.member-level');
    const pointsValue = document.querySelector('.points-value');
    const memberCard = document.querySelector('.member-card');
    
    if (cardAvatar) cardAvatar.src = userData.avatar || 'images/default-avatar.png';
    if (cardNickname) cardNickname.textContent = userData.nickname || '未设置昵称';
    
    // 更新个人信息页面的头像
    const currentAvatar = document.getElementById('currentAvatar');
    if (currentAvatar) currentAvatar.src = userData.avatar || 'images/default-avatar.png';
    
    // 更新个人信息页面的会员ID和邀请码
    const memberIdElement = document.getElementById('memberId');
    const invitationCodeElement = document.getElementById('invitationCode');
    
    if (memberIdElement) memberIdElement.textContent = userData.id || '';
    if (invitationCodeElement) invitationCodeElement.textContent = userData.invitationCode || '';
    
    // 更新当前邮箱和手机号（用于验证模态框）
    const currentEmailElement = document.getElementById('currentEmail');
    const currentPhoneElement = document.getElementById('currentPhone');
    
    if (currentEmailElement) currentEmailElement.value = userData.email || '';
    if (currentPhoneElement) currentPhoneElement.value = userData.phoneNumber || '';
    
    // 根据积分确定会员等级
    let level = 'basic';
    if (userData.totalPoints >= 8000) {
        level = 'black';
    } else if (userData.totalPoints >= 3000) {
        level = 'gold';
    } else if (userData.totalPoints >= 1000) {
        level = 'silver';
    }
    
    // 更新会员卡样式
    if (memberCard) {
        memberCard.classList.remove('basic', 'silver', 'gold', 'black');
        memberCard.classList.add(level);
        
        // 处理黑金会员卡编号
        const cardNumber = document.querySelector('.card-number');
        if (cardNumber) {
            if (level === 'black') {
                cardNumber.style.display = 'block';
                // 生成6位数字编号
                const memberNumber = String(userData.id).padStart(6, '0');
                cardNumber.textContent = `BLACK-NO.${memberNumber}`;
            } else {
                cardNumber.style.display = 'none';
            }
        }
        
        // 处理会员日显示
        const memberDayBadge = document.querySelector('.member-day-badge');
        if (memberDayBadge) {
            const today = new Date();
            if (today.getDate() === 18) {
                memberDayBadge.style.display = 'block';
            } else {
                memberDayBadge.style.display = 'none';
            }
        }
    }
    
    if (memberLevel) {
        memberLevel.textContent = getMemberLevelName(level);
    }
    
    if (pointsValue) {
        pointsValue.textContent = userData.totalPoints || 0;
    }
    
    // 更新等级进度条
    updateLevelProgress(userData.totalPoints);
    
    // 更新积分商城显示
    updatePointsDisplay();
    
    // 更新签到显示
    updateSignInStatus();
    
    // 更新会员权益显示
    updateBenefitsDisplay(level);
    
    // 更新个人信息表单
    const nicknameInput = document.getElementById('nickname');
    const emailInput = document.getElementById('email');
    const phoneInput = document.getElementById('phone');
    const birthYearSelect = document.getElementById('birthYear');
    const birthMonthSelect = document.getElementById('birthMonth');
    const birthDaySelect = document.getElementById('birthDay');
    
    if (nicknameInput) nicknameInput.value = userData.nickname || '';
    if (emailInput) emailInput.value = userData.email || '';
    if (phoneInput) phoneInput.value = userData.phoneNumber || '';
    
    // 更新生日选择器
    if (userData.birthday && birthYearSelect && birthMonthSelect && birthDaySelect) {
        const birthdayDate = new Date(userData.birthday);
        birthYearSelect.value = birthdayDate.getFullYear();
        birthMonthSelect.value = birthdayDate.getMonth() + 1;
        
        // 触发年月变化事件，更新日期选项
        birthYearSelect.dispatchEvent(new Event('change'));
        
        // 设置日期
        birthDaySelect.value = birthdayDate.getDate();
    }

    // 更新积分有效期
    const pointsValidity = document.querySelector('.points-validity .value');
    if (pointsValidity) {
        let validityDate = new Date();
        switch(userData.level) {
            case 'basic':
                validityDate.setFullYear(validityDate.getFullYear() + 1);
                break;
            case 'silver':
                validityDate.setMonth(validityDate.getMonth() + 18);
                break;
            case 'gold':
                validityDate.setFullYear(validityDate.getFullYear() + 2);
                break;
            case 'black':
                pointsValidity.textContent = '长期有效';
                return;
        }
        pointsValidity.textContent = validityDate.toLocaleDateString();
    }
}

// 获取会员等级名称
function getMemberLevelName(level) {
    switch(level) {
        case 'black': return "黑金会员";
        case 'gold': return "黄金会员";
        case 'silver': return "白银会员";
        default: return "基础会员";
    }
}

// 更新等级进度条
function updateLevelProgress(points) {
    const progressBar = document.querySelector('.progress');
    const progressText = document.querySelector('.progress-text');
    
    if (!progressBar || !progressText) return;
    
    let nextLevel, nextLevelPoints, currentLevelPoints, levelName;
    
    if (points < 1000) {
        // 基础会员 -> 白银会员
        nextLevel = 1000;
        currentLevelPoints = 0;
        levelName = "白银会员";
    } else if (points < 3000) {
        // 白银会员 -> 黄金会员
        nextLevel = 3000;
        currentLevelPoints = 1000;
        levelName = "黄金会员";
    } else if (points < 8000) {
        // 黄金会员 -> 黑金会员
        nextLevel = 8000;
        currentLevelPoints = 3000;
        levelName = "黑金会员";
    } else {
        // 已是黑金会员
        progressBar.style.width = '100%';
        progressText.textContent = '已达到最高等级';
        return;
    }
    
    // 计算进度百分比
    const progress = Math.min(100, ((points - currentLevelPoints) / (nextLevel - currentLevelPoints)) * 100);
    progressBar.style.width = `${progress}%`;
    
    // 更新文本
    const pointsNeeded = nextLevel - points;
    progressText.textContent = `距离${levelName}还需 ${pointsNeeded} 积分`;
}

// 初始化用户数据
function initUserData(userData) {
    // 检查用户是否有birthdayLocked标志
    if (userData.birthdayLocked) {
        disableBirthdaySelectors();
    }
}

// 处理个人信息提交
async function handlePersonalInfoSubmit(e) {
    e.preventDefault();
    
    const birthYearSelect = document.getElementById('birthYear');
    const birthMonthSelect = document.getElementById('birthMonth');
    const birthDaySelect = document.getElementById('birthDay');
    
    // 检查是否修改了生日
    const originalBirthday = window.mockUserData.birthday;
    const newBirthday = `${birthYearSelect.value}-${birthMonthSelect.value}-${birthDaySelect.value}`;
    
    // 标记是否需要更新生日
    let updateBirthday = false;
    
    if (originalBirthday !== newBirthday) {
        // 检查用户是否已锁定生日修改
        if (window.mockUserData.birthdayLocked) {
            showToast('出生日期每年只能修改一次');
            return;
        }
        
        // 显示确认对话框
        if (!confirm('出生日期每年只可更改一次，请确认是否修改？')) {
            return;
        }
        
        // 标记需要更新生日
        updateBirthday = true;
    }

    const formData = {
        nickname: document.getElementById('nickname').value,
        // 只有在确认修改生日后才更新生日
        birthday: updateBirthday ? newBirthday : originalBirthday,
        email: document.getElementById('email').value,
        phoneNumber: document.getElementById('phone').value
    };

    try {
        // 模拟API调用
        await new Promise(resolve => setTimeout(resolve, 500));
        
        // 如果生日被修改，记录更新时间
        if (updateBirthday) {
            // 设置生日锁定标志
            window.mockUserData.birthdayLocked = true;
            disableBirthdaySelectors();
        }
        
        // 更新当前用户数据
        Object.assign(window.mockUserData, formData);
        
        // 更新测试用户数据
        const userIndex = window.testUsers.findIndex(user => user.id === window.mockUserData.id);
        if (userIndex !== -1) {
            Object.assign(window.testUsers[userIndex], formData);
            // 同步birthdayLocked标志
            window.testUsers[userIndex].birthdayLocked = window.mockUserData.birthdayLocked;
        }

        // 根据是否修改了生日显示不同的提示
        if (updateBirthday) {
            showToast('信息已保存，出生日期已更新');
        } else {
            showToast('信息已保存');
        }
        updateUserInterface(window.mockUserData);
        
    } catch (error) {
        console.error('保存失败:', error);
        showToast('保存失败，请重试');
    }
}

// 处理头像上传
function handleAvatarUpload(e) {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(event) {
            // 更新模拟数据
            window.mockUserData.avatar = event.target.result;
            
            // 更新界面
            updateUserInterface(window.mockUserData);
        };
        reader.readAsDataURL(file);
    }
}

// 显示添加地址模态框
function showAddAddressModal() {
    const addressModal = document.getElementById('addressModal');
    if (addressModal) {
        addressModal.style.display = 'block';
        
        // 重置表单
        const form = document.getElementById('addressForm');
        if (form) {
            form.reset();
            
            // 清除编辑索引
            form.removeAttribute('data-edit-index');
            
            // 确保只有一个提交事件监听器
            form.onsubmit = saveAddress;
            
            // 更新模态框标题
            const modalTitle = addressModal.querySelector('.modal-header h3');
            if (modalTitle) {
                modalTitle.textContent = '添加收货地址';
            }
            
            // 重新初始化省市区选择器
            initRegionSelector();
        }
    }
}

// 关闭添加地址模态框
function closeAddressModal() {
    const addressModal = document.getElementById('addressModal');
    if (addressModal) {
        addressModal.style.display = 'none';
        
        // 重置表单
        const form = document.getElementById('addressForm');
        if (form) {
            form.reset();
            form.removeAttribute('data-edit-index');
        }
    }
}

// 保存地址
function saveAddress(e) {
    e.preventDefault();
    e.stopPropagation(); // 阻止事件冒泡
    
    const form = document.getElementById('addressForm');
    if (!form) return;
    
    // 检查是否是编辑模式
    const editIndex = form.getAttribute('data-edit-index');
    
    const name = document.getElementById('name').value;
    const phone = document.getElementById('addressPhone').value;
    const province = form.elements['province'].value;
    const city = form.elements['city'].value;
    const district = form.elements['district'].value;
    const address = document.getElementById('detailAddress').value;
    const isDefault = document.getElementById('defaultAddress').checked;
    
    // 验证表单
    if (!name || !phone || !province || !city || !district || !address) {
        alert('请填写完整的地址信息');
        return;
    }
    
    // 创建新地址对象
    const newAddress = {
        name,
        phone,
        province,
        city,
        district,
        address,
        isDefault
    };
    
    // 如果设为默认地址，将其他地址设为非默认
    if (isDefault) {
        if (!window.mockUserData.addresses) {
            window.mockUserData.addresses = [];
        }
        
        window.mockUserData.addresses.forEach(addr => {
            addr.isDefault = false;
        });
    }
    
    // 判断是新增还是编辑
    if (editIndex !== null && editIndex !== undefined) {
        // 编辑现有地址
        window.mockUserData.addresses[editIndex] = newAddress;
        
        // 关闭模态框
        closeAddressModal();
        
        // 更新地址列表显示
        updateAddressList();
        
        showToast('地址更新成功！');
    } else {
        // 添加新地址
        if (!window.mockUserData.addresses) {
            window.mockUserData.addresses = [];
        }
        window.mockUserData.addresses.push(newAddress);
        
        // 关闭模态框
        closeAddressModal();
        
        // 更新地址列表显示
        updateAddressList();
        
        showToast('地址添加成功！');
    }
}

// 编辑地址
function editAddress(index) {
    const address = window.mockUserData.addresses[index];
    if (!address) return;
    
    // 打开模态框
    const addressModal = document.getElementById('addressModal');
    if (!addressModal) return;
    
    addressModal.style.display = 'block';
    
    // 更新模态框标题
    const modalTitle = addressModal.querySelector('.modal-header h3');
    if (modalTitle) {
        modalTitle.textContent = '编辑收货地址';
    }
    
    // 填充表单
    const form = document.getElementById('addressForm');
    if (!form) return;
    
    // 存储当前编辑的地址索引
    form.setAttribute('data-edit-index', index);
    
    document.getElementById('name').value = address.name;
    document.getElementById('addressPhone').value = address.phone;
    
    // 重新初始化省市区选择器
    initRegionSelector();
    
    // 获取选择器
    const provinceSelect = form.elements['province'];
    const citySelect = form.elements['city'];
    const districtSelect = form.elements['district'];
    
    // 设置省份
    provinceSelect.value = address.province;
    
    // 手动触发省份变化事件
    const provinceEvent = new Event('change');
    provinceSelect.dispatchEvent(provinceEvent);
    
    // 手动填充城市选项
    citySelect.innerHTML = '<option value="">请选择城市</option>';
    if (window.regionData && window.regionData[address.province]) {
        Object.keys(window.regionData[address.province]).forEach(city => {
            citySelect.innerHTML += `<option value="${city}">${city}</option>`;
        });
    }
    
    // 设置城市
    citySelect.value = address.city;
    
    // 手动触发城市变化事件
    const cityEvent = new Event('change');
    citySelect.dispatchEvent(cityEvent);
    
    // 手动填充区县选项
    districtSelect.innerHTML = '<option value="">请选择区县</option>';
    if (window.regionData && window.regionData[address.province] && window.regionData[address.province][address.city]) {
        window.regionData[address.province][address.city].forEach(district => {
            districtSelect.innerHTML += `<option value="${district}">${district}</option>`;
        });
    }
    
    // 设置区县
    districtSelect.value = address.district;
    
    document.getElementById('detailAddress').value = address.address;
    document.getElementById('defaultAddress').checked = address.isDefault;
    
    // 确保只有一个提交事件监听器
    form.onsubmit = saveAddress;
}

// 删除地址
function deleteAddress(index) {
    if (!window.mockUserData.addresses || index >= window.mockUserData.addresses.length) {
        return;
    }
    
    if (confirm('确定要删除这个地址吗？')) {
        window.mockUserData.addresses.splice(index, 1);
        updateAddressList();
        
        // 使用更友好的提示方式
        showToast('地址删除成功！');
    }
}

// 更新地址列表
function updateAddressList() {
    const addressList = document.querySelector('.address-list');
    if (!addressList) return;
    
    // 清空地址列表
    addressList.innerHTML = '';
    
    // 检查地址数组是否存在
    if (!window.mockUserData.addresses || window.mockUserData.addresses.length === 0) {
        addressList.innerHTML = '<div class="empty-address">暂无收货地址，请添加</div>';
        return;
    }
    
    // 添加地址项
    window.mockUserData.addresses.forEach((address, index) => {
        // 跳过空地址
        if (!address.name && !address.phone && !address.address) {
            return;
        }
        
        const addressItem = document.createElement('div');
        addressItem.className = 'address-item';
        addressItem.innerHTML = `
            <div class="address-info">
                <div class="address-header">
                    <h4>${address.name} ${address.phone}</h4>
                    ${address.isDefault ? '<span class="default-tag">默认</span>' : ''}
                </div>
                <p>${address.province} ${address.city} ${address.district} ${address.address}</p>
            </div>
            <div class="address-actions">
                <button class="edit-btn" onclick="editAddress(${index})">编辑</button>
                <button class="delete-btn" onclick="deleteAddress(${index})">删除</button>
            </div>
        `;
        
        addressList.appendChild(addressItem);
    });
}

// 初始化省市区选择器
function initRegionSelector() {
    // 模拟省市区数据
    const regions = {
        '广东省': {
            '广州市': ['天河区', '海珠区', '越秀区', '白云区'],
            '深圳市': ['南山区', '福田区', '罗湖区', '宝安区'],
            '珠海市': ['香洲区', '金湾区', '斗门区']
        },
        '北京市': {
            '北京市': ['东城区', '西城区', '朝阳区', '海淀区', '丰台区']
        },
        '上海市': {
            '上海市': ['黄浦区', '徐汇区', '长宁区', '静安区', '普陀区']
        }
    };
    
    const provinceSelect = document.querySelector('select[name="province"]');
    const citySelect = document.querySelector('select[name="city"]');
    const districtSelect = document.querySelector('select[name="district"]');
    
    if (!provinceSelect || !citySelect || !districtSelect) return;
    
    // 清空并添加省份选项
    provinceSelect.innerHTML = '<option value="">请选择省份</option>';
    Object.keys(regions).forEach(province => {
        provinceSelect.innerHTML += `<option value="${province}">${province}</option>`;
    });
    
    // 移除旧的事件监听器（如果有）
    const newProvinceSelect = provinceSelect.cloneNode(true);
    provinceSelect.parentNode.replaceChild(newProvinceSelect, provinceSelect);
    
    const newCitySelect = citySelect.cloneNode(true);
    citySelect.parentNode.replaceChild(newCitySelect, citySelect);
    
    // 省份变化时更新城市
    newProvinceSelect.addEventListener('change', function() {
        const province = this.value;
        newCitySelect.innerHTML = '<option value="">请选择城市</option>';
        document.querySelector('select[name="district"]').innerHTML = '<option value="">请选择区县</option>';
        
        if (province && regions[province]) {
            Object.keys(regions[province]).forEach(city => {
                newCitySelect.innerHTML += `<option value="${city}">${city}</option>`;
            });
        }
    });
    
    // 城市变化时更新区县
    newCitySelect.addEventListener('change', function() {
        const province = document.querySelector('select[name="province"]').value;
        const city = this.value;
        document.querySelector('select[name="district"]').innerHTML = '<option value="">请选择区县</option>';
        
        if (province && city && regions[province] && regions[province][city]) {
            regions[province][city].forEach(district => {
                document.querySelector('select[name="district"]').innerHTML += `<option value="${district}">${district}</option>`;
            });
        }
    });
    
    // 保存区域数据到window对象，以便其他函数使用
    window.regionData = regions;
}

// 更新积分显示
function updatePointsDisplay() {
    // 更新会员卡片上的累计积分显示
    const pointsValue = document.querySelector('.points-value');
    if (pointsValue) {
        pointsValue.textContent = window.mockUserData.totalPoints;
        // 更新显示文本
        const pointsLabel = pointsValue.previousElementSibling;
        if (pointsLabel && pointsLabel.classList.contains('points-label')) {
            pointsLabel.textContent = '累计积分';
        }
    }

    // 更新积分商城顶部的积分余额显示
    const mallPoints = document.querySelector('.current-points');
    if (mallPoints) {
        mallPoints.textContent = window.mockUserData.availablePoints;
    }

    // 检查每个商品是否可兑换（使用可用积分和会员等级）
    document.querySelectorAll('.product-card').forEach(card => {
        const btn = card.querySelector('.exchange-btn');
        const requiredPoints = parseInt(btn.getAttribute('data-points'));
        const productLevel = card.getAttribute('data-level') || 'basic';
        
        // 检查会员等级权限
        let canExchange = false;
        switch (window.mockUserData.level) {
            case 'black':
                canExchange = true; // 黑金会员可以兑换所有商品
                break;
            case 'gold':
                canExchange = ['basic', 'silver', 'gold'].includes(productLevel);
                break;
            case 'silver':
                canExchange = ['basic', 'silver'].includes(productLevel);
                break;
            case 'basic':
                canExchange = productLevel === 'basic';
                break;
        }

        // 检查积分是否足够且有权限兑换
        if (requiredPoints > window.mockUserData.availablePoints || !canExchange) {
            btn.disabled = true;
            btn.textContent = !canExchange ? '等级不足' : '积分不足';
            btn.classList.add('disabled');
        } else {
            btn.disabled = false;
            btn.textContent = '立即兑换';
            btn.classList.remove('disabled');
        }
    });

    // 更新等级进度
    updateLevelProgress();
}

// 更新等级进度
function updateLevelProgress() {
    const progress = document.querySelector('.progress');
    const progressText = document.querySelector('.progress-text');
    
    if (!progress || !progressText) return;

    const totalPoints = window.mockUserData.totalPoints;
    let nextLevelPoints = 1000;
    let progressPercentage = 0;

    if (totalPoints < 1000) {
        // 基础会员到白银
        nextLevelPoints = 1000;
        progressPercentage = (totalPoints / 1000) * 100;
    } else if (totalPoints < 3000) {
        // 白银到黄金
        nextLevelPoints = 3000;
        progressPercentage = ((totalPoints - 1000) / 2000) * 100;
    } else if (totalPoints < 8000) {
        // 黄金到黑金
        nextLevelPoints = 8000;
        progressPercentage = ((totalPoints - 3000) / 5000) * 100;
    } else {
        // 已是黑金会员
        progressPercentage = 100;
    }

    progress.style.width = `${progressPercentage}%`;
    
    if (totalPoints >= 8000) {
        progressText.textContent = '已达到最高等级';
    } else {
        progressText.textContent = `距离下一等级还需 ${nextLevelPoints - totalPoints} 积分`;
    }
}

// 处理兑换
function handleExchange(e) {
    const btn = e.target;
    const requiredPoints = parseInt(btn.getAttribute('data-points'));
    const productName = btn.closest('.product-info').querySelector('h3').textContent;

    if (window.mockUserData.availablePoints >= requiredPoints) {
        if (confirm(`确定要使用 ${requiredPoints} 积分兑换 ${productName} 吗？`)) {
            // 扣除积分
            window.mockUserData.availablePoints -= requiredPoints;
            
            // 更新积分显示
            updatePointsDisplay();
            
            // 更新用户界面
            updateUserInterface(window.mockUserData);

            // 添加兑换记录
            addExchangeRecord(productName, requiredPoints);

            // 显示成功提示
            alert('兑换成功！');
        }
    } else {
        alert('积分不足，无法兑换');
    }
}

// 添加兑换记录
function addExchangeRecord(productName, points) {
    const historyList = document.querySelector('.history-list');
    const now = new Date();
    const dateStr = now.toLocaleDateString();
    const timeStr = now.toLocaleTimeString();

    const recordHtml = `
        <div class="history-item">
            <div class="history-info">
                <h4>${productName}</h4>
                <p>${dateStr} ${timeStr}</p>
            </div>
            <span class="history-points">-${points}积分</span>
        </div>
    `;

    historyList.insertAdjacentHTML('afterbegin', recordHtml);
}

// 更新会员权益显示
function updateBenefitsDisplay(level) {
    // 更新积分倍率显示
    const pointsRateElement = document.querySelector('#benefit-points-rate p');
    if (pointsRateElement) {
        switch(level) {
            case 'black':
                pointsRateElement.textContent = '1元=2.0积分';
                break;
            case 'gold':
                pointsRateElement.textContent = '1元=1.5积分';
                break;
            case 'silver':
                pointsRateElement.textContent = '1元=1.2积分';
                break;
            default:
                pointsRateElement.textContent = '1元=1积分';
        }
    }
    
    // 根据会员等级显示对应权益
    document.querySelectorAll('.silver-benefit, .gold-benefit, .black-benefit').forEach(item => {
        item.style.display = 'none';
    });
    
    if (level === 'silver' || level === 'gold' || level === 'black') {
        document.querySelectorAll('.silver-benefit').forEach(item => {
            item.style.display = 'block';
        });
    }
    
    if (level === 'gold' || level === 'black') {
        document.querySelectorAll('.gold-benefit').forEach(item => {
            item.style.display = 'block';
        });
    }
    
    if (level === 'black') {
        document.querySelectorAll('.black-benefit').forEach(item => {
            item.style.display = 'block';
        });
    }
}

// 显示修改邮箱模态框
function showEmailModal() {
    const emailModal = document.getElementById('emailModal');
    if (emailModal) {
        emailModal.style.display = 'block';
        
        // 显示第一步，隐藏第二步
        document.getElementById('emailStep1').classList.add('active');
        document.getElementById('emailStep2').classList.remove('active');
        
        // 设置当前邮箱
        document.getElementById('currentEmail').value = window.mockUserData.email || '';
    }
}

// 显示修改手机号模态框
function showPhoneModal() {
    const phoneModal = document.getElementById('phoneModal');
    if (phoneModal) {
        phoneModal.style.display = 'block';
        
        // 显示第一步，隐藏第二步
        document.getElementById('phoneStep1').classList.add('active');
        document.getElementById('phoneStep2').classList.remove('active');
        
        // 设置当前手机号
        document.getElementById('currentPhone').value = window.mockUserData.phoneNumber || '';
    }
}

// 关闭模态框
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
    }
}

// 发送验证码
function sendVerificationCode(btnId) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    
    // 禁用按钮
    btn.disabled = true;
    btn.classList.add('disabled');
    
    // 倒计时
    let countdown = 60;
    const originalText = btn.textContent;
    btn.textContent = `${countdown}秒后重新发送`;
    
    const timer = setInterval(() => {
        countdown--;
        btn.textContent = `${countdown}秒后重新发送`;
        
        if (countdown <= 0) {
            clearInterval(timer);
            btn.disabled = false;
            btn.classList.remove('disabled');
            btn.textContent = originalText;
        }
    }, 1000);
    
    // 模拟发送验证码
    alert('验证码已发送，请查收');
}

// 邮箱验证下一步
function emailNextStep() {
    const code = document.getElementById('emailVerifyCode').value;
    if (!code) {
        alert('请输入验证码');
        return;
    }
    
    // 模拟验证成功
    document.getElementById('emailStep1').classList.remove('active');
    document.getElementById('emailStep2').classList.add('active');
}

// 邮箱验证返回上一步
function emailBackStep() {
    document.getElementById('emailStep2').classList.remove('active');
    document.getElementById('emailStep1').classList.add('active');
}

// 手机号验证下一步
function phoneNextStep() {
    const code = document.getElementById('phoneVerifyCode').value;
    if (!code) {
        alert('请输入验证码');
        return;
    }
    
    // 模拟验证成功
    document.getElementById('phoneStep1').classList.remove('active');
    document.getElementById('phoneStep2').classList.add('active');
}

// 手机号验证返回上一步
function phoneBackStep() {
    document.getElementById('phoneStep2').classList.remove('active');
    document.getElementById('phoneStep1').classList.add('active');
}

// 提交修改邮箱
function submitEmailChange(e) {
    e.preventDefault();
    
    const newEmail = document.getElementById('newEmail').value;
    const code = document.getElementById('newEmailVerifyCode').value;
    
    if (!newEmail) {
        alert('请输入新邮箱');
        return;
    }
    
    if (!code) {
        alert('请输入验证码');
        return;
    }
    
    // 模拟修改成功
    window.mockUserData.email = newEmail;
    updateUserInterface(window.mockUserData);
    
    // 关闭模态框
    closeModal('emailModal');
    
    // 显示成功提示
    alert('邮箱修改成功！');
}

// 提交修改手机号
function submitPhoneChange(e) {
    e.preventDefault();
    
    const newPhone = document.getElementById('newPhone').value;
    const code = document.getElementById('newPhoneVerifyCode').value;
    
    if (!newPhone) {
        alert('请输入新手机号');
        return;
    }
    
    if (!code) {
        alert('请输入验证码');
        return;
    }
    
    // 模拟修改成功
    window.mockUserData.phoneNumber = newPhone;
    updateUserInterface(window.mockUserData);
    
    // 关闭模态框
    closeModal('phoneModal');
    
    // 显示成功提示
    alert('手机号修改成功！');
}

// 显示友好的提示信息
function showToast(message) {
    // 创建toast元素
    const toast = document.createElement('div');
    toast.className = 'toast-message';
    toast.textContent = message;
    
    // 添加到页面
    document.body.appendChild(toast);
    
    // 显示toast
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);
    
    // 2秒后隐藏并移除
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            document.body.removeChild(toast);
        }, 300);
    }, 2000);
}

// 初始化生日选择器
function initBirthdaySelector() {
    const yearSelect = document.getElementById('birthYear');
    const monthSelect = document.getElementById('birthMonth');
    const daySelect = document.getElementById('birthDay');
    
    if (!yearSelect || !monthSelect || !daySelect) return;
    
    // 生成年份选项（1950年到当前年份）
    const currentYear = new Date().getFullYear();
    for (let year = currentYear; year >= 1950; year--) {
        const option = document.createElement('option');
        option.value = year;
        option.textContent = year;
        yearSelect.appendChild(option);
    }
    
    // 生成月份选项
    for (let month = 1; month <= 12; month++) {
        const option = document.createElement('option');
        option.value = month;
        option.textContent = month;
        monthSelect.appendChild(option);
    }
    
    // 根据年月更新日期选项
    function updateDays() {
        const year = parseInt(yearSelect.value) || currentYear;
        const month = parseInt(monthSelect.value) || 1;
        
        // 获取当月天数
        const daysInMonth = new Date(year, month, 0).getDate();
        
        // 清空并重新生成日期选项
        daySelect.innerHTML = '<option value="">日</option>';
        for (let day = 1; day <= daysInMonth; day++) {
            const option = document.createElement('option');
            option.value = day;
            option.textContent = day;
            daySelect.appendChild(option);
        }
    }
    
    // 监听年月变化，更新日期选项
    yearSelect.addEventListener('change', updateDays);
    monthSelect.addEventListener('change', updateDays);
    
    // 初始化日期选项
    updateDays();
    
    // 如果用户有生日数据，设置选中值
    if (window.mockUserData.birthday) {
        const birthdayDate = new Date(window.mockUserData.birthday);
        yearSelect.value = birthdayDate.getFullYear();
        monthSelect.value = birthdayDate.getMonth() + 1;
        
        // 触发年月变化事件，更新日期选项
        yearSelect.dispatchEvent(new Event('change'));
        
        // 设置日期
        daySelect.value = birthdayDate.getDate();
    }
}

// 为测试用户添加生日数据
window.testUsers.forEach(user => {
    if (!user.birthday) {
        // 随机生成一个生日（1980-2000年之间）
        const year = Math.floor(Math.random() * (2000 - 1980 + 1)) + 1980;
        const month = Math.floor(Math.random() * 12) + 1;
        const day = Math.floor(Math.random() * 28) + 1; // 简化处理，避免月份天数问题
        
        // 格式化为YYYY-MM-DD
        const monthStr = month < 10 ? `0${month}` : `${month}`;
        const dayStr = day < 10 ? `0${day}` : `${day}`;
        user.birthday = `${year}-${monthStr}-${dayStr}`;
    }
});

// 禁用生日选择器
function disableBirthdaySelectors() {
    const birthYearSelect = document.getElementById('birthYear');
    const birthMonthSelect = document.getElementById('birthMonth');
    const birthDaySelect = document.getElementById('birthDay');
    
    birthYearSelect.disabled = true;
    birthMonthSelect.disabled = true;
    birthDaySelect.disabled = true;
}

// 更新签到显示函数
function updateSignInDisplay() {
    const signInDays = document.getElementById('signInDays');
    const days = document.querySelectorAll('.day');
    const makeupCards = document.getElementById('makeupCards');
    
    // 清除所有签到状态和连线
    days.forEach(day => {
        day.classList.remove('signed', 'today', 'future');
    });
    
    // 先标记所有已签到的天数
    const signedDays = window.mockUserData.signInDays || 0;
    for (let i = 1; i <= signedDays; i++) {
        const dayElement = document.querySelector(`.day[data-day="${i}"]`);
        if (dayElement) {
            dayElement.classList.add('signed');
        }
    }
    
    // 添加连线 - 遍历所有天数，如果当前天和前一天都已签到，则添加连线
    days.forEach((day, index) => {
        if (index > 0) { // 跳过第一天
            const prevDay = days[index - 1];
            if (prevDay.classList.contains('signed') && day.classList.contains('signed')) {
                day.style.setProperty('--line-opacity', '1');
            } else {
                day.style.setProperty('--line-opacity', '0');
            }
        }
    });
    
    // 更新连续签到天数显示
    if (signInDays) {
        signInDays.textContent = window.mockUserData.signInDays || 0;
    }
    
    // 更新补签卡数量
    if (makeupCards) {
        makeupCards.textContent = window.mockUserData.makeupCards || 0;
    }
    
    // 标记今天和未来的天数
    const currentDay = window.mockUserData.signInDays + 1;
    days.forEach(day => {
        const dayNum = parseInt(day.getAttribute('data-day'));
        if (dayNum === currentDay) {
            day.classList.add('today');
        } else if (dayNum > currentDay) {
            day.classList.add('future');
        }
    });
}

// 初始化积分商城
function initPointsMall() {
    // 商品数据
    const products = [
        // 基础兑换品（高频低价值）
        {
            id: 1,
            name: "10元代金券",
            description: "可抵扣任意消费10元",
            points: 100,
            limit: "每月限3张",
            image: "images/products/voucher.jpg",
            level: "basic"
        },
        {
            id: 2,
            name: "升杯券",
            description: "任意饮品免费升级大杯",
            points: 50,
            limit: "每月限5张",
            image: "images/products/upgrade.jpg",
            level: "basic"
        },
        {
            id: 3,
            name: "配送费减免券",
            description: "线上订单免配送费",
            points: 80,
            limit: "每月限3张",
            image: "images/products/delivery.jpg",
            level: "basic"
        },
        {
            id: 4,
            name: "8折饮品券",
            description: "任意饮品享8折优惠",
            points: 50,
            limit: "每月限2张",
            image: "images/products/discount.jpg",
            level: "basic"
        },
        // 中级兑换品（中频中价值）
        {
            id: 5,
            name: "品牌联名杯",
            description: "限量版联名设计保温杯",
            points: 1500,
            limit: "季度限量",
            image: "images/products/branded-cup.jpg",
            level: "silver"
        },
        {
            id: 6,
            name: "精选咖啡豆(200g)",
            description: "精选单一产地咖啡豆",
            points: 1200,
            limit: "月限量",
            image: "images/products/coffee-beans.jpg",
            level: "silver"
        },
        {
            id: 7,
            name: "买一赠一券",
            description: "购买任意饮品赠送同价位及以下饮品",
            points: 300,
            limit: "每月限1张",
            image: "images/products/buy-one-get-one.jpg",
            level: "silver"
        },
        {
            id: 8,
            name: "咖啡甜品套餐券",
            description: "任意咖啡+甜品组合优惠券",
            points: 350,
            limit: "每月限2张",
            image: "images/products/coffee-dessert.jpg",
            level: "silver"
        },
        {
            id: 9,
            name: "白银专享早鸟券",
            description: "工作日7-9点使用的特别优惠",
            points: 250,
            limit: "月限4张",
            image: "images/products/early-bird.jpg",
            level: "silver"
        },
        {
            id: 10,
            name: "咖啡渣环保盆栽套装",
            description: "咖啡渣再利用的环保种植套装",
            points: 800,
            limit: "季度限量（含种植指导）",
            image: "images/products/eco-planter.jpg",
            level: "silver"
        },
        {
            id: 11,
            name: "门店预留座位服务",
            description: "提前预约门店座位，无需排队",
            points: 400,
            limit: "周末/节假日可用，月限2次",
            image: "images/products/reserved-seat.jpg",
            level: "silver"
        },
        {
            id: 12,
            name: "联名艺术家杯垫",
            description: "与知名艺术家合作设计的限量杯垫",
            points: 600,
            limit: "系列收集（每月上新1款）",
            image: "images/products/artist-coaster.jpg",
            level: "silver"
        },
        // 高级兑换品（低频高价值）
        {
            id: 9,
            name: "咖啡拉花课程",
            description: "专业咖啡师教授拉花技巧",
            points: 3000,
            limit: "8折积分兑换",
            image: "images/products/latte-art.jpg",
            level: "gold"
        },
        {
            id: 10,
            name: "限量联名周边",
            description: "与知名设计师合作限量周边",
            points: 5000,
            limit: "季度更新",
            image: "images/products/limited-merch.jpg",
            level: "gold"
        },
        {
            id: 13,
            name: "高端手冲咖啡套装",
            description: "专业级手冲咖啡器具全套",
            points: 4500,
            limit: "年度限量100套",
            image: "images/products/pour-over-set.jpg",
            level: "gold"
        },
        {
            id: 14,
            name: "年度尊享特权卡",
            description: "一年内享受多项专属优惠与服务",
            points: 8888,
            limit: "每月专属福利",
            image: "images/products/privilege-card.jpg",
            level: "gold"
        },
        {
            id: 15,
            name: "明星咖啡师私教课程",
            description: "由知名咖啡师一对一指导",
            points: 7000,
            limit: "季度限3席位",
            image: "images/products/master-class.jpg",
            level: "gold"
        },
        {
            id: 5,
            name: "咖啡师上门服务",
            description: "专业咖啡师上门为您制作咖啡",
            points: 8000,
            limit: "年限2次",
            image: "images/products/barista.jpg",
            level: "black"
        },
        {
            id: 6,
            name: "品牌活动VIP席",
            description: "品牌活动专属VIP席位预留",
            points: 10000,
            limit: "优先预约",
            image: "images/products/vip-event.jpg",
            level: "black"
        },
        {
            id: 16,
            name: "咖啡庄园深度体验之旅",
            description: "前往顶级咖啡产地深度体验",
            points: 15000,
            limit: "年限1次（含往返机票）",
            image: "images/products/coffee-tour.jpg",
            level: "black"
        },
        {
            id: 17,
            name: "私人咖啡豆定制服务",
            description: "根据个人口味定制专属咖啡豆",
            points: 6000,
            limit: "季度限1次",
            image: "images/products/custom-beans.jpg",
            level: "black"
        },
        {
            id: 18,
            name: "典藏版镀金咖啡器具礼盒",
            description: "限量版镀金工艺咖啡器具套装",
            points: 12000,
            limit: "全球限量50套",
            image: "images/products/gold-coffee-set.jpg",
            level: "black"
        }
    ];

    // 渲染商品列表
    renderProducts(products);

    // 分类标签点击事件
    document.querySelectorAll('.category-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            // 移除所有标签的active类
            document.querySelectorAll('.category-tab').forEach(t => t.classList.remove('active'));
            // 为当前点击的标签添加active类
            this.classList.add('active');
            
            const category = this.getAttribute('data-category');
            
            // 根据选择的分类筛选商品
            if (category === 'all') {
                renderProducts(products);
            } else {
                const filteredProducts = products.filter(product => product.level === category);
                renderProducts(filteredProducts);
            }
        });
    });
}

// 渲染商品列表
function renderProducts(products) {
    const productGrid = document.querySelector('.product-grid');
    productGrid.innerHTML = '';
    
    products.forEach(product => {
        const productCard = document.createElement('div');
        productCard.className = `product-card ${product.level}-card`;
        productCard.setAttribute('data-level', product.level);
        
        // 根据会员等级设置不同的卡片样式
        let cardHTML = '';
        
        if (product.level === 'basic') {
            cardHTML = `
                <div class="product-image">
                    <img src="${product.image}" alt="${product.name}">
                    <span class="product-tag basic">基础商品</span>
                </div>
                <div class="product-info">
                    <h3>${product.name}</h3>
                    <p>${product.description}</p>
                    <div class="points-required">${product.points}积分</div>
                    <p class="exchange-limit">${product.limit || ''}</p>
                    <button class="exchange-btn basic-btn" data-points="${product.points}">立即兑换</button>
                </div>
            `;
        } else if (product.level === 'silver') {
            cardHTML = `
                <div class="product-image">
                    <img src="${product.image}" alt="${product.name}">
                    <span class="product-tag silver">白银专享</span>
                </div>
                <div class="product-info">
                    <h3>${product.name}</h3>
                    <p>${product.description}</p>
                    <div class="points-required">${product.points}积分</div>
                    <p class="exchange-limit">${product.limit || ''}</p>
                    <button class="exchange-btn silver-btn" data-points="${product.points}">立即兑换</button>
                </div>
            `;
        } else if (product.level === 'gold') {
            cardHTML = `
                <div class="product-image">
                    <img src="${product.image}" alt="${product.name}">
                    <span class="product-tag gold">黄金专享</span>
                </div>
                <div class="product-info">
                    <h3>${product.name}</h3>
                    <p>${product.description}</p>
                    <div class="points-required">${product.points}积分</div>
                    <p class="exchange-limit">${product.limit || ''}</p>
                    <button class="exchange-btn gold-btn" data-points="${product.points}">立即兑换</button>
                </div>
            `;
        } else if (product.level === 'black') {
            cardHTML = `
                <div class="product-image">
                    <img src="${product.image}" alt="${product.name}">
                    <span class="product-tag black">黑金专享</span>
                </div>
                <div class="product-info">
                    <h3>${product.name}</h3>
                    <p>${product.description}</p>
                    <div class="points-required">${product.points}积分</div>
                    <p class="exchange-limit">${product.limit || '限量供应'}</p>
                    <button class="exchange-btn black-btn" data-points="${product.points}">立即兑换</button>
                </div>
            `;
        }
        
        productCard.innerHTML = cardHTML;
        
        productGrid.appendChild(productCard);
    });
    
    // 更新兑换按钮状态
    updateExchangeButtonStatus();
}

// 获取等级文本
function getLevelText(level) {
    switch(level) {
        case 'basic': return '基础';
        case 'silver': return '白银';
        case 'gold': return '黄金';
        case 'black': return '黑金';
        default: return '';
    }
}

// 更新兑换按钮状态
function updateExchangeButtonStatus() {
    document.querySelectorAll('.product-card').forEach(card => {
        const btn = card.querySelector('.basic-btn, .silver-btn, .gold-btn, .black-btn, .exchange-btn');
        const requiredPoints = parseInt(btn.getAttribute('data-points'));
        const productLevel = card.getAttribute('data-level');
        
        // 检查会员等级权限
        let canExchange = false;
        switch (window.mockUserData.level) {
            case 'black':
                canExchange = true; // 黑金会员可以兑换所有商品
                break;
            case 'gold':
                canExchange = ['basic', 'silver', 'gold'].includes(productLevel);
                break;
            case 'silver':
                canExchange = ['basic', 'silver'].includes(productLevel);
                break;
            case 'basic':
                canExchange = productLevel === 'basic';
                break;
        }

        // 检查积分是否足够且有权限兑换
        if (requiredPoints > window.mockUserData.availablePoints || !canExchange) {
            btn.disabled = true;
            btn.textContent = !canExchange ? '等级不足' : '积分不足';
            btn.classList.add('disabled');
        } else {
            btn.disabled = false;
            btn.textContent = '立即兑换';
            btn.classList.remove('disabled');
        }
    });
}

// 添加CSS样式
const style = document.createElement('style');
style.textContent = `
    .points-header {
        background: linear-gradient(135deg, #f8f8f8, #e9e9e9);
        border-radius: 12px;
        padding: 20px;
        margin-bottom: 25px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        box-shadow: 0 3px 10px rgba(0,0,0,0.05);
        position: relative;
        overflow: hidden;
    }
    
    .points-header::before {
        content: '';
        position: absolute;
        top: -20px;
        right: -20px;
        width: 120px;
        height: 120px;
        background: radial-gradient(circle, rgba(0,0,0,0.03), transparent 70%);
        border-radius: 50%;
    }
    
    .current-points-container {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
    }
    
    .points-label {
        font-size: 18px;
        color: #555;
        margin-bottom: 8px;
        font-weight: 600;
        letter-spacing: 0.5px;
    }
    
    .current-points {
        font-size: 38px;
        font-weight: bold;
        color: #231e15;
        font-family: 'Arial', sans-serif;
        letter-spacing: 1px;
        text-shadow: 1px 1px 2px rgba(0,0,0,0.05);
    }
    
    .points-info {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 10px;
    }
    
    .points-validity {
        display: flex;
        align-items: center;
        gap: 8px;
    }
    
    .points-validity .label {
        color: #777;
        font-size: 14px;
    }
    
    .points-validity .value {
        color: #231e15;
        font-weight: 600;
        font-size: 14px;
    }
    
    .exchange-history a {
        display: inline-block;
        padding: 8px 15px;
        background-color: #231e15;
        color: white;
        text-decoration: none;
        border-radius: 20px;
        font-size: 14px;
        transition: all 0.3s ease;
    }
    
    .exchange-history a:hover {
        background-color: #000;
        transform: translateY(-2px);
    }
    
    .exchange-btn.disabled {
        background-color: #ccc;
        cursor: not-allowed;
    }
    
    @media (max-width: 768px) {
        .points-header {
            flex-direction: column;
            align-items: flex-start;
            gap: 15px;
        }
        
        .points-info {
            align-items: flex-start;
            width: 100%;
        }
    }
`;
document.head.appendChild(style); 
