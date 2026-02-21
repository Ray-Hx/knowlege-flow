<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NButton, NIcon, useMessage, NEmpty } from 'naive-ui'
import { LibraryOutline, TimeOutline, CheckmarkCircleOutline, RefreshOutline } from '@vicons/ionicons5'
import ReviewSession from './components/ReviewSession.vue'
import { spacedApi, SpacedStats, ReviewNote } from '@/api/spaced'

const message = useMessage()
const isReviewing = ref(false)
const stats = ref<SpacedStats>({
  totalNotes: 0,
  dueToday: 0,
  reviewedToday: 0,
  masteredNotes: 0
})
const dueNotes = ref<ReviewNote[]>([])
const loading = ref(false)
const initializing = ref(false)

const fetchStats = async () => {
  try {
    stats.value = await spacedApi.getStats()
  } catch (e) {
    console.error(e)
  }
}

const startReview = async () => {
  loading.value = true
  try {
    const notes = await spacedApi.getDueNotes()
    if (notes.length > 0) {
      dueNotes.value = notes
      isReviewing.value = true
    } else {
      message.info('没有需要复习的笔记')
    }
  } catch (e: any) {
    message.error(e.message || '加载笔记失败')
  } finally {
    loading.value = false
  }
}

const handleReviewFinish = (stats?: { remembered: number; forgot: number }) => {
  isReviewing.value = false
  if (stats) {
    message.success(`复习完成！记住 ${stats.remembered} 篇，需加强 ${stats.forgot} 篇`)
  }
  fetchStats()
}

const initQueue = async () => {
  initializing.value = true
  try {
    await spacedApi.initReviewQueue()
    message.success('复习队列已初始化')
    fetchStats()
  } catch (e: any) {
    message.error(e.message || '初始化失败')
  } finally {
    initializing.value = false
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<template>
  <div class="review-page p-6 max-w-6xl mx-auto h-full flex flex-col">
    <!-- Review Session Mode -->
    <div v-if="isReviewing" class="h-full flex flex-col">
      <div class="mb-4">
        <n-button text class="text-white hover:text-white/80" @click="isReviewing = false">
          &lt; 返回概览
        </n-button>
      </div>
      <div class="flex-1 flex items-center justify-center">
        <ReviewSession :notes="dueNotes" @finish="handleReviewFinish" />
      </div>
    </div>

    <!-- Dashboard Mode -->
    <div v-else class="space-y-8 animate-fade-in">
      <div class="flex justify-between items-center">
        <div>
          <h2 class="text-3xl font-bold text-gray-800 tracking-tight">AI 智能复习</h2>
          <p class="text-gray-500 mt-1">基于笔记内容自动生成问题，科学记忆</p>
        </div>
        <n-button type="default" size="medium" class="glass-button" @click="initQueue" :loading="initializing">
          <template #icon><n-icon><RefreshOutline /></n-icon></template>
          初始化复习队列
        </n-button>
      </div>

      <!-- Stats Grid -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div class="glass-card p-6 flex flex-col items-center justify-center hover:scale-105 transition-transform">
          <div class="text-gray-500 text-sm mb-2 font-medium uppercase tracking-wider">待复习</div>
          <div class="text-4xl font-bold text-indigo-600">{{ stats.dueToday }}</div>
          <n-icon size="24" class="mt-2 text-indigo-400"><TimeOutline /></n-icon>
        </div>
        <div class="glass-card p-6 flex flex-col items-center justify-center hover:scale-105 transition-transform">
          <div class="text-gray-500 text-sm mb-2 font-medium uppercase tracking-wider">今日已复习</div>
          <div class="text-4xl font-bold text-emerald-500">{{ stats.reviewedToday }}</div>
          <n-icon size="24" class="mt-2 text-emerald-400"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="glass-card p-6 flex flex-col items-center justify-center hover:scale-105 transition-transform">
          <div class="text-gray-500 text-sm mb-2 font-medium uppercase tracking-wider">总笔记数</div>
          <div class="text-4xl font-bold text-blue-500">{{ stats.totalNotes }}</div>
          <n-icon size="24" class="mt-2 text-blue-400"><LibraryOutline /></n-icon>
        </div>
        <div class="glass-card p-6 flex flex-col items-center justify-center hover:scale-105 transition-transform">
          <div class="text-gray-500 text-sm mb-2 font-medium uppercase tracking-wider">已掌握</div>
          <div class="text-4xl font-bold text-purple-500">{{ stats.masteredNotes }}</div>
          <div class="text-xs text-purple-400 mt-2">Notes</div>
        </div>
      </div>

      <!-- Main Action Area -->
      <div class="glass-card p-12 text-center border-2 border-white/50 relative overflow-hidden">
        <div class="absolute inset-0 bg-gradient-to-r from-indigo-500/5 to-purple-500/5 pointer-events-none"></div>
        <div v-if="stats.dueToday > 0">
          <h3 class="text-2xl font-bold mb-6 text-gray-800">今天有 <span class="text-indigo-600">{{ stats.dueToday }}</span> 篇笔记需要复习</h3>
          <p class="text-gray-500 mb-6">AI 将根据笔记内容自动生成问答题，帮助你巩固记忆</p>
          <n-button type="primary" size="large" class="w-64 h-16 text-xl shadow-xl liquid-button" @click="startReview" :loading="loading">
            开始复习
          </n-button>
        </div>
        <div v-else class="py-8">
          <n-empty description="所有待办复习已完成！" size="large">
            <template #extra>
              <n-button size="small" class="glass-button" @click="initQueue" :loading="initializing">刷新复习队列</n-button>
            </template>
          </n-empty>
        </div>
      </div>


    </div>
  </div>
</template>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.5s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
