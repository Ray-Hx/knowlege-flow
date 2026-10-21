<script setup lang="ts">
import { h, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { NLayout, NLayoutSider, NLayoutHeader, NLayoutContent, NMenu, NIcon, NAvatar, NText, NBadge } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import { 
  HomeOutline, 
  BookOutline, 
  ShareSocialOutline, 
  SearchOutline, 
  CalendarOutline,
  ChatbubbleEllipsesOutline,
  LogOutOutline,
  NotificationsOutline,
  ReaderOutline,
  ChatboxOutline,
  PlanetOutline,
  LayersOutline
} from '@vicons/ionicons5'
import { useUserStore } from '@/store/user'
import { onMounted } from 'vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)

function renderIcon(icon: any) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

function renderBadgeIcon(icon: any, countGetter: () => number) {
  return () => h(
    NBadge, 
    { value: countGetter(), max: 99, dot: false, showZero: false }, 
    { default: () => h(NIcon, null, { default: () => h(icon) }) }
  )
}

const menuOptions: MenuOption[] = [
  {
    label: () => h(RouterLink, { to: '/dashboard' }, { default: () => '仪表盘' }),
    key: 'dashboard',
    icon: renderIcon(HomeOutline)
  },
  {
    label: () => h(RouterLink, { to: '/notes' }, { default: () => '我的笔记' }),
    key: 'notes',
    icon: renderIcon(BookOutline)
  },
  {
    label: () => h(RouterLink, { to: '/community/feed' }, { default: () => '社区大厅' }),
    key: 'community',
    icon: renderIcon(PlanetOutline)
  },
  {
    label: () => h(RouterLink, { to: '/graph' }, { default: () => '知识图谱' }),
    key: 'graph',
    icon: renderIcon(ShareSocialOutline)
  },
  {
    label: () => h(RouterLink, { to: '/messages' }, { default: () => '消息中心' }),
    key: 'messages',
    icon: renderBadgeIcon(ChatboxOutline, () => userStore.unreadMessageCount)
  },
  {
    label: () => h(RouterLink, { to: '/search' }, { default: () => '全文搜索' }),
    key: 'search',
    icon: renderIcon(SearchOutline)
  },
  {
    label: () => h(RouterLink, { to: '/content' }, { default: () => '订阅源' }),
    key: 'content',
    icon: renderIcon(LayersOutline)
  },
  {
    label: () => h(RouterLink, { to: '/aggregation' }, { default: () => '内容聚合' }),
    key: 'aggregation',
    icon: renderIcon(ReaderOutline)
  },
  {
    label: () => h(RouterLink, { to: '/ai' }, { default: () => 'AI助手' }),
    key: 'ai',
    icon: renderIcon(ChatbubbleEllipsesOutline)
  },
  {
    label: () => h(RouterLink, { to: '/review' }, { default: () => '间隔复习' }),
    key: 'review',
    icon: renderIcon(CalendarOutline)
  }
]

function handleUserSelect(key: string) {
  if (key === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

// 简单的选中项逻辑，实际可能需要根据路由 match
const activeKey = ref<string | null>(null)
// 监听路由变化更新 activeKey
import { watchEffect } from 'vue'
watchEffect(() => {
  const path = route.path
  if (path.includes('/dashboard')) activeKey.value = 'dashboard'
  else if (path.includes('/notes')) activeKey.value = 'notes'
  else if (path.includes('/community')) activeKey.value = 'community'
  else if (path.includes('/graph')) activeKey.value = 'graph'
  else if (path.includes('/messages')) activeKey.value = 'messages'
  else if (path.includes('/search')) activeKey.value = 'search'
  else if (path.includes('/content')) activeKey.value = 'content'
  else if (path.includes('/aggregation')) activeKey.value = 'aggregation'
  else if (path.includes('/ai')) activeKey.value = 'ai'
  else if (path.includes('/review')) activeKey.value = 'review'
})

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchUnreadMessageCount()
    // 简单的轮询，每10秒刷一次
    setInterval(() => {
      userStore.fetchUnreadMessageCount()
    }, 10000)
  }
})

