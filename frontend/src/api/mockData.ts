import { LearningPlan, LearningTask, Pomodoro } from './learning'
import { SpacedCard, SpacedStats, ReviewStatus } from './spaced'
import { Conversation, Message } from './ai'

// --- Mock Data Store ---

export const MOCK_USER = {
    id: 1,
    username: 'GuestUser',
    nickname: '访客用户',
    email: 'guest@example.com',
    avatar: '',
    role: 'user'
}

export const MOCK_NOTES = [
    {
        id: 1,
        title: 'Project KnowledgeFlow Architecture',
        content: '# Architecture\n\nThis is the architecture of KnowledgeFlow...',
        summary: 'Detailed architecture design.',
        tags: ['Architecture', 'Java'],
        folderId: 1,
        createTime: '2024-01-10T10:00:00',
        updateTime: '2024-01-12T14:30:00'
    },
    {
        id: 2,
        title: 'Vue 3 Best Practices',
        content: '# Vue 3\n\nUse Composition API...',
        summary: 'Guide for Vue 3 development.',
        tags: ['Frontend', 'Vue'],
        folderId: 2,
        createTime: '2024-01-11T09:00:00',
        updateTime: '2024-01-12T10:00:00'
    },
    {
        id: 3,
        title: 'Spring Boot 3.2 Features',
        content: '# Spring Boot 3.2\n\nVirtual Threads support...',
        summary: 'New features in Spring Boot 3.2.',
        tags: ['Backend', 'Java'],
        folderId: 1,
        createTime: '2024-01-13T16:20:00',
        updateTime: '2024-01-13T16:20:00'
    },
    {
        id: 4,
        title: 'React vs Vue Performance',
        content: '# Performance Comparison\n\n...',
        summary: 'Detailed benchmark.',
        tags: ['Frontend'],
        folderId: 2,
        createTime: '2024-01-14T11:00:00',
        updateTime: '2024-01-14T11:00:00'
    }
]

export const MOCK_PLANS: LearningPlan[] = [
    {
        id: 1,
        userId: 1,
        name: 'Java 高级并发编程',
        description: '深入理解 JUC，掌握高并发系统设计',
        startDate: '2024-01-01',
        endDate: '2024-02-01',
        status: 1,
        progress: 45,
        createTime: '2023-12-30T10:00:00',
        updateTime: '2024-01-15T10:00:00'
    },
    {
        id: 2,
        userId: 1,
        name: '前端架构师之路',
        description: 'Vue 3 源码分析与工程化实践',
        startDate: '2024-01-10',
        endDate: '2024-03-10',
        status: 1,
        progress: 20,
        createTime: '2024-01-05T10:00:00',
        updateTime: '2024-01-15T10:00:00'
    },
    {
        id: 3,
        userId: 1,
        name: '微服务架构设计',
        description: 'Spring Cloud Alibaba 全家桶',
        startDate: '2024-02-01',
        endDate: '2024-04-01',
        status: 0,
        progress: 0,
        createTime: '2024-01-15T10:00:00',
        updateTime: '2024-01-15T10:00:00'
    }
]

export const MOCK_TASKS: LearningTask[] = [
    {
        id: 1,
        planId: 1,
        userId: 1,
        title: '阅读《Java并发编程实战》前三章',
        description: '理解线程安全性',
        priority: 2,
        status: 2,
        dueDate: '2024-01-15',
        estimatedMinutes: 120,
        actualMinutes: 130,
        createTime: '2024-01-14T10:00:00',
        updateTime: '2024-01-15T20:00:00'
    },
    {
        id: 2,
        planId: 1,
        userId: 1,
        title: '实现一个简单的线程池',
        description: '手写 ThreadPoolExecutor',
        priority: 2,
        status: 0,
        dueDate: '2024-01-17',
        estimatedMinutes: 180,
        actualMinutes: 0,
        createTime: '2024-01-16T10:00:00',
        updateTime: '2024-01-16T10:00:00'
    },
    {
        id: 3,
        planId: 2,
        userId: 1,
        title: 'Vue 3 响应式原理分析',
        description: '阅读 reactivity 模块源码',
        priority: 1,
        status: 1,
        dueDate: '2024-01-18',
        estimatedMinutes: 240,
        actualMinutes: 60,
        createTime: '2024-01-16T14:00:00',
        updateTime: '2024-01-17T10:00:00'
    },
    {
        id: 4,
        planId: 1,
        userId: 1,
        title: 'ReentrantLock 源码分析',
        description: '理解 AQS 原理',
        priority: 2,
        status: 0,
        dueDate: '2024-01-19',
        estimatedMinutes: 150,
        actualMinutes: 0,
        createTime: '2024-01-17T09:00:00',
        updateTime: '2024-01-17T09:00:00'
    }
]

