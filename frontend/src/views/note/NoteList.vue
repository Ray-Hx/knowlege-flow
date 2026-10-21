<script setup lang="ts">
import { ref, onMounted, computed, h } from 'vue'
import { useRouter } from 'vue-router'
import { 
  NLayout, NLayoutSider, NLayoutContent, NInput, NButton, NTree, NSpace, NTag, NIcon, 
  useMessage, useDialog, NEmpty, NSpin, NPagination, NDropdown, NModal, TreeOption
} from 'naive-ui'
import { 
  SearchOutline, AddOutline, FolderOpenOutline, DocumentTextOutline, 
  TrashOutline, CreateOutline, EllipsisHorizontal, TimeOutline
} from '@vicons/ionicons5'
import { noteApi, Note, NoteFolder } from '@/api/note'
import { updateFolder, deleteFolder, createFolder } from '@/api/folder'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

// 状态
const loading = ref(false)
const folders = ref<NoteFolder[]>([])
const notes = ref<Note[]>([])
const selectedFolderId = ref<number | undefined>(undefined)
const searchQuery = ref('')
const pagination = ref({ page: 1, pageSize: 12, itemCount: 0 })

// 文件夹编辑状态
const showFolderModal = ref(false)
const folderModalMode = ref<'create' | 'edit'>('create')
const editingFolder = ref<NoteFolder | null>(null)
const folderName = ref('')

// 带默认文件夹的列表
const foldersWithDefault = computed<TreeOption[]>(() => [
  { id: 0, name: '默认文件夹', key: 0, label: '默认文件夹' },
  ...folders.value.map(f => ({ ...f, key: f.id, label: f.name }))
])

// 获取文件夹列表
const fetchFolders = async () => {
  try {
    const res = await noteApi.getFolders()
    folders.value = res
  } catch (e) {
    console.error(e)
  }
}

// 获取笔记列表
const fetchNotes = async () => {
  loading.value = true
  try {
    const res = await noteApi.getNotes({
      page: pagination.value.page,
      size: pagination.value.pageSize,
      folderId: selectedFolderId.value,
      keyword: searchQuery.value
    })
    notes.value = res.records
    pagination.value.itemCount = res.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 事件处理
const handleFolderSelect = (keys: Array<string | number>) => {
  selectedFolderId.value = keys[0] as number
  pagination.value.page = 1
  fetchNotes()
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  fetchNotes()
}

const handleSearch = () => {
  pagination.value.page = 1
  fetchNotes()
}

const handleCreateNote = () => {
  router.push('/notes/new')
}

const goToNote = (id: number) => {
  router.push(`/notes/${id}`)
}

const handleDelete = (note: Note, e: Event) => {
  e.stopPropagation()
  dialog.warning({
    title: '确认删除',
    content: '确定要删除这条笔记吗？此操作无法撤销。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await noteApi.deleteNote(note.id)
        message.success('删除成功')
        fetchNotes()
      } catch (e: any) {
        message.error(e.message || '删除失败')
      }
    }
  })
}

// 文件夹操作
const openCreateFolderModal = () => {
  folderModalMode.value = 'create'
  folderName.value = ''
  editingFolder.value = null
  showFolderModal.value = true
}

const openEditFolderModal = (folder: NoteFolder) => {
  if (folder.id === 0) {
    message.warning('默认文件夹不可编辑')
    return
  }
  folderModalMode.value = 'edit'
  folderName.value = folder.name
  editingFolder.value = folder
  showFolderModal.value = true
}

