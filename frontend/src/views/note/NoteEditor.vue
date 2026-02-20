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
                <n-button @click="openArticleModal" title="插入订阅文章">
                    📰 插入文章
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

        <!-- 文章搜索弹窗 -->
        <n-modal v-model:show="showArticleModal" preset="card" title="选择要引用的文章" style="width: 600px; max-width: 90vw;">
            <div class="space-y-4">
                <n-input 
                    v-model:value="articleSearchKeyword" 
                    placeholder="搜索文章标题..." 
                    clearable
                />
                <n-spin :show="articleSearching">
                    <div class="article-list">
                        <n-empty v-if="articleSearchResults.length === 0" description="暂无文章，请先在内容中心订阅" />
                        <div v-else class="article-items">
                            <div 
                                v-for="article in articleSearchResults" 
                                :key="article.id"
                                class="article-item"
                                @click="insertArticle(article)"
                            >
                                <div class="article-title">{{ article.title }}</div>
                                <div class="article-meta">
                                    <span v-if="article.sourceName">来源: {{ article.sourceName }}</span>
                                    <span v-if="article.publishTime">{{ article.publishTime }}</span>
                                </div>
                                <div class="article-summary" v-if="article.summary">
                                    {{ article.summary }}
                                </div>
                            </div>
                        </div>
                    </div>
                </n-spin>
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
import { noteApi, NoteFolder } from '@/api/note'
import { contentApi, ContentItem } from '@/api/content'

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

// 文章搜索弹窗
const showArticleModal = ref(false)
const articleSearchKeyword = ref('')
const articleSearchResults = ref<ContentItem[]>([])
const articleSearching = ref(false)

// 思维导图
const showMindmap = ref(false)
const getCurrentMarkdown = () => {
    return vditor.value?.getValue() || ''
}

// 搜索文章
const searchArticles = async () => {
    articleSearching.value = true
    try {
        const res = await contentApi.listItems({
            page: 1,
            size: 20,
            keyword: articleSearchKeyword.value || undefined
        })
        articleSearchResults.value = res.records || []
    } catch (e) {
        console.error(e)
    } finally {
        articleSearching.value = false
    }
}

// 打开文章搜索弹窗
const openArticleModal = () => {
    showArticleModal.value = true
    articleSearchKeyword.value = ''
    searchArticles()
}

// 插入文章引用
const insertArticle = (article: ContentItem) => {
    const ref = `[[!article:${article.id}]]`
    if (vditor.value) {
        vditor.value.insertValue(ref)
    }
    showArticleModal.value = false
    message.success(`已插入: ${article.title}`)
}

// 搜索防抖
let searchTimer: any = null
watch(articleSearchKeyword, () => {
    clearTimeout(searchTimer)
    searchTimer = setTimeout(() => {
        searchArticles()
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

const handleAiAction = async (key: string) => {
    if (!vditor.value) return
    
    const selection = vditor.value.getSelection()
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
            // 替换选中的文本
            vditor.value.insertValue(result, true)
            message.success('AI 处理完成')
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
                // 渲染 [[!article:id]] 为文章引用徽章
                html = html.replace(/\[\[!article:(\d+)\]\]/g, 
                    '<span class="article-ref-badge" data-id="$1">📰 文章引用 #$1</span>'
                )
                // 渲染 [[笔记标题]] 为笔记链接
                html = html.replace(/\[\[([^\]!][^\]]*)\]\]/g, 
                    '<span class="note-ref-badge">📝 $1</span>'
                )
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
            if (vditor.value) {
                vditor.value.setValue(data.contentMd)
            }
        }
    } catch (error) {
        message.error('加载笔记失败')
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
    } catch (error) {
        message.error('保存失败')
    } finally {
        saving.value = false
    }
}

onMounted(async () => {
    // 加载文件夹列表
    try {
        folders.value = await noteApi.getFolders()
    } catch (e) {
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

<!-- 全局样式用于 Vditor 内部渲染 -->
<style>
.article-ref-badge {
    display: inline-block;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 2px 10px;
    border-radius: 12px;
    font-size: 13px;
    margin: 0 2px;
    cursor: pointer;
}

.article-ref-badge:hover {
    opacity: 0.9;
    transform: scale(1.02);
}

.note-ref-badge {
    display: inline-block;
    background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    color: white;
    padding: 2px 10px;
    border-radius: 12px;
    font-size: 13px;
    margin: 0 2px;
    cursor: pointer;
}

.note-ref-badge:hover {
    opacity: 0.9;
    transform: scale(1.02);
}

/* 笔记自动补全提示样式 */
.hint-note {
    display: block;
    padding: 6px 12px;
    color: #333;
    font-size: 14px;
}

.hint-note:hover {
    background-color: #f0f7ff;
}
</style>
