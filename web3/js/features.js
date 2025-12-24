// 添加新的JS文件处理动画效果
document.addEventListener('DOMContentLoaded', () => {
    // 初始化AOS动画库
    AOS.init({
        duration: 1000,
        once: true
    });

    // 数字计数动画
    const counters = document.querySelectorAll('.counter');
    const observerOptions = {
        threshold: 0.5
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const counter = entry.target;
                const target = parseInt(counter.textContent);
                let count = 0;
                const duration = 2000; // 动画持续2秒
                const increment = target / (duration / 16); // 60fps

                function updateCount() {
                    count += increment;
                    if (count < target) {
                        counter.textContent = Math.floor(count);
                        requestAnimationFrame(updateCount);
                    } else {
                        counter.textContent = target;
                    }
                }

                counter.classList.add('visible');
                updateCount();
                observer.unobserve(counter);
            }
        });
    }, observerOptions);

    counters.forEach(counter => observer.observe(counter));

    // 为每个产区卡片添加图片切换功能
    const originCards = document.querySelectorAll('.origin-card');
    console.log('Found cards:', originCards.length); // 检查是否找到卡片
    
    // 添加中文到英文的映射
    const regionMapping = {
        '巴西': 'brazil',
        '埃塞俄比亚': 'ethiopia',
        '哥伦比亚': 'colombia'
    };

    // 定义每个产区对应的咖啡豆包装图片
    const regionImages = {
        'brazil': 'images/cafebeans/brazil.png',
        'ethiopia': 'images/cafebeans/ethiopia.png',
        'colombia': 'images/cafebeans/colombia.png'
    };

    originCards.forEach(card => {
        const img = card.querySelector('.card-image img');
        const originalSrc = img.src;
        let isPackageShown = false;

        // 鼠标进入时切换到包装图片
        card.addEventListener('mouseenter', () => {
            const chineseRegion = img.alt.split('产区')[0];
            const region = regionMapping[chineseRegion];
            const packageImage = regionImages[region];

            if (!packageImage) return;

            // 每次进入时都重置类
            img.classList.remove('fade-in');
            img.classList.add('fade-out');
            
            // 使用 requestAnimationFrame 确保过渡效果
            requestAnimationFrame(() => {
                setTimeout(() => {
                    img.src = packageImage;
                    requestAnimationFrame(() => {
                        img.classList.remove('fade-out');
                        img.classList.add('fade-in');
                    });
                }, 200);
            });
            
            isPackageShown = true;
        });

        // 鼠标离开时恢复原图
        card.addEventListener('mouseleave', () => {
            if (!isPackageShown) return;
            
            // 每次离开时都重置类
            img.classList.remove('fade-in');
            img.classList.add('fade-out');
            
            // 使用 requestAnimationFrame 确保过渡效果
            requestAnimationFrame(() => {
                setTimeout(() => {
                    img.src = originalSrc;
                    requestAnimationFrame(() => {
                        img.classList.remove('fade-out');
                        img.classList.add('fade-in');
                    });
                }, 200);
            });
            
            isPackageShown = false;
        });
    });
}); 