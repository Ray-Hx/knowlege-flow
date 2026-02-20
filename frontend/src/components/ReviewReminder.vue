<template>
  <div class="review-reminder glass-card p-6">
    <div class="flex justify-between items-center mb-4">
      <h3 class="text-lg font-bold text-gray-700">🔔 今日复习</h3>
      <span v-if="data" class="text-sm text-gray-500">{{ data.totalDue }}张卡片</span>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-8">
      <n-spin size="small" />
    </div>

    <!-- Empty State -->
    <div v-else-if="!data || data.totalDue === 0" class="text-center py-8">
      <div class="text-6xl mb-4">🎉</div>
      <p class="text-gray-600 font-medium">太棒了！今天没有需要复习的卡片</p>
      <p class="text-gray-400 text-sm mt-2">继续保持，明天见！</p>
    </div>

    <!-- Review Cards -->
    <div v-else class="space-y-4">
      <!-- High Priority -->
      <div v-if="data.highPriority.length > 0">
        <p class="text-xs font-medium text-orange-500 mb-2 flex items-center gap-1">
          🔥 高优先级 (遗忘风险高)
        </p>
        <div class="space-y-2">
          <div 
            v-for="card in data.highPriority.slice(0, 3)" 
            :key="card.cardId"
            class="flex items-center gap-3 p-3 bg-orange-50/50 rounded-lg hover:bg-orange-100/50 cursor-pointer transition-colors"
            @click="handleCardClick(card)"
          >
            <div class="w-2 h-2 bg-orange-400 rounded-full flex-sh rink-0"></div>
            <span class="flex-1 truncate text-sm">{{ card.question }}</span>
            <span class="text-xs text-gray-400 whitespace-nowrap">{{ card.daysAgo }}天前</span>
          </div>
        </div>
      </div>

      <!-- Regular Cards -->
      <div v-if="data.regularCards.length > 0">
        <p class="text-xs font-medium text-blue-500 mb-2">
          📝 常规复习
        </p>
        <div class="space-y-2">
          <div 
            v-for="card in data.regularCards.slice(0, 3)" 
            :key="card.cardId"
            class="flex items-center gap-3 p-3 bg-white/40 rounded-lg hover:bg-white/60 cursor-pointer transition-colors"
            @click="handleCardClick(card)"
          >
            <div class="w-2 h-2 bg-blue-400 rounded-full flex-shrink-0"></div>
            <span class="flex-1 truncate text-sm">{{ card.question }}</span>
            <span class="text-xs text-gray-400 whitespace-nowrap">{{ card.daysAgo }}天前</span>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex gap-2 mt-4 pt-4 border-t border-gray-200/50">
        <button 
          class="flex-1 px-4 py-2 bg-indigo-500 text-white rounded-lg hover:bg-indigo-600 transition-colors font-medium"
          @click="startReview"
        >
          开始复习
        </button>
        <button 
          class="px-4 py-2 text-sm text-gray-600 hover:bg-white/40 rounded-lg transition-colors"
          @click="viewAll"
        >
          查看全部
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NSpin } from 'naive-ui'
import { spacedApi, type ReviewTodayDto, type ReviewCardSummary } from '@/api/spaced'

const router = useRouter()
const loading = ref(true)
const data = ref<ReviewTodayDto | null>(null)

const loadData = async () => {
  loading.value = true
  try {
    data.value = await spacedApi.getDueCardsToday()
  } catch (error) {
    console.error('加载复习数据失败', error)
  } finally {
    loading.value = false
  }
}

const handleCardClick = (card: ReviewCardSummary) => {
  // 跳转到复习页面，带上卡片ID
  router.push(`/review?cardId=${card.cardId}`)
}

const startReview = () => {
  // 开始批量复习模式
  router.push('/review')
}

const viewAll = () => {
  // 查看所有待复习卡片
  router.push('/review')
}

onMounted(() => {
  loadData()
})

// 暴露refresh方法供父组件调用
defineExpose({
  refresh: loadData
})
</script>
