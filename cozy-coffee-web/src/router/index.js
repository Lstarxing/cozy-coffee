import { createRouter, createWebHistory, START_LOCATION } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    scrollBehavior(to, from, savedPosition) {
        // 初始导航（页面刷新或第一次进入）：from 为 START_LOCATION，matched 为空
        // 带 hash 时必须滚动到目标 section，否则刷新 /#menu /#origins /#membership 会停在顶
        const isInitialNavigation = from === START_LOCATION

        // 同页 hash 跳转（click 触发）：NavBar 已用原生 scrollIntoView 处理，
        // router 不参与滚动，否则会回顶覆盖已发生的平滑滚动
        if (to.hash && to.path === from.path && !isInitialNavigation) {
            return false
        }
        // 跨页 hash 跳转（如 /about → /#origins）与 初始带 hash 导航（刷新）：
        // 等首页组件挂载后再滚
        if (to.hash && (to.path !== from.path || isInitialNavigation)) {
            return new Promise(resolve => {
                requestAnimationFrame(() => {
                    requestAnimationFrame(() => {
                        const el = document.querySelector(to.hash)
                        if (el) {
                            el.scrollIntoView({ behavior: 'smooth', block: 'start' })
                        }
                        resolve(false)
                    })
                })
            })
        }
        if (savedPosition) {
            return savedPosition
        }
        return { top: 0 }
    },
    routes: [
        {
            path: '/',
            name: 'home',
            component: () => import('@/views/Home.vue')
        },
        {
            path: '/about',
            name: 'about',
            component: () => import('@/views/About.vue')
        },
        {
            path: '/login',
            name: 'login',
            component: () => import('@/views/Login.vue'),
            meta: { hideFooter: true }
        },
        {
            path: '/register',
            name: 'register',
            component: () => import('@/views/Register.vue'),
            meta: { hideFooter: true }
        },
        {
            path: '/member',
            component: () => import('@/views/member/MemberLayout.vue'),
            meta: { requiresAuth: true, hideNavBar: true, hideFooter: true },
            redirect: '/member/center',
            children: [
                { path: 'center', name: 'member-center', component: () => import('@/views/member/MemberCenter.vue') },
                { path: 'order', name: 'coffee-order', component: () => import('@/views/member/CoffeeOrder.vue') },
                { path: 'mall', name: 'points-mall', component: () => import('@/views/member/PointsMall.vue') },
                { path: 'orders/coffee', name: 'coffee-orders-history', component: () => import('@/views/member/CoffeeOrdersHistory.vue') },
                { path: 'orders/redeem', name: 'redeem-orders-history', component: () => import('@/views/member/RedeemOrdersHistory.vue') },
                { path: 'profile', name: 'personal-info', component: () => import('@/views/member/PersonalInfo.vue') },
                { path: 'coupons', name: 'my-coupons', component: () => import('@/views/member/MyCoupons.vue') },
                { path: 'benefits', name: 'member-benefits', component: () => import('@/views/member/MemberBenefits.vue') },
            ]
        }
    ]
})

// 路由守卫：通过 /api/auth/me 验证 cookie 登录状态
router.beforeEach(async (to, from, next) => {
    if (to.meta.requiresAuth) {
        const userStore = useUserStore()
        await userStore.init()
        if (userStore.isLoggedIn) {
            next()
            return
        }
        next({ name: 'login', query: { redirect: to.fullPath } })
        return
    }
    next()
})

export default router
