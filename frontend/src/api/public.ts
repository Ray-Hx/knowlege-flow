import request from '@/utils/request'
import { NoteLinkDto } from './note' // Assuming NoteLinkDto or similar exists, or redefine

export interface PublicUserProfile {
    username: string;
    nickname?: string;
    avatar?: string;
    bio?: string;
    learningGoals?: string;
}

export interface PublicNote {
    id: number;
    title: string;
    summary: string;
    folderName?: string;
    createdTime: string;
    updatedTime: string;
}

export const publicApi = {
    getProfile(username: string) {
        return request.get<any, PublicUserProfile>(`/public/profile/${username}`)
    },
    listNotes(username: string, page = 1, size = 10) {
        return request.get<any, { content: PublicNote[], totalElements: number }>(`/public/notes/${username}`, {
            params: { page, size }
        })
    }
}
