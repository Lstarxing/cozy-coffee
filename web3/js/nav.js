document.addEventListener('DOMContentLoaded', function() {
    // 获取所有导航链接
    const navLinks = document.querySelectorAll('.nav-links a');
    
    // 点击导航链接时更新激活状态
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            const href = this.getAttribute('href');
            
            // 如果是外部链接（不以#开头），使用默认行为
            if (!href.startsWith('#')) {
                return;
            }
            
            e.preventDefault(); // 只对内部链接阻止默认行为
            
            // 移除所有链接的active类
            navLinks.forEach(l => l.classList.remove('active'));
            // 为当前点击的链接添加active类
            this.classList.add('active');
            
            // 获取目标section的ID
            const targetId = href.substring(1);
            const targetSection = document.getElementById(targetId);
            
            // 平滑滚动到目标位置
            if (targetSection) {
                targetSection.scrollIntoView({
                    behavior: 'smooth'
                });
            }
        });
    });

    // 根据滚动位置更新激活状态
    function updateActiveLink() {
        const sections = document.querySelectorAll('section');
        const scrollPosition = window.scrollY + 100; // 添加偏移量，提前触发

        sections.forEach(section => {
            const sectionTop = section.offsetTop - 70; // 减去导航栏高度
            const sectionHeight = section.offsetHeight;
            const sectionId = section.getAttribute('id');
            
            if (scrollPosition >= sectionTop && scrollPosition < sectionTop + sectionHeight) {
                // 移除所有链接的active类
                navLinks.forEach(link => link.classList.remove('active'));
                
                // 为当前部分的链接添加active类
                const activeLink = document.querySelector(`.nav-links a[href="#${sectionId}"]`);
                if (activeLink) {
                    activeLink.classList.add('active');
                }
            }
        });
    }

    // 监听滚动事件，使用节流函数优化性能
    let isScrolling = false;
    window.addEventListener('scroll', function() {
        if (!isScrolling) {
            window.requestAnimationFrame(function() {
                updateActiveLink();
                isScrolling = false;
            });
            isScrolling = true;
        }
    });
    
    // 页面加载时检查一次
    updateActiveLink();
}); 