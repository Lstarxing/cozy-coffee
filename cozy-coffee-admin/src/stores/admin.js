import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAdminStore = defineStore('admin', () => {
    const adminInfo = ref(null)
    const isLoggedIn = ref(false)

    const login = async (username, password) => {
        // TODO: 实际登录API
        if (username === 'admin' && password === 'admin123') {
            adminInfo.value = { username, role: 'admin' }
            isLoggedIn.value = true
            localStorage.setItem('adminToken', 'mock-admin-token')
            localStorage.setItem('adminInfo', JSON.stringify(adminInfo.value))
            return true
        }
        return false
    }

    const logout = () => {
        adminInfo.value = null
        isLoggedIn.value = false
        localStorage.removeItem('adminToken')
        localStorage.removeItem('adminInfo')
    }

    const init = () => {
        const token = localStorage.getItem('adminToken')
        const info = localStorage.getItem('adminInfo')
        if (token && info) {
            adminInfo.value = JSON.parse(info)
            isLoggedIn.value = true
        }
    }

    return { adminInfo, isLoggedIn, login, logout, init }
})
