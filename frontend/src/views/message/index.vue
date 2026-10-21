<template>
  <div class="h-full w-full bg-gray-50 flex overflow-hidden rounded-2xl">
    <!-- 左侧：会话列表 -->
    <div class="w-80 bg-white border-r border-gray-100 flex flex-col font-sans">
      <div class="p-4 border-b border-gray-100 flex items-center justify-between">
        <h2 class="text-xl font-bold text-gray-800">消息中心</h2>
      </div>
      
      <div class="flex-1 overflow-y-auto">
        <n-spin :show="loadingConversations">
          <n-list hoverable clickable class="!bg-transparent">
            <template v-if="conversations.length === 0 && !loadingConversations">
              <div class="p-8 text-center text-gray-400">暂无会话</div>
            </template>
            <n-list-item 
              v-for="conv in conversations" 
              :key="conv.contactId"
              :class="['transition-colors p-4 cursor-pointer', activeContactId === conv.contactId ? 'bg-indigo-50 border-r-4 border-indigo-500' : 'hover:bg-gray-50']"
              @click="selectContact(conv.contactId, conv.contactName, conv.contactAvatar)"
            >
              <div class="flex items-center gap-3">
                <n-badge :value="conv.unreadCount" :max="99">
                  <n-avatar round size="large" :src="conv.contactAvatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
                </n-badge>
                <div class="flex-1 min-w-0">
                  <div class="flex justify-between items-baseline mb-1">
                    <h3 class="font-medium text-gray-800 truncate">{{ conv.contactName }}</h3>
                    <span class="text-xs text-gray-400 shrink-0">{{ formatTimeShort(conv.lastTime) }}</span>
                  </div>
                  <p class="text-sm text-gray-500 truncate">{{ conv.lastMessage }}</p>
                </div>
              </div>
            </n-list-item>
          </n-list>
        </n-spin>
      </div>
    </div>

    <!-- 右侧：聊天窗口 -->
    <div class="flex-1 flex flex-col bg-[#f0f2f5] relative h-full">
      <template v-if="activeContactId">
        <!-- 聊天头部 -->
        <div class="h-14 bg-white border-b border-gray-200 flex items-center px-6 shrink-0 z-10 shadow-sm">
          <div class="flex items-center gap-3">
            <n-avatar round size="small" :src="activeContactAvatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
            <span class="font-medium text-lg text-gray-800">{{ activeContactName }}</span>
            <n-button text class="ml-2 text-indigo-500 text-xs" @click="$router.push(`/profile/${activeContactId}`)">查看主页</n-button>
          </div>
        </div>

        <!-- 消息流 -->
        <div class="flex-1 overflow-y-auto p-6" ref="messageListRef" @scroll="handleScroll">
          <!-- 加载更多提示 -->
          <div v-if="loadingHistory" class="text-center py-2">
            <n-spin size="small" />
          </div>
          <div v-if="!hasMoreHistory && history.length > 0" class="text-center py-2 text-xs text-gray-400">
            没有更多历史消息了
          </div>

          <!-- 消息气泡 -->
          <div class="flex flex-col gap-6 mt-4">
            <div 
              v-for="msg in history" 
              :key="msg.id"
              :class="['flex', String(msg.senderId) === String(currentUserId) ? 'justify-end' : 'justify-start']"
            >
              <div class="flex gap-3 max-w-[70%]" :class="[String(msg.senderId) === String(currentUserId) ? 'flex-row-reverse' : 'flex-row']">
                <!-- 头像 -->
                <n-avatar 
                  round 
                  class="shrink-0 mt-1"
                  :src="msg.senderAvatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" 
                />
                
                <!-- 内容区 -->
                <div :class="['flex flex-col', String(msg.senderId) === String(currentUserId) ? 'items-end' : 'items-start']">
                  <!-- 昵称和时间 -->
                  <div class="flex items-baseline gap-2 mb-1 px-1">
                    <span class="text-xs text-gray-500">{{ String(msg.senderId) === String(currentUserId) ? '我' : activeContactName }}</span>
                    <span class="text-[10px] text-gray-400">{{ formatTimeDetailed(msg.createdTime) }}</span>
                  </div>
                  <!-- 气泡本身 -->
                  <div 
                    :class="[
                      'px-4 py-2.5 rounded-2xl text-[15px] leading-relaxed break-words shadow-sm',
                      String(msg.senderId) === String(currentUserId) 
                        ? 'bg-indigo-500 text-white rounded-tr-none' 
                        : 'bg-white text-gray-800 rounded-tl-none'
                    ]"
                  >
                    {{ msg.content }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 发送区 -->
        <div class="bg-white border-t border-gray-200 p-4 shrink-0">
          <div class="flex gap-3 max-w-5xl mx-auto items-end">
            <n-input
              v-model:value="inputValue"
              type="textarea"
              placeholder="按 Enter 发送，Shift + Enter 换行"
              :autosize="{ minRows: 1, maxRows: 5 }"
              class="flex-1 !bg-gray-50 rounded-xl"
              @keydown.enter.prevent="handleEnter"
            />
            <n-button 
              type="primary" 
              class="shrink-0 h-[40px] px-6 rounded-xl shadow-sm shadow-indigo-200"
              :disabled="!inputValue.trim()"
              :loading="sending"
              @click="send"
            >
              发送
            </n-button>
          </div>
        </div>
      </template>

      <!-- 未选择联系人时 -->
      <div v-else class="flex-1 flex flex-col items-center justify-center text-gray-400">
        <n-icon size="60" class="mb-4 text-gray-300">
          <ChatbubbleOutline />
        </n-icon>
        <p>选择一个联系人开始聊天</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { NList, NListItem, NAvatar, NBadge, NSpin, NInput, NButton, NIcon, useMessage } from 'naive-ui'
import { ChatbubbleOutline } from '@vicons/ionicons5'
import { getConversations, getChatHistory, sendMessage, markAsRead } from '@/api/message'
import type { MessageConversationDto, UserMessageDto } from '@/api/message'
import { useUserStore } from '@/store/user'
import { getUserPublicProfile } from '@/api/community'
import dayjs from 'dayjs'
import isToday from 'dayjs/plugin/isToday'
import isYesterday from 'dayjs/plugin/isYesterday'

dayjs.extend(isToday)
dayjs.extend(isYesterday)

const route = useRoute()
const message = useMessage()
const userStore = useUserStore()

const currentUserId = computed(() => userStore.userInfo?.id)
const currentUserAvatar = computed(() => userStore.userInfo?.avatar)

// State
const conversations = ref<MessageConversationDto[]>([])
const loadingConversations = ref(false)

const activeContactId = ref<number | null>(null)
const activeContactName = ref('')
const activeContactAvatar = ref<string | null>(null)

const history = ref<UserMessageDto[]>([])
const loadingHistory = ref(false)
const historyPage = ref(1)
const hasMoreHistory = ref(true)

const inputValue = ref('')
const sending = ref(false)
const messageListRef = ref<HTMLElement | null>(null)

let lastScrollHeight = 0

// Format utils
const formatTimeShort = (timeStr: string) => {
  const d = dayjs(timeStr)
  if (d.isToday()) return d.format('HH:mm')
  if (d.isYesterday()) return '昨天'
  return d.format('MM-DD')
}

const formatTimeDetailed = (timeStr: string) => {
  const d = dayjs(timeStr)
  if (d.isToday()) return d.format('今天 HH:mm')
  if (d.isYesterday()) return d.format('昨天 HH:mm')
  return d.format('YYYY-MM-DD HH:mm')
}

// Load initial data
onMounted(async () => {
  await loadConversations()
  
  // if contactId is in query, select it or fetch it
  const queryContactId = Number(route.query.contactId)
  if (queryContactId && !isNaN(queryContactId)) {
    const existingConv = conversations.value.find(c => c.contactId === queryContactId)
    if (existingConv) {
      selectContact(existingConv.contactId, existingConv.contactName, existingConv.contactAvatar)
    } else {
      // Need to fetch user profile to get name/avatar to start a new fake chat row temporarily
      try {
        const profile = await getUserPublicProfile(queryContactId)
        activeContactId.value = queryContactId
        activeContactName.value = profile.username
        activeContactAvatar.value = profile.avatar
        history.value = []
        hasMoreHistory.value = false
      } catch(e) {
        message.error('无法发起与该用户的私信')
      }
    }
  }
})

// Focus & Load History
const selectContact = async (id: number, name: string, avatar: string | null) => {
  activeContactId.value = id
  activeContactName.value = name
  activeContactAvatar.value = avatar
  
  history.value = []
  historyPage.value = 1
  hasMoreHistory.value = true
  lastScrollHeight = 0
  
  // Clear unread
  const convIndex = conversations.value.findIndex(c => c.contactId === id)
  if (convIndex > -1 && conversations.value[convIndex].unreadCount > 0) {
    try {
      await markAsRead(id)
      conversations.value[convIndex].unreadCount = 0
      userStore.fetchUnreadMessageCount() // global notification update
    } catch(e) {}
  }
  
  await loadHistory(true)
}

const loadConversations = async () => {
  loadingConversations.value = true
  try {
    conversations.value = await getConversations()
  } catch (e: any) {
    message.error('加载会话列表失败')
  } finally {
    loadingConversations.value = false
  }
}

const loadHistory = async (scrollToBottom: boolean = false) => {
  if (!activeContactId.value || loadingHistory.value || !hasMoreHistory.value) return
  
  loadingHistory.value = true
  // remember scroll height before new items arrive
  if (messageListRef.value) {
    lastScrollHeight = messageListRef.value.scrollHeight
  }

  try {
    const res = await getChatHistory(activeContactId.value, historyPage.value, 20)
    // records are ordered desc in backend usually, meaning page 1 contains newest
    // so we need to reverse them to display chronological order: top=old, bottom=new
    const oldMessages = [...res.records].reverse()
    
    console.log('--- Debug Chat History ---')
    console.log('currentUserId.value:', currentUserId.value)
    if (oldMessages.length > 0) {
      console.log('Sample msg.senderId:', oldMessages[oldMessages.length - 1].senderId)
      console.log('Comparison:', String(oldMessages[oldMessages.length - 1].senderId) === String(currentUserId.value))
    }
    
    // prepend to history
    history.value = [...oldMessages, ...history.value]
    
    if (res.records.length < 20) {
      hasMoreHistory.value = false
    } else {
      historyPage.value++
    }
    
    nextTick(() => {
      if (messageListRef.value) {
        if (scrollToBottom) {
          messageListRef.value.scrollTop = messageListRef.value.scrollHeight
        } else {
          // retain scroll position
          const newScrollHeight = messageListRef.value.scrollHeight
          messageListRef.value.scrollTop = newScrollHeight - lastScrollHeight
        }
      }
    })
    
  } catch (e: any) {
    message.error('加载聊天记录失败')
  } finally {
    loadingHistory.value = false
  }
}

const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  // fetch more history if hit top
  if (target.scrollTop <= 10 && !loadingHistory.value && hasMoreHistory.value) {
    loadHistory()
  }
}

