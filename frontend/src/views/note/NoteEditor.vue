<template>
    <div class="note-editor-container">
        <div class="editor-header">
            <n-input
                v-model:value="noteTitle"
                placeholder="输入笔记标题..."
                class="title-input"
                size="large"
                :bordered="false"
            />
            <n-space>
                <!-- 文件夹选择 -->
                <n-select
                    v-model:value="selectedFolderId"
                    :options="folderOptions"
                    placeholder="选择文件夹"
                    style="width: 160px"
                    size="medium"
                />
                <n-button @click="openInsertModal" title="插入内容">
                    🔗 插入引用
                </n-button>
                <n-dropdown :options="aiOptions" @select="handleAiAction">
                    <n-button :loading="aiProcessing">
                        🤖 AI助手
                    </n-button>
                </n-dropdown>
                <n-button @click="showMindmap = true" title="思维导图">
                    🗺️ 思维导图
                </n-button>
                <n-button type="primary" @click="saveNote" :loading="saving">
                    保存
                </n-button>
                <div class="flex items-center ml-2">
                    <n-switch v-model:value="isPublic" size="medium">
                        <template #checked>公开</template>
                        <template #unchecked>私有</template>
                    </n-switch>
                </div>
            </n-space>
        </div>
        <div class="editor-wrapper flex-1 overflow-hidden relative mt-2">
            <div id="vditor" class="absolute inset-0 border border-gray-100 rounded"></div>
        </div>

        <!-- 多源引用内容搜索弹窗 -->
        <n-modal v-model:show="showInsertModal" preset="card" title="选择要引用的内容" style="width: 600px; max-width: 90vw;">
            <div class="space-y-4">
                <n-input 
                    v-model:value="searchKeyword" 
                    placeholder="输入关键词搜索..." 
                    clearable
                />
                <n-tabs type="segment" v-model:value="insertTab" @update:value="performSearch">
                    <n-tab-pane name="article" tab="📰 订阅文章">
                        <n-spin :show="searching">
                            <div class="article-list custom-scrollbar" style="max-height: 400px; overflow-y: auto;">
                                <n-empty v-if="articleResults.length === 0" description="暂无相关文章" />
                                <div v-else class="article-items">
                                    <div v-for="item in articleResults" :key="item.id" class="article-item" @click="insertFeature('article', item)">
                                        <div class="article-title">{{ item.title }}</div>
                                        <div class="article-meta mt-1">
                                            <span v-if="item.sourceName">来源: {{ item.sourceName }}</span>
                                            <span v-if="item.publishTime">{{ item.publishTime }}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </n-spin>
                    </n-tab-pane>
                    <n-tab-pane name="my_note" tab="📝 我的笔记">
                        <n-spin :show="searching">
                            <div class="article-list custom-scrollbar" style="max-height: 400px; overflow-y: auto;">
                                <n-empty v-if="myNoteResults.length === 0" description="暂无相关笔记" />
                                <div v-else class="article-items">
                                    <div v-for="item in myNoteResults" :key="item.id" class="article-item" @click="insertFeature('my_note', item)">
                                        <div class="article-title">{{ item.title }}</div>
                                        <div class="article-meta mt-1 text-gray-500 text-xs truncate">{{ item.summary || '无摘要' }}</div>
                                    </div>
                                </div>
                            </div>
                        </n-spin>
                    </n-tab-pane>
                    <n-tab-pane name="community_note" tab="🌐 社区笔记">
                        <n-spin :show="searching">
                            <div class="article-list custom-scrollbar" style="max-height: 400px; overflow-y: auto;">
                                <n-empty v-if="communityResults.length === 0" description="暂无相关社区笔记" />
                                <div v-else class="article-items">
                                    <div v-for="item in communityResults" :key="item.id" class="article-item" @click="insertFeature('community_note', item)">
                                        <div class="article-title">{{ item.title }}</div>
                                        <div class="article-meta mt-1 flex gap-3 text-xs text-indigo-500">
                                            <span v-if="item.authorName">👤 {{ item.authorName }}</span>
                                            <span>❤️ {{ item.likeCount || 0 }}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </n-spin>
                    </n-tab-pane>
                </n-tabs>
            </div>
        </n-modal>

        <!-- 思维导图弹窗 -->
        <n-modal v-model:show="showMindmap" preset="card" title="🗺️ 笔记思维导图" style="width: 90vw; height: 80vh;">
            <NoteMindmap v-if="showMindmap" :markdown="getCurrentMarkdown()" style="height: 60vh;" />
        </n-modal>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage, NModal, NEmpty, NSpin, NSelect } from 'naive-ui'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import NoteMindmap from '@/components/NoteMindmap.vue'
