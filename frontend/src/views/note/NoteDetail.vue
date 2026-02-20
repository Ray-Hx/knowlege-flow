<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NInput, NButton, NSpace, NSelect, useMessage, NSpin, NCard, NCollapseTransition, NIcon, NModal, NEmpty } from 'naive-ui'
import { ArrowBackOutline, SaveOutline, LinkOutline } from '@vicons/ionicons5'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'
import { noteApi, NoteFolder, NoteLink, ContentLink } from '@/api/note'
import { contentApi, ContentItem } from '@/api/content'
import { noteReadingTracker } from '@/utils/noteReadingTracker'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const loading = ref(false)
const saving = ref(false)
const noteId = ref<number | null>(null)
const folderOptions = ref<any[]>([])
const showLinksPanel = ref(true)

// 笔记链接数据
const outgoingLinks = ref<NoteLink[]>([])
const incomingLinks = ref<NoteLink[]>([])
const contentLinks = ref<ContentLink[]>([])

// 文章搜索弹窗
const showArticleModal = ref(false)
const articleSearchKeyword = ref('')
const articleSearchResults = ref<ContentItem[]>([])
const articleSearching = ref(false)

const model = ref<{
  title: string;
  folderId: number | null;
  contentMd: string;
  tags: string[];
}>({
  title: '',
  folderId: null,
  contentMd: '',
  tags: []
})

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

// 插入文章引用（使用标题）
const insertArticle = (article: ContentItem) => {
  const ref = `[[!article:${article.id}]]`
  model.value.contentMd += `\n${ref}`
  showArticleModal.value = false
  message.success(`已插入引用: ${article.title}`)
}

// 搜索防抖
let searchTimer: any = null
watch(articleSearchKeyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    searchArticles()
  }, 300)
})

// 扁平化文件夹树用于 Select
const flattenFolders = (list: NoteFolder[], prefix = '') => {
  let result: any[] = []
  list.forEach(f => {
    result.push({ label: prefix + f.name, value: f.id })
    if (f.children && f.children.length > 0) {
      result = result.concat(flattenFolders(f.children, prefix + f.name + ' / '))
    }
  })
  return result
}

const fetchFolders = async () => {
  try {
    const res = await noteApi.getFolders()
    folderOptions.value = flattenFolders(res)
  } catch (e) {
    console.error(e)
  }
}

