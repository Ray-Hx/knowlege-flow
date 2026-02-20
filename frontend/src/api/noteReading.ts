import request from '@/utils/request'

export const noteReadingApi = {
    /**
     * 开始笔记阅读记录
     */
    startReading(noteId: number) {
        return request.post<any, number>('/note-reading/start', { noteId })
    },

    /**
     * 更新心跳
     */
    updateHeartbeat(logId: number) {
        return request.put(`/note-reading/${logId}/heartbeat`)
    },

    /**
     * 结束阅读
     */
    endReading(logId: number) {
        return request.post(`/note-reading/${logId}/end`)
    }
}
