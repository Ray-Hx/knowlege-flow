<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useMessage, NButton, NSpin, NIcon, NScrollbar, NAvatar, NTooltip } from 'naive-ui'
import { Send, ChatbubbleEllipsesOutline, TrashOutline, DocumentTextOutline, Add, SparklesOutline, SyncOutline } from '@vicons/ionicons5'
import { aiApi, Conversation, Message } from '@/api/ai'
import { noteApi } from '@/api/note'
import { useUserStore } from '@/store/user'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

const message = useMessage()
const userStore = useUserStore()
const conversations = ref<Conversation[]>([])
const messages = ref<Message[]>([])
const input = ref('')
const currentConversationId = ref<number | null>(null)
const sending = ref(false)
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const syncing = ref(false)
const messageListRef = ref<any>(null)
const messageRefs = ref<Record<number, HTMLElement>>({})

// 用户头像（从 store 获取）
const userAvatar = ref(userStore.userInfo?.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')



// 渲染 Markdown
const renderMarkdown = async (content: string, el: HTMLElement) => {
  if (!el || !content) return
  await Vditor.preview(el as HTMLDivElement, content, {
    mode: 'light',
    theme: { current: 'light' },
    hljs: { style: 'github', lineNumber: true },
    markdown: {
      toc: false,
      mark: true,
      footnotes: true,
      autoSpace: true,
    },
    after() {
      // 渲染完成后滚动
      scrollToBottom()
    }
  })
}

// 监听消息变化，渲染 AI 消息的 Markdown
watch(messages, async () => {
  await nextTick()
  for (const msg of messages.value) {
    if (msg.role === 'assistant' && messageRefs.value[msg.id]) {
      await renderMarkdown(msg.content, messageRefs.value[msg.id])
    }
  }
}, { deep: true })

const fetchConversations = async () => {
  loadingConversations.value = true
  try {
    const res = await aiApi.getConversations(1, 100)
    conversations.value = res.records
    if (!currentConversationId.value && conversations.value.length > 0) {
      selectConversation(conversations.value[0].id)
    }
  } catch (e) {
    message.error('加载对话列表失败')
  } finally {
    loadingConversations.value = false
  }
}

const selectConversation = async (id: number) => {
  currentConversationId.value = id
  loadingMessages.value = true
  messages.value = []
  messageRefs.value = {}
  try {
    messages.value = await aiApi.getMessages(id)
    scrollToBottom()
  } catch (e) {
    message.error('加载消息失败')
  } finally {
    loadingMessages.value = false
  }
}

const createConversation = async () => {
  try {
    const conv = await aiApi.createConversation(`新对话 ${new Date().toLocaleString()}`)
    conversations.value.unshift(conv)
    selectConversation(conv.id)
  } catch (e) {
    message.error('创建对话失败')
  }
}

const deleteConversation = async (e: Event, id: number) => {
  e.stopPropagation()
  try {
    await aiApi.deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (currentConversationId.value === id) {
      currentConversationId.value = null
      messages.value = []
      if (conversations.value.length > 0) {
        selectConversation(conversations.value[0].id)
      }
    }
    message.success('已删除')
  } catch (e) {
    message.error('删除失败')
  }
}

const handleSend = async () => {
  if (!input.value.trim() || !currentConversationId.value) return
  
  const userMsg = input.value
  input.value = ''
  sending.value = true
  
  // 乐观更新：先显示用户消息
  const tempUserMsgId = Date.now()
  messages.value.push({
    id: tempUserMsgId,
    role: 'user',
    content: userMsg,
    createdTime: new Date().toISOString()
  })
  scrollToBottom()

  try {
    const aiMsg = await aiApi.sendMessage(currentConversationId.value, userMsg)
    messages.value.push(aiMsg)
    scrollToBottom()
  } catch (e) {
    message.error('发送消息失败')
  } finally {
    sending.value = false
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTo({ top: 99999, behavior: 'smooth' })
    }
  })
}

const setMessageRef = (id: number, el: HTMLDivElement | null) => {
  if (el) {
    messageRefs.value[id] = el
  }
}

// 同步笔记到知识库
const syncNotesToKnowledge = async () => {
  syncing.value = true
  try {
    const count = await noteApi.rebuildIndex()
    message.success(`已同步 ${count} 条笔记到知识库`)
  } catch (e) {
    message.error('同步失败，请稍后重试')
  } finally {
    syncing.value = false
  }
}

onMounted(() => {
  fetchConversations()
})
</script>

