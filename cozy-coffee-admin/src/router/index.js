import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue')
    },
    {
        path: '/',
        component: () => import('../layouts/AdminLayout.vue'),
        redirect: '/dashboard',
        meta: { requiresAuth: true },
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/Dashboard.vue'),
                meta: { title: '控制台' }
            },
            {
                path: 'users',
                name: 'Users',
                component: () => import('../views/Users.vue'),
                meta: { title: '用户管理' }
            },
            {
                path: 'products',
                name: 'Products',
                component: () => import('../views/Products.vue'),
                meta: { title: '商品管理' }
            },
            {
                path: 'orders',
                name: 'Orders',
                component: () => import('../views/Orders.vue'),
                meta: { title: '订单管理' }
            },
            {
                path: 'redemptions',
                name: 'Redemptions',
                component: () => import('../views/Redemptions.vue'),
                meta: { title: '兑换管理' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('adminToken')
    if (to.meta.requiresAuth && !token) {
        next('/login')
    } else {
        next()
    }
})

export default router
