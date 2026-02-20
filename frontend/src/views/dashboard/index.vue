<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { NGrid, NGridItem, NCard, NStatistic, NIcon, NSpin, NButton, NTooltip } from 'naive-ui'
import { BookOutline, TrendingUpOutline, DocumentTextOutline, AlertCircleOutline, TimeOutline, FlameOutline, ThumbsUpOutline } from '@vicons/ionicons5'
import * as echarts from 'echarts'
import { dashboardApi, DashboardStats, DailyStudy, RecentNote, TagStats } from '@/api/dashboard'


const router = useRouter()

// 加载状态
const loading = ref(true)

// 统计数据
const stats = ref<DashboardStats | null>(null)
const weeklyData = ref<DailyStudy[]>([])
const recentNotes = ref<RecentNote[]>([])
const calendarData = ref<DailyStudy[]>([])
const yearlyData = ref<DailyStudy[]>([])
const tagData = ref<TagStats[]>([])
const trendData = ref<DailyStudy[]>([])

// Chart refs
const trendChartRef = ref<HTMLElement | null>(null)
const tagChartRef = ref<HTMLElement | null>(null)
const heatmapChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let tagChart: echarts.ECharts | null = null
let heatmapChart: echarts.ECharts | null = null

// 统计卡片配置
const statCards = computed(() => {
  if (!stats.value) return []
  return [
    { label: '总笔记数', value: stats.value.totalNotes.toString(), icon: BookOutline, color: 'text-blue-500', bg: 'bg-blue-50' },
    { label: '今日新增', value: stats.value.todayNewNotes.toString(), icon: DocumentTextOutline, color: 'text-green-500', bg: 'bg-green-50' },
    { label: '待复习', value: stats.value.pendingNotes.toString(), icon: AlertCircleOutline, color: 'text-orange-500', bg: 'bg-orange-50' },
    { label: '本周新增', value: stats.value.weeklyNewNotes.toString(), icon: TrendingUpOutline, color: 'text-purple-500', bg: 'bg-purple-50' },
  ]
})

// ======== 近12周折线面积图 ========
const initTrendChart = () => {
  if (!trendChartRef.value || trendData.value.length === 0) return
  trendChart?.dispose()
  trendChart = echarts.init(trendChartRef.value)

  const labels = trendData.value.map(d => {
    const date = new Date(d.date)
    return `${date.getMonth() + 1}/${date.getDate()}`
  })
  const values = trendData.value.map(d => d.noteCount || 0)

  trendChart.setOption({
    tooltip: { trigger: 'axis', formatter: (params: any) => `${params[0].name}周<br/>新增笔记: ${params[0].value}` },
    grid: { left: '3%', right: '4%', bottom: '8%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#9ca3af', fontSize: 11 },
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
      axisLabel: { color: '#9ca3af', fontSize: 11 }
    },
    series: [{
      name: '笔记数',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      data: values,
      lineStyle: { color: '#6366f1', width: 3 },
      itemStyle: { color: '#6366f1' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(99,102,241,0.3)' },
          { offset: 1, color: 'rgba(99,102,241,0.02)' }
        ])
      }
    }]
  })
}

// ======== 标签分布南丁格尔玫瑰图 ========
const initTagChart = () => {
  if (!tagChartRef.value || tagData.value.length === 0) return
  tagChart?.dispose()
  tagChart = echarts.init(tagChartRef.value)

  const colors = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#06b6d4', '#f97316', '#84cc16', '#ef4444', '#64748b']

  tagChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => `${params.name}<br/>笔记数: ${params.value} (${params.percent}%)`
    },
    legend: {
      orient: 'vertical',
      right: '2%',
      top: 'center',
      textStyle: { color: '#6b7280', fontSize: 11 },
      itemWidth: 10,
      itemHeight: 10
    },
    series: [{
      name: '知识领域',
      type: 'pie',
      radius: ['30%', '70%'],
      roseType: 'area',
      center: ['38%', '50%'],
      data: tagData.value.map((t, i) => ({
        name: t.tag,
        value: t.noteCount,
        itemStyle: { color: colors[i % colors.length] }
      })),
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 'bold' },
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.1)' }
      }
    }]
  })
}

