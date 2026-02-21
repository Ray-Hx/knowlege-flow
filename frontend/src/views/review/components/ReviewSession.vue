<script setup lang="ts">
import { ref, computed } from 'vue'
import { useMessage, NButton, NProgress, NInput } from 'naive-ui'
import { spacedApi, ReviewNote } from '@/api/spaced'

const props = defineProps<{
  notes: ReviewNote[]
}>()

const emit = defineEmits(['finish'])

const message = useMessage()
const currentIndex = ref(0)
const currentQuestionIndex = ref(0)
const showAnswer = ref(false)
const userAnswer = ref('')
const submitting = ref(false)
const sessionStats = ref({
  correct: 0,
  hard: 0,
  forgot: 0
})

const currentNote = computed(() => props.notes[currentIndex.value])
const currentQuestion = computed(() => {
  const note = currentNote.value
  if (!note || !note.questions || note.questions.length === 0) return null
  return note.questions[currentQuestionIndex.value]
})
const totalQuestions = computed(() => {
  let total = 0
  props.notes.forEach(n => {
    total += n.questions?.length || 0
  })
  return total
})
const completedQuestions = computed(() => {
  let total = 0
  for (let i = 0; i < currentIndex.value; i++) {
    total += props.notes[i].questions?.length || 0
  }
  total += currentQuestionIndex.value
  return total
})
const progress = computed(() => (completedQuestions.value / totalQuestions.value) * 100)

const handleReveal = () => {
  showAnswer.value = true
}

