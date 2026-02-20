<template>
    <div class="mindmap-container">
        <div class="mindmap-toolbar">
            <n-space>
                <n-button @click="zoomIn" size="small">放大</n-button>
                <n-button @click="zoomOut" size="small">缩小</n-button>
                <n-button @click="fit" size="small">适应窗口</n-button>
            </n-space>
        </div>
        <div ref="svgRef" class="mindmap-svg"></div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { Transformer } from 'markmap-lib'
import { Markmap } from 'markmap-view'

const props = defineProps<{
    markdown: string
}>()

const svgRef = ref<HTMLElement | null>(null)
let markmapInstance: Markmap | null = null
const transformer = new Transformer()

const renderMindmap = () => {
    if (!svgRef.value || !props.markdown) return
    
    // 清空容器
    svgRef.value.innerHTML = '<svg id="mindmap-svg" style="width: 100%; height: 100%;"></svg>'
    
    // 解析 Markdown
    const { root } = transformer.transform(props.markdown)
    
    // 获取 SVG 元素并渲染
    const svg = svgRef.value.querySelector('#mindmap-svg')
    if (svg) {
        markmapInstance = Markmap.create(svg as SVGElement, {
            autoFit: true,
            color: (node: any) => {
                const colors = ['#1890ff', '#52c41a', '#faad14', '#eb2f96', '#722ed1']
                return colors[node.state?.depth % colors.length || 0]
            },
            paddingX: 16,
        }, root)
    }
}

const zoomIn = () => {
    if (markmapInstance) {
        markmapInstance.rescale(1.25)
    }
}

const zoomOut = () => {
    if (markmapInstance) {
        markmapInstance.rescale(0.8)
    }
}

const fit = () => {
    if (markmapInstance) {
        markmapInstance.fit()
    }
}

onMounted(() => {
    renderMindmap()
})

watch(() => props.markdown, () => {
    renderMindmap()
})

onUnmounted(() => {
    markmapInstance = null
})
</script>

<style scoped>
.mindmap-container {
    display: flex;
    flex-direction: column;
    height: 100%;
    background: #fafafa;
    border-radius: 8px;
}

.mindmap-toolbar {
    padding: 12px;
    border-bottom: 1px solid #eee;
    background: white;
}

.mindmap-svg {
    flex: 1;
    overflow: hidden;
}
</style>
