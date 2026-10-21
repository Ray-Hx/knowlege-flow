import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
    state: () => ({
        token: localStorage.getItem('token') || '',
        userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
        unreadMessageCount: 0
    }),
    getters: {
        isLoggedIn: (state) => !!state.token
    },
    actions: {
        setToken(token: string) {
            this.token = token
            localStorage.setItem('token', token)
        },
        setUserInfo(info: any) {
            this.userInfo = info
            localStorage.setItem('userInfo', JSON.stringify(info))
        },
        logout() {
            this.token = ''
            this.userInfo = {}
            this.unreadMessageCount = 0
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
        },
        async fetchUnreadMessageCount() {
            if (!this.token) return
            try {
                const { getUnreadTotal } = await import('@/api/message')
                const res = await getUnreadTotal()
                this.unreadMessageCount = res || 0
            } catch (e) {
                // Ignore error silently
            }
        }
    }
})
