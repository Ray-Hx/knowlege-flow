import { noteReadingApi } from '@/api/noteReading'

class NoteReadingTracker {
    private currentLogId: number | null = null
    private currentNoteId: number | null = null
    private heartbeatInterval: number | null = null
    private isActive = false

    /**
     * 开始追踪笔记阅读
     */
    async start(noteId: number) {
        try {
            // 结束之前的阅读
            await this.stop()

            // 开始新的阅读记录
            const logId = await noteReadingApi.startReading(noteId)
            this.currentLogId = logId
            this.currentNoteId = noteId
            this.isActive = true

            // 启动心跳（每30秒更新一次）
            this.startHeartbeat()

            // 监听页面可见性变化
            this.setupVisibilityListener()

            // 监听页面卸载
            this.setupUnloadListener()
        } catch (error) {
            console.error('Failed to start note reading tracking', error)
        }
    }

    /**
     * 停止追踪
     */
    async stop() {
        if (this.currentLogId && this.isActive) {
            try {
                await noteReadingApi.endReading(this.currentLogId)
            } catch (error) {
                console.error('Failed to end reading', error)
            }
        }

        this.stopHeartbeat()
        this.currentLogId = null
        this.currentNoteId = null
        this.isActive = false
    }

    /**
     * 启动心跳
     */
    private startHeartbeat() {
        this.stopHeartbeat()

        this.heartbeatInterval = window.setInterval(() => {
            if (this.currentLogId && this.isActive && !document.hidden) {
                noteReadingApi.updateHeartbeat(this.currentLogId).catch(err => {
                    console.error('Heartbeat failed', err)
                })
            }
        }, 30000) // 每30秒发送一次心跳
    }

    /**
     * 停止心跳
     */
    private stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval)
            this.heartbeatInterval = null
        }
    }

    /**
     * 监听页面可见性
     */
    private setupVisibilityListener() {
        document.addEventListener('visibilitychange', this.handleVisibilityChange)
    }

    /**
     * 处理可见性变化
     */
    private handleVisibilityChange = () => {
        if (document.hidden) {
            // 页面隐藏，暂停追踪
            this.isActive = false
            this.stopHeartbeat()
        } else {
            // 页面可见，恢复追踪
            this.isActive = true
            this.startHeartbeat()
        }
    }

    /**
     * 监听页面卸载
     */
    private setupUnloadListener() {
        window.addEventListener('beforeunload', () => {
            if (this.currentLogId) {
                // 使用sendBeacon确保请求发送
                navigator.sendBeacon(
                    `/api/note-reading/${this.currentLogId}/end`,
                    JSON.stringify({})
                )
            }
        })
    }
}

export const noteReadingTracker = new NoteReadingTracker()