export const MOCK_POMODOROS: Pomodoro[] = [
    {
        id: 1,
        userId: 1,
        taskId: 1,
        startTime: '2024-01-17T09:00:00',
        endTime: '2024-01-17T09:25:00',
        duration: 25,
        status: 0
    },
    {
        id: 2,
        userId: 1,
        taskId: 1,
        startTime: '2024-01-17T09:30:00',
        endTime: '2024-01-17T09:55:00',
        duration: 25,
        status: 0
    }
]

export const MOCK_SPACED_CARDS: SpacedCard[] = [
    {
        id: 1,
        frontContent: 'Java 中 synchronized 关键字的锁升级过程是怎样的？',
        backContent: '无锁 -> 偏向锁 -> 轻量级锁 -> 重量级锁。\n\n1. 偏向锁：各种判断，CAS 修改 Mark Word。\n2. 轻量级锁：栈帧中创建 Lock Record，CAS 指向。\n3. 重量级锁：Monitor 机制，阻塞线程。',
        tags: ['Java', 'Concurrency'],
        createdTime: '2024-01-10T10:00:00',
        reviewStatus: {
            nextReviewTime: '2024-01-17T10:00:00',
            intervalDays: 1,
            easinessFactor: 2.5,
            repetitionCount: 1,
            isDueToday: true
        }
    },
    {
        id: 2,
        frontContent: 'Vue 3 中 ref 和 reactive 的区别？',
        backContent: 'ref: 用于基本类型（也可以对象），通过 .value 访问，模板自动解包。\nreactive: 用于对象/数组，基于 Proxy，不能解构（会丢失响应性）。',
        tags: ['Vue'],
        createdTime: '2024-01-11T10:00:00',
        reviewStatus: {
            nextReviewTime: '2024-01-17T12:00:00',
            intervalDays: 1,
            easinessFactor: 2.5,
            repetitionCount: 0,
            isDueToday: true
        }
    },
    {
        id: 3,
        frontContent: 'CAP 定理中 P 代表什么？',
        backContent: 'Partition Tolerance（分区容错性）。指分布式系统在遇到任何网络分区故障的时候，仍然能够保证对外提供满足一致性和可用性的服务。',
        tags: ['System Design'],
        createdTime: '2024-01-12T10:00:00',
        reviewStatus: {
            nextReviewTime: '2024-01-18T10:00:00',
            intervalDays: 2,
            easinessFactor: 2.6,
            repetitionCount: 2,
            isDueToday: false
        }
    }
]

export const MOCK_SPACED_STATS: SpacedStats = {
    totalCards: 125,
    dueToday: 12,
    reviewedToday: 5,
    masteredCards: 45,
    totalMinutes: 320
}

export const MOCK_CONVERSATIONS: Conversation[] = [
    {
        id: 1,
        title: 'Java 并发编程学习助手',
        createdTime: '2024-01-16T10:00:00',
        updatedTime: '2024-01-16T10:30:00'
    },
    {
        id: 2,
        title: '解释 CAP 定理',
        createdTime: '2024-01-17T09:00:00',
        updatedTime: '2024-01-17T09:05:00'
    }
]

// Extended Mock Message type to include conversationId for internal filtering
interface MockMessage extends Message {
    conversationId: number
}