<template>
  <div class="ai-page h-full flex gap-4 overflow-hidden">
    <!-- Sidebar -->
    <div class="w-72 glass-card flex flex-col overflow-hidden">
      <div class="p-4 border-b border-white/30 flex justify-between items-center bg-white/30 backdrop-blur-sm">
        <h3 class="font-bold text-gray-700">对话历史</h3>
        <div class="flex gap-2">
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button size="small" type="info" :loading="syncing" circle @click="syncNotesToKnowledge">
                <template #icon><n-icon><SyncOutline /></n-icon></template>
              </n-button>
            </template>
            同步笔记到知识库
          </n-tooltip>
          <n-button size="small" type="primary" class="liquid-button" circle @click="createConversation">
            <template #icon><n-icon><Add /></n-icon></template>
          </n-button>
        </div>
      </div>
      
      <div class="flex-1 overflow-auto p-2">
        <n-spin :show="loadingConversations">
          <div v-if="conversations.length === 0 && !loadingConversations" class="p-4 text-center text-gray-400 text-sm">
            暂无对话记录
          </div>
          <div class="space-y-2" v-else>
            <div
              v-for="conv in conversations"
              :key="conv.id"
              class="group relative p-3 rounded-xl cursor-pointer transition-all duration-300 border border-transparent"
              :class="currentConversationId === conv.id ? 'bg-white/60 shadow-sm border-white/40' : 'hover:bg-white/30'"
              @click="selectConversation(conv.id)"
            >
              <div class="flex justify-between items-center">
                <div class="truncate text-sm font-medium text-gray-700 flex-1 pr-2">
                  {{ conv.title }}
                </div>
                <n-button 
                  size="tiny" 
                  quaternary 
                  circle 
                  type="error"
                  class="opacity-0 group-hover:opacity-100 transition-opacity"
                  @click="(e) => deleteConversation(e, conv.id)"
                >
                  <template #icon><n-icon><TrashOutline /></n-icon></template>
                </n-button>
              </div>
              <div class="text-xs text-gray-400 mt-1">
                {{ new Date(conv.createdTime).toLocaleDateString() }}
              </div>
            </div>
          </div>
        </n-spin>
      </div>
    </div>

    <!-- Chat Area -->
    <div class="flex-1 flex flex-col h-full glass-card overflow-hidden relative">
      <div v-if="!currentConversationId" class="flex-1 flex items-center justify-center text-gray-400 flex-col">
        <div class="w-24 h-24 bg-gradient-to-tr from-indigo-100 to-purple-100 rounded-full flex items-center justify-center mb-6 animate-pulse">
          <n-icon size="48" class="text-indigo-400"><ChatbubbleEllipsesOutline /></n-icon>
        </div>
        <p class="text-lg font-medium text-gray-600">开始一次新的对话</p>
        <p class="text-sm opacity-60 mt-2">基于您的个人知识库提供智能问答</p>
      </div>

      <template v-else>
        <!-- Messages -->
        <n-scrollbar ref="messageListRef" class="flex-1 p-6" trigger="none">
          <n-spin :show="loadingMessages">
            <div class="space-y-6 pb-4 max-w-4xl mx-auto">
              <div 
                v-for="msg in messages" 
                :key="msg.id" 
                class="flex gap-3 animate-fade-in"
                :class="msg.role === 'user' ? 'flex-row-reverse' : 'flex-row'"
              >
                <!-- Avatar -->
                <div class="flex-shrink-0">
                  <n-avatar 
                    v-if="msg.role === 'user'"
                    round 
                    :size="40"
                    :src="userAvatar"
                    class="shadow-md border-2 border-white"
                  />
                  <div 
                    v-else 
                    class="w-10 h-10 rounded-full bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center shadow-md border-2 border-white"
                  >
                    <n-icon size="20" color="white"><SparklesOutline /></n-icon>
                  </div>
                </div>

                <!-- Message Bubble -->
                <div 
                  class="max-w-[80%] rounded-2xl shadow-sm"
                  :class="msg.role === 'user' 
                    ? 'bg-gradient-to-br from-indigo-500 to-purple-600 text-white p-4' 
                    : 'bg-white/90 border border-gray-100 text-gray-800'"
                >
                  <!-- 用户消息：直接显示文本 -->
                  <div v-if="msg.role === 'user'" class="whitespace-pre-wrap leading-relaxed text-sm">
                    {{ msg.content }}
                  </div>
                  
                  <!-- AI 消息：Markdown 渲染 -->
                  <div 
                    v-else 
                    :ref="(el) => setMessageRef(msg.id, el as HTMLDivElement)"
                    class="markdown-body p-4 prose prose-sm max-w-none prose-headings:text-gray-800 prose-p:text-gray-700 prose-code:text-purple-600 prose-pre:bg-gray-50 prose-pre:border prose-pre:border-gray-200"
                  >
                    <!-- Vditor 会渲染到这里 -->
                  </div>
                  
                  <!-- References -->
                  <div v-if="msg.references && msg.references.length > 0" class="mt-3 pt-3 border-t border-gray-200/50 px-4 pb-4">
                    <div class="text-xs text-gray-500 mb-2 font-bold flex items-center gap-1">
                      <n-icon><DocumentTextOutline /></n-icon> 参考资料
                    </div>
                    <div class="grid grid-cols-1 gap-2">
                      <div 
                        v-for="ref in msg.references" 
                        :key="ref.noteId" 
                        class="text-xs bg-gray-50 p-2 rounded-lg cursor-pointer hover:bg-indigo-50 transition-colors border border-gray-100"
                      >
                        <router-link :to="`/notes/${ref.noteId}`" class="font-bold text-indigo-600 hover:underline block mb-1">
                          {{ ref.title }}
                        </router-link>
                        <p class="truncate text-gray-500">{{ ref.snippet }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </n-spin>
        </n-scrollbar>

        <!-- Input Area -->
        <div class="p-4 bg-white/40 backdrop-blur-md border-t border-white/30">
          <div class="flex gap-3 max-w-4xl mx-auto items-end">
            <div class="flex-1 relative">
              <textarea
                v-model="input"
                class="w-full bg-white/60 border border-white/50 rounded-2xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-indigo-500/30 focus:bg-white/80 transition-all resize-none text-gray-700 placeholder-gray-400"
                placeholder="输入你的问题... (Shift+Enter 换行)"
                rows="1"
                @keydown.enter.prevent="(e) => { if(!e.shiftKey) handleSend() }"
                style="min-height: 48px; max-height: 120px;"
              ></textarea>
            </div>
            <button 
              class="w-12 h-12 rounded-full flex items-center justify-center text-white shadow-lg liquid-button disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="sending || !input.trim()"
              @click="handleSend"
            >
              <n-spin v-if="sending" size="small" stroke="white" />
              <n-icon v-else size="20"><Send /></n-icon>
            </button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.4s ease-out forwards;
  opacity: 0;
  transform: translateY(10px);
}

