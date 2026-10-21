import request from '@/utils/request'
import type { PageResult } from '@/api/community'

export interface UserMessageDto {
    id: number
    senderId: number
    senderName: string
    senderAvatar: string | null
    receiverId: number
    receiverName: string
    receiverAvatar: string | null
    content: string
    isRead: number
    createdTime: string
}

export interface MessageConversationDto {
    contactId: number
    contactName: string
    contactAvatar: string | null
    lastMessage: string
    lastTime: string
    unreadCount: number
}

// 发送私信
export function sendMessage(receiverId: number, content: string) {
    return request.post<any, void>('/messages/send', { receiverId, content })
}

// 获取某人聊天历史
export function getChatHistory(contactId: number, page: number = 1, size: number = 20) {
    return request.get<any, PageResult<UserMessageDto>>(`/messages/history/${contactId}`, {
        params: { page, size }
    })
}

// 获取会话列表
export function getConversations() {
    return request.get<any, MessageConversationDto[]>('/messages/conversations')
}

// 标记为已读
export function markAsRead(contactId: number) {
    return request.post<any, void>(`/messages/read/${contactId}`)
}

// 获取全局未读条数
export function getUnreadTotal() {
    return request.get<any, number>('/messages/unread')
}
