import { createRouter, createWebHistory } from 'vue-router'

function isTokenExpired(token) {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        return payload.exp * 1000 < Date.now()
    } catch {
        return true
    }
}

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
                meta: { title: '控制台', roles: ['admin', 'super_admin'] }
            },
            {
                path: 'users',
                name: 'Users',
                component: () => import('../views/Users.vue'),
                meta: { title: '用户管理', roles: ['admin', 'super_admin'] }
            },
            {
                path: 'users/:id',
                name: 'UserDetail',
                component: () => import('../views/UserDetail.vue'),
                meta: { title: '用户详情', roles: ['admin', 'super_admin'] }
            },
            {
                path: 'products',
                redirect: '/products/coffee',
                meta: { title: '商品管理' }
            },
            {
                path: 'products/coffee',
                name: 'CoffeeProducts',
                component: () => import('../views/products/CoffeeProducts.vue'),
                meta: { title: '咖啡菜单', roles: ['admin', 'super_admin'] }
            },
            {
                path: 'products/points',
                name: 'PointsProducts',
                component: () => import('../views/products/PointsProducts.vue'),
                meta: { title: '积分商品', roles: ['admin', 'super_admin'] }
            },
            {
                path: 'orders',
                name: 'Orders',
                component: () => import('../views/Orders.vue'),
                meta: { title: '订单管理', roles: ['admin', 'super_admin'] }
            },
            {
                path: 'redemptions',
                name: 'Redemptions',
                component: () => import('../views/Redemptions.vue'),
                meta: { title: '兑换管理', roles: ['admin', 'super_admin'] }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：校验 token 过期 + RBAC 角色
router.beforeEach((to, from, next) => {
    if (to.meta.requiresAuth) {
        const token = localStorage.getItem('adminToken')
        if (!token || isTokenExpired(token)) {
            localStorage.removeItem('adminToken')
            localStorage.removeItem('adminInfo')
            next('/login')
            return
        }

        // RBAC 角色校验
        const userRole = (() => {
            try {
                const info = localStorage.getItem('adminInfo')
                return info ? JSON.parse(info).role : null
            } catch { return null }
        })()
        const requiredRoles = to.meta.roles
        if (requiredRoles && !requiredRoles.includes(userRole)) {
            next('/dashboard')
            return
        }
    }
    next()
})

export default router
