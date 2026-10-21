<template>
  <div class="public-profile h-screen bg-gradient-to-br from-indigo-50 to-purple-50 p-4 md:p-8 w-full overflow-y-auto">
    <div class="max-w-5xl mx-auto w-full">
      <!-- Header / Nav -->
      <div class="flex items-center mb-8 gap-2 cursor-pointer w-fit" @click="router.push('/community/feed')">
        <div class="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white">
          <n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M20 11H7.83l5.59-5.59L12 4l-8 8l8 8l1.41-1.41L7.83 13H20z"/></svg></n-icon>
        </div>
        <span class="font-bold text-xl text-gray-800">返回大厅</span>
      </div>

      <n-spin :show="loading">
        <div v-if="profile" class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <!-- Left: Profile Card -->
          <div class="md:col-span-1">
            <n-card class="glass-card text-center sticky top-8">
              <div class="relative inline-block mb-4">
                <n-avatar round :size="120" :src="profile.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" class="border-4 border-white shadow-lg" />
              </div>
              
              <h2 class="text-2xl font-bold text-gray-800 mb-1">{{ profile.username }}</h2>
              
              <div class="flex justify-center gap-4 mt-6 mb-6">
                <div class="text-center">
                  <div class="text-xl font-bold text-gray-800">{{ profile.followingCount || 0 }}</div>
                  <div class="text-xs text-gray-500">关注</div>
                </div>
                <div class="w-px bg-gray-200"></div>
                <div class="text-center">
                  <div class="text-xl font-bold text-gray-800">{{ profile.followerCount || 0 }}</div>
                  <div class="text-xs text-gray-500">粉丝</div>
                </div>
                <div class="w-px bg-gray-200"></div>
                <div class="text-center">
                  <div class="text-xl font-bold text-indigo-600">{{ profile.publicNoteCount || 0 }}</div>
                  <div class="text-xs text-gray-500">公开笔记</div>
                </div>
                <div class="w-px bg-gray-200"></div>
                <div class="text-center">
                  <div class="text-xl font-bold text-indigo-600">{{ profile.totalLikes || 0 }}</div>
                  <div class="text-xs text-gray-500">获赞</div>
                </div>
                <div class="w-px bg-gray-200"></div>
                <div class="text-center">
                  <div class="text-xl font-bold text-indigo-600">{{ profile.totalViews || 0 }}</div>
                  <div class="text-xs text-gray-500">阅读</div>
                </div>
              </div>

              <p class="text-gray-600 mb-6 text-center leading-relaxed text-sm">
                {{ profile.bio || '这个用户很懒，什么都没写...' }}
              </p>

                <div class="flex gap-3 mt-4">
                  <n-button 
                    v-if="profile.isFollowing" 
                    secondary 
                    type="tertiary" 
                    round 
                    class="flex-1" 
                    @click="handleUnfollow"
                    :loading="followLoading"
                  >
                    已关注
                  </n-button>
                  <n-button 
                    v-else 
                    type="primary" 
                    round 
                    class="flex-1" 
                    @click="handleFollow"
                    :loading="followLoading"
                  >
                    关注
                  </n-button>
                  <n-button
                    type="primary"
                    ghost
                    round
                    class="flex-1"
                    @click="router.push({ path: '/messages', query: { contactId: profile.id } })"
                  >
                    发私信
                  </n-button>
                </div>

                <!-- Additional Info -->
                <div class="mt-8 pt-6 border-t border-gray-100 text-left space-y-4">
                  <div v-if="profile.email" class="flex items-center gap-3 text-gray-600">
                    <n-icon size="20" class="text-indigo-400"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5l-8-5V6l8 5l8-5v2z"/></svg></n-icon>
                    <span class="text-sm truncate" :title="profile.email">{{ profile.email }}</span>
                  </div>
                  <div v-if="profile.githubId" class="flex items-center gap-3 text-gray-600">
                    <n-icon size="20" class="text-indigo-400"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2A10 10 0 0 0 2 12c0 4.42 2.87 8.17 6.84 9.5c.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34c-.46-1.16-1.11-1.47-1.11-1.47c-.91-.62.07-.6.07-.6c1 .07 1.53 1.03 1.53 1.03c.87 1.52 2.34 1.07 2.91.83c.09-.65.35-1.09.63-1.34c-2.22-.25-4.55-1.11-4.55-4.92c0-1.11.38-2 1.03-2.71c-.1-.25-.45-1.29.1-2.64c0 0 .84-.27 2.75 1.02c.79-.22 1.65-.33 2.5-.33c.85 0 1.71.11 2.5.33c1.91-1.29 2.75-1.02 2.75-1.02c.55 1.35.2 2.39.1 2.64c.65.71 1.03 1.6 1.03 2.71c0 3.82-2.34 4.66-4.57 4.91c.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0 0 12 2z"/></svg></n-icon>
                    <span class="text-sm">{{ profile.githubId }}</span>
                  </div>
                  <div v-if="profile.learningGoals" class="mt-4">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">学习目标</div>
                    <div class="text-sm text-gray-600 bg-indigo-50/50 p-3 rounded-xl border border-indigo-100/50 italic">
                      "{{ profile.learningGoals }}"
                    </div>
                  </div>
                </div>
              </n-card>
          </div>

          <!-- Right: Public Content & Heatmap -->
          <div class="md:col-span-2 space-y-6">
            
            <!-- Heatmap -->
            <n-card title="学习活跃度" class="glass-card !overflow-visible">
              <div ref="heatmapRef" class="w-full h-48"></div>
            </n-card>

            <!-- Public Notes -->
            <n-card title="公开笔记" class="glass-card">
              <n-list hoverable clickable>
                <div v-if="notes.length === 0" class="py-8 text-center">
                  <n-empty description="暂无公开笔记" />
                </div>
                <n-list-item v-for="note in notes" :key="note.id" @click="goToNote(note.id)">
                  <n-thing>
                    <template #header>
                      <div class="flex items-center gap-2">
                        <n-icon class="text-indigo-500"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg></n-icon>
                        <span class="font-bold text-gray-700">{{ note.title }}</span>
                      </div>
                    </template>
                    <template #description>
                      <div class="flex gap-4 text-xs text-gray-400 mt-1">
                        <span>{{ new Date(note.updatedTime).toLocaleDateString() }}</span>
                        <span v-if="note.folderName">{{ note.folderName }}</span>
                      </div>
                    </template>
                    <div class="mt-2 text-gray-600 text-sm line-clamp-2">
                      {{ note.summary || '暂无摘要...' }}
                    </div>
                  </n-thing>
                </n-list-item>
              </n-list>

              <div class="flex justify-center mt-6" v-if="pagination.total > 0">
                <n-pagination
                  v-model:page="pagination.page"
                  :item-count="pagination.total"
                  :page-size="pagination.size"
                  @update:page="handlePageChange"
                />
              </div>
            </n-card>
          </div>
        </div>

        <div v-else-if="!loading" class="flex flex-col items-center justify-center py-20">
          <n-empty description="未找到用户主页" size="large" />
          <n-button class="mt-4" @click="router.push('/community/feed')">返回社区</n-button>
        </div>
      </n-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NCard, NAvatar, NList, NListItem, NThing, NIcon, NPagination, NSpin, NEmpty, NButton, useMessage } from 'naive-ui'
