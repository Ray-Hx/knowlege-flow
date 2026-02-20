<template>
  <div class="h-full flex flex-col p-6 max-w-[1400px] mx-auto w-full">
    <!-- 头部说明区 -->
    <div class="flex justify-between items-center mb-6">
      <div class="flex items-center gap-4">
        <n-button circle size="large" @click="router.back()">
          <template #icon>
            <n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M20 11H7.83l5.59-5.59L12 4l-8 8l8 8l1.41-1.41L7.83 13H20v-2z"/></svg></n-icon>
          </template>
        </n-button>
        <div>
          <h1 class="text-2xl font-bold bg-gradient-to-r from-violet-600 to-indigo-600 bg-clip-text text-transparent flex items-center gap-2">
            🌌 动态知识银河
          </h1>
          <p class="text-gray-500 mt-1">发现全网知识的网络张力，点击节点前往对应笔记</p>
        </div>
      </div>
      
      <!-- 控制台 -->
      <div class="flex gap-4 items-center bg-white px-4 py-2 rounded-xl shadow-sm border border-gray-100">
         <span class="text-sm font-medium text-gray-600">获取的星图节点上限</span>
         <n-input-number v-model:value="nodeLimit" :min="50" :max="1000" :step="50" size="small" class="w-32" />
         <n-button type="primary" secondary @click="fetchGlobalGraph" :loading="loading">
            刷新星空
         </n-button>
      </div>
    </div>

    <!-- 知识图谱渲染区 -->
    <div class="flex-1 bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden relative">
      <div v-if="loading && chartData.nodes.length === 0" class="absolute inset-0 flex items-center justify-center bg-white/50 z-10">
        <n-spin size="large" description="正在观测星空连结..." />
      </div>
      <div v-else-if="chartData.nodes.length === 0" class="absolute inset-0 flex flex-col items-center justify-center text-gray-400 z-10">
        <div class="text-6xl mb-4">🪐</div>
        <p class="text-lg">暂无公共知识节点</p>
      </div>
      
      <v-chart
        v-if="chartData.nodes.length > 0"
        class="w-full h-full"
        :option="chartOption"
        autoresize
        @click="handleNodeClick"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NSpin, NInputNumber, useMessage } from 'naive-ui'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GraphChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { getGlobalGraph } from '@/api/community'

// 注册 ECharts 核心组件
use([
  CanvasRenderer,
  GraphChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent
])

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const nodeLimit = ref(300)

// 图谱数据接口
interface GraphNode {
  id: number
  title: string
  userId: number
  links?: GraphNode[]
  related?: { target: { id: number; title: string; userId: number }; label: string }[]
}

const rawNodes = ref<GraphNode[]>([])

// 格式化为 ECharts series[0].data 和 links
const chartData = computed(() => {
  const nodes: any[] = []
  const links: any[] = []
  
  // 建立唯一字典以保证 target 目标有效
  const nodeDict = new Set<number>()

  rawNodes.value.forEach(node => {
     nodeDict.add(node.id)
     // 根据是否被大量引用计算大小 (中心度) 或者简单统一随机分配大小
     // 这里我们初步设定统一尺寸，后续在 links 统计时再放大重要节点
     nodes.push({
       id: node.id.toString(),
       name: node.title,
       value: node.id,
       symbolSize: 20, // 初始大小
       itemStyle: {
         color: getColor(node.userId)
       },
       // 附带业务数据用于点击跳转
       noteId: node.id
     })
  })

  // 解析关系
  const degreeMap = new Map<number, number>()

  rawNodes.value.forEach(node => {
    // 显式引用关系
    if (node.links && node.links.length > 0) {
      node.links.forEach(target => {
        if (nodeDict.has(target.id) && target.id !== node.id) {
          links.push({
            source: node.id.toString(),
            target: target.id.toString(),
            lineStyle: { color: '#bdc3c7', curveness: 0.2, width: 1.5 }
          })
          degreeMap.set(target.id, (degreeMap.get(target.id) || 0) + 1)
          degreeMap.set(node.id, (degreeMap.get(node.id) || 0) + 1)
        }
      })
    }
    // AI隐式关联关系
    if (node.related && node.related.length > 0) {
      node.related.forEach(rel => {
        const targetId = rel.target?.id
        if (targetId && nodeDict.has(targetId) && targetId !== node.id) {
          links.push({
            source: node.id.toString(),
            target: targetId.toString(),
            label: { show: true, formatter: rel.label, fontSize: 10 },
            lineStyle: { type: 'dashed', color: '#8e44ad', curveness: 0.3, width: 1 }
          })
          degreeMap.set(targetId, (degreeMap.get(targetId) || 0) + 0.5)
          degreeMap.set(node.id, (degreeMap.get(node.id) || 0) + 0.5)
        }
      })
    }
  })

  // 按入出度调整节点大小
  nodes.forEach(n => {
     const degree = degreeMap.get(Number(n.id)) || 0
     // 大小基于 degree 计算, max 60, min 15
     n.symbolSize = Math.max(15, Math.min(60, 15 + degree * 3))
     if (n.symbolSize > 35) {
       n.itemStyle.shadowBlur = Math.min(20, degree * 2)
       n.itemStyle.shadowColor = n.itemStyle.color
     }
  })

  return { nodes, links }
})

// 为不同作者生成随性宇宙色彩
const getColor = (userId: number) => {
  const colors = [
    '#6c5ce7', // 薰衣草紫
    '#00cec9', // 蓝绿
    '#0984e3', // 天蓝
    '#ff7675', // 浅红
    '#fdcb6e', // 亮橘黄
    '#e84393', // 亮粉
    '#00b894'  // 绿
  ]
  return colors[userId % colors.length]
}

const chartOption = computed(() => {
  return {
    backgroundColor: '#0f172a', // 星空深蓝背景
    tooltip: {
      formatter: function (info: any) {
        if (info.dataType === 'node') {
          return `<div style="padding: 4px;">
                    <div style="font-weight: bold; color: #1e293b; margin-bottom: 4px;">${info.data.name}</div>
                    <div style="font-size: 12px; color: #64748b;">(点击阅读详情)</div>
                  </div>`
        }
        if (info.dataType === 'edge') {
          return `${info.data.label?.formatter || '引用关联'}`
        }
        return ''
      }
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: chartData.value.nodes,
        links: chartData.value.links,
        roam: true, // 允许拖拽放大缩小
        draggable: true,
        label: {
          show: true,
          position: 'right',
          formatter: '{b}',
          color: '#e2e8f0',
          fontSize: 12,
          textBorderColor: '#000',
          textBorderWidth: 2
        },
        force: {
          repulsion: 300,        // 节点间互斥力
          edgeLength: [50, 150],  // 连线长度区间
          gravity: 0.1          // 节点整体往中心拉的重力
        },
        edgeSymbol: ['circle', 'arrow'],
        edgeSymbolSize: [4, 8],
        lineStyle: {
          color: 'source',
          curveness: 0.3
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 4
          }
        }
      }
    ]
  }
})

// 获取全局图谱
const fetchGlobalGraph = async () => {
  loading.value = true
  try {
    const res = await getGlobalGraph(nodeLimit.value)
    rawNodes.value = res || []
  } catch (error) {
    message.error(error.message || '观测星空失败，请稍后重试')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 节点点击跳转
const handleNodeClick = (params: any) => {
  if (params.dataType === 'node' && params.data.noteId) {
    router.push(`/community/note/${params.data.noteId}`)
  }
}

onMounted(() => {
  fetchGlobalGraph()
})

</script>

<style scoped>
/* Scoped styles mainly handle inner responsive height */
</style>
