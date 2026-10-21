import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
    {
        path: '/',
        redirect: '/dashboard'
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/auth/Login.vue'),
        meta: { title: '登录' }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/auth/Register.vue'),
        meta: { title: '注册' }
    },
    {
        path: '/profile/:id',
        name: 'UserPublicProfile',
        component: () => import('../views/profile/UserPublicProfile.vue'),
        meta: { title: '个人主页' }
    },
    {
        path: '/community/galaxy',
        name: 'CommunityGalaxyMap',
        component: () => import('../views/community/GalaxyView.vue'),
        meta: { title: '知识星空' }
    },
    {
        path: '/',
        component: () => import('../layout/MainLayout.vue'),
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/dashboard/index.vue'),
                meta: { title: '仪表盘', requiresAuth: true }
            },
            {
                path: 'notes',
                name: 'NoteList',
                component: () => import('../views/note/NoteList.vue'),
                meta: { title: '笔记列表', requiresAuth: true }
            },
            {
                path: 'notes/new',
                name: 'NoteCreate',
                component: () => import('../views/note/NoteEditor.vue'),
                meta: { title: '创建笔记', requiresAuth: true }
            },
            {
                path: 'notes/:id',
                name: 'NoteEdit',
                component: () => import('../views/note/NoteEditor.vue'),
                meta: { title: '编辑笔记', requiresAuth: true }
            },
            {
                path: 'graph',
                name: 'Graph',
                component: () => import('../views/graph/GraphView.vue'),
                meta: { title: '知识图谱', requiresAuth: true }
            },
            {
                path: 'messages',
                name: 'MessageCenter',
                component: () => import('../views/message/index.vue'),
                meta: { title: '消息中心', requiresAuth: true }
            },
            {
                path: 'search',
                name: 'Search',
                component: () => import('../views/search/index.vue'),
                meta: { title: '全文搜索', requiresAuth: true }
            },
            {
                path: 'content',
                name: 'Content',
                component: () => import('../views/content/index.vue'),
                meta: { title: '订阅源', requiresAuth: true }
            },
            {
                path: 'aggregation',
                name: 'Aggregation',
                component: () => import('../views/aggregation/index.vue'),
                meta: { title: '内容聚合', requiresAuth: true }
            },
            {
                path: 'ai',
                name: 'AiAssistant',
                component: () => import('../views/ai/index.vue'),
                meta: { title: 'AI助手', requiresAuth: true }
            },
            {
                path: 'review',
                name: 'Review',
                component: () => import('../views/review/index.vue'),
                meta: { title: '间隔复习', requiresAuth: true }
            },
            {
                path: 'community/feed',
                name: 'CommunityFeed',
                component: () => import('../views/community/index.vue'),
                meta: { title: '社区大厅', requiresAuth: true }
            },
            {
                path: 'community/note/:id',
                name: 'PublicNoteDetail',
                component: () => import('../views/community/PublicNoteDetail.vue'),
                meta: { title: '笔记详情', requiresAuth: true }
            },
            {
                path: 'profile/settings',
                name: 'ProfileSettings',
                component: () => import('../views/profile/ProfileSettings.vue'),
                meta: { title: '个人设置', requiresAuth: true }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

import { useUserStore } from '../store/user'

// 简单的路由守卫
router.beforeEach((to, _from, next) => {
    document.title = (to.meta.title as string) ? `${to.meta.title} - KnowledgeFlow` : 'KnowledgeFlow'

    const userStore = useUserStore()

    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
        next('/login')
    } else {
        next()
    }
})

export default router
