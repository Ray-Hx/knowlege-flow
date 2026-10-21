import request from '@/utils/request'

export interface NoteNode {
    id: number
    title: string
    userId: number
    links: NoteNode[]
    related: {
        target: NoteNode
        label: string
    }[]
}

export const graphApi = {
    // 获取用户个人知识图谱
    getGraph() {
        return request.get<NoteNode[]>('/graph/user')
    },

    // 从 MySQL 双向链接事实表回填并重建 Neo4j 显式关系
    rebuildGraph() {
        return request.post<number>('/graph/rebuild')
    },

    // 获取社区星空图谱
    getCommunityGraph(limit: number = 300) {
        return request.get<NoteNode[]>('/graph/community/galaxy', {
            params: { limit }
        })
    },

    // (之前可能存在)
    analyzeRelations() {
        return request.post<number>('/graph/analyze')
    }
}
