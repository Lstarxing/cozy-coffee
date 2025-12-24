// 根据滚动位置显示资质卡片
function showCredentials() {
    const credentialItems = document.querySelectorAll('.credential-item');
    
    credentialItems.forEach(item => {
        const itemTop = item.getBoundingClientRect().top;
        const windowHeight = window.innerHeight;
        
        if (itemTop < windowHeight * 0.8) {
            item.classList.add('show');
        }
    });
}

// 根据用户来源显示不同资质
function showRelevantCredentials() {
    const itCredentials = document.querySelectorAll('.it-credential');
    const coffeeCredentials = document.querySelectorAll('.coffee-credential');
    
    // 这里可以根据实际需求设置用户来源判断逻辑
    const isITUser = false;  // 示例：根据实际情况设置
    
    if (isITUser) {
        itCredentials.forEach(item => item.style.order = '1');
        coffeeCredentials.forEach(item => item.style.order = '2');
    }
}

// 修改展示逻辑
function showStoreViews() {
    const credentialItems = document.querySelectorAll('.credential-item');
    
    credentialItems.forEach((item, index) => {
        const itemTop = item.getBoundingClientRect().top;
        const windowHeight = window.innerHeight;
        
        if (itemTop < windowHeight * 0.8) {
            // 添加延迟动画效果
            setTimeout(() => {
                item.classList.add('show');
            }, index * 200); // 每个项目延迟200ms
        }
    });
}

// 监听滚动事件
window.addEventListener('scroll', showStoreViews);

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
    showRelevantCredentials();
    showStoreViews();

    const videoContainer = document.querySelector('.video-container');
    const heroVideo = document.getElementById('heroVideo');
    const playButton = document.querySelector('#playButton');
    const fullscreenContainer = document.querySelector('#fullscreenVideo');
    const fullVideo = document.querySelector('#fullVideo');
    const closeButton = document.querySelector('#closeButton');
    const body = document.body;

    // 自动播放背景视频
    heroVideo.play().catch(error => {
        console.log("Background video autoplay failed:", error);
    });

    // 点击播放按钮
    playButton.addEventListener('click', () => {
        // 暂停背景视频
        heroVideo.pause();
        // 显示全屏视频容器
        fullscreenContainer.classList.add('active');
        // 播放全屏视频
        fullVideo.play();
        // 禁止背景滚动
        body.classList.add('no-scroll');
    });

    // 点击关闭按钮
    closeButton.addEventListener('click', () => {
        // 暂停全屏视频
        fullVideo.pause();
        // 重置全屏视频时间
        fullVideo.currentTime = 0;
        // 隐藏全屏视频容器
        fullscreenContainer.classList.remove('active');
        // 恢复背景滚动
        body.classList.remove('no-scroll');
        // 继续播放背景视频
        heroVideo.play();
    });

    // ESC键关闭视频
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && fullscreenContainer.classList.contains('active')) {
            closeButton.click();
        }
    });

    // 视频播放结束时关闭
    fullVideo.addEventListener('ended', () => {
        closeButton.click();
    });
}); 