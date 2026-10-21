<template>
  <n-modal
    v-model:show="showModal"
    :mask-closable="true"
    preset="card"
    class="global-search-modal"
    :style="{ width: '800px', top: '10vh' }"
    :bordered="false"
    size="huge"
    content-style="padding: 0;"
  >
    <div class="search-container">
      <div class="search-input-wrapper">
        <n-icon class="search-icon" size="24" color="#a0aec0">
          <search-outline />
        </n-icon>
        <input 
          ref="searchInputRef"
          v-model="keyword"
          class="search-input"
          placeholder="搜索笔记、文章、标签... (Cmd+K)" 
          @input="handleInput"
          @keydown.down.prevent="selectNext"
          @keydown.up.prevent="selectPrev"
          @keydown.enter.prevent="handleEnter"
        />
        <div class="esc-hint">ESC 退出</div>
      </div>

      <div class="search-results-wrapper" :class="{ 'has-content': results.length > 0 || loading }">
        <div v-if="loading" class="result-state">
          <n-spin size="small" />
          <span class="state-text">深度检索中...</span>
        </div>
        
        <div v-else-if="keyword && results.length === 0" class="result-state">
          <n-empty description="在知识宇宙中未找到相关内容" />
        </div>

        <div v-else-if="results.length > 0" class="results-list" ref="resultsListRef">
          <div 
            v-for="(item, index) in results" 
            :key="item.type + '-' + item.id"
            class="result-item"
            :class="{ active: selectedIndex === index }"
            @mouseenter="selectedIndex = index"
            @click="handleSelect(item)"
          >
            <div class="item-icon-wrapper" :class="item.type">
              <n-icon size="20">
                <document-text-outline v-if="item.type === 'NOTE'" />
                <newspaper-outline v-else />
              </n-icon>
            </div>
            <div class="item-content">
              <div class="item-header">
                <span class="item-title" v-html="item.title"></span>
                <span class="item-type-badge">{{ item.type === 'NOTE' ? '个人笔记' : '外部文章' }}</span>
              </div>
              <div class="item-snippet" v-if="item.snippet" v-html="item.snippet"></div>
              <div class="item-meta">最后更新: {{ new Date(item.updateTime).toLocaleDateString() }}</div>
            </div>
            <div class="item-action">
              <n-icon size="18" color="#a0aec0"><arrow-forward-outline /></n-icon>
            </div>
          </div>
        </div>
      </div>
      <div class="search-footer">
        <span class="key-hint"><kbd>↑</kbd> <kbd>↓</kbd> 导航</span>
        <span class="key-hint"><kbd>↵</kbd> 打开</span>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { searchApi, GlobalSearchResult } from '@/api/search'
import { SearchOutline, DocumentTextOutline, NewspaperOutline, ArrowForwardOutline } from '@vicons/ionicons5'

const router = useRouter()
const showModal = ref(false)
const keyword = ref('')
const loading = ref(false)
const results = ref<GlobalSearchResult[]>([])
const selectedIndex = ref(-1)
const searchInputRef = ref<HTMLInputElement | null>(null)
const resultsListRef = ref<HTMLElement | null>(null)

// 防抖定时器
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// Emits for Article Viewer modal opening
const emit = defineEmits(['openArticle'])

// 监听快捷键
const handleKeyDown = (e: KeyboardEvent) => {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    openModal()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})

const openModal = () => {
  showModal.value = true
  // 如果之前有内容，保留输入，但也重置选项
  selectedIndex.value = -1
  nextTick(() => {
    searchInputRef.value?.focus()
    // 选中文本方便快速覆盖
    searchInputRef.value?.select()
  })
}

const handleInput = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  
  if (!keyword.value.trim()) {
    results.value = []
    return
  }
  
  loading.value = true
  debounceTimer = setTimeout(async () => {
    try {
      const res = await searchApi.globalSearch(keyword.value)
      // 根据您的 axios/request 封装，如果是直接返回 data 或者是 { code, data, msg }
      // 这里推测 `res` 即为 data 数组或带有 data 属性的对象。
      results.value = (res as any).data || res || []
      selectedIndex.value = results.value.length > 0 ? 0 : -1
    } catch (error) {
      console.error('全局搜索失败:', error)
    } finally {
      loading.value = false
    }
  }, 300)
}