const handleEnter = (e: KeyboardEvent) => {
  if (e.shiftKey) return // shift+enter allows newline
  send()
}

const send = async () => {
  const text = inputValue.value.trim()
  if (!text || !activeContactId.value) return
  
  sending.value = true
  try {
    await sendMessage(activeContactId.value, text)
    // optimistic update or refetch page 1
    const newMsg: UserMessageDto = {
      id: Date.now(), // temp fake id
      senderId: currentUserId.value!,
      senderName: '我',
      senderAvatar: currentUserAvatar.value || null,
      receiverId: activeContactId.value,
      receiverName: activeContactName.value,
      receiverAvatar: activeContactAvatar.value,
      content: text,
      isRead: 0,
      createdTime: new Date().toISOString()
    }
    history.value.push(newMsg)
    inputValue.value = ''
    
    nextTick(() => {
      if (messageListRef.value) {
        messageListRef.value.scrollTop = messageListRef.value.scrollHeight
      }
    })
    
    // Refresh conversation list to bump it up
    await loadConversations()
    
  } catch (e: any) {
    message.error(e.message || '发送失败')
  } finally {
    sending.value = false
  }
}
</script>

<style scoped>
/* 自定义滚动条隐蔽一点 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}
.overflow-y-auto::-webkit-scrollbar-thumb {
  background-color: rgba(0,0,0,0.1);
  border-radius: 4px;
}
</style>
