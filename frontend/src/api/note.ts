import request from '@/utils/request'

export interface NoteFolder {
    id: number;
    name: string;
    parentId?: number;
    children?: NoteFolder[];
}

export interface Note {
    id: number;
    title: string;
    summary?: string;
    folderId?: number;
    folderName?: string;
    tags?: string[];
    status: number;
    createdTime: string;
    updatedTime: string;
    authorId?: number;
    authorName?: string;
    authorAvatar?: string;
    isPublic?: number;
    viewCount?: number;
    likeCount?: number;
}

export interface NoteLink {
    id: number;
    title: string;
}

export interface ContentLink {
    id: number;
    title: string;
    url: string;
    sourceName?: string;
    summary?: string;
}

export interface NoteDetail extends Note {
    contentMd: string;
    outgoingLinks?: NoteLink[];
    incomingLinks?: NoteLink[];
    contentLinks?: ContentLink[];
    isLiked?: boolean;
}

export interface NoteVersion {
    id: number;
    noteId: number;
    title: string;
    contentMd?: string;
    createdTime: string;
}

export const noteApi = {
    // 获取文件夹树
    getFolders() {
        return request.get<any, NoteFolder[]>('/folders/tree')
    },

    // 创建文件夹
    createFolder(name: string, parentId?: number) {
        return request.post('/folders', { name, parentId })
    },

    // 获取笔记列表
    getNotes(params: { page: number; size: number; folderId?: number; keyword?: string }) {
        return request.get<any, { records: Note[], total: number }>('/notes', { params })
    },

    // 获取笔记详情
    getNoteDetail(id: number) {
        return request.get<any, NoteDetail>(`/notes/${id}`)
    },

    // 创建笔记
    createNote(data: { title: string; contentMd: string; folderId?: number; tags?: string[]; status?: number; isPublic?: number }) {
        return request.post<any, Note>('/notes', data)
    },

    // 更新笔记
    updateNote(id: number, data: Partial<NoteDetail>) {
        return request.put(`/notes/${id}`, data)
    },

    // 删除笔记
    deleteNote(id: number) {
        return request.delete(`/notes/${id}`)
    },

    // 同步所有笔记到知识库（RAG 向量索引）
    rebuildIndex() {
        return request.post<any, number>('/notes/rebuild-index')
    },

    // 获取笔记历史版本列表
    getNoteVersions(id: number) {
        return request.get<any, NoteVersion[]>(`/notes/${id}/versions`)
    },

    // 获取指定笔记历史版本详情
    getNoteVersionDetail(id: number, versionId: number) {
        return request.get<any, NoteVersion>(`/notes/${id}/versions/${versionId}`)
    },

    // 恢复到指定历史版本（版本回溯 / 时光机）
    restoreVersion(noteId: number, versionId: number) {
        return request.post<any, void>(`/notes/${noteId}/versions/${versionId}/restore`)
    }
}
