import request from '@/utils/request'
import type { Note } from '@/api/note'

export interface PageResult<T> {
    records: T[]
    total: number
    pages: number
    current: number
    size: number
}

// 获取社区大厅推荐笔记 feed
export function getCommunityFeed(keyword: string, page: number, size: number) {
    return request.get<any, PageResult<Note>>('/community/feed', {
        params: { keyword, page, size }
    })
}

// 获取我关注的用户的动态 feed
export function getFollowingFeed(page: number, size: number) {
    return request.get<any, PageResult<Note>>('/community/feed/following', {
        params: { page, size }
    })
}

// 获取公开笔记详情
export function getPublicNoteDetail(id: number) {
    return request.get<any, any>(`/community/note/${id}`)
}

// 获取社区右侧热力排行榜
export function getRanking(limit: number = 10) {
    return request.get<any, Note[]>('/community/ranking', {
        params: { limit }
    })
}

// 获取全网动态知识图谱 (银河)
export function getGlobalGraph(limit: number = 300) {
    return request.get<any, any[]>('/community/galaxy', {
        params: { limit }
    })
}

// 点赞 / 取消点赞
export function toggleLike(noteId: number) {
    return request.post<any, any>(`/community/note/${noteId}/like`)
}

// 获取相关推荐公开笔记
export function getRelatedPublicNotes(id: number, limit: number = 5) {
    return request.get<any, Note[]>(`/community/note/${id}/related`, {
        params: { limit }
    })
}

// ============ 评论相关 ============

export interface NoteComment {
    id: number
    noteId: number
    parentId: number | null
    content: string
    userId: number
    authorName: string
    authorAvatar: string | null
    createdTime: string
    replies?: NoteComment[]
}

// 获取评论列表（无需登录）
export function getComments(noteId: number) {
    return request.get<any, NoteComment[]>(`/community/note/${noteId}/comments`)
}

// 发表评论（需登录）
export function addComment(noteId: number, content: string, parentId?: number) {
    return request.post<any, NoteComment>(`/community/note/${noteId}/comments`, {
        content,
        parentId: parentId ?? null
    })
}

// 删除评论（需登录，仅自己的）
export function deleteComment(noteId: number, commentId: number) {
    return request.delete(`/community/note/${noteId}/comments/${commentId}`)
}

// ============ 用户公开主页 ============

export interface UserPublicProfile {
    id: number
    username: string
    email: string
    avatar: string | null
    bio: string | null
    githubId: string | null
    learningGoals: string | null
    publicNoteCount: number
    totalViews: number
    totalLikes: number
    followerCount: number
    followingCount: number
    isFollowing: boolean
}

// 获取用户公开资料
export function getUserPublicProfile(userId: number) {
    return request.get<any, UserPublicProfile>(`/community/users/${userId}/profile`)
}

// 关注用户
export function followUser(userId: number) {
    return request.post<any, any>(`/community/users/${userId}/follow`)
}

// 取消关注用户
export function unfollowUser(userId: number) {
    return request.delete<any, any>(`/community/users/${userId}/unfollow`)
}

// 获取用户公开笔记
export function getUserPublicNotes(userId: number, page: number, size: number) {
    return request.get<any, PageResult<Note>>(`/community/users/${userId}/notes`, {
        params: { page, size }
    })
}

import type { DailyStudy } from '@/api/dashboard'

// 获取用户学习热力图
export function getUserHeatmap(userId: number) {
    return request.get<any, DailyStudy[]>(`/community/users/${userId}/heatmap`)
}