import { getUserPublicProfile, getUserPublicNotes, getUserHeatmap, followUser, unfollowUser } from '@/api/community'
import type { UserPublicProfile } from '@/api/community'
import { useUserStore } from '@/store/user'
import type { Note } from '@/api/note'
import * as echarts from 'echarts/core'
import { CalendarComponent, TooltipComponent, VisualMapComponent } from 'echarts/components'
import { HeatmapChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([CalendarComponent, TooltipComponent, VisualMapComponent, HeatmapChart, CanvasRenderer])

const route = useRoute()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

const userId = ref<number>(Number(route.params.id))
const loading = ref(false)
const followLoading = ref(false)
const profile = ref<UserPublicProfile | null>(null)
const notes = ref<Note[]>([])
const pagination = ref({ page: 1, size: 10, total: 0 })

// Heatmap
const heatmapRef = ref<HTMLElement | null>(null)
const chartInstance = shallowRef<echarts.ECharts | null>(null)

const initHeatmap = (heatmapData: any[]) => {
  if (!heatmapRef.value) return
  
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
  
  chartInstance.value = echarts.init(heatmapRef.value)
  
  const today = new Date()
  const oneYearAgo = new Date(today.getFullYear() - 1, today.getMonth(), today.getDate())
  const startDate = echarts.format.formatTime('yyyy-MM-dd', oneYearAgo)
  const endDate = echarts.format.formatTime('yyyy-MM-dd', today)

  // const data = heatmapData.map(item => [item.date, item.count])
  const data = heatmapData.map(item => [item.date, item.noteCount || 0])

  const option = {
    tooltip: {
      position: 'top',
      formatter: function (p: any) {
        const format = echarts.format.formatTime('yyyy-MM-dd', p.data[0])
        return `${format}: ${p.data[1]} 篇笔记`
      }
    },
    visualMap: {
      min: 0,
      max: 5,
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: {
        color: ['#ebedf0', '#c6e48b', '#7bc96f', '#239a3b', '#196127']
      },
      show: true,
      text: ['多', '少'],
    },
    calendar: {
      top: 30,
      left: 30,
      right: 30,
      cellSize: ['auto', 16],
      range: [startDate, endDate],
      itemStyle: {
        borderWidth: 3,
        borderColor: '#fff',
        color: '#ebedf0'
      },
      splitLine: { show: false },
      yearLabel: { show: false },
      dayLabel: {
        nameMap: ['日', '一', '二', '三', '四', '五', '六'],
        color: '#9ca3af',
        fontSize: 10
      },
      monthLabel: {
        nameMap: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
        color: '#9ca3af',
        fontSize: 10
      }
    },
    series: {
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: data
    }
  }
  
  chartInstance.value.setOption(option)
}

const handleResize = () => {
    chartInstance.value?.resize()
}

const fetchData = async () => {
  loading.value = true
  try {
    const id = userId.value
    if (!id || isNaN(id)) throw new Error('Invalid User ID')
    
    // 1. Fetch Profile
    const profileRes = await getUserPublicProfile(id)
    profile.value = profileRes

    // 2. Fetch Notes
    await fetchNotes()

    // 3. Fetch Heatmap
    const heatmapRes = await getUserHeatmap(id)
    await nextTick()
    if (heatmapRef.value) {
      initHeatmap(heatmapRes)
    }

  } catch (e: any) {
    console.error('获取用户数据失败', e)
    message.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const fetchNotes = async () => {
  try {
    const res = await getUserPublicNotes(userId.value, pagination.value.page, pagination.value.size)
    notes.value = res.records
    pagination.value.total = res.total || 0
  } catch (e: any) {
    console.error('获取公开笔记失败', e)
    message.error('加载笔记失败')
  }
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  fetchNotes()
}

const goToNote = (id: number) => {
  router.push(`/community/note/${id}`)
}

const handleFollow = async () => {
  if (!userStore.isLoggedIn) {
     message.warning('请先登录后进行关注')
     router.push('/login')
     return
  }
  followLoading.value = true
  try {
    await followUser(userId.value)
    message.success('关注成功')
    if (profile.value) {
      profile.value.isFollowing = true
      profile.value.followerCount += 1
    }
  } catch (error: any) {
    message.error(error.message || '关注失败')
  } finally {
    followLoading.value = false
  }
}

const handleUnfollow = async () => {
  followLoading.value = true
  try {
    await unfollowUser(userId.value)
    message.success('已取消关注')
    if (profile.value) {
      profile.value.isFollowing = false
      profile.value.followerCount = Math.max(0, profile.value.followerCount - 1)
    }
  } catch (error: any) {
    message.error(error.message || '取消关注失败')
  } finally {
    followLoading.value = false
  }
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
})

</script>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 1.5rem;
}
</style>
