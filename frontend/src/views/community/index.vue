<template>
  <div class="h-full flex flex-col p-6 max-w-[1400px] mx-auto w-full">
    <!-- 头部搜索区 -->
    <div class="flex justify-between items-center mb-8">
      <div>
        <h1 class="text-2xl font-bold bg-gradient-to-r from-violet-600 to-indigo-600 bg-clip-text text-transparent">
          社区大厅
        </h1>
        <p class="text-gray-500 mt-1">发现优秀的公开笔记与知识</p>
      </div>
      <div class="flex items-center gap-4">
        <n-button
          type="primary"
          secondary
          size="large"
          round
          @click="router.push('/community/galaxy')"
          class="flex shadow-sm hover:shadow transition-shadow px-5 group"
        >
          <template #icon>
            <span class="group-hover:scale-110 transition-transform">🌌</span>
          </template>
          知识银河星云
        </n-button>
        
        <div class="w-96">
          <n-input
            v-model:value="keyword"
            placeholder="搜索公开笔记..."
            clearable
            size="large"
            round
            @keyup.enter="handleSearch"
            :loading="loading"
          >
            <template #prefix>
              <n-icon class="mr-2 text-gray-400">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5A6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5S14 7.01 14 9.5S11.99 14 9.5 14z"/></svg>
              </n-icon>
            </template>
          </n-input>
        </div>
      </div>
    </div>

    <!-- 主体区域：左侧内容 + 右侧排行榜 -->
    <div class="flex-1 flex gap-6 overflow-hidden">
      
      <!-- 左瀑布流 -->
      <div class="flex-1 flex flex-col overflow-hidden bg-white/50 rounded-2xl p-2 border border-gray-100 shadow-sm backdrop-blur-sm">
        
        <n-tabs v-model:value="activeTab" type="line" animated class="px-2" @update:value="handleTabChange">
          <n-tab-pane name="all" tab="🌍 推荐广场">
             <!-- Empty space, handled globally below -->
          </n-tab-pane>
          <n-tab-pane name="following" tab="❤️ 我关注的">
             <!-- Empty space, handled globally below -->
          </n-tab-pane>
        </n-tabs>

        <div v-if="loading && !notes.length" class="flex justify-center py-20 flex-1">
          <n-spin size="large" />
        </div>

        <div v-else-if="!notes.length" class="flex flex-col items-center justify-center py-20 text-gray-400 flex-1">
          <div class="text-6xl mb-4">📭</div>
          <p class="text-lg">暂无相关的公开笔记，等你来探索！</p>
        </div>

        <div v-else class="flex-1 min-h-0 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3 gap-6 auto-rows-max overflow-y-auto pb-8 pt-4 custom-scrollbar pr-2 mt-2 px-2">
          <div
            v-for="note in notes"
            :key="note.id"
            class="group bg-white rounded-2xl p-5 border border-gray-100 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 cursor-pointer flex flex-col h-64"
            @click="viewNote(note.id)"
          >
            <!-- 卡片头部: 作者信息 -->
            <div class="flex items-center gap-3 mb-4" @click.stop="goToProfile(note.authorId!)">
              <n-avatar
                round
                size="small"
                :src="note.authorAvatar || 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'"
                class="border border-gray-100 cursor-pointer"
              />
              <span class="text-sm text-gray-600 font-medium truncate hover:text-indigo-600 cursor-pointer transition-colors">{{ note.authorName || '匿名用户' }}</span>
              <span class="ml-auto text-xs text-gray-400 bg-gray-50 px-2 py-1 rounded-full">
                {{ formatTime(note.updatedTime) }}
              </span>
            </div>

            <!-- 标题与内容 -->
            <h3 class="text-lg font-bold text-gray-800 mb-2 group-hover:text-indigo-600 transition-colors line-clamp-1">
              {{ note.title }}
            </h3>
            
            <p class="text-gray-500 text-sm leading-relaxed line-clamp-4 flex-1">
              {{ note.summary || '暂无摘要...' }}
            </p>

            <!-- 底部元数据 -->
            <div class="mt-4 pt-4 border-t border-gray-50 flex items-center justify-between text-xs text-gray-400">
              <div class="flex items-center gap-4">
                <span class="flex items-center gap-1">
                   <n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5s5 2.24 5 5s-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3s3-1.34 3-3s-1.34-3-3-3z"/></svg></n-icon>
                   {{ note.viewCount || 0 }}
                </span>
                 <span class="flex items-center gap-1 hover:text-red-500 transition-colors">
                   <n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="m12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5C2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></n-icon>
                   {{ note.likeCount || 0 }}
                </span>
              </div>
              <span class="px-2 py-0.5 rounded bg-indigo-50 text-indigo-500 font-medium">
                 {{ note.folderName || '未分类' }}
              </span>
            </div>
          </div>
        </div>
        
        <!-- 加载更多 -->
        <div v-if="notes.length > 0" class="flex justify-center mt-6 pb-2 shrink-0">
           <n-pagination
             v-model:page="page"
             :page-count="totalPages"
             :page-size="size"
             @update:page="fetchNotes"
           />
        </div>
      </div>

      <!-- 右侧社区热榜 -->
      <div class="w-[320px] flex-shrink-0 hidden lg:flex flex-col bg-white rounded-2xl border border-gray-100 p-5 shadow-sm overflow-hidden h-fit max-h-full">
        <h2 class="text-lg font-bold mb-4 flex items-center gap-2 text-gray-800">
          <span class="text-orange-500">🔥</span> 社区热议榜
        </h2>
        
        <n-spin :show="rankingLoading">
          <div v-if="rankingList.length === 0" class="text-gray-400 text-sm py-4 text-center">
            暂无热榜数据
          </div>
          <div v-else class="flex flex-col gap-3 overflow-y-auto custom-scrollbar pr-2 pb-2">
            <div 
              v-for="(rank, index) in rankingList" 
              :key="rank.id"
              class="flex items-start gap-3 p-3 rounded-xl hover:bg-gray-50/80 cursor-pointer transition-colors border border-transparent hover:border-gray-100"
              @click="viewNote(rank.id)"
            >
              <div 
                class="w-6 h-6 rounded flex items-center justify-center font-bold text-xs flex-shrink-0"
                :class="[
                  index === 0 ? 'bg-red-100 text-red-600' : 
                  index === 1 ? 'bg-orange-100 text-orange-600' : 
                  index === 2 ? 'bg-amber-100 text-amber-600' : 
                  'bg-gray-100 text-gray-500'
                ]"
              >
                {{ index + 1 }}
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-sm font-bold text-gray-800 line-clamp-1 group-hover:text-indigo-600 mb-1">
                  {{ rank.title }}
                </div>
                <div class="flex justify-between items-center text-xs text-gray-500">
                  <span class="truncate pr-2">{{ rank.authorName || '匿名' }}</span>
                  <span class="flex items-center gap-1 flex-shrink-0">
                    <n-icon class="text-red-400"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="m12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5C2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></n-icon>
                    {{ rank.likeCount || 0 }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </n-spin>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NInput, NIcon, NAvatar, NSpin, NPagination, useMessage, NButton, NTabs, NTabPane } from 'naive-ui'