</script>

<template>
  <n-layout has-sider class="h-screen bg-transparent">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="80"
      :width="260"
      :collapsed="collapsed"
      show-trigger="arrow-circle"
      @collapse="collapsed = true"
      @expand="collapsed = false"
      class="glass-panel m-4 rounded-3xl"
      content-style="display: flex; flex-direction: column;"
    >
      <div class="h-24 flex items-center justify-center">
        <div 
          class="flex items-center justify-center bg-gray-900 text-white rounded-2xl shadow-lg transition-all duration-500"
          :class="collapsed ? 'w-10 h-10' : 'w-12 h-12'"
        >
          <n-icon size="24" :component="BookOutline" />
        </div>
        <n-text 
          v-if="!collapsed" 
          class="ml-3 text-xl font-bold text-gray-900"
        >
          Knowledge
        </n-text>
      </div>

      <n-menu
        :collapsed="collapsed"
        :collapsed-width="80"
        :collapsed-icon-size="24"
        :icon-size="24"
        :options="menuOptions"
        v-model:value="activeKey"
        class="flex-1 px-2"
      />

      <div class="p-4 mb-2">
        <div 
          class="glass-card p-3 flex items-center gap-3 cursor-pointer hover:bg-white/50 transition-colors"
          @click="router.push('/profile/settings')"
        >
          <n-avatar round size="small" :src="userStore.userInfo.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
          <div v-if="!collapsed" class="flex flex-col overflow-hidden">
            <span class="text-sm font-bold text-gray-700 truncate">{{ userStore.userInfo.username || '用户' }}</span>
            <span class="text-xs text-gray-500 truncate">{{ (userStore.userInfo.roles && userStore.userInfo.roles.length > 0) ? userStore.userInfo.roles.join(', ') : '普通用户' }}</span>
          </div>
          <n-icon v-if="!collapsed" :component="LogOutOutline" class="ml-auto text-gray-400 hover:text-red-500" @click.stop="handleUserSelect('logout')" />
        </div>
      </div>
    </n-layout-sider>
    
    <n-layout class="bg-transparent py-4 pr-4 pl-0">
      <n-layout-header class="glass-panel h-16 px-6 mb-4 rounded-2xl flex items-center justify-between">
        <div class="header-left flex items-center gap-4">
          <n-text class="text-lg font-medium text-gray-700">{{ route.meta.title }}</n-text>
        </div>
<!--        <div class="header-right flex items-center gap-4">-->
<!--          <n-button circle secondary class="glass-button">-->
<!--            <template #icon><n-icon :component="SearchOutline" /></template>-->
<!--          </n-button>-->
<!--          <n-button circle secondary class="glass-button">-->
<!--            <template #icon><n-icon :component="NotificationsOutline" /></template>-->
<!--          </n-button>-->
<!--        </div>-->
      </n-layout-header>
      
      <n-layout-content class="glass-panel rounded-3xl p-6 h-[calc(100vh-8rem)] overflow-hidden">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" class="h-full" />
          </transition>
        </router-view>
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<style scoped>
:deep(.n-layout-sider) {
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

:deep(.n-menu .n-menu-item-content) {
  padding-left: 12px !important;
}

:deep(.n-menu .n-menu-item-content::before) {
  border-radius: 12px !important;
  left: 4px;
  right: 4px;
}

:deep(.n-menu .n-menu-item-content:hover::before) {
  background-color: rgba(0, 0, 0, 0.03) !important;
}

:deep(.n-menu .n-menu-item-content.n-menu-item-content--selected::before) {
  background: rgba(0, 0, 0, 0.85) !important;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

:deep(.n-menu .n-menu-item-content.n-menu-item-content--selected .n-menu-item-content__icon) {
  color: #fff !important;
}

:deep(.n-menu .n-menu-item-content.n-menu-item-content--selected .n-menu-item-content__label) {
  color: #fff !important;
  font-weight: 600;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
