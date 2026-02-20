<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NSpin, NEmpty, NButton, NIcon } from 'naive-ui' 
import { RefreshOutline } from '@vicons/ionicons5'
import ForceGraph from '@/components/graph/ForceGraph.vue'
import { graphApi, GraphData } from '@/api/graph'

const loading = ref(false)
const graphData = ref<GraphData>({ nodes: [], links: [] })
const containerRef = ref<HTMLElement>()
const dimensions = ref({ width: 800, height: 600 })

// 模拟数据（如果后端暂无数据）
const mockData: GraphData = {
  nodes: [
    { id: 'note:1', label: 'Java 基础', type: 'note', value: 10 },
    { id: 'note:2', label: 'JVM 原理', type: 'note', value: 8 },
    { id: 'content:1', label: 'Effective Java', type: 'content', value: 5 },
    { id: 'note:3', label: 'Spring Boot', type: 'note', value: 9 }
  ],
  links: [
    { source: 'note:1', target: 'note:2', type: 'reference' },
    { source: 'note:1', target: 'content:1', type: 'source' },
    { source: 'note:3', target: 'note:1', type: 'prerequisite' }
  ]
}

const fetchGraph = async () => {
  loading.value = true
  try {
    const res = await graphApi.getGraph()
    if (res && res.nodes && res.nodes.length > 0) {
      graphData.value = res
    } else {
      // 如果没有数据，使用模拟数据展示效果
      graphData.value = mockData
    }
  } catch (e) {
    console.error(e)
    graphData.value = mockData
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (containerRef.value) {
    dimensions.value = {
      width: containerRef.value.clientWidth,
      height: containerRef.value.clientHeight
    }
  }
  fetchGraph()
})
</script>

<template>
  <div class="h-full flex flex-col p-4">
    <div class="flex justify-between items-center mb-4">
      <h2 class="text-xl font-bold">知识图谱</h2>
      <n-button size="small" @click="fetchGraph">
        <template #icon><n-icon><RefreshOutline /></n-icon></template>
        刷新
      </n-button>
    </div>
    
    <div ref="containerRef" class="flex-1 bg-white rounded shadow-sm relative">
      <n-spin :show="loading" class="h-full">
        <div v-if="graphData.nodes.length > 0" class="h-full">
          <ForceGraph 
            :data="graphData" 
            :width="dimensions.width" 
            :height="dimensions.height" 
          />
        </div>
        <div v-else class="h-full flex items-center justify-center">
          <n-empty description="暂无图谱数据" />
        </div>
      </n-spin>
    </div>
  </div>
</template>
