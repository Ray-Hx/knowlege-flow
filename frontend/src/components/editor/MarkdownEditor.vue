<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '开始编辑...'
  },
  height: {
    type: [Number, String],
    default: 'calc(100vh - 200px)'
  }
})

const emit = defineEmits(['update:modelValue', 'save'])

const editorRef = ref<HTMLElement>()
const vditor = ref<Vditor | null>(null)

// 初始化编辑器
const initVditor = () => {
  if (!editorRef.value) return
  
  vditor.value = new Vditor(editorRef.value, {
    height: props.height,
    mode: 'ir', // 即时渲染模式
    placeholder: props.placeholder,
    toolbar: [
      'emoji', 'headings', 'bold', 'italic', 'strike', 'link', '|',
      'list', 'ordered-list', 'check', 'outdent', 'indent', '|',
      'quote', 'line', 'code', 'inline-code', 'insert-before', 'insert-after', '|',
      'upload', 'record', 'table', '|',
      'undo', 'redo', '|',
      'edit-mode', 'content-theme', 'code-theme', 'export',
      {
        name: 'save',
        tip: '保存 (Ctrl+S)',
        className: 'right',
        icon: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32"><path d="M28 8l-4-4h-22v28h28v-24zM8 24h16v-12h-16v12zM22 4l3 3h-3v-3z"></path></svg>',
        click: () => {
          emit('save', vditor.value?.getValue())
        }
      }
    ],
    cache: {
      enable: false
    },
    after: () => {
      vditor.value?.setValue(props.modelValue)
    },
    input: (val) => {
      emit('update:modelValue', val)
    }
  })
}

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  if (vditor.value && newVal !== vditor.value.getValue()) {
    vditor.value.setValue(newVal)
  }
})

onMounted(() => {
  initVditor()
})

onBeforeUnmount(() => {
  if (vditor.value) {
    vditor.value.destroy()
    vditor.value = null
  }
})

// 暴露方法
defineExpose({
  getVditor: () => vditor.value
})
</script>

<template>
  <div ref="editorRef" class="vditor-container"></div>
</template>

<style scoped>
.vditor-container {
  border: none;
}
:deep(.vditor) {
  border: 1px solid #e5e7eb;
  border-radius: 0.375rem;
}
</style>
