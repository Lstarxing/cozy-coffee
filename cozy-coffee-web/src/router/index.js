import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'

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
            component: Home
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
            name: 'member',
            component: () => import('@/views/Member.vue'),
            meta: { requiresAuth: true, hideNavBar: true, hideFooter: true }
        }
    ]
})

// 路由守卫：检查需要登录的页面
router.beforeEach((to, from, next) => {
    if (to.meta.requiresAuth) {
        const token = localStorage.getItem('token')
        if (!token) {
            next({ name: 'login' })
            return
        }
    }
    next()
})

export default router

