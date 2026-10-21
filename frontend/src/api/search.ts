import request from '@/utils/request'

export interface SearchResult {
    id: number;
    title: string;
    summary: string;
    folderName: string;
    updatedTime: string;
    highlights?: {
        title?: string[];
        summary?: string[];
        content?: string[];
    };
}

export interface Recommendation {
    id: number;
    title: string;
    summary: string;
    type: 'NOTE' | 'CONTENT';
    reason: string;
    tags: string[];
    updatedTime: string;
    url?: string;
}

export interface GlobalSearchResult {
    id: string;
    type: 'NOTE' | 'CONTENT';
    title: string;
    snippet: string;
    updateTime: string;
}

export const searchApi = {
    search(keyword: string, page = 1, size = 10) {
        return request.get<any, { content: SearchResult[], totalElements: number }>('/search/notes', {
            params: { keyword, page: page - 1, size } // Spring Page is 0-indexed
        })
    },

    getRecommendations(limit = 10) {
        return request.get<any, Recommendation[]>('/search/recommendations', {
            params: { limit }
        })
    },

    globalSearch(keyword: string) {
        return request.get<any, GlobalSearchResult[]>('/search/global', {
            params: { keyword }
        })
    }
}
