import request from '@/utils/request'

// ================ Interfaces ================

/**
 * AI生成的复习问题
 */
export interface ReviewQuestion {
    id: number
    question: string
    answer: string
}

/**
 * 待复习笔记（与后端 ReviewNoteDto 字段完全对齐）
 */
export interface ReviewNote {
    noteId: number
    title: string       // 后端字段名是 title 而非 noteTitle
    summary?: string
    nextReviewTime: string
    repetitionCount: number
    questions: ReviewQuestion[]
}

/**
 * 复习统计
 */
export interface SpacedStats {
    totalNotes: number
    dueToday: number
    reviewedToday: number
    masteredNotes: number
}

// ================ API ================

export const spacedApi = {
    /**
     * 获取待复习笔记列表
     */
    getDueNotes() {
        return request.get<any, ReviewNote[]>('/spaced/due')
    },

    /**
     * 提交复习结果
     */
    submitReview(noteId: number, quality: number) {
        return request.post(`/spaced/review/${noteId}`, { quality })
    },

    /**
     * 获取学习统计
     */
    getStats() {
        return request.get<any, SpacedStats>('/spaced/stats')
    },

    /**
     * 初始化复习队列（将所有笔记加入复习）
     */
    initReviewQueue() {
        return request.post('/spaced/init')
    },

    /**
     * 重新生成笔记的复习问题
     */
    regenerateQuestions(noteId: number) {
        return request.post<any, ReviewQuestion[]>(`/spaced/questions/${noteId}/regenerate`)
    }
}