@keyframes fadeIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Markdown 样式优化 */
:deep(.markdown-body) {
  font-size: 14px;
  line-height: 1.7;
}

:deep(.markdown-body h1),
:deep(.markdown-body h2),
:deep(.markdown-body h3) {
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
  color: #1f2937;
}

:deep(.markdown-body h1) { font-size: 1.5em; }
:deep(.markdown-body h2) { font-size: 1.3em; }
:deep(.markdown-body h3) { font-size: 1.1em; }

:deep(.markdown-body p) {
  margin: 0.5em 0;
}

:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

:deep(.markdown-body li) {
  margin: 0.25em 0;
}

:deep(.markdown-body code) {
  background: #f3f4f6;
  padding: 0.15em 0.4em;
  border-radius: 4px;
  font-size: 0.9em;
  color: #7c3aed;
}

:deep(.markdown-body pre) {
  background: #1e1e1e;
  border-radius: 8px;
  padding: 1em;
  overflow-x: auto;
  margin: 0.75em 0;
}

:deep(.markdown-body pre code) {
  background: transparent;
  padding: 0;
  color: #d4d4d4;
}

:deep(.markdown-body blockquote) {
  border-left: 4px solid #8b5cf6;
  padding-left: 1em;
  margin: 0.5em 0;
  color: #6b7280;
  background: #f9fafb;
  border-radius: 0 8px 8px 0;
  padding: 0.5em 1em;
}

:deep(.markdown-body table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.75em 0;
}

:deep(.markdown-body th),
:deep(.markdown-body td) {
  border: 1px solid #e5e7eb;
  padding: 0.5em 0.75em;
  text-align: left;
}

:deep(.markdown-body th) {
  background: #f9fafb;
  font-weight: 600;
}

:deep(.markdown-body a) {
  color: #6366f1;
  text-decoration: none;
}

:deep(.markdown-body a:hover) {
  text-decoration: underline;
}

/* Vditor 样式覆盖 */
:deep(.vditor-reset) {
  font-size: 14px !important;
  color: #374151 !important;
}

:deep(.vditor-reset pre) {
  border-radius: 8px !important;
}
</style>
