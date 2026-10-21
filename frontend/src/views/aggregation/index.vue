<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { 
    NCard, NButton, NIcon, NText, NSpace, NList, NListItem, NThing, NTag, NEmpty, NSpin,
    useMessage, useDialog
} from 'naive-ui'
import { 
    CloudUploadOutline, TrashOutline, DocumentTextOutline, TimeOutline,
    ReaderOutline
} from '@vicons/ionicons5'
import { contentApi, ContentItem } from '@/api/content'
import ArticleViewerModal from '@/components/common/ArticleViewerModal.vue'

const message = useMessage()
const dialog = useDialog()

const items = ref<ContentItem[]>([])
const loading = ref(false)
const uploading = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const articleModalRef = ref<any>(null)

const fetchItems = async () => {
    loading.value = true
    try {
        const data = await contentApi.listItems({
            page: 1,
            size: 50,
            isUpload: true
        })
        items.value = data.records
    } catch (error: any) {
        message.error(error.message || '获取内容失败')
    } finally {
        loading.value = false
    }
}

const handleUploadClick = () => {
    fileInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
    const target = event.target as HTMLInputElement
    if (!target.files || target.files.length === 0) return

    const file = target.files[0]
    uploading.value = true
    try {
        message.loading('上传中...', { duration: 0 })
        await contentApi.uploadFile(file)
        message.destroyAll()
        message.success('上传成功，正在生成索引...')
        fetchItems()
    } catch (error: any) {
        message.destroyAll()
        const errMsg = error?.response?.data?.message || error?.message || '上传失败'
        message.error(errMsg)
    } finally {
        uploading.value = false
        target.value = ''
    }
}

const handleDeleteItem = (id: number) => {
    dialog.warning({
        title: '警告',
        content: '确定要删除此文件吗？此操作不可恢复，且关联的 AI 知识库切片也将被永久删除。',
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: async () => {
            try {
                await contentApi.deleteItem(id)
                message.success('删除成功')
                fetchItems()
            } catch (error: any) {
                message.error(error.message || '删除失败')
            }
        }
    })
}

const openArticle = (id: number) => {
    if (articleModalRef.value) {
        articleModalRef.value.open(id)
    }
}

const formatDate = (dateStr?: string) => {
    if (!dateStr) return '未知'
    const date = new Date(dateStr)
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
    fetchItems()
})
</script>

<template>
    <div class="h-full flex flex-col gap-4">
        <n-card class="glass-card shadow-sm" content-style="padding: 1.5rem;">
            <div class="flex justify-between items-center">
                <div class="flex items-center gap-3">
                    <div class="p-3 bg-indigo-50 text-indigo-600 rounded-2xl">
                        <n-icon :size="24" :component="ReaderOutline" />
                    </div>
                    <div>
                        <n-text class="text-xl font-bold">内容聚合</n-text>
                        <br />
                        <n-text depth="3" class="text-sm">管理您上传的本地文档，内容将自动索引至 AI 知识库</n-text>
                    </div>
                </div>
                <n-button type="primary" secondary round size="large" @click="handleUploadClick" :loading="uploading">
                    <template #icon>
                        <n-icon :component="CloudUploadOutline" />
                    </template>
                    上传文档
                </n-button>
            </div>
        </n-card>

        <input
            ref="fileInputRef"
            type="file"
            class="hidden"
            accept=".pdf,.md,.txt,.docx"
            @change="handleFileChange"
        />

        <n-card class="glass-card flex-1 overflow-hidden" content-style="padding: 0; display: flex; flex-direction: column;">
            <n-spin :show="loading" class="flex-1">
                <div class="flex flex-col h-full">
                    <div v-if="items.length === 0" class="flex-1 flex items-center justify-center">
                        <n-empty description="暂无上传内容">
                            <template #icon>
                                <n-icon :component="DocumentTextOutline" />
                            </template>
                        </n-empty>
                    </div>
                    <div v-else class="flex-1 overflow-y-auto p-4">
                        <n-list hoverable clickable>
                            <n-list-item v-for="item in items" :key="item.id">
                                <n-thing @click="openArticle(item.id)">
                                    <template #header>
                                        <n-text class="font-bold cursor-pointer hover:text-indigo-600 transition-colors">
                                            {{ item.title }}
                                        </n-text>
                                    </template>
                                    <template #description>
                                        <n-space size="small" style="margin-top: 4px;">
                                            <n-tag v-if="item.author" size="small" :bordered="false" type="info">
                                                {{ item.author }}
                                            </n-tag>
                                            <div class="flex items-center gap-1 text-xs text-gray-400">
                                                <n-icon :component="TimeOutline" />
                                                {{ formatDate(item.createdTime) }}
                                            </div>
                                        </n-space>
                                    </template>
                                    <div class="mt-2 text-sm text-gray-500 line-clamp-2">
                                        {{ item.summary || '暂无摘要' }}
                                    </div>
                                </n-thing>
                                <template #suffix>
                                    <n-button circle tertiary type="error" size="small" @click.stop="handleDeleteItem(item.id)">
                                        <template #icon>
                                            <n-icon :component="TrashOutline" />
                                        </template>
                                    </n-button>
                                </template>
                            </n-list-item>
                        </n-list>
                    </div>
                </div>
            </n-spin>
        </n-card>

        <ArticleViewerModal ref="articleModalRef" />
    </div>
</template>

<style scoped>
.glass-card {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-radius: 1.5rem;
}

:deep(.n-list-item) {
    padding: 1.25rem !important;
    border-radius: 1rem;
    margin-bottom: 0.5rem;
    transition: all 0.2s ease;
}

:deep(.n-list-item:hover) {
    background: rgba(255, 255, 255, 0.9);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>
