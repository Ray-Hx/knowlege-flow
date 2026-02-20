<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NInput, NButton, NList, NListItem, NThing, NTag, NPagination, NEmpty, NIcon, NSpin } from 'naive-ui'
import { SearchOutline, DocumentTextOutline } from '@vicons/ionicons5'
import { searchApi, SearchResult, Recommendation } from '@/api/search'

const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const results = ref<SearchResult[]>([])
const pagination = ref({ page: 1, pageSize: 10, itemCount: 0 })
const hasSearched = ref(false)

// 推荐内容
const recommendations = ref<Recommendation[]>([])
const loadRecommendations = async () => {
  try {
    const res = await searchApi.getRecommendations()
    recommendations.value = res
  } catch (e) {
    console.error('加载推荐失败', e)
  }
}

import { onMounted } from 'vue'

onMounted(() => {
  loadRecommendations()
})

const handleSearch = async () => {
  if (!keyword.value.trim()) return
  
  loading.value = true
  hasSearched.value = true
  try {
    const res = await searchApi.search(keyword.value, pagination.value.page, pagination.value.pageSize)
    results.value = res.content
    pagination.value.itemCount = res.totalElements
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  handleSearch()
}

const goToNote = (id: number, type?: 'NOTE' | 'CONTENT', url?: string) => {
  if (type === 'CONTENT' && url) {
    window.open(url, '_blank')
    return
  }
  router.push(`/notes/${id}`)
}

const getSummary = (item: SearchResult) => {
  // 优先使用高亮摘要
  if (item.highlights?.summary?.[0]) return item.highlights.summary[0]
  if (item.highlights?.content?.[0]) return item.highlights.content[0]
  return item.summary || '暂无摘要'
}
</script>

<template>
  <div class="search-page p-4 max-w-4xl mx-auto">
    <div class="mb-8 text-center pt-8">
      <h2 class="text-2xl font-bold mb-6 text-gray-700">全文知识搜索</h2>
      <div class="flex gap-2 max-w-xl mx-auto">
        <n-input 
          v-model:value="keyword" 
          size="large" 
          placeholder="搜索笔记内容..." 
          @keyup.enter="handleSearch"
          class="flex-1"
        >
          <template #prefix>
            <n-icon><SearchOutline /></n-icon>
          </template>
        </n-input>
        <n-button type="primary" size="large" @click="handleSearch" :loading="loading">
          搜索
        </n-button>
      </div>
    </div>

    <n-spin :show="loading">
      <div v-if="results.length > 0" class="search-results">
        <p class="text-gray-500 mb-4 ml-1">找到 {{ pagination.itemCount }} 条相关结果</p>
        <n-list hoverable clickable>
          <n-list-item v-for="item in results" :key="item.id" @click="goToNote(item.id)">
            <n-thing>
              <template #header>
                <div class="flex items-center gap-2">
                  <n-icon class="text-indigo-500"><DocumentTextOutline /></n-icon>
                  <span v-html="item.highlights?.title?.[0] || item.title" class="font-medium text-lg text-indigo-600"></span>
                </div>
              </template>
              <template #description>
                <div class="flex gap-2 mt-1">
                  <n-tag size="small" type="info" :bordered="false" v-if="item.folderName">
                    {{ item.folderName }}
                  </n-tag>
                  <span class="text-gray-400 text-xs self-center">
                    更新于 {{ new Date(item.updatedTime).toLocaleDateString() }}
                  </span>
                </div>
              </template>
              <div 
                class="mt-2 text-gray-600 text-sm line-clamp-2"
                v-html="getSummary(item)"
              ></div>
            </n-thing>
          </n-list-item>
        </n-list>
        
        <div class="flex justify-center mt-6">
          <n-pagination
            v-model:page="pagination.page"
            :item-count="pagination.itemCount"
            :page-size="pagination.pageSize"
            @update:page="handlePageChange"
          />
        </div>
      </div>
      
      <div v-else-if="hasSearched && !loading" class="flex justify-center mt-12">
        <n-empty description="未找到相关内容" />
      </div>

      <!-- 智能推荐 -->
      <div v-else-if="!hasSearched && !loading && recommendations.length > 0" class="recommendations mt-8">
        <h3 class="text-lg font-bold text-gray-700 mb-4 flex items-center gap-2">
          <span>✨ 为你推荐</span>
        </h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div 
            v-for="item in recommendations" 
            :key="item.id" 
            class="glass-card p-4 cursor-pointer hover:bg-white/60 transition-all border border-transparent hover:border-indigo-100"
            @click="goToNote(item.id, item.type, item.url)"
          >
            <div class="flex justify-between items-start mb-2">
              <span class="font-bold text-gray-800 line-clamp-1">{{ item.title }}</span>
              <n-tag size="small" :type="item.type === 'NOTE' ? 'success' : 'info'" round :bordered="false">
                {{ item.reason }}
              </n-tag>
            </div>
            <p class="text-sm text-gray-500 line-clamp-2 mb-3 h-10">{{ item.summary || '暂无摘要' }}</p>
            <div class="flex items-center justify-between text-xs text-gray-400">
              <div class="flex gap-2">
                <span v-if="item.type === 'NOTE'">📝 笔记</span>
                <span v-else>📰订阅</span>
              </div>
              <span>{{ new Date(item.updatedTime).toLocaleDateString() }}</span>
            </div>
          </div>
        </div>
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
:deep(em) {
  font-style: normal;
  color: #d97706; /* amber-600 */
  background-color: #fef3c7; /* amber-100 */
  padding: 0 2px;
  border-radius: 2px;
}
</style>
