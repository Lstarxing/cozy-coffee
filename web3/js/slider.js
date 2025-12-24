document.addEventListener('DOMContentLoaded', function() {
    let slideIndex = 0;
    const slides = document.querySelectorAll('.slide');
    const dots = document.querySelectorAll('.dot');
    let slideTimer;
    let isTransitioning = false;  // 添加过渡状态标记

    // 初始化轮播图
    function initSlider() {
        // 显示第一张图片
        showSlides(slideIndex);
        // 为导航点添加点击事件
        dots.forEach((dot, index) => {
            dot.addEventListener('click', () => {
                if (isTransitioning || slideIndex === index) return;  // 避免重复点击
                slideIndex = index;
                showSlides(slideIndex);
            });
        });
        // 启动自动轮播
        startAutoSlide();
    }

    // 显示指定索引的幻灯片
    function showSlides(n) {
        if (isTransitioning) return;  // 如果正在过渡中，则返回
        isTransitioning = true;  // 设置过渡状态

        // 移除所有幻灯片和导航点的活动状态
        slides.forEach(slide => {
            slide.classList.remove('active');
        });
        dots.forEach(dot => dot.classList.remove('active'));

        // 显示当前幻灯片和激活对应导航点
        slides[n].classList.add('active');
        dots[n].classList.add('active');

        // 过渡完成后重置状态
        setTimeout(() => {
            isTransitioning = false;
        }, 800);  // 与 CSS 过渡时间匹配
    }

    // 切换到下一张幻灯片
    function nextSlide() {
        if (isTransitioning) return;  // 防止过渡中打断
        slideIndex++;
        if (slideIndex >= slides.length) {
            slideIndex = 0;
        }
        showSlides(slideIndex);
    }

    // 切换到上一张幻灯片
    function prevSlide() {
        if (isTransitioning) return;  // 防止过渡中打断
        slideIndex--;
        if (slideIndex < 0) {
            slideIndex = slides.length - 1;
        }
        showSlides(slideIndex);
    }

    // 箭头点击事件
    document.querySelector('.prev-arrow').addEventListener('click', (e) => {
        e.preventDefault();
        stopAutoSlide();
        prevSlide();
        startAutoSlide();
    });

    document.querySelector('.next-arrow').addEventListener('click', (e) => {
        e.preventDefault();
        stopAutoSlide();
        nextSlide();
        startAutoSlide();
    });

    // 添加键盘控制
    document.addEventListener('keydown', function(e) {
        if (e.key === 'ArrowLeft') {
            stopAutoSlide();
            prevSlide();
            startAutoSlide();
        } else if (e.key === 'ArrowRight') {
            stopAutoSlide();
            nextSlide();
            startAutoSlide();
        }
    });

    // 自动播放控制
    function startAutoSlide() {
        stopAutoSlide();  // 清除之前的定时器
        slideTimer = setInterval(nextSlide, 2000);
    }

    function stopAutoSlide() {
        clearInterval(slideTimer);
    }

    // 鼠标悬停控制
    const sliderContainer = document.querySelector('.hero-slider');
    sliderContainer.addEventListener('mouseenter', stopAutoSlide);
    sliderContainer.addEventListener('mouseleave', startAutoSlide);

    // 初始化轮播图
    initSlider();
}); 