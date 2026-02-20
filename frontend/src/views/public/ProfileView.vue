<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NCard, NAvatar, NTag, NList, NListItem, NThing, NIcon, NPagination, NSpin, NEmpty, NButton, useMessage } from 'naive-ui'
import { PersonOutline, DocumentTextOutline, LogoGithub, ShareSocialOutline, ArrowBack } from '@vicons/ionicons5'
import { publicApi, PublicUserProfile, PublicNote } from '@/api/public'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const username = ref('')
const loading = ref(false)
const profile = ref<PublicUserProfile | null>(null)
const notes = ref<PublicNote[]>([])
const pagination = ref({ page: 1, size: 10, total: 0 })

const fetchProfile = async () => {
  loading.value = true
  try {
    profile.value = await publicApi.getProfile(username.value)
    fetchNotes()
  } catch (e) {
    message.error(e.message || '用户不存在或无法访问')
    profile.value = null // Reset
  } finally {
    loading.value = false
  }
}

const fetchNotes = async () => {
  try {
    const res = await publicApi.listNotes(username.value, pagination.value.page, pagination.value.size)
    notes.value = res.content
    pagination.value.total = res.totalElements
  } catch (e) {
    console.error(e)
  }
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  fetchNotes()
}

const goToNote = (id: number) => {
  // Public Access to Note Detail? 
  // We might need a public note view or reusing NoteDetail with a flag?
  // Current NoteDetail usually requires Auth.
  // For simplicity, let's assume we can view it if we implemented public access properly.
  // But wait, standard /notes/:id route is guarded by auth in router.
  // We need a public route like /u/:username/note/:id or just /n/:id if public.
  // Let's stick to list for now or redirect to login.
  message.info('查看详情功能需登录或进一步实现公开详情页')
}

watch(() => route.params.username, (newVal) => {
  if (newVal) {
    username.value = newVal as string
    fetchProfile()
  }
}, { immediate: true })
</script>

<template>
  <div class="public-profile min-h-screen bg-gradient-to-br from-indigo-50 to-purple-50 p-4 md:p-8">
    <div class="max-w-5xl mx-auto">
      <!-- Header / Nav -->
      <div class="flex justify-between items-center mb-8">
        <div class="flex items-center gap-2 cursor-pointer" @click="router.push('/')">
          <div class="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white">
            <n-icon><ShareSocialOutline /></n-icon>
          </div>
          <span class="font-bold text-xl text-gray-800">KnowledgeFlow</span>
        </div>
        <div>
          <n-button secondary round @click="router.push('/login')">登录 / 注册</n-button>
        </div>
      </div>

      <n-spin :show="loading">
        <div v-if="profile" class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <!-- Left: Profile Card -->
          <div class="md:col-span-1">
            <n-card class="glass-card text-center sticky top-8">
              <div class="relative inline-block mb-4">
                <n-avatar round :size="120" :src="profile.avatar" class="border-4 border-white shadow-lg" />
                <div class="absolute bottom-2 right-2 w-4 h-4 bg-green-500 rounded-full border-2 border-white"></div>
              </div>
              
              <h2 class="text-2xl font-bold text-gray-800 mb-1">{{ profile.nickname || profile.username }}</h2>
              <p class="text-gray-500 text-sm mb-4">@{{ profile.username }}</p>
              
              <div class="flex justify-center gap-2 mb-6">
                <n-tag v-if="profile.learningGoals" type="info" size="small" round>
                  {{ JSON.stringify(profile.learningGoals).length > 20 ? '专注学习中' : 'Learning' }} <!-- Simplified parse -->
                </n-tag>
              </div>

              <p class="text-gray-600 mb-6 text-left px-4 leading-relaxed">
                {{ profile.bio || '这个用户很懒，什么都没写...' }}
              </p>

              <div class="flex justify-center gap-4">
                <n-button circle secondary>
                  <template #icon><n-icon><LogoGithub /></n-icon></template>
                </n-button>
                <n-button circle secondary>
                  <template #icon><n-icon><ShareSocialOutline /></n-icon></template>
                </n-button>
              </div>
            </n-card>
          </div>

          <!-- Right: Public Content -->
          <div class="md:col-span-2 space-y-6">
            <n-card title="公开笔记" class="glass-card">
              <template #header-extra>
                <n-tag type="success" size="small" round>
                  {{ pagination.total }} 篇
                </n-tag>
              </template>
              
              <n-list hoverable clickable>
                <div v-if="notes.length === 0" class="py-8 text-center">
                  <n-empty description="暂无公开笔记" />
                </div>
                <n-list-item v-for="note in notes" :key="note.id" @click="goToNote(note.id)">
                  <n-thing>
                    <template #header>
                      <div class="flex items-center gap-2">
                        <n-icon class="text-indigo-500"><DocumentTextOutline /></n-icon>
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
          <n-empty description="未找到用户或用户未公开主页" size="large" />
          <n-button class="mt-4" @click="router.push('/')">返回首页</n-button>
        </div>
      </n-spin>
    </div>
  </div>
</template>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 1.5rem;
}
</style>