// ======== 全年热力图（ECharts calendar）========
const initHeatmapChart = () => {
  if (!heatmapChartRef.value || yearlyData.value.length === 0) return
  heatmapChart?.dispose()
  heatmapChart = echarts.init(heatmapChartRef.value)

  // 计算日期范围
  const dates = yearlyData.value.map(d => d.date)
  const startDate = dates[0]
  const endDate = dates[dates.length - 1]

  // 转换数据格式 [[date, count], ...]
  const data = yearlyData.value
    .filter(d => (d.noteCount || 0) > 0)
    .map(d => [d.date, d.noteCount || 0])

  heatmapChart.setOption({
    tooltip: {
      formatter: (params: any) => {
        const val = params.value[1]
        return `${params.value[0]}<br/>创建笔记: <b>${val}</b> 篇`
      }
    },
    visualMap: {
      show: true,
      type: 'piecewise',
      orient: 'horizontal',
      left: 'right',
      bottom: 0,
      pieces: [
        { min: 0, max: 0, color: '#ebedf0', label: '0' },
        { min: 1, max: 1, color: '#c7d2fe', label: '1' },
        { min: 2, max: 3, color: '#818cf8', label: '2-3' },
        { min: 4, max: 6, color: '#4f46e5', label: '4-6' },
        { min: 7, color: '#312e81', label: '7+' }
      ],
      textStyle: { color: '#9ca3af', fontSize: 11 }
    },
    calendar: {
      top: 30,
      left: 40,
      right: 120,
      bottom: 30,
      range: [startDate, endDate],
      cellSize: [14, 14],
      splitLine: { show: false },
      itemStyle: {
        borderWidth: 2,
        borderColor: '#ffffff',
        color: '#ebedf0'
      },
      dayLabel: {
        show: true,
        nameMap: ['日', '一', '二', '三', '四', '五', '六'],
        color: '#9ca3af',
        fontSize: 10,
        firstDay: 0
      },
      monthLabel: {
        show: true,
        nameMap: ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月'],
        color: '#6b7280',
        fontSize: 11
      },
      yearLabel: { show: false }
    },
    series: [{
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: data,
      emphasis: {
        itemStyle: { shadowBlur: 6, shadowColor: 'rgba(99,102,241,0.4)' }
      }
    }]
  })
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const [statsData, weekly, notes, calendar, yearly, tags, trend] = await Promise.all([
      dashboardApi.getStats(),
      dashboardApi.getWeeklyStudy(),
      dashboardApi.getRecentNotes(5),
      dashboardApi.getMonthlyCalendar(),
      dashboardApi.getYearlyHeatmap(),
      dashboardApi.getTagDistribution(),
      dashboardApi.getLearningTrend()
    ])
    stats.value = statsData
    weeklyData.value = weekly
    recentNotes.value = notes
    calendarData.value = calendar
    yearlyData.value = yearly
    tagData.value = tags
    trendData.value = trend

    await nextTick()
    setTimeout(() => {
      initTrendChart()
      initTagChart()
      initHeatmapChart()
    }, 100)
  } catch (error) {
    console.error('加载仪表盘数据失败', error)
  } finally {
    loading.value = false
  }
}

// 跳转到笔记详情
const goToNote = (noteId: number) => {
  router.push(`/notes/${noteId}`)
}

// 日历热力图颜色计算
const getCalendarCellClass = (day: DailyStudy) => {
  const notes = day.noteCount || 0
  if (notes === 0) return 'bg-gray-100 text-gray-400'
  if (notes < 2) return 'bg-green-200 text-green-800'
  if (notes < 4) return 'bg-green-400 text-white'
  return 'bg-green-600 text-white'
}

const handleResize = () => {
  trendChart?.resize()
  tagChart?.resize()
  heatmapChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  tagChart?.dispose()
  heatmapChart?.dispose()
})
</script>

