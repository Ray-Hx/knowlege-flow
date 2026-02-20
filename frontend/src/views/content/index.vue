<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { 
    NLayout, NLayoutContent, NLayoutSider, NCard, NList, NListItem, NThing, NSpace, NTag, NButton, NIcon, NText, NModal, NForm, NFormItem, NInput, NSelect, NInputNumber, useMessage
} from 'naive-ui'
import { 
    AddOutline, RefreshOutline, CheckmarkCircleOutline, TrashOutline, LogoRss, LogoGithub, ReaderOutline
} from '@vicons/ionicons5'
import { contentApi, ContentSource, ContentItem } from '@/api/content'
import { useDialog } from 'naive-ui'

const message = useMessage()
const dialog = useDialog()

// Data
const sources = ref<ContentSource[]>([])
const items = ref<ContentItem[]>([])
const loading = ref(false)
const currentSourceId = ref<number | undefined>(undefined)
const showAddModal = ref(false)

// Form Data
const newSource = ref<ContentSource>({
    type: 'RSS',
    name: '',
    url: '',
    syncInterval: 60,
    status: 1
})

// Methods
const fetchSources = async () => {
    try {
        const data = await contentApi.listSources()
        sources.value = data
    } catch (error) {
        message.error(error.message || '获取订阅源失败')
    }
}

const fetchItems = async () => {
    loading.value = true
    try {
        const data = await contentApi.listItems({
            page: 1,
            size: 50,
            sourceId: currentSourceId.value
        })
        items.value = data.records
    } catch (error) {
        message.error(error.message || '获取内容失败')
    } finally {
        loading.value = false
    }
}

const handleAddSource = async () => {
    try {
        await contentApi.addSource(newSource.value)
        message.success('添加成功')
        showAddModal.value = false
        fetchSources()
    } catch (error) {
        message.error(error.message || '添加失败')
    }
}

const handleDeleteSource = (id: number) => {
    dialog.warning({
        title: '警告',
        content: '确定要删除这个订阅源吗？相关内容也将被删除。',
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: async () => {
            try {
                await contentApi.deleteSource(id)
                message.success('删除成功')
                fetchSources()
                if (currentSourceId.value === id) {
                    currentSourceId.value = undefined
                    fetchItems()
                }
            } catch (error) {
                message.error(error.message || '删除失败')
            }
        }
    })
}

const handleSync = async (id: number) => {
    try {
        message.loading('正在同步...')
        await contentApi.syncSource(id)
        message.success('同步完成')
        fetchItems() // 立即刷新内容
    } catch (error: any) {
        const errMsg = error?.response?.data?.message || error?.message || '同步失败'
        message.error(errMsg)
    }
}

const selectSource = (id?: number) => {
    currentSourceId.value = id
    fetchItems()
}

const getIcon = (type: string) => {
    switch(type) {
        case 'RSS': return LogoRss
        case 'GITHUB': return LogoGithub
        default: return ReaderOutline
    }
}

const openUrl = (url: string) => {
    window.open(url, '_blank')
}

// Stats
const totalUnread = ref(0) // Logic to update this could be added

onMounted(() => {
    fetchSources()
    fetchItems()
})
</script>

