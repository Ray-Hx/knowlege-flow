import request from '@/utils/request'

export interface DashboardStats {
    totalNotes: number
    pendingNotes: number
    weeklyNewNotes: number
    todayNewNotes: number
}

export interface DailyStudy {
    date: string
    noteCount?: number
    reviewCount?: number
}

export interface TagStats {
    tag: string
    noteCount: number
}

export interface RecentNote {
    id: number
    title: string
    summary?: string
    updatedTime: string
    updatedTimeText: string
}

export const dashboardApi = {
    // 获取统计数据
    getStats() {
        return request.get<any, DashboardStats>('/dashboard/stats')
    },

    // 获取本周每日学习数据
    getWeeklyStudy() {
        return request.get<any, DailyStudy[]>('/dashboard/weekly')
    },

    // 获取月度日历数据
    getMonthlyCalendar(year?: number, month?: number) {
        return request.get<any, DailyStudy[]>('/dashboard/calendar', {
            params: { year, month }
        })
    },

    // 获取最近笔记
    getRecentNotes(limit = 5) {
        return request.get<any, RecentNote[]>('/dashboard/recent-notes', {
            params: { limit }
        })
    },

    // 获取全年热力图数据（365天）
    getYearlyHeatmap() {
        return request.get<any, DailyStudy[]>('/dashboard/yearly-heatmap')
    },

    // 获取标签分布（Top 10）
    getTagDistribution() {
        return request.get<any, TagStats[]>('/dashboard/tag-distribution')
    },

    // 获取近12周学习趋势
    getLearningTrend() {
        return request.get<any, DailyStudy[]>('/dashboard/learning-trend')
    }
}