const handleSaveFolder = async () => {
  if (!folderName.value.trim()) {
    message.warning('请输入文件夹名称')
    return
  }
  try {
    if (folderModalMode.value === 'create') {
      await createFolder({ name: folderName.value })
      message.success('创建成功')
    } else if (editingFolder.value) {
      await updateFolder(editingFolder.value.id, { name: folderName.value })
      message.success('更新成功')
    }
    showFolderModal.value = false
    fetchFolders()
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}

const handleDeleteFolder = (folder: NoteFolder) => {
  if (folder.id === 0) {
    message.warning('默认文件夹不可删除')
    return
  }
  dialog.warning({
    title: '确认删除',
    content: `确定要删除文件夹"${folder.name}"吗？文件夹内的笔记将移至默认文件夹。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteFolder(folder.id)
        message.success('删除成功')
        fetchFolders()
        if (selectedFolderId.value === folder.id) {
          selectedFolderId.value = undefined
          fetchNotes()
        }
      } catch (e: any) {
        message.error(e.message || '删除失败')
      }
    }
  })
}

// 文件夹右键菜单
const folderMenuOptions = [
  { label: '编辑', key: 'edit' },
  { label: '删除', key: 'delete', props: { style: 'color: red' } }
]

const handleFolderMenuSelect = (key: string, folder: NoteFolder) => {
  if (key === 'edit') openEditFolderModal(folder)
  if (key === 'delete') handleDeleteFolder(folder)
}

const renderFolderSuffix = ({ option }: { option: TreeOption }) => {
  if (option.id === 0) return null
  const folder = option as unknown as NoteFolder
  return h(NDropdown, {
    trigger: 'click',
    options: folderMenuOptions,
    onSelect: (k: string) => handleFolderMenuSelect(k, folder)
  }, {
    default: () => h(NButton, {
      size: 'tiny',
      quaternary: true,
      circle: true,
      class: 'opacity-0 group-hover:opacity-100',
      onClick: (e: Event) => e.stopPropagation()
    }, { icon: () => h(NIcon, null, { default: () => h(EllipsisHorizontal) }) })
  })
}

// 统一渲染前缀图标（无论是不是默认文件夹，全部使用统一的蓝色文件夹线框图）
const renderFolderPrefix = ({ option }: { option: TreeOption }) => {
  return h(
    NIcon, 
    { class: 'text-indigo-500 mr-2 mt-0.5' }, 
    { default: () => h(FolderOpenOutline) }
  )
}

// 笔记操作菜单
const renderMenuOptions = (note: Note) => [
  { label: '编辑', key: 'edit', icon: () => h(NIcon, null, { default: () => h(CreateOutline) }) },
  { label: '删除', key: 'delete', icon: () => h(NIcon, null, { default: () => h(TrashOutline) }), props: { style: 'color: red' } }
]

const handleMenuSelect = (key: string, note: Note) => {
  if (key === 'edit') goToNote(note.id)
  if (key === 'delete') handleDelete(note, new Event('click'))
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString()
}

onMounted(() => {
  fetchFolders()
  fetchNotes()
})
</script>

<template>
  <n-layout has-sider class="h-full bg-gray-50/50 rounded-2xl overflow-hidden border border-gray-100 shadow-sm">
    <!-- 侧边文件夹栏 -->
    <n-layout-sider 
      bordered 
      width="260" 
      content-style="padding: 20px;" 
      class="bg-white/80 backdrop-blur-sm"
    >
      <div class="mb-6 flex justify-between items-center px-1">
        <h2 class="text-lg font-bold text-gray-800 flex items-center gap-2">
          <n-icon class="text-indigo-500"><FolderOpenOutline /></n-icon>
          我的文件夹
        </h2>
        <n-button size="tiny" quaternary circle type="primary" class="bg-indigo-50 hover:bg-indigo-100 text-indigo-600" @click="openCreateFolderModal">
          <template #icon><n-icon><AddOutline /></n-icon></template>
        </n-button>
      </div>
      <n-tree
        block-line
        :data="foldersWithDefault"
        key-field="id"
        label-field="name"
        selectable
        :render-prefix="renderFolderPrefix"
        :render-suffix="renderFolderSuffix"
        :on-update:selected-keys="handleFolderSelect"
        class="custom-tree"
      />
    </n-layout-sider>
    
    <!-- 主内容区 -->
    <n-layout-content content-style="padding: 32px;" class="bg-gray-50/30">
      <!-- 顶部工具栏 -->
      <div class="flex justify-between items-center mb-8">
        <div class="flex items-center gap-4 flex-1 max-w-xl">
          <n-input 
            v-model:value="searchQuery" 
            placeholder="搜索笔记..." 
            size="large" 
            round
            @keyup.enter="handleSearch"
            class="shadow-sm border-transparent hover:border-indigo-200 transition-all font-medium"
          >
            <template #prefix>
              <n-icon class="text-gray-400"><SearchOutline /></n-icon>
            </template>
          </n-input>
        </div>
        <n-button type="primary" size="large" round class="shadow-lg shadow-indigo-200" @click="handleCreateNote">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新建笔记
        </n-button>
      </div>
      
      <div v-if="searchQuery && notes.length > 0" class="mb-4 text-sm text-gray-500">
        共找到 <span class="font-bold text-indigo-600">{{ pagination.itemCount }}</span> 个关于 "<span class="italic">{{ searchQuery }}</span>" 的结果
      </div>
      
      <n-spin :show="loading">
        <!-- 笔记 List -->
        <div v-if="notes.length > 0" class="flex flex-col gap-4">
          <div 
            v-for="note in notes" 
            :key="note.id" 
            class="group bg-white rounded-xl p-4 border border-gray-100 hover:border-indigo-100 shadow-sm hover:shadow-lg hover:shadow-indigo-100/30 transition-all duration-300 hover:-translate-x-1 cursor-pointer flex items-center gap-4"
            @click="goToNote(note.id)"
          >
            <!-- Icon -->
            <div class="w-12 h-12 rounded-lg bg-indigo-50 text-indigo-500 flex items-center justify-center flex-shrink-0 group-hover:bg-indigo-500 group-hover:text-white transition-colors duration-300">
              <n-icon size="24"><DocumentTextOutline /></n-icon>
            </div>

            <!-- Content -->
            <div class="flex-1 min-w-0">
                <div class="flex justify-between items-center mb-1">
                  <h3 
                    class="font-bold text-gray-800 text-lg group-hover:text-indigo-600 transition-colors truncate"
                    v-html="note.title"
                  ></h3>
                  <div class="flex items-center gap-4 text-xs text-gray-400 flex-shrink-0">
                     <div class="flex items-center gap-1 bg-gray-50 px-2 py-1 rounded-md group-hover:bg-indigo-50 group-hover:text-indigo-500 transition-colors">
                       <n-icon><FolderOpenOutline /></n-icon>
                       <span class="max-w-[120px] truncate">{{ note.folderName || '默认文件夹' }}</span>
                     </div>
                     <div class="flex items-center gap-1">
                       <n-icon><TimeOutline /></n-icon>
                       <span>{{ formatDate(note.updatedTime) }}</span>
                     </div>
                  </div>
                </div>
                
                <p 
                  class="text-gray-500 text-sm truncate"
                  v-html="note.summary || '暂无摘要...'"
                ></p>
            </div>

            <!-- Actions -->
            <div class="opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0 pl-2" @click.stop>
              <n-dropdown :options="renderMenuOptions(note)" @select="(k) => handleMenuSelect(k, note)">
                <n-button size="small" quaternary circle>
                  <template #icon><n-icon><EllipsisHorizontal /></n-icon></template>
                </n-button>
              </n-dropdown>
            </div>
          </div>
        </div>

        <div v-else-if="!loading" class="flex flex-col items-center justify-center py-20 text-gray-400">
          <n-empty description="这里的知识一片荒芜..." size="large" />
          <n-button quaternary type="primary" class="mt-4" @click="handleCreateNote">
            去写第一篇笔记
          </n-button>
        </div>

        <!-- 分页 -->
        <div class="flex justify-center mt-10" v-if="notes.length > 0">
          <n-pagination
            v-model:page="pagination.page"
            :item-count="pagination.itemCount"
            :page-size="pagination.pageSize"
            @update:page="handlePageChange"
          />
        </div>
      </n-spin>
    </n-layout-content>

    <!-- 文件夹编辑弹窗 -->
    <n-modal v-model:show="showFolderModal" preset="dialog" :title="folderModalMode === 'create' ? '新建文件夹' : '编辑文件夹'">
      <n-input v-model:value="folderName" placeholder="输入文件夹名称" />
      <template #action>
        <n-button @click="showFolderModal = false">取消</n-button>
        <n-button type="primary" @click="handleSaveFolder">保存</n-button>
      </template>
    </n-modal>
  </n-layout>
</template>

<style scoped>
.custom-tree :deep(.n-tree-node-wrapper) {
  border-radius: 8px !important;
  transition: background-color 0.2s ease;
}
.custom-tree :deep(.n-tree-node-wrapper:hover) {
  background-color: #f9fafb !important;
}
.custom-tree :deep(.n-tree-node--selected .n-tree-node-wrapper) {
  background-color: #eef2ff !important;
}
.custom-tree :deep(.n-tree-node-content) {
  padding: 8px 0;
  background-color: transparent !important;
}
.custom-tree :deep(.n-tree-node-content:hover) {
  background-color: transparent !important;
}
.custom-tree :deep(.n-tree-node--selected .n-tree-node-content) {
  color: #4f46e5 !important;
  background-color: transparent !important;
}
.custom-tree :deep(.n-tree-node) {
  position: relative;
}
.custom-tree :deep(.n-tree-node:hover .opacity-0) {
  opacity: 1;
}
</style>
