<script setup lang="ts">
import { ref, computed } from 'vue'
import { useMessage, NButton, NProgress, NTag } from 'naive-ui'
import { useRouter } from 'vue-router'
import { spacedApi, ReviewNote } from '@/api/spaced'

const props = defineProps<{
  notes: ReviewNote[]
}>()

const emit = defineEmits(['finish'])
const router = useRouter()
const message = useMessage()

const currentIndex = ref(0)
const submitting = ref(false)
const sessionStats = ref({ remembered: 0, forgot: 0 })

const currentNote = computed(() => props.notes[currentIndex.value])
const progress = computed(() => Math.round((currentIndex.value / props.notes.length) * 100))

const goToNote = () => {
  if (currentNote.value?.noteId) {
    router.push(`/note/${currentNote.value.noteId}`)
  }
}

const handleGrade = async (quality: number) => {
  if (!currentNote.value || submitting.value) return

  submitting.value = true
  try {
    await spacedApi.submitReview(currentNote.value.noteId, quality)

    if (quality >= 3) sessionStats.value.remembered++
    else sessionStats.value.forgot++

    if (currentIndex.value < props.notes.length - 1) {
      currentIndex.value++
    } else {
      message.success('🎉 本次复习完成！')
      emit('finish', sessionStats.value)
    }
  } catch (e: any) {
    message.error(e.message || '提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="w-full max-w-2xl mx-auto flex flex-col gap-6">

    <!-- 进度条 -->
    <div class="flex items-center gap-4">
      <n-progress
        type="line"
        :percentage="progress"
        :show-indicator="false"
        :height="6"
        :rail-color="'rgba(255,255,255,0.2)'"
        color="#818cf8"
        class="flex-1"
      />
      <span class="text-white/70 text-sm font-medium whitespace-nowrap">
        {{ currentIndex + 1 }} / {{ notes.length }}
      </span>
    </div>

    <!-- 笔记卡片 -->
    <div class="bg-white/85 backdrop-blur-xl rounded-3xl shadow-2xl border border-white/60 overflow-hidden">

      <!-- 顶部标签区 -->
      <div class="px-8 pt-7 pb-4 border-b border-gray-100 flex items-start justify-between gap-4">
        <div class="flex-1 min-w-0">
          <div class="text-xs text-indigo-500 uppercase tracking-widest font-bold mb-2 opacity-70">📖 待复习笔记</div>
          <h2 class="text-2xl font-bold text-gray-800 leading-snug">{{ currentNote?.title || '（无标题）' }}</h2>
        </div>
        <n-tag v-if="currentNote?.repetitionCount !== undefined" :bordered="false" size="small" type="info" class="shrink-0 mt-1">
          第 {{ (currentNote.repetitionCount || 0) + 1 }} 次复习
        </n-tag>
      </div>

      <!-- 摘要内容 -->
      <div class="px-8 py-6">
        <div v-if="currentNote?.summary" class="text-gray-600 leading-relaxed text-base">
          {{ currentNote.summary }}
        </div>
        <div v-else class="text-gray-400 italic text-sm">
          该笔记暂无摘要，点击下方按钮查看原文内容。
        </div>
      </div>

      <!-- 查看原笔记按钮 -->
      <div class="px-8 pb-6">
        <n-button
          secondary
          size="medium"
          class="w-full rounded-xl"
          @click="goToNote"
        >
          <template #icon>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M18 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V8a2 2 0 012-2h6"/>
              <polyline points="15 3 21 3 21 9"/>
              <line x1="10" y1="14" x2="21" y2="3"/>
            </svg>
          </template>
          查看完整笔记
        </n-button>
      </div>

      <!-- 分割线 -->
      <div class="border-t border-gray-100 mx-8"></div>

      <!-- 自我评估区 -->
      <div class="px-8 py-7">
        <div class="text-center text-sm text-gray-500 mb-5 font-medium">读完后，你还记得这篇笔记的内容吗？</div>
        <div class="grid grid-cols-3 gap-3">
          <button
            class="py-4 rounded-2xl font-bold text-sm transition-all duration-200 hover:scale-105 active:scale-95 flex flex-col items-center gap-1"
            :class="submitting ? 'opacity-50 cursor-not-allowed bg-red-50 text-red-400 border border-red-100' : 'bg-red-50 hover:bg-red-100 text-red-600 border border-red-100 hover:border-red-200 shadow-sm'"
            @click="handleGrade(0)"
            :disabled="submitting"
          >
            <span class="text-2xl">😅</span>
            <span>忘了</span>
            <span class="text-xs font-normal opacity-70">明天再复习</span>
          </button>
          <button
            class="py-4 rounded-2xl font-bold text-sm transition-all duration-200 hover:scale-105 active:scale-95 flex flex-col items-center gap-1"
            :class="submitting ? 'opacity-50 cursor-not-allowed bg-blue-50 text-blue-400 border border-blue-100' : 'bg-blue-50 hover:bg-blue-100 text-blue-600 border border-blue-100 hover:border-blue-200 shadow-sm'"
            @click="handleGrade(3)"
            :disabled="submitting"
          >
            <span class="text-2xl">🙂</span>
            <span>记得</span>
            <span class="text-xs font-normal opacity-70">几天后复习</span>
          </button>
          <button
            class="py-4 rounded-2xl font-bold text-sm transition-all duration-200 hover:scale-105 active:scale-95 flex flex-col items-center gap-1"
            :class="submitting ? 'opacity-50 cursor-not-allowed bg-emerald-50 text-emerald-400 border border-emerald-100' : 'bg-emerald-50 hover:bg-emerald-100 text-emerald-600 border border-emerald-100 hover:border-emerald-200 shadow-sm'"
            @click="handleGrade(5)"
            :disabled="submitting"
          >
            <span class="text-2xl">😄</span>
            <span>熟练</span>
            <span class="text-xs font-normal opacity-70">更久后复习</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 本次进度小统计 -->
    <div v-if="currentIndex > 0" class="flex justify-center gap-6 text-sm text-white/60">
      <span>✅ 记住 {{ sessionStats.remembered }} 篇</span>
      <span>❌ 忘了 {{ sessionStats.forgot }} 篇</span>
    </div>
  </div>
</template>
