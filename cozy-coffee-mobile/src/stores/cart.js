/**
 * 购物车状态管理 (Pinia Store)
 * 
 * 功能：
 * 1. 添加/移除商品
 * 2. 增减商品数量
 * 3. 计算总价和总数量
 * 4. 持久化到本地存储
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCartStore = defineStore('cart', () => {
    // ==================== State ====================

    /**
     * 购物车商品列表
     * 结构: [{ id, name, price, image, quantity }, ...]
     */
    const items = ref([])

    // ==================== Getters (Computed) ====================

    /**
     * 计算购物车商品总数量
     */
    const totalCount = computed(() => {
        return items.value.reduce((sum, item) => sum + item.quantity, 0)
    })

    /**
     * 计算购物车总价
     * 返回格式化的字符串（保留两位小数）
     */
    const totalPrice = computed(() => {
        const total = items.value.reduce((sum, item) => {
            return sum + (parseFloat(item.price) * item.quantity)
        }, 0)
        return total.toFixed(2)
    })

    // ==================== Actions ====================

    /**
     * 添加商品到购物车
     * @param {Object} product - 商品对象 { id, name, price, image }
     * 
     * 逻辑说明：
     * - 如果购物车中已存在该商品，则数量 +1
     * - 如果不存在，则新增一条记录，数量为 1
     */
    const addItem = (product) => {
        const existingItem = items.value.find(item => item.id === product.id)

        if (existingItem) {
            // 已存在，数量 +1
            existingItem.quantity++
        } else {
            // 不存在，新增记录
            items.value.push({
                ...product,
                quantity: 1
            })
        }

        // 持久化到本地
        saveToStorage()
    }

    /**
     * 增加指定商品的数量
     * @param {number|string} productId - 商品 ID
     */
    const increaseQty = (productId) => {
        const item = items.value.find(item => item.id === productId)
        if (item) {
            item.quantity++
            saveToStorage()
        }
    }

    /**
     * 减少指定商品的数量
     * @param {number|string} productId - 商品 ID
     * 
     * 逻辑说明：
     * - 数量减到 0 时，自动从购物车移除该商品
     */
    const decreaseQty = (productId) => {
        const index = items.value.findIndex(item => item.id === productId)
        if (index !== -1) {
            if (items.value[index].quantity > 1) {
                items.value[index].quantity--
            } else {
                // 数量为 1，减少后移除
                items.value.splice(index, 1)
            }
            saveToStorage()
        }
    }

    /**
     * 从购物车移除指定商品
     * @param {number|string} productId - 商品 ID
     */
    const removeItem = (productId) => {
        const index = items.value.findIndex(item => item.id === productId)
        if (index !== -1) {
            items.value.splice(index, 1)
            saveToStorage()
        }
    }

    /**
     * 清空购物车
     */
    const clearCart = () => {
        items.value = []
        saveToStorage()
    }

    // ==================== 持久化 ====================

    /**
     * 保存购物车到本地存储
     */
    const saveToStorage = () => {
        uni.setStorageSync('cart', JSON.stringify(items.value))
    }

    /**
     * 从本地存储恢复购物车
     */
    const loadFromStorage = () => {
        try {
            const saved = uni.getStorageSync('cart')
            if (saved) {
                items.value = JSON.parse(saved)
            }
        } catch (e) {
            console.error('恢复购物车失败:', e)
        }
    }

    // 初始化时从本地恢复
    loadFromStorage()

    // ==================== 导出 ====================
    return {
        // State
        items,
        // Getters
        totalCount,
        totalPrice,
        // Actions
        addItem,
        increaseQty,
        decreaseQty,
        removeItem,
        clearCart
    }
})
