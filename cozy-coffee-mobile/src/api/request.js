/**
 * HTTP 请求封装
 * 
 * 功能：
 * 1. 统一请求配置（baseURL、超时、headers）
 * 2. 自动携带 token
 * 3. 统一错误处理
 * 4. 支持 Mock 模式切换
 */

// ==================== 配置 ====================

const config = {
    // 后端 API 基础地址
    // #ifdef H5
    baseURL: '/api',
    // #endif
    // #ifndef H5
    baseURL: 'http://localhost:8080/api',
    // #endif

    // 请求超时时间（毫秒）
    timeout: 10000,

    // 是否启用 Mock 模式（开发阶段使用模拟数据）
    useMock: false
}

// ==================== 请求封装 ====================

/**
 * 发起 HTTP 请求
 * @param {Object} options - 请求配置
 * @param {string} options.url - 请求路径
 * @param {string} [options.method='GET'] - 请求方法
 * @param {Object} [options.data] - 请求数据
 * @param {boolean} [options.showLoading=false] - 是否显示加载提示
 * @returns {Promise<Object>} 响应数据
 */
export const request = (options) => {
    return new Promise((resolve, reject) => {
        // 显示加载提示
        if (options.showLoading) {
            uni.showLoading({ title: '加载中...', mask: true })
        }

        // 获取 token
        const token = uni.getStorageSync('token')

        uni.request({
            url: config.baseURL + options.url,
            method: options.method || 'GET',
            data: options.data || {},
            timeout: config.timeout,
            header: {
                'Content-Type': 'application/json',
                'Authorization': token ? `Bearer ${token}` : ''
            },
            success: (res) => {
                // 隐藏加载提示
                if (options.showLoading) {
                    uni.hideLoading()
                }

                // HTTP 状态码判断
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    // 业务成功（后端返回 code: 200）
                    if (res.data.code === 200 || res.data.success === true) {
                        resolve(res.data)
                    } else if (res.data.code === 401) {
                        // 未授权
                        uni.removeStorageSync('token')
                        uni.showToast({ title: '请重新登录', icon: 'none' })
                        reject(res.data)
                    } else {
                        // 业务错误，仍然 resolve 以便页面处理
                        resolve(res.data)
                    }
                } else if (res.statusCode === 401) {
                    // 未授权
                    uni.removeStorageSync('token')
                    uni.showToast({ title: '请重新登录', icon: 'none' })
                    reject(res.data || { code: 401, message: '未授权' })
                } else {
                    // HTTP 错误（404、500 等）
                    console.error(`HTTP Error: ${res.statusCode}`, res.data)
                    reject(res.data || { code: res.statusCode, message: 'HTTP Error' })
                }
            },
            fail: (err) => {
                if (options.showLoading) {
                    uni.hideLoading()
                }
                uni.showToast({ title: '网络错误，请重试', icon: 'none' })
                reject(err)
            }
        })
    })
}

// ==================== 便捷方法 ====================

export const get = (url, data, options = {}) => {
    return request({ url, method: 'GET', data, ...options })
}

export const post = (url, data, options = {}) => {
    return request({ url, method: 'POST', data, ...options })
}

export const put = (url, data, options = {}) => {
    return request({ url, method: 'PUT', data, ...options })
}

export const del = (url, data, options = {}) => {
    return request({ url, method: 'DELETE', data, ...options })
}

export default {
    request,
    get,
    post,
    put,
    del,
    config
}
