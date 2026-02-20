import request from '@/utils/request'

export interface LoginParams {
    username: string;
    password?: string;
    captchaKey?: string;
    captchaCode?: string;
}

export interface RegisterParams {
    username: string;
    password?: string;
    email?: string; // Backend expects email for validation
    captchaKey?: string;
    captchaCode?: string;
}

export interface UserInfo {
    id: number;
    username: string;
    nickname?: string; // Optional as backend uses username/email often
    email?: string;
    avatar?: string;
    bio?: string;
    roles?: string[];
}

export interface CaptchaResult {
    captchaKey: string;
    captchaImage: string;
}

export const authApi = {
    // 登录
    login(data: LoginParams) {
        return request.post<any, string>('/auth/login', data)
    },

    // 注册
    register(data: RegisterParams) {
        return request.post<any, any>('/auth/register', data)
    },

    // 获取用户信息
    getUserInfo() {
        return request.get<any, UserInfo>('/user/profile')
    },

    // 获取验证码
    getCaptcha() {
        return request.get<any, CaptchaResult>('/auth/captcha')
    }
}
