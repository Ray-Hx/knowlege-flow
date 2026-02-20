<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NInput, NButton, NSelect, NGrid, NGridItem, NCard, NTag, NIcon, NPagination, NEmpty } from 'naive-ui'
import { SearchOutline, FilterOutline, AddOutline, FolderOpenOutline } from '@vicons/ionicons5'
import { MOCK_NOTES } from '@/api/mockData'

const router = useRouter()
const searchText = ref('')
const selectedFolder = ref(null)

const folderOptions = [
  { label: 'All Folders', value: null },
  { label: 'Design Docs', value: 'Design Docs' },
  { label: 'Learning', value: 'Learning' },
  { label: 'Tech Notes', value: 'Tech Notes' },
  { label: 'Meetings', value: 'Meetings' }
]

const filteredNotes = computed(() => {
  return MOCK_NOTES.filter(note => {
    const matchesSearch = note.title.toLowerCase().includes(searchText.value.toLowerCase()) || 
                          note.summary.toLowerCase().includes(searchText.value.toLowerCase())
    const matchesFolder = selectedFolder.value ? note.folder === selectedFolder.value : true
    return matchesSearch && matchesFolder
  })
})

function goToNote(id: number) {
  router.push(`/notes/${id}`)
}
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- Header Controls -->
    <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-6">
      <div class="flex items-center gap-4 w-full md:w-auto">
        <h2 class="text-2xl font-bold whitespace-nowrap">My Notes</h2>
        <n-tag type="info" round :bordered="false" class="px-3">
          {{ filteredNotes.length }}
        </n-tag>
      </div>
      
      <div class="flex gap-3 w-full md:w-auto">
        <n-input v-model:value="searchText" placeholder="Search notes..." class="w-full md:w-64">
          <template #prefix>
            <n-icon :component="SearchOutline" />
          </template>
        </n-input>
        <n-select v-model:value="selectedFolder" :options="folderOptions" placeholder="Folder" class="w-40" />
        <n-button type="primary" class="shadow-md shadow-blue-500/30">
          <template #icon>
            <n-icon :component="AddOutline" />
          </template>
          New
        </n-button>
      </div>
    </div>

    <!-- Note Grid -->
    <div v-if="filteredNotes.length > 0" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6 auto-rows-fr">
      <div v-for="note in filteredNotes" :key="note.id">
        <n-card 
          hoverable 
          class="h-full rounded-xl cursor-pointer transition-all duration-300 hover:-translate-y-1 hover:shadow-lg border border-gray-100 dark:border-gray-800"
          @click="goToNote(note.id)"
        >
          <template #header>
            <div class="font-bold text-lg truncate" :title="note.title">{{ note.title }}</div>
          </template>
          <template #header-extra>
             <n-tag size="small" :bordered="false" round class="bg-gray-100 dark:bg-gray-700 text-gray-500">
               {{ note.updatedAt.split(' ')[0] }}
             </n-tag>
          </template>
          
          <div class="text-gray-500 dark:text-gray-400 text-sm line-clamp-3 mb-4 h-16">
            {{ note.summary }}
          </div>
          
          <template #footer>
            <div class="flex justify-between items-center">
              <div class="flex items-center gap-1 text-xs text-gray-400">
                <n-icon :component="FolderOpenOutline" />
                {{ note.folder }}
              </div>
              <div class="flex gap-2">
                <n-tag v-for="tag in note.tags.slice(0, 2)" :key="tag" size="tiny" :bordered="false" type="primary" class="opacity-80">
                  #{{ tag }}
                </n-tag>
                <span v-if="note.tags.length > 2" class="text-xs text-gray-400 flex items-center">+{{ note.tags.length - 2 }}</span>
              </div>
            </div>
          </template>
        </n-card>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="flex-1 flex justify-center items-center">
      <n-empty description="No notes found matching your criteria">
        <template #extra>
          <n-button size="small" @click="searchText = ''; selectedFolder = null">
            Clear filters
          </n-button>
        </template>
      </n-empty>
    </div>
    
    <!-- Pagination (Mock) -->
    <div class="mt-8 flex justify-center" v-if="filteredNotes.length > 0">
        <n-pagination :item-count="filteredNotes.length" :page-size="9" />
    </div>
  </div>
</template>
