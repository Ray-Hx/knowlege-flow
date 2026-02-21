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
    preview: {
      theme: { current: 'light' },
      transform(html: string) {
        html = html.replace(/\[\[!article:(\d+)(?:\|([^\]]+))?\]\]/g, (_match, id, title) => {
            const displayTitle = title || `文章引用 #${id}`;
            return `<a class="article-ref-badge" href="/content/article/${id}" target="_blank">📰 ${displayTitle}</a>`;
        })
        html = html.replace(/\[\[([^\]!][^\]]*)\]\]/g, 
            '<a class="note-ref-badge" href="/notes?keyword=$1" target="_blank">📝 $1</a>'
        )
        return html
      }
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

<style>
/* 继承全局样式用于 Vditor 内部渲染 */
a.article-ref-badge {
    display: inline-block;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white !important;
    padding: 2px 10px;
    border-radius: 12px;
    font-size: 13px;
    margin: 0 2px;
    cursor: pointer;
    text-decoration: none !important;
}
a.article-ref-badge:hover {
    opacity: 0.9;
    transform: scale(1.02);
}
a.note-ref-badge {
    display: inline-block;
    background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    color: white !important;
    padding: 2px 10px;
    border-radius: 12px;
    font-size: 13px;
    margin: 0 2px;
    cursor: pointer;
    text-decoration: none !important;
}
a.note-ref-badge:hover {
    opacity: 0.9;
    transform: scale(1.02);
}
</style>