<template>
  <div class="dashboard">
    <h2 class="text-2xl font-bold mb-6 text-gray-800">仪表盘</h2>
    
    <n-spin :show="loading">
      <!-- 统计卡片 -->
      <n-grid :x-gap="16" :y-gap="16" cols="1 s:2 m:4" responsive="screen">
        <n-grid-item v-for="stat in statCards" :key="stat.label">
          <div class="glass-card p-4 hover:bg-white/60 transition-all cursor-default">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-gray-500 text-sm font-medium mb-1">{{ stat.label }}</p>
                <p class="text-3xl font-bold text-gray-800">{{ stat.value }}</p>
              </div>
              <div :class="['p-3 rounded-2xl', stat.bg]">
                <n-icon size="28" :class="[stat.color]">
                  <component :is="stat.icon" />
                </n-icon>
              </div>
            </div>
          </div>
        </n-grid-item>
      </n-grid>


      <!-- 全年学习热力图 (ECharts calendar) -->
      <div class="mt-6 glass-card p-6">
        <h3 class="text-lg font-bold text-gray-700 mb-2">🔥 全年学习热力图</h3>
        <div v-if="yearlyData.length === 0" class="flex items-center justify-center h-32 text-gray-400 text-sm">
          暂无数据
        </div>
        <div v-else ref="heatmapChartRef" style="height: 160px; width: 100%;"></div>
      </div>

      <div class="mt-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 最近笔记 -->
        <div class="glass-card p-6">
          <h3 class="text-lg font-bold text-gray-700 mb-4">最近编辑</h3>
          <div class="space-y-2">
            <div v-if="recentNotes.length === 0" class="text-gray-400 text-center py-4">
              暂无笔记
            </div>
            <div 
              v-for="note in recentNotes" 
              :key="note.id" 
              class="flex justify-between items-center p-3 hover:bg-white/40 rounded-xl cursor-pointer transition-colors group"
              @click="goToNote(note.id)"
            >
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-lg bg-indigo-100/50 flex items-center justify-center text-indigo-500 group-hover:bg-indigo-500 group-hover:text-white transition-colors">
                  <n-icon :component="DocumentTextOutline" />
                </div>
                <span class="text-gray-700 font-medium truncate max-w-[200px]">{{ note.title }}</span>
              </div>
              <span class="text-gray-400 text-xs whitespace-nowrap">{{ note.updatedTimeText }}</span>
            </div>
          </div>
        </div>

        <!-- 学习日历热力图（本月） -->
        <div class="glass-card p-6">
          <h3 class="text-lg font-bold text-gray-700 mb-4">📅 本月笔记日历</h3>
          <div class="grid grid-cols-7 gap-2">
            <div class="text-center text-xs text-gray-400 font-medium" v-for="day in ['日', '一', '二', '三', '四', '五', '六']" :key="day">{{ day }}</div>
          </div>
          <div class="grid grid-cols-7 gap-2 mt-2">
            <div 
              v-for="(day, index) in calendarData" 
              :key="index"
              class="aspect-square rounded-lg flex items-center justify-center text-xs font-medium transition-all cursor-default"
              :class="getCalendarCellClass(day)"
              :title="`${day.date}: ${day.noteCount || 0}篇笔记`"
            >
              {{ new Date(day.date).getDate() }}
            </div>
          </div>
          <div class="flex items-center justify-end gap-2 mt-4 text-xs text-gray-400">
            <span>少</span>
            <div class="w-3 h-3 rounded bg-gray-100"></div>
            <div class="w-3 h-3 rounded bg-green-200"></div>
            <div class="w-3 h-3 rounded bg-green-400"></div>
            <div class="w-3 h-3 rounded bg-green-600"></div>
            <span>多</span>
          </div>
        </div>
      </div>

      <div class="mt-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 近12周学习趋势 -->
        <div class="glass-card p-6 flex flex-col">
          <h3 class="text-lg font-bold text-gray-700 mb-4">📈 近12周学习趋势</h3>
          <div ref="trendChartRef" class="flex-1 min-h-[240px] w-full"></div>
        </div>

        <!-- 知识领域分布 -->
        <div class="glass-card p-6 flex flex-col">
          <h3 class="text-lg font-bold text-gray-700 mb-4">🏷️ 知识领域分布</h3>
          <div v-if="tagData.length === 0" class="flex-1 flex items-center justify-center text-gray-400">
            <div class="text-center">
              <div class="text-4xl mb-2">🏷️</div>
              <p class="text-sm">给笔记添加标签后这里会显示分布图</p>
            </div>
          </div>
          <div v-else ref="tagChartRef" class="flex-1 min-h-[240px] w-full"></div>
        </div>
      </div>
    </n-spin>
  </div>
</template>
