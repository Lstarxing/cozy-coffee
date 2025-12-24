document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM Content Loaded'); // 调试日志
    
    const counters = document.querySelectorAll('.counter');
    console.log('Found counters:', counters.length); // 检查是否找到元素
    
    // 设置计数器动画
    counters.forEach(counter => {
        // 获取目标值
        const target = parseInt(counter.getAttribute('data-val'));
        
        // 设置动画参数
        const duration = 2000; // 动画持续时间（毫秒）
        const steps = 100; // 动画步数
        const stepValue = target / steps;
        let current = 0;
        
        // 创建动画
        const updateCounter = () => {
            if (current < target) {
                current += stepValue;
                counter.textContent = Math.round(current);
                requestAnimationFrame(updateCounter);
            } else {
                counter.textContent = target;
            }
        };
        
        // 创建观察者
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    updateCounter();
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.5 });
        
        // 开始观察
        observer.observe(counter);
    });
}); 