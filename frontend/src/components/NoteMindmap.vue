<template>
    <div class="mindmap-container">
        <div class="mindmap-toolbar">
            <n-space>
                <n-button @click="zoomIn" size="small">放大</n-button>
                <n-button @click="zoomOut" size="small">缩小</n-button>
                <n-button @click="fit" size="small">适应窗口</n-button>
                <n-divider vertical />
                <n-button @click="downloadSVG" size="small">下载 SVG</n-button>
                <n-button @click="downloadPNG" size="small">下载图片</n-button>
            </n-space>
        </div>
        <div ref="svgRef" class="mindmap-svg"></div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { Transformer } from 'markmap-lib'
import { Markmap } from 'markmap-view'
import { NSpace, NButton, NDivider, useMessage } from 'naive-ui'

const props = defineProps<{
    markdown: string
    title?: string
}>()

const message = useMessage()
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

const getFullSVG = () => {
    const svg = svgRef.value?.querySelector('#mindmap-svg') as SVGGraphicsElement
    if (!svg) return null

    // Clone the SVG to avoid modifying the displayed one
    const clonedSvg = svg.cloneNode(true) as SVGGraphicsElement
    
    // Get the actual bounding box of the mindmap content
    // We need the original SVG for getBBox
    const bbox = svg.getBBox()
    const padding = 20
    
    const width = bbox.width + padding * 2
    const height = bbox.height + padding * 2
    
    clonedSvg.setAttribute('width', width.toString())
    clonedSvg.setAttribute('height', height.toString())
    clonedSvg.setAttribute('viewBox', `${bbox.x - padding} ${bbox.y - padding} ${width} ${height}`)
    
    // Add background color
    clonedSvg.style.backgroundColor = 'white'
    
    // Inline styles if necessary (markmap styles are usually injected globally)
    // For a standalone SVG, we might need to copy these styles.
    // However, markmap-view usually puts styles in the SVG or in a global <style> tag.
    const styles = document.querySelectorAll('style')
    let cssText = ''
    styles.forEach(style => {
        if (style.textContent?.includes('markmap')) {
            // Sanitize CSS to prevent "Tainted canvas" error
            // Remove @import and url() which might point to external resources like fonts
            let sanitized = style.textContent
                .replace(/@import\s+[^;]+;/g, '')
                .replace(/url\([^)]+\)/g, 'none')
            cssText += sanitized
        }
    })
    const styleElement = document.createElement('style')
    styleElement.textContent = cssText
    clonedSvg.insertBefore(styleElement, clonedSvg.firstChild)

    return {
        xml: new XMLSerializer().serializeToString(clonedSvg),
        width,
        height
    }
}

const downloadSVG = () => {
    const svgData = getFullSVG()
    if (!svgData) return

    const blob = new Blob([svgData.xml], { type: 'image/svg+xml;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${props.title || 'mindmap'}.svg`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    message.success('SVG 已开始下载')
}

const downloadPNG = () => {
    const svgData = getFullSVG()
    if (!svgData) return

    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const img = new Image()
    // Use btoa for base64 encoding to avoid tainting issues with some browsers
    // and ensure all characters are handled correctly
    const base64 = window.btoa(unescape(encodeURIComponent(svgData.xml)))
    const url = `data:image/svg+xml;base64,${base64}`

    img.onload = () => {
        const scale = 2
        canvas.width = svgData.width * scale
        canvas.height = svgData.height * scale
        
        ctx.fillStyle = 'white'
        ctx.fillRect(0, 0, canvas.width, canvas.height)
        
        ctx.scale(scale, scale)
        ctx.drawImage(img, 0, 0)
        
        try {
            const pngUrl = canvas.toDataURL('image/png')
            const link = document.createElement('a')
            link.href = pngUrl
            link.download = `${props.title || 'mindmap'}.png`
            document.body.appendChild(link)
            link.click()
            document.body.removeChild(link)
            message.success('图片已开始下载')
        } catch (e) {
            console.error('Export failed:', e)
            message.error('由于瀏覽器安全限制，無法導出圖片。請嘗試下載 SVG。')
        }
    }
    
    img.onerror = () => {
        message.error('生成图片失败')
    }

    img.src = url
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
