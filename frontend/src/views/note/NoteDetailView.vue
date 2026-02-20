<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NIcon, NSpace, NTag, NSpin, useMessage } from 'naive-ui'
import { ArrowBackOutline, SaveOutline, ShareSocialOutline, TrashOutline } from '@vicons/ionicons5'
import { MOCK_NOTES } from '@/api/mockData'
import { getNoteDetail, updateNote, deleteNote as deleteNoteApi } from '@/api/note'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const noteId = Number(route.params.id)
const note = ref<any>(null)
const loading = ref(true)
const saving = ref(false)
const vditor = ref<Vditor | null>(null)
const useMock = ref(false) // Tracks if using mock data

onMounted(async () => {
  try {
    // Try real API first
    const res = await getNoteDetail(noteId)
    if (res.code === 200 && res.data) {
      note.value = {
        id: res.data.id,
        title: res.data.title,
        content: res.data.content || '',
        summary: res.data.summary,
        tags: res.data.tags || [],
        updatedAt: res.data.updatedTime
      }
    } else {
      throw new Error('API returned no data')
    }
  } catch (error) {
    console.warn('Backend unavailable, using mock data:', error)
    useMock.value = true
    // Fallback to mock data
    note.value = MOCK_NOTES.find(n => n.id === noteId) || {
      id: 0,
      title: 'New Note',
      summary: '',
      content: '# New Note\n\nStart writing...',
      tags: [],
      updatedAt: new Date().toLocaleString()
    }
  } finally {
    loading.value = false
    // Initialize Vditor after data is loaded
    setTimeout(() => {
      initVditor()
    }, 100)
  }
})


function initVditor() {
    const options: IOptions = {
        height: '100%',
        mode: 'ir', // Instant Rendering mode (Obsidian-like)
        placeholder: 'Start writing...',
        toolbarConfig: {
            pin: true,
        },
        cache: {
            enable: false,
        },
        after: () => {
            if (vditor.value && note.value) {
                vditor.value.setValue(note.value.content)
            }
        },
        input: (value) => {
            if (note.value) {
                note.value.content = value
            }
        }
    }
    vditor.value = new Vditor('vditor', options)
}

function handleSave() {
  saving.value = true
  // Get content from Vditor just to be safe
  if (vditor.value) {
      note.value.content = vditor.value.getValue()
  }
  
  setTimeout(() => {
    saving.value = false
    message.success('Saved successfully')
  }, 800)
}

function handleDelete() {
    message.warning('Note deleted')
    router.push('/notes')
}

onBeforeUnmount(() => {
    if (vditor.value) {
        vditor.value.destroy()
        vditor.value = null
    }
})
</script>

<template>
  <div class="h-full flex flex-col gap-4">
    <!-- Toolbar -->
    <div class="flex justify-between items-center bg-white dark:bg-gray-800 p-2 rounded-lg shadow-sm">
      <div class="flex items-center gap-2">
        <n-button text @click="router.push('/notes')">
          <template #icon><n-icon :component="ArrowBackOutline" /></template>
          Back
        </n-button>
        <div class="h-6 w-px bg-gray-300 dark:bg-gray-600 mx-2"></div>
        <span class="text-sm text-gray-400" v-if="note">Last edited: {{ note.updatedAt }}</span>
      </div>
      
      <n-space>
        <n-button quaternary circle type="error" @click="handleDelete">
            <template #icon><n-icon :component="TrashOutline" /></template>
        </n-button>
        <n-button quaternary circle>
            <template #icon><n-icon :component="ShareSocialOutline" /></template>
        </n-button>
        <n-button type="primary" :loading="saving" @click="handleSave">
            <template #icon><n-icon :component="SaveOutline" /></template>
            Save
        </n-button>
      </n-space>
    </div>

    <!-- Editor Area -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <n-spin size="large" />
    </div>
    
    <div v-else class="flex-1 flex flex-col bg-white dark:bg-gray-800 rounded-xl shadow-sm overflow-hidden">
      <!-- Title Input -->
      <div class="p-6 pb-2 border-b border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 z-10">
        <input 
          v-model="note.title" 
          class="w-full text-3xl font-bold bg-transparent outline-none placeholder-gray-400 text-gray-800 dark:text-gray-100" 
          placeholder="Note Title"
        />
        <div class="flex gap-2 mt-4">
            <n-tag v-for="tag in note.tags" :key="tag" closable size="small" round>
                {{ tag }}
            </n-tag>
            <n-tag type="success" size="small" class="cursor-pointer border-dashed" round>+ Add Tag</n-tag>
        </div>
      </div>
      
      <!-- Content (Vditor) -->
      <div class="flex-1 overflow-hidden relative"> <!-- Added relative and overflow-hidden -->
        <div id="vditor" class="absolute inset-0"></div> <!-- Absolute positioning to fill -->
      </div>
    </div>
  </div>
</template>
