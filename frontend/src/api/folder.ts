import request from '@/utils/request'

export interface FolderDto {
    id: number
    name: string
    parentId?: number
    children?: FolderDto[]
    createdTime: string
}

export interface CreateFolderDto {
    name: string
    parentId?: number
}

export interface UpdateFolderDto {
    name?: string
    parentId?: number
}

/**
 * 获取文件夹树
 */
export function getFolderTree(): Promise<any> {
    return request.get('/folders/tree')
}

/**
 * 创建文件夹
 */
export function createFolder(data: CreateFolderDto): Promise<any> {
    return request.post('/folders', data)
}

/**
 * 更新文件夹
 */
export function updateFolder(id: number, data: UpdateFolderDto): Promise<any> {
    return request.put(`/folders/${id}`, data)
}

/**
 * 删除文件夹
 */
export function deleteFolder(id: number): Promise<any> {
    return request.delete(`/folders/${id}`)
}
