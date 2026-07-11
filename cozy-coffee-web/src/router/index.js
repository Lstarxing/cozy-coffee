import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition
        } else {
            return { top: 0 }
        }
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
        try {
            const baseURL = (import.meta.env.VITE_API_BASE_URL || '') + '/api'
            const response = await fetch(baseURL + '/auth/me', { credentials: 'include' })
            if (response.ok) {
                next()
                return
            }
        } catch (e) {
            // 网络错误，跳转登录
        }
        next({ name: 'login', query: { redirect: to.fullPath } })
        return
    }
    next()
})

export default router
