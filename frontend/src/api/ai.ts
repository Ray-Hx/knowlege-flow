import request from '@/utils/request'

export interface NoteReference {
    noteId: number;
    title: string;
    snippet: string;
    score: number;
    sourceType?: 'NOTE' | 'CONTENT';
    subType?: 'NOTE' | 'UPLOAD' | 'FEED';
}

export interface Message {
    id: number;
    role: 'user' | 'assistant' | 'system';
    content: string;
    references?: NoteReference[];
    createdTime: string;
}

export interface Conversation {
    id: number;
    title: string;
    createdTime: string;
    updatedTime: string;
    lastMessage?: Message;
}

export interface DeepAnalysisResult {
    content: string;
    generatedTime: string;
}

export const aiApi = {
    createConversation(title: string = '新对话') {
        return request.post<any, Conversation>('/ai/conversations', null, { params: { title } })
    },

    getConversations(page = 1, size = 20) {
        return request.get<any, { records: Conversation[], total: number }>('/ai/conversations', {
            params: { page, size }
        })
    },

    deleteConversation(id: number) {
        return request.delete(`/ai/conversations/${id}`)
    },

    getMessages(conversationId: number) {
        return request.get<any, Message[]>(`/ai/conversations/${conversationId}/messages`)
    },

    sendMessage(conversationId: number, message: string) {
        return request.post<any, Message>('/ai/chat/send', {
            conversationId,
            content: message,  // 使用正确的字段名 content
            useRag: true       // 使用正确的字段名 useRag
        })
    },

    async streamMessage(
        conversationId: number,
        message: string,
        callbacks: {
            onMessage?: (text: string) => void,
            onReferences?: (refs: NoteReference[]) => void,
            onDone?: (msg: Message) => void,
            onError?: (err: any) => void
        }
    ) {
        const token = localStorage.getItem('token')
        const url = '/api/ai/chat/send/stream'

        const controller = new AbortController()
        // 如果8秒内没有收到任何数据块，则中止请求
        let firstChunkReceived = false
        const timeoutId = setTimeout(() => {
            if (!firstChunkReceived) {
                controller.abort()
            }
        }, 8000)

        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': token ? `Bearer ${token}` : ''
                },
                body: JSON.stringify({
                    conversationId,
                    content: message,
                    useRag: true
                }),
                signal: controller.signal
            })

            if (!response.ok) {
                clearTimeout(timeoutId)
                throw new Error(`HTTP ${response.status}: ${response.statusText}`)
            }

            const reader = response.body?.getReader()
            const decoder = new TextDecoder('utf-8')
            if (!reader) {
                clearTimeout(timeoutId)
                throw new Error('No readable stream')
            }

            let buffer = ''

            while (true) {
                const { done, value } = await reader.read()
                if (done) break

                if (!firstChunkReceived) {
                    firstChunkReceived = true
                    clearTimeout(timeoutId)
                }

                buffer += decoder.decode(value, { stream: true })

                // 处理 SSE 格式，每个事件由 \n\n 分隔
                let boundary = buffer.indexOf('\n\n')
                while (boundary !== -1) {
                    const block = buffer.substring(0, boundary)
                    buffer = buffer.substring(boundary + 2)

                    let eventName = 'message'
                    let data = ''

                    const lines = block.split('\n')
                    for (const line of lines) {
                        if (line.startsWith('event:')) {
                            eventName = line.substring(6).trim()
                        } else if (line.startsWith('data:')) {
                            data += (data ? '\n' : '') + line.substring(5).replace(/^ /, '')
                        }
                    }

                    if (data !== '') {
                        if (eventName === 'message' && callbacks.onMessage) {
                            callbacks.onMessage(data)
                        } else if (eventName === 'references' && callbacks.onReferences) {
                            try {
                                callbacks.onReferences(JSON.parse(data))
                            } catch (e) { }
                        } else if (eventName === 'done' && callbacks.onDone) {
                            try {
                                callbacks.onDone(JSON.parse(data))
                            } catch (e) { }
                        } else if (eventName === 'error' && callbacks.onError) {
                            callbacks.onError(new Error(data))
                        }
                    }

                    boundary = buffer.indexOf('\n\n')
                }
            }
        } catch (e: any) {
            clearTimeout(timeoutId)
            if (e.name === 'AbortError') {
                console.warn('SSE stream timed out (no data in 8s), will fall back to non-stream')
            }
            // 重新抛出，让调用方能捕获并回退
            throw e
        }
    },

    generateDeepAnalysis(content: string, targetType?: string, targetId?: number) {
        return request.post<any, DeepAnalysisResult>('/ai/analyze', { content, targetType, targetId })
    },

    // AI 写作助手
    polishText(text: string) {
        return request.post<any, string>('/ai/text/polish', { text })
    },

    translateText(text: string, targetLang: 'zh' | 'en' = 'en') {
        return request.post<any, string>('/ai/text/translate', { text, targetLang })
    },

    expandText(text: string) {
        return request.post<any, string>('/ai/text/expand', { text })
    }
}

