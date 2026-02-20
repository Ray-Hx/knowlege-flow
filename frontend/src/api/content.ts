import request from '@/utils/request'

export interface ContentSource {
    id?: number
    type: string
    name: string
    url: string
    syncInterval: number
    lastSyncTime?: string
    status: number
}

export interface ContentItem {
    id: number
    sourceId: number
    sourceName: string
    title: string
    url: string
    author?: string
    publishTime?: string
    summary?: string
    tags?: string[]
    status: number // 0-Unread, 1-Read, 2-Starred
    createdTime: string
}

export interface PageResult<T> {
    records: T[]
    total: number
    current: number
    size: number
}

export const contentApi = {
    // Sources
    listSources: () => {
        return request.get<any, ContentSource[]>('/content/sources')
    },
    addSource: (data: ContentSource) => {
        return request.post<any, ContentSource>('/content/sources', data)
    },
    updateSource: (id: number, data: ContentSource) => {
        return request.put<any, ContentSource>(`/content/sources/${id}`, data)
    },
    deleteSource: (id: number) => {
        return request.delete(`/content/sources/${id}`)
    },
    syncSource: (id: number) => {
        // 使用同步接口，立即获取结果
        return request.post(`/content/sources/${id}/sync-now`)
    },
    syncSourceAsync: (id: number) => {
        // 使用异步接口，通过消息队列处理
        return request.post(`/content/sources/${id}/sync`)
    },

    // Items
    listItems: (params: { page: number; size: number; status?: number; sourceId?: number; keyword?: string }) => {
        return request.get<any, PageResult<ContentItem>>('/content/items', { params })
    },
    getItem: (id: number) => {
        return request.get<any, ContentItem>(`/content/items/${id}`)
    },
    updateStatus: (id: number, status: number) => {
        return request.put(`/content/items/${id}/status`, null, { params: { status } })
    },
    deleteItem: (id: number) => {
        return request.delete(`/content/items/${id}`)
    }
}