import { noteApi, NoteFolder, Note } from '@/api/note'
import { contentApi, ContentItem } from '@/api/content'
import { getCommunityFeed } from '@/api/community'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const noteId = ref<number | null>(route.params.id ? Number(route.params.id) : null)
const noteTitle = ref('')
const saving = ref(false)
const isPublic = ref(false)
const vditor = ref<Vditor | null>(null)
const initialContent = ref('')

// 文件夹选择
const folders = ref<NoteFolder[]>([])
const selectedFolderId = ref<number>(0)
const folderOptions = computed(() => [
    { label: '📁 默认文件夹', value: 0 },
    ...folders.value.map(f => ({ label: f.name, value: f.id }))
])

// 引用插入弹窗的整体状态
const showInsertModal = ref(false)
const insertTab = ref('article')
const searchKeyword = ref('')
const searching = ref(false)

const articleResults = ref<ContentItem[]>([])
const myNoteResults = ref<Note[]>([])
const communityResults = ref<Note[]>([])

// 思维导图
const showMindmap = ref(false)
const getCurrentMarkdown = () => {
    return vditor.value?.getValue() || ''
}

// 执行搜索
const performSearch = async () => {
    searching.value = true
    try {
        if (insertTab.value === 'article') {
            const res = await contentApi.listItems({
                page: 1,
                size: 20,
                keyword: searchKeyword.value || undefined
            })
            articleResults.value = res.records || []
        } else if (insertTab.value === 'my_note') {
            const res = await noteApi.getNotes({
                page: 1,
                size: 20,
                keyword: searchKeyword.value || undefined
            })
            myNoteResults.value = res.records || []
        } else if (insertTab.value === 'community_note') {
            const res = await getCommunityFeed(searchKeyword.value || '', 1, 20)
            communityResults.value = res.records || []
        }
    } catch (e: any) {
        console.error(e)
    } finally {
        searching.value = false
    }
}

// 打开弹窗
const openInsertModal = () => {
    showInsertModal.value = true
    searchKeyword.value = ''
    performSearch()
}

// 统一插入
const insertFeature = (type: string, item: any) => {
    let insertStr = ''
    if (type === 'article') {
        insertStr = `[[!article:${item.id}|${item.title}]]`
    } else if (type === 'my_note') {
        insertStr = `[${item.title}](/community/note/${item.id})`
    } else if (type === 'community_note') {
        insertStr = `[${item.title}](/community/note/${item.id})`
    }
    
    if (vditor.value && insertStr) {
        vditor.value.insertValue(insertStr)
        message.success(`已插入引用: ${item.title}`)
    }
    showInsertModal.value = false
}

// 搜索防抖
let searchTimer: any = null
watch(searchKeyword, () => {
    clearTimeout(searchTimer)
    searchTimer = setTimeout(() => {
        performSearch()
    }, 300)
})

// AI 写作助手
import { aiApi } from '@/api/ai'
import { NDropdown, NSwitch } from 'naive-ui'