<template>
    <div class="h-full flex gap-4">
        <!-- Sidebar: Sources -->
        <div class="w-1/4 min-w-[250px] flex flex-col gap-4">
            <n-card class="glass-card flex-1" content-style="padding: 0;">
                <div class="p-4 flex justify-between items-center border-b border-gray-100">
                    <n-text class="text-lg font-bold">我的订阅</n-text>
                    <n-button circle size="small" secondary @click="showAddModal = true">
                        <template #icon><n-icon :component="AddOutline" /></template>
                    </n-button>
                </div>
                <div class="p-2 space-y-2 overflow-y-auto max-h-[calc(100vh-12rem)]">
                    <div 
                        class="p-3 rounded-xl cursor-pointer transition-all hover:bg-white/50 flex items-center justify-between group"
                        :class="currentSourceId === undefined ? 'bg-indigo-50 text-indigo-600' : ''"
                        @click="selectSource(undefined)"
                    >
                        <div class="flex items-center gap-2">
                            <n-icon :size="18" :component="ReaderOutline" />
                            <span class="font-medium">全部内容</span>
                        </div>
                    </div>
                    
                    <div 
                        v-for="source in sources" 
                        :key="source.id"
                        class="p-3 rounded-xl cursor-pointer transition-all hover:bg-white/50 flex items-center justify-between group"
                        :class="currentSourceId === source.id ? 'bg-indigo-50 text-indigo-600' : ''"
                        @click="selectSource(source.id)"
                    >
                        <div class="flex items-center gap-2 overflow-hidden">
                            <n-icon :size="18" :component="getIcon(source.type)" />
                            <span class="font-medium truncate">{{ source.name }}</span>
                        </div>
                        <n-space class="opacity-0 group-hover:opacity-100 transition-opacity">
                            <n-button circle size="tiny" tertiary @click.stop="handleSync(source.id!)">
                                <template #icon><n-icon :component="RefreshOutline" /></template>
                            </n-button>
                            <n-button circle size="tiny" tertiary type="error" @click.stop="handleDeleteSource(source.id!)">
                                <template #icon><n-icon :component="TrashOutline" /></template>
                            </n-button>
                        </n-space>
                    </div>
                </div>
            </n-card>
        </div>

        <!-- Main: Items List -->
        <div class="flex-1 h-full overflow-hidden flex flex-col gap-4">
            <n-card class="glass-card flex-1 overflow-hidden" content-style="padding: 0; display: flex; flex-direction: column; height: 100%;">
                <div class="p-4 border-b border-gray-100 flex justify-between items-center">
                    <n-text class="text-lg font-bold">
                        {{ currentSourceId ? sources.find(s => s.id === currentSourceId)?.name : '最新更新' }}
                    </n-text>
                    <n-button secondary size="small" @click="fetchItems">
                        <template #icon><n-icon :component="RefreshOutline" /></template>
                        刷新
                    </n-button>
                </div>
                
                <div class="flex-1 overflow-y-auto p-4 custom-scrollbar">
                    <n-list hoverable clickable>
                        <n-list-item v-for="item in items" :key="item.id">
                            <n-thing content-style="margin-top: 10px;">
                                <template #header>
                                    <n-text class="font-bold text-lg cursor-pointer hover:text-indigo-600 transition-colors" @click="openUrl(item.url)">
                                        {{ item.title }}
                                    </n-text>
                                </template>
                                <template #description>
                                    <div class="flex items-center gap-2 text-xs text-gray-400">
                                        <n-tag size="small" :bordered="false" type="info" class="rounded-full px-2">
                                            {{ item.sourceName }}
                                        </n-tag>
                                        <span>{{ item.author || 'Unknown' }}</span>
                                        <span>•</span>
                                        <span>{{ new Date(item.createdTime).toLocaleString() }}</span>
                                    </div>
                                </template>
                                <div class="text-gray-600 mt-2 line-clamp-2">
                                    {{ item.summary || '暂无摘要...' }}
                                </div>
                            </n-thing>
                        </n-list-item>
                    </n-list>
                    
                    <div v-if="items.length === 0 && !loading" class="h-full flex items-center justify-center text-gray-400">
                        暂无内容
                    </div>
                </div>
            </n-card>
        </div>

        <!-- Add Source Modal -->
        <n-modal v-model:show="showAddModal">
            <n-card style="width: 500px;" title="添加订阅源" :bordered="false" size="huge" role="dialog" aria-modal="true" class="glass-panel">
                <n-form>
                    <n-form-item label="类型">
                        <n-select v-model:value="newSource.type" :options="[{label: 'RSS Feed', value: 'RSS'}, {label: 'GitHub Release', value: 'GITHUB'}]" />
                    </n-form-item>
                    <n-form-item label="名称">
                        <n-input v-model:value="newSource.name" placeholder="请输入源名称" />
                    </n-form-item>
                    <n-form-item label="URL">
                        <n-input v-model:value="newSource.url" placeholder="https://..." />
                    </n-form-item>
                    <n-form-item label="同步间隔 (分钟)">
                        <n-input-number v-model:value="newSource.syncInterval" />
                    </n-form-item>
                </n-form>
                <template #footer>
                    <n-space justify="end">
                        <n-button @click="showAddModal = false">取消</n-button>
                        <n-button type="primary" @click="handleAddSource">确定</n-button>
                    </n-space>
                </template>
            </n-card>
        </n-modal>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}
</style>