export const MOCK_MESSAGES: MockMessage[] = [
    {
        id: 1,
        conversationId: 1,
        role: 'user',
        content: '什么是 AQS？',
        createdTime: '2024-01-16T10:00:00'
    },
    {
        id: 2,
        conversationId: 1,
        role: 'assistant',
        content: 'AQS (AbstractQueuedSynchronizer) 是 Java 并发包的核心框架，用于构建锁和同步器。\n\n它维护了一个 volatile int state（代表共享资源）和一个 FIFO 线程等待队列（多线程争用资源被阻塞时会进入此队列）。',
        references: [
            { noteId: 1, title: 'Java 高级并发', snippet: 'AQS 核心原理...', score: 0.95 }
        ],
        createdTime: '2024-01-16T10:00:05'
    }
]

export const MOCK_GRAPH_DATA = {
    nodes: MOCK_NOTES.map(n => ({ id: n.id, label: n.title, type: 'Note', size: 40 })),
    edges: [
        { source: MOCK_NOTES[0].id, target: MOCK_NOTES[1].id, label: 'Related' }
    ]
}

// --- Mock Matcher Logic ---

export const matchMockData = (url: string, method: string, data?: any) => {
    console.log(`[Mock] Matching: ${method} ${url}`)

    // User
    if (url.includes('/users/profile')) return MOCK_USER

    // Notes
    if (url.includes('/notes') && method === 'get') {
        // List
        if (!url.match(/\/notes\/\d+/)) return { content: MOCK_NOTES, totalElements: MOCK_NOTES.length }
        // Detail
        const id = url.split('/').pop()
        return MOCK_NOTES.find(n => n.id === Number(id)) || MOCK_NOTES[0]
    }

    // Graph
    if (url.includes('/graph')) {
        return {
            nodes: MOCK_NOTES.map(n => ({ id: String(n.id), label: n.title, group: 1 })),
            links: [{ source: '1', target: '2', value: 1 }]
        }
    }

    // Learning Plans
    if (url.includes('/learning/plans') && method === 'get') {
        if (url.match(/\/learning\/plans\/\d+/)) {
            const id = parseInt(url.split('/').pop() || '1')
            return MOCK_PLANS.find(p => p.id === id) || MOCK_PLANS[0]
        }
        return { content: MOCK_PLANS, totalElements: MOCK_PLANS.length }
    }

    // Learning Tasks
    if (url.includes('/learning/tasks/today')) {
        return MOCK_TASKS.filter(t => t.dueDate >= new Date().toISOString().split('T')[0] && t.status !== 2).slice(0, 3)
    }
    if (url.includes('/learning/tasks') && method === 'get') {
        return { content: MOCK_TASKS, totalElements: MOCK_TASKS.length }
    }

    // Pomodoro
    if (url.includes('/learning/pomodoros') && method === 'get') {
        if (url.includes('/stats')) return { todayCount: 4, todayMinutes: 100, weekCount: 12, weekMinutes: 300, streakDays: 5 }
        return { content: MOCK_POMODOROS, totalElements: MOCK_POMODOROS.length }
    }

    // Spaced Repetition
    if (url.includes('/spaced/due')) return MOCK_SPACED_CARDS.filter(c => c.reviewStatus?.isDueToday) // due cards
    if (url.includes('/spaced/stats')) return MOCK_SPACED_STATS
    if (url.includes('/spaced/cards') && method === 'get') return { content: MOCK_SPACED_CARDS, totalElements: MOCK_SPACED_CARDS.length }

    // AI
    if (url.includes('/ai/conversations') && method === 'get') {
        return { content: MOCK_CONVERSATIONS, totalElements: MOCK_CONVERSATIONS.length }
    }
    if (url.match(/\/ai\/conversations\/\d+\/messages/) && method === 'get') {
        // Simple mock return all messages for now, or filter by ID if we parse regex
        // const id = ...
        return MOCK_MESSAGES
    }
    if (url.match(/\/ai\/chat\/send/) && method === 'post') {
        const parsed = JSON.parse(data || '{}')
        return {
            id: Date.now(),
            role: 'assistant',
            content: `[Mock] I received your message: "${parsed.content}". \n\nThis is a simulated response because the backend is offline.`,
            createdTime: new Date().toISOString(),
            references: [
                { noteId: 1, title: 'Mock Reference Note', snippet: 'Content from mock reference...', score: 0.88 }
            ]
        }
    }

    return null
}
