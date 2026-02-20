import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { useUserStore } from '@/store/user'
import { createDiscreteApi } from 'naive-ui'

const { message } = createDiscreteApi(['message'])

const request: AxiosInstance = axios.create({
    baseURL: '/api',
    timeout: 120000, // 2 分钟超时，AI 响应可能需要较长时间
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器
request.interceptors.request.use(
    (config) => {
        const userStore = useUserStore()
        if (userStore.token) {
            config.headers.Authorization = `Bearer ${userStore.token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

import { matchMockData } from '@/api/mockData'

// ... (keep imports)

// ... (keep creates)

// ... (keep request interceptor)

// 响应拦截器
request.interceptors.response.use(
    (response: AxiosResponse) => {
        // Backend Result<T> structure: { code: number, message: string, data: T }
        // But some older code might return { code, msg, data }
        const { code, data } = response.data
        const msg = response.data.message || response.data.msg
        // 假设后端返回格式: { code: 200, msg: 'success', data: ... }
        // Adjust for direct return if backend doesn't wrap in code/msg or if mock data is direct
        if (code === 200) {
            return data
        } else if (code === undefined && data === undefined) {
            // Maybe it's a direct response or mock data
            return response.data
        } else {
            return Promise.reject(new Error(msg || '请求失败'))
        }
    },
    async (error) => {
        // Fallback to Mock Data if Network Error
        if (!error.response || error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
            console.warn('[Network Error] Switching to Mock Data for:', error.config.url)
            const mockData = matchMockData(error.config.url, error.config.method, error.config.data)
            if (mockData) {
                message.warning('这里是预览效果（后端未连接），使用模拟数据')
                // Return a structure mimicking a successful Axios response data extraction
                // The interceptor above expects { code: 200, data: ... } or direct data. 
                // Since interface expects specific type, let's return the data directly if the resolve handler expects it.
                // Looking at success handler: it returns `data` from `response.data`.
                // So we should pretend this is the inner data.
                return mockData
            }
        }

        let msg = 'Network Error'
        if (error.response) {
            const status = error.response.status
            switch (status) {
                case 401:
                    msg = '未授权，请重新登录'
                    // Avoid redirect loops if mock is allowed
                    if (!window.location.pathname.includes('/login')) {
                        const userStore = useUserStore()
                        userStore.logout()
                        // window.location.reload() // Don't reload immediately, allow mock viewing if desired? 
                        // Actually strict auth requirement usually means redirect. 
                        // But for demo/mock, maybe just show warning.
                    }
                    break
                case 403:
                    msg = '拒绝访问'
                    break
                case 404:
                    // Try Mock on 404 too? Maybe not. 404 implies backend is there but path is wrong.
                    msg = '资源不存在'
                    break
                case 500:
                    msg = '服务器内部错误'
                    break
                default:
                    msg = error.response.data?.message || error.response.data?.msg || `请求错误 ${status}`
            }
        }
        return Promise.reject(new Error(msg))
    }
)

export default request