const aiProcessing = ref(false)
const aiOptions = [
    { label: '✨ 润色', key: 'polish' },
    { label: '🌐 翻译成英文', key: 'translate-en' },
    { label: '🇨🇳 翻译成中文', key: 'translate-zh' },
    { label: '📝 扩写', key: 'expand' }
]

const currentSelection = ref('')

const handleAiAction = async (key: string) => {
    if (!vditor.value) return
    
    // 如果直接获取为空（可能因为点击外部按钮丢失了焦点），则尝试使用我们之前缓存的选中文本
    let selection = vditor.value.getSelection()
    if (!selection || selection.trim() === '') {
        selection = currentSelection.value
    }
    
    if (!selection || selection.trim() === '') {
        message.warning('请先选中要处理的文本')
        return
    }
    
    aiProcessing.value = true
    try {
        let result = ''
        if (key === 'polish') {
            result = await aiApi.polishText(selection)
        } else if (key === 'translate-en') {
            result = await aiApi.translateText(selection, 'en')
        } else if (key === 'translate-zh') {
            result = await aiApi.translateText(selection, 'zh')
        } else if (key === 'expand') {
            result = await aiApi.expandText(selection)
        }
        
        if (result) {
            // 在光标/选区处追加（不替换原来的文本）
            // 使用特定的 class 来实现短暂的 CSS 高亮动画
            const insertStr = `\n\n<mark class="ai-highlight-text">🤖 **AI 补充：**</mark>\n${result}\n\n`
            vditor.value.insertValue(insertStr, false)
            message.success('AI 处理完成')
            
            // 可选：如果不希望 <mark> 标签永久留在 Markdown 源码中，
            // 可以在动画结束后（比如 3 秒）将 <mark> 标签移除，只保留纯文本
            setTimeout(() => {
                if (vditor.value) {
                    let content = vditor.value.getValue()
                    if (content.includes('<mark class="ai-highlight-text">')) {
                        content = content.replace(/<mark class="ai-highlight-text">(.*?)<\/mark>/g, '$1')
                        vditor.value.setValue(content)
                    }
                }
            }, 3000)
        }
    } catch (e: any) {
        message.error(e.message || 'AI 处理失败')
    } finally {
        aiProcessing.value = false
    }
}


const initVditor = () => {
    vditor.value = new Vditor('vditor', {
        height: '100%',
        width: '100%',
        placeholder: '开始记录你的想法...\n\n💡 提示：\n- 使用 [[笔记标题]] 引用其他笔记\n- 点击上方"插入文章"按钮引用订阅文章',
        cache: {
            enable: false,
        },
        value: initialContent.value,
        mode: 'ir',
        toolbar: [
            'emoji',
            'headings',
            'bold',
            'italic',
            'strike',
            'link',
            '|',
            'list',
            'ordered-list',
            'check',
            'outdent',
            'indent',
            '|',
            'quote',
            'line',
            'code',
            'inline-code',
            'insert-before',
            'insert-after',
            '|',
            'upload',
            'table',
            '|',
            'undo',
            'redo',
            '|',
            'fullscreen',
            'edit-mode',
        ],
        select: (value: string) => {
            currentSelection.value = value
        },
        hint: {
            // 自定义自动补全
            extend: [
                {
                    key: '[[',
                    hint: async (key: string) => {
                        // 搜索笔记标题
                        try {
                            const res = await noteApi.getNotes({
                                page: 1,
                                size: 10,
                                keyword: key
                            })
                            return (res.records || []).map(note => ({
                                value: `[[${note.title}]]`,
                                html: `<span class="hint-note">📝 ${note.title}</span>`
                            }))
                        } catch (e) {
                            return []
                        }
                    }
                }
            ]
        },
        preview: {
            theme: {
                current: 'light',
                path: 'https://cdn.jsdelivr.net/npm/vditor/dist/css/content-theme',
            },
            transform(html: string) {
                // 渲染 [[!article:id|标题]] 为文章引用徽章
                html = html.replace(/\[\[!article:(\d+)(?:\|([^\]]+))?\]\]/g, (_, id, title) => {
                    const displayTitle = title ? title : `文章引用 #${id}`
                    // 这里我们为了演示好看只显示 span 徽章，或者如果要跳转可写成 a 标签
                    // 但通常外部文章链接需要通过 contentLinks 在详情页呈现，这里只改名
                    return `<span class="article-ref-badge" data-id="${id}">📰 ${displayTitle}</span>`
                })
                // 渲染 [[笔记标题]] 为笔记跳转链接
                html = html.replace(/\[\[([^\]!][^\]]*)\]\]/g, (_, title) => {
                    return `<a class="note-ref-badge" href="/notes?keyword=${encodeURIComponent(title)}" target="_blank" style="text-decoration: none;">📝 ${title}</a>`
                })
                // 拦截普通的 [笔记标题](/community/note/ID) 让它有胶囊样式
                html = html.replace(/<a([^>]+)href="\/community\/note\/(\d+)"([^>]*)>(.*?)<\/a>/g, (_, p1, id, p2, text) => {
                    return `<a${p1}href="/community/note/${id}" target="_blank" class="note-ref-badge" style="text-decoration: none;"${p2}>📝 ${text}</a>`
                })
                return html
            }
        },
        after: () => {
            // Vditor initialized
        }
    })
}

