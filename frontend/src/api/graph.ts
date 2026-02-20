import request from '@/utils/request'

export interface GraphNode {
    id: number;
    name: string;
    label: string;
}

export interface GraphLink {
    source: number;
    target: number;
    type: string;
}

export interface GraphData {
    nodes: GraphNode[];
    links: GraphLink[];
}

export const graphApi = {
    // Get full graph data
    getGraph() {
        return request.get<any, GraphData>('/graph/user')
    },

    // Trigger AI analysis for relations
    analyzeGraph() {
        return request.post<any, number>('/graph/analyze')
    }
}
