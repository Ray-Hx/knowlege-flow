import request from '@/utils/request'

export interface UserProfile {
    id: number
    username: string
    email: string
    avatar: string
    bio: string
    githubId: string
    learningGoals: string
}

export interface UpdateProfile {
    email?: string
    bio?: string
    githubId?: string
    learningGoals?: string
}

export interface ChangePassword {
    oldPassword?: string
    newPassword?: string
}

export const userApi = {
    getProfile: () => {
        return request.get<any, UserProfile>('/user/profile')
    },
    updateProfile: (data: UpdateProfile) => {
        return request.put<any, UserProfile>('/user/profile', data)
    },
    uploadAvatar: (file: File) => {
        const formData = new FormData()
        formData.append('file', file)
        return request.post<any, string>('/user/profile/avatar', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
    },
    changePassword: (data: ChangePassword) => {
        return request.post<any, void>('/user/profile/password', data)
    }
}