const fetchNote = async (id: number) => {
  loading.value = true
  try {
    const res = await noteApi.getNoteDetail(id)
    model.value = {
      title: res.title,
      folderId: res.folderId || null,
      contentMd: res.contentMd,
      tags: res.tags || []
    }
    outgoingLinks.value = res.outgoingLinks || []
    incomingLinks.value = res.incomingLinks || []
    contentLinks.value = res.contentLinks || []
  } catch (e: any) {
    message.error('加载笔记失败')
    router.push('/notes')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!model.value.title) {
    message.warning('请输入标题')
    return
  }
  
  saving.value = true
  try {
    if (noteId.value) {
      await noteApi.updateNote(noteId.value, {
        ...model.value,
        folderId: model.value.folderId ?? undefined
      })
      message.success('保存成功')
      await fetchNote(noteId.value)
    } else {
      const res = await noteApi.createNote(model.value as any)
      message.success('创建成功')
      router.replace(`/notes/${res.id}`)
      noteId.value = res.id
    }
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const goToNote = (id: number) => {
  router.push(`/notes/${id}`)
}

const openContentLink = (url: string) => {
  window.open(url, '_blank')
}

onMounted(() => {
  fetchFolders()
  const idParam = route.params.id
  if (idParam && idParam !== 'new') {
    noteId.value = Number(idParam)
    fetchNote(noteId.value)
    
    // 开始追踪笔记阅读时间
    noteReadingTracker.start(noteId.value)
  }
})

// 在组件卸载前停止追踪
onBeforeUnmount(() => {
  noteReadingTracker.stop()
})

// 监听路由变化（切换笔记）
watch(() => route.params.id, (newId) => {
  if (newId && newId !== 'new') {
    const id = Number(newId)
    noteId.value = id
   fetchNote(id)
    
    // 重新开始追踪新笔记
    noteReadingTracker.start(id)
  }
})
</script>

<template>
  <div class="note-editor h-full flex p-4 gap-4">
    <!-- 主编辑区 -->
    <div class="flex-1 flex flex-col bg-white rounded-lg shadow-sm">
      <div class="header p-4 flex justify-between items-center border-b">
        <div class="flex items-center gap-4 flex-1">
          <n-button circle secondary @click="router.back()">
            <template #icon><n-icon><ArrowBackOutline /></n-icon></template>
          </n-button>
          <n-input 
            v-model:value="model.title" 
            placeholder="无标题笔记" 
            class="text-lg font-bold flex-1" 
            :bordered="false"
          />
        </div>
        <n-space>
          <n-select 
            v-model:value="model.folderId" 
            :options="folderOptions" 
            placeholder="选择文件夹" 
            class="w-48" 
            size="small"
          />
          <n-button quaternary @click="showLinksPanel = !showLinksPanel">
            <template #icon><n-icon><LinkOutline /></n-icon></template>
          </n-button>
          <n-button type="primary" :loading="saving" @click="handleSave">
            <template #icon><n-icon><SaveOutline /></n-icon></template>
            保存
          </n-button>
        </n-space>
      </div>
      
      <div class="editor-content flex-1 overflow-hidden relative p-4">
        <n-spin :show="loading">
          <MarkdownEditor 
            v-model="model.contentMd" 
            @save="handleSave" 
            height="100%"
          />
        </n-spin>
      </div>
    </div>

    <!-- 链接面板 -->
    <n-collapse-transition :show="showLinksPanel">
      <div class="links-panel w-72 flex flex-col gap-4">
        <!-- 引用的订阅内容 -->
        <n-card size="small" title="📰 引用的文章" v-if="contentLinks.length > 0">
          <div class="flex flex-col gap-2">
            <div 
              v-for="link in contentLinks" 
              :key="link.id" 
              class="link-item p-2 rounded bg-blue-50 hover:bg-blue-100 cursor-pointer"
              @click="openContentLink(link.url)"
            >
              <div class="font-medium text-sm text-blue-800 truncate">{{ link.title }}</div>
              <div class="text-xs text-gray-500 mt-1" v-if="link.sourceName">来源: {{ link.sourceName }}</div>
            </div>
          </div>
        </n-card>

        <!-- 引用的笔记 -->
        <n-card size="small" title="📝 引用的笔记" v-if="outgoingLinks.length > 0">
          <div class="flex flex-col gap-2">
            <div 
              v-for="link in outgoingLinks" 
              :key="link.id" 
              class="link-item p-2 rounded bg-green-50 hover:bg-green-100 cursor-pointer"
              @click="goToNote(link.id)"
            >
              <div class="text-sm text-green-800">[[{{ link.title }}]]</div>
            </div>
          </div>
        </n-card>

        <!-- 被引用 -->
        <n-card size="small" title="🔗 被以下笔记引用" v-if="incomingLinks.length > 0">
          <div class="flex flex-col gap-2">
            <div 
              v-for="link in incomingLinks" 
              :key="link.id" 
              class="link-item p-2 rounded bg-purple-50 hover:bg-purple-100 cursor-pointer"
              @click="goToNote(link.id)"
            >
              <div class="text-sm text-purple-800">{{ link.title }}</div>
            </div>
          </div>
        </n-card>

        <!-- 插入引用按钮 -->
        <n-card size="small" title="➕ 插入引用">
          <div class="flex flex-col gap-2">
            <n-button block @click="openArticleModal">
              📰 插入订阅文章
            </n-button>
            <div class="text-xs text-gray-500 mt-2">
              <p>💡 输入 <code class="bg-gray-100 px-1 rounded">[[笔记标题]]</code> 引用其他笔记</p>
            </div>
          </div>
        </n-card>
      </div>
    </n-collapse-transition>

    <!-- 文章搜索弹窗 -->
    <n-modal v-model:show="showArticleModal" preset="card" title="选择要引用的文章" style="width: 600px; max-width: 90vw;">
      <div class="space-y-4">
        <n-input 
          v-model:value="articleSearchKeyword" 
          placeholder="搜索文章标题..." 
          clearable
        />
        <n-spin :show="articleSearching">
          <div class="max-h-96 overflow-y-auto">
            <n-empty v-if="articleSearchResults.length === 0" description="暂无文章" />
            <div v-else class="space-y-2">
              <div 
                v-for="article in articleSearchResults" 
                :key="article.id"
                class="p-3 border rounded-lg hover:bg-blue-50 cursor-pointer transition"
                @click="insertArticle(article)"
              >
                <div class="font-medium">{{ article.title }}</div>
                <div class="text-xs text-gray-500 mt-1 flex gap-2">
                  <span v-if="article.sourceName">来源: {{ article.sourceName }}</span>
                  <span v-if="article.publishTime">{{ article.publishTime }}</span>
                </div>
                <div class="text-sm text-gray-600 mt-1 line-clamp-2" v-if="article.summary">
                  {{ article.summary }}
                </div>
              </div>
            </div>
          </div>
        </n-spin>
      </div>
    </n-modal>
  </div>
</template>

<style scoped>
.links-panel {
  min-width: 280px;
}
.link-item {
  transition: all 0.2s;
}
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