const loadNote = async () => {
    if (!noteId.value) return
    try {
        const data = await noteApi.getNoteDetail(noteId.value)
        if (data) {
            noteTitle.value = data.title
            initialContent.value = data.contentMd
            isPublic.value = data.isPublic === 1
            selectedFolderId.value = data.folderId || 0
            if (vditor.value) {
                vditor.value.setValue(data.contentMd)
            }
        }
    } catch (error: any) {
        message.error(error.message || '加载笔记失败')
    }
}

const saveNote = async () => {
    const content = vditor.value?.getValue() || ''
    if (!noteTitle.value) {
        message.warning('请输入标题')
        return
    }

    saving.value = true
    try {
        if (noteId.value) {
            await noteApi.updateNote(noteId.value, {
                title: noteTitle.value,
                contentMd: content,
                folderId: selectedFolderId.value,
                status: 2, // 默认发布
                isPublic: isPublic.value ? 1 : 0
            })
            message.success('保存成功')
        } else {
            const data = await noteApi.createNote({
                title: noteTitle.value,
                contentMd: content,
                folderId: selectedFolderId.value,
                tags: [],
                status: 2,
                isPublic: isPublic.value ? 1 : 0
            })
            if (data) {
                message.success('创建成功')
                noteId.value = data.id
                router.replace(`/notes/${data.id}`)
            }
        }
    } catch (error: any) {
        message.error(error.message || '保存失败')
    } finally {
        saving.value = false
    }
}

onMounted(async () => {
    // 加载文件夹列表
    try {
        folders.value = await noteApi.getFolders()
    } catch (e: any) {
        console.error('Failed to load folders', e)
    }
    
    if (noteId.value) {
        await loadNote()
    }
    initVditor()
})

onUnmounted(() => {
    if (vditor.value) {
        vditor.value.destroy()
    }
})
</script>

<style scoped>
.note-editor-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    background-color: #fff;
    border-radius: 8px;
    padding: 16px;
}

.editor-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    gap: 16px;
}

.title-input {
    font-size: 18px;
    font-weight: bold;
    flex: 1;
}

.editor-wrapper {
    flex: 1;
}

.article-list {
    max-height: 400px;
    overflow-y: auto;
}

.article-items {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.article-item {
    padding: 12px;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
}

.article-item:hover {
    background-color: #f0f7ff;
    border-color: #1677ff;
}

.article-title {
    font-weight: 500;
    color: #333;
}

.article-meta {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
    display: flex;
    gap: 12px;
}

.article-summary {
    font-size: 13px;
    color: #666;
    margin-top: 6px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>

</style>