const selectNext = () => {
  if (results.value.length === 0) return
  selectedIndex.value = (selectedIndex.value + 1) % results.value.length
  scrollToSelected()
}

const selectPrev = () => {
  if (results.value.length === 0) return
  selectedIndex.value = selectedIndex.value <= 0 ? results.value.length - 1 : selectedIndex.value - 1
  scrollToSelected()
}

const scrollToSelected = () => {
  nextTick(() => {
    if (!resultsListRef.value) return
    const activeItem = resultsListRef.value.querySelector('.result-item.active') as HTMLElement
    if (activeItem) {
      activeItem.scrollIntoView({ block: 'nearest' })
    }
  })
}

const handleEnter = () => {
  if (selectedIndex.value >= 0 && selectedIndex.value < results.value.length) {
    handleSelect(results.value[selectedIndex.value])
  }
}

const handleSelect = (item: GlobalSearchResult) => {
  showModal.value = false
  if (item.type === 'NOTE') {
    // Navigate to note editor view (or detail). Here assuming /note/:id
    router.push(`/note/${item.id}`)
  } else if (item.type === 'CONTENT') {
    // Emit event to open article viewer
    emit('openArticle', Number(item.id))
  }
}

defineExpose({
  openModal
})
</script>

<style scoped>
.global-search-modal {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.search-container {
  display: flex;
  flex-direction: column;
  background-color: var(--n-color-modal);
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--n-divider-color);
  position: relative;
}

.search-icon {
  margin-right: 16px;
}

.search-input {
  flex: 1;
  font-size: 1.25rem;
  border: none;
  outline: none;
  background: transparent;
  color: var(--n-text-color);
}

.search-input::placeholder {
  color: #a0aec0;
}

.esc-hint {
  font-size: 12px;
  color: #a0aec0;
  background: var(--n-divider-color);
  padding: 2px 8px;
  border-radius: 4px;
}

.search-results-wrapper {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-results-wrapper.has-content {
  max-height: 50vh;
  overflow-y: auto;
}

.result-state {
  padding: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #718096;
}

.state-text {
  margin-left: 12px;
}

.results-list {
  padding: 12px;
}

.result-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.result-item:last-child {
  margin-bottom: 0;
}

.result-item.active,
.result-item:hover {
  background-color: var(--n-action-color);
}

.item-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  margin-right: 16px;
  flex-shrink: 0;
}

.item-icon-wrapper.NOTE {
  background-color: rgba(66, 153, 225, 0.1);
  color: #4299e1;
}

.item-icon-wrapper.CONTENT {
  background-color: rgba(72, 187, 120, 0.1);
  color: #48bb78;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.item-title {
  font-weight: 600;
  font-size: 1.1rem;
  color: var(--n-text-color);
  margin-right: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-type-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background-color: var(--n-divider-color);
  color: var(--n-text-color-3);
  white-space: nowrap;
}

.item-snippet {
  font-size: 0.9rem;
  color: var(--n-text-color-3);
  margin-bottom: 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta {
  font-size: 12px;
  color: var(--n-text-color-disabled);
}

.item-action {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding-left: 16px;
  opacity: 0;
  transition: opacity 0.2s;
}

.result-item.active .item-action,
.result-item:hover .item-action {
  opacity: 1;
}

.search-footer {
  padding: 12px 24px;
  border-top: 1px solid var(--n-divider-color);
  display: flex;
  align-items: center;
  font-size: 12px;
  color: var(--n-text-color-disabled);
}

.key-hint {
  margin-right: 16px;
  display: flex;
  align-items: center;
}

kbd {
  background-color: var(--n-action-color);
  border: 1px solid var(--n-divider-color);
  border-radius: 4px;
  padding: 2px 6px;
  font-family: inherit;
  font-size: 11px;
  margin: 0 4px;
}

/* Global styles for ES highlights */
:deep(em) {
  font-style: normal;
  background-color: rgba(250, 204, 21, 0.4); /* Tailwind Yellow-400 */
  color: var(--n-text-color);
  border-radius: 2px;
  padding: 0 2px;
}
</style>