import { getCommunityFeed, getRanking, getFollowingFeed } from '@/api/community'
import type { Note } from '@/api/note'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()
const message = useMessage()

const activeTab = ref('all')
const loading = ref(false)
const notes = ref<Note[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(12) // 从 9 加到 12 填补右侧被推后的下巴
const totalPages = ref(1)

const rankingLoading = ref(false)
const rankingList = ref<Note[]>([])

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  return dayjs(time).fromNow()
}

// 获取笔记
const fetchNotes = async () => {
  try {
    loading.value = true
    let res;
    if (activeTab.value === 'following') {
       res = await getFollowingFeed(page.value, size.value)
    } else {
       res = await getCommunityFeed(keyword.value, page.value, size.value)
    }
    notes.value = res.records
    totalPages.value = res.pages
  } catch (error) {
    message.error('获取社区动态失败，请检查是否已登录')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 切换Tab
const handleTabChange = (val: string) => {
  activeTab.value = val
  page.value = 1
  if (val === 'following') keyword.value = '' // 清空搜索以便于只看关注
  fetchNotes()
}

// 获取热议榜
const fetchRanking = async () => {
  try {
    rankingLoading.value = true
    const res = await getRanking(10)
    rankingList.value = res
  } catch (error) {
    message.error('获取排行榜失败')
    console.error(error)
  } finally {
    rankingLoading.value = false
  }
}

// 搜索 (Debounce logic implied by enter key or add debounce if watching)
const handleSearch = () => {
  page.value = 1
  fetchNotes()
}

// 监听搜索词清空
watch(keyword, (newVal) => {
  if (!newVal) {
    handleSearch()
  }
})

// 查看详情
const viewNote = (id: number) => {
  router.push(`/community/note/${id}`)
}

// 查看作者主页
const goToProfile = (userId: number) => {
  if (userId) {
    router.push(`/profile/${userId}`)
  }
}

onMounted(() => {
  fetchNotes()
  fetchRanking()
})
</script>

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
