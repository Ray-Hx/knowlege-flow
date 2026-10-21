<template>
    <n-modal
        v-model:show="showModal"
        preset="dialog"
        :title="null"
        style="width: 800px; max-width: 95vw"
        :show-icon="false"
        class="article-viewer-modal"
    >
        <div v-if="loading" class="loading-container">
            <n-spin size="large" />
            <div class="loading-text">加载内容中...</div>
        </div>
        
        <div v-else-if="article" class="article-content-container">
            <div class="article-header">
                <h1 class="article-title">{{ article.title }}</h1>
                <div class="article-meta">
                    <span v-if="article.sourceName" class="meta-item">
                        <n-icon><CompassOutline /></n-icon>
                        {{ article.sourceName }}
                    </span>
                    <span v-if="article.author" class="meta-item">
                        <n-icon><PersonOutline /></n-icon>
                        {{ article.author }}
                    </span>
                    <span v-if="article.publishTime" class="meta-item">
                        <n-icon><TimeOutline /></n-icon>
                        {{ formatTime(article.publishTime) }}
                    </span>
                    <a :href="article.url" target="_blank" class="meta-action">
                        <n-icon><OpenOutline /></n-icon> 阅读原文
                    </a>
                </div>
            </div>
            
            <n-divider style="margin: 16px 0;" />
            
            <n-scrollbar style="max-height: 60vh;">
                <div 
                    v-if="article.contentText" 
                    class="article-body markdown-body" 
                    v-html="article.contentText"
                ></div>
                <div v-else class="empty-content">
                    <n-empty description="该文章没有提供正文内容">
                        <template #extra>
                            <n-button type="primary" tag="a" :href="article.url" target="_blank">
                                前往原文阅读
                            </n-button>
                        </template>
                    </n-empty>
                </div>
            </n-scrollbar>
        </div>
    </n-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { contentApi, ContentItem } from '@/api/content'
import { useMessage } from 'naive-ui'
import { format } from 'date-fns'
import { 
    CompassOutline, 
    PersonOutline, 
    TimeOutline, 
    OpenOutline 
} from '@vicons/ionicons5'

const showModal = ref(false)
const loading = ref(false)
const article = ref<ContentItem | null>(null)
const message = useMessage()

const open = async (id: number) => {
    showModal.value = true
    loading.value = true
    article.value = null
    
    try {
        const res = await contentApi.getItem(id)
        if (res) {
            article.value = res
        }
    } catch (e: any) {
        message.error(e?.message || '获取文章内容失败')
    } finally {
        loading.value = false
    }
}

const formatTime = (timeStr?: string) => {
    if (!timeStr) return ''
    try {
        return format(new Date(timeStr), 'yyyy-MM-dd HH:mm')
    } catch {
        return timeStr
    }
}

// 暴露给父组件调用
defineExpose({
    open
})
</script>

<style scoped>
.article-viewer-modal {
    padding: 0;
}

.loading-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 300px;
}

.loading-text {
    margin-top: 16px;
    color: #666;
}

.article-content-container {
    padding: 24px 8px;
}

.article-header {
    margin-bottom: 8px;
}

.article-title {
    font-size: 22px;
    font-weight: 600;
    line-height: 1.4;
    color: #1f2225;
    margin: 0 0 12px 0;
}

.article-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
    font-size: 13px;
    color: #6b7280;
}

.meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
}

.meta-action {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #1677ff;
    text-decoration: none;
    transition: color 0.2s;
    margin-left: auto;
}

.meta-action:hover {
    color: #4096ff;
}

.article-body {
    padding-right: 12px;
    font-size: 15px;
    line-height: 1.7;
    color: #333;
}

/* 简单的 markdown-body 样式以适应 RSS 抓取的 HTML */
.markdown-body :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 4px;
    margin: 16px 0;
}

.markdown-body :deep(a) {
    color: #1677ff;
    text-decoration: none;
}

.markdown-body :deep(a:hover) {
    text-decoration: underline;
}

.markdown-body :deep(p) {
    margin-bottom: 1em;
}

.markdown-body :deep(blockquote) {
    border-left: 4px solid #e5e7eb;
    padding-left: 16px;
    margin-left: 0;
    color: #6b7280;
}

.markdown-body :deep(pre) {
    background-color: #f3f4f6;
    padding: 12px;
    border-radius: 4px;
    overflow-x: auto;
}

.markdown-body :deep(code) {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 0.9em;
    background-color: #f3f4f6;
    padding: 2px 4px;
    border-radius: 3px;
}

.markdown-body :deep(pre code) {
    background-color: transparent;
    padding: 0;
}

.empty-content {
    display: flex;
    justify-content: center;
    padding: 40px 0;
}
</style>