const handleGrade = async (quality: number) => {
  if (!currentNote.value) return
  
  // Move to next question or next note
  const note = currentNote.value
  const isLastQuestion = currentQuestionIndex.value >= (note.questions?.length || 1) - 1
  
  if (!isLastQuestion) {
    // Next question in same note
    currentQuestionIndex.value++
    showAnswer.value = false
    userAnswer.value = ''
    return
  }

  // Last question of this note, submit review and move to next note
  submitting.value = true
  try {
    await spacedApi.submitReview(note.noteId, quality)
    
    // Update local stats
    if (quality >= 4) sessionStats.value.correct++
    else if (quality >= 3) sessionStats.value.hard++
    else sessionStats.value.forgot++

    // Next note
    if (currentIndex.value < props.notes.length - 1) {
      currentIndex.value++
      currentQuestionIndex.value = 0
      showAnswer.value = false
      userAnswer.value = ''
    } else {
      // Finished
      message.success('本次复习完成！')
      emit('finish')
    }
  } catch (e: any) {
    message.error(e.message || '提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="review-session w-full max-w-3xl py-4 px-4 bg-transparent">
    <div class="mb-8 flex items-center justify-between">
      <div class="flex gap-4">
        <span class="text-white/80 font-medium bg-black/20 px-3 py-1 rounded-full text-sm backdrop-blur-sm">
          笔记: {{ currentIndex + 1 }} / {{ notes.length }}
        </span>
        <span v-if="currentNote?.questions && currentNote.questions.length > 1" class="text-white/60 font-medium bg-black/10 px-3 py-1 rounded-full text-sm backdrop-blur-sm">
          问题: {{ currentQuestionIndex + 1 }} / {{ currentNote.questions.length }}
        </span>
      </div>
      <div class="w-48">
        <n-progress
          type="line"
          :percentage="progress"
          :show-indicator="false"
          :height="8"
          :rail-color="'rgba(255,255,255,0.2)'"
          color="#a855f7"
          class="drop-shadow-sm"
        />
      </div>
    </div>

    <div class="card-container min-h-[500px] perspective-1000 relative mb-10">
      <div class="glass-card h-full w-full p-10 flex flex-col shadow-2xl border-t-4 border-indigo-500/80 bg-white/70 backdrop-blur-xl transition-all duration-500">
        <!-- Note Title -->
        <div class="mb-6 pb-4 border-b border-gray-200/50">
          <div class="text-xs text-indigo-500 uppercase tracking-widest font-bold mb-2 opacity-70">笔记</div>
          <div class="text-xl font-semibold text-gray-800">{{ currentNote?.title }}</div>
          <div v-if="currentNote?.summary" class="text-sm text-gray-500 mt-1 line-clamp-2">{{ currentNote.summary }}</div>
        </div>

        <!-- Question -->
        <div class="flex-1">
          <div class="text-xs text-purple-500 uppercase tracking-widest font-bold mb-3 opacity-70">问题</div>
          <div v-if="currentQuestion" class="text-lg font-medium text-gray-800 whitespace-pre-wrap leading-relaxed mb-6">
            {{ currentQuestion.question }}
          </div>
          <div v-else class="text-gray-500">
            暂无问题，请点击下方按钮生成
          </div>

          <!-- User Answer Input -->
          <div v-if="!showAnswer && currentQuestion" class="mt-6">
            <div class="text-xs text-gray-500 uppercase tracking-widest font-bold mb-2">你的回答 (可选)</div>
            <n-input
              v-model:value="userAnswer"
              type="textarea"
              placeholder="在此输入你的答案..."
              :autosize="{ minRows: 2, maxRows: 4 }"
              class="bg-white/50"
            />
          </div>

          <!-- Answer -->
          <div v-if="showAnswer && currentQuestion" class="mt-6 pt-6 border-t border-indigo-100 animate-fade-in-up">
            <div class="text-xs text-emerald-500 uppercase tracking-widest font-bold mb-3 opacity-70">参考答案</div>
            <div class="text-lg text-gray-700 whitespace-pre-wrap leading-relaxed bg-emerald-50/50 p-4 rounded-lg">
              {{ currentQuestion.answer }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Controls -->
    <div class="flex justify-center min-h-[80px]">
      <n-button 
        v-if="!showAnswer" 
        type="primary" 
        size="large" 
        class="w-56 h-14 text-xl shadow-xl liquid-button"
        @click="handleReveal"
        :disabled="!currentQuestion"
      >
        显示答案
      </n-button>
      
      <div v-else class="grid grid-cols-4 gap-6 w-full max-w-2xl animate-fade-in">
        <button 
          class="h-16 rounded-xl bg-white/20 hover:bg-red-500/20 text-white font-bold border border-white/30 backdrop-blur-sm transition-all hover:scale-105 hover:border-red-400"
          @click="handleGrade(0)"
          :disabled="submitting"
        >
          <div class="text-sm opacity-80">忘记</div>
          <div class="text-xs font-normal">0分</div>
        </button>
        <button 
          class="h-16 rounded-xl bg-white/20 hover:bg-orange-500/20 text-white font-bold border border-white/30 backdrop-blur-sm transition-all hover:scale-105 hover:border-orange-400"
          @click="handleGrade(2)"
          :disabled="submitting"
        >
          <div class="text-sm opacity-80">困难</div>
          <div class="text-xs font-normal">2分</div>
        </button>
        <button 
          class="h-16 rounded-xl bg-white/20 hover:bg-blue-500/20 text-white font-bold border border-white/30 backdrop-blur-sm transition-all hover:scale-105 hover:border-blue-400"
          @click="handleGrade(4)"
          :disabled="submitting"
        >
          <div class="text-sm opacity-80">良好</div>
          <div class="text-xs font-normal">4分</div>
        </button>
        <button 
          class="h-16 rounded-xl bg-white/20 hover:bg-emerald-500/20 text-white font-bold border border-white/30 backdrop-blur-sm transition-all hover:scale-105 hover:border-emerald-400"
          @click="handleGrade(5)"
          :disabled="submitting"
        >
          <div class="text-sm opacity-80">完美</div>
          <div class="text-xs font-normal">5分</div>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-fade-in-up {
  animation: fadeInUp 0.4s ease-out forwards;
  opacity: 0;
  transform: translateY(20px);
}

.animate-fade-in {
  animation: fadeIn 0.3s ease-out forwards;
  opacity: 0;
}

@keyframes fadeInUp {
  to { opacity: 1; transform: translateY(0); }
}

@keyframes fadeIn {
  to { opacity: 1; }
}
</style>
