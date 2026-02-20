import request from '@/utils/request'

export interface NoteReference {
    noteId: number;
    title: string;
    snippet: string;
    score: number;
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

