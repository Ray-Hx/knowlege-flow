<script setup lang="ts">
import { onMounted, ref, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as d3 from 'd3'
import { MOCK_GRAPH_DATA } from '@/api/mockData'
import { graphApi } from '@/api/graph'
import { aiApi } from '@/api/ai'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { NButton, NSpin, NIcon, NTag, NCollapseTransition, useMessage } from 'naive-ui'
import { RefreshOutline, ExpandOutline, SparklesOutline, CloseOutline, ArrowForward, ArrowBack } from '@vicons/ionicons5'

import { useUserStore } from '@/store/user'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
import { nextTick } from 'vue'

const containerRef = ref<HTMLDivElement | null>(null)
const loading = ref(true)
const selectedNode = ref<any>(null)
const inspectorOpen = ref(false) // 只有选中节点时才打开
const inspectorPos = ref({ x: 0, y: 20, width: 320, height: 500 }) // x <= 0 使用 right:20px 定位
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const previewRef = ref<HTMLDivElement | null>(null)

// AI Analysis State
const analyzing = ref(false)
const analysisResult = ref('')
const analysisTime = ref('')
const showIsolatedNodes = ref(false) // 默认隐藏孤立节点

// Type Styling
const TYPE_COLORS: Record<string, string> = {
    'Note': '#10B981',    // Emerald 500
    'Concept': '#8B5CF6', // Violet 500
    'Tag': '#F59E0B',     // Amber 500
    'Source': '#3B82F6',  // Blue 500
    'default': '#6B7280'  // Gray 500
}

const TYPE_GRADIENTS: Record<string, [string, string]> = {
    'Note': ['#34D399', '#059669'],    // Emerald 400 -> 600
    'Concept': ['#A78BFA', '#7C3AED'], // Violet 400 -> 600
    'Tag': ['#FBBF24', '#D97706'],     // Amber 400 -> 600
    'Source': ['#60A5FA', '#2563EB'],  // Blue 400 -> 600
    'default': ['#9CA3AF', '#4B5563']  // Gray 400 -> 600
}

const LINK_COLORS: Record<string, string> = {
    'LINKS_TO': '#6366F1', // Indigo 500 - 更明显
    'RELATED': '#10B981',  // Emerald 500 - AI关联用绿色
    'TAGGED': '#F59E0B',   // Amber 500
    'CITES': '#EC4899',    // Pink 400
    'default': '#6B7280'   // Gray 500
}

// D3 variables
let simulation: d3.Simulation<d3.SimulationNodeDatum, undefined> | null = null
let svg: d3.Selection<SVGSVGElement, unknown, null, undefined> | null = null
let resizeObserver: ResizeObserver | null = null
let g: d3.Selection<SVGGElement, unknown, null, undefined> | null = null

// AI Action State
const aiAnalyzing = ref(false)
const analysisProgress = ref({ current: 0, total: 0, message: '' })

// Data - will be populated in onMounted
const nodes = ref<any[]>([])
const links = ref<any[]>([])


// Computed relations for the selected node
const relatedNodes = computed(() => {
    if (!selectedNode.value) return { upstream: [], downstream: [] }
    
    // Find upstream (nodes that point TO selected)
    const upstream = links.value.filter((l: any) => l.target.id === selectedNode.value.id || l.target === selectedNode.value)
        .map((l: any) => l.source.id && typeof l.source !== 'object' ? nodes.value.find((n:any)=>n.id===l.source) : l.source)

    // Find downstream (nodes that selected points TO)
    const downstream = links.value.filter((l: any) => l.source.id === selectedNode.value.id || l.source === selectedNode.value)
        .map((l: any) => l.target.id && typeof l.target !== 'object' ? nodes.value.find((n:any)=>n.id===l.target) : l.target)

    return { upstream, downstream }
})

// 过滤孤立节点的计算属性
const filteredNodes = computed(() => {
    if (showIsolatedNodes.value) {
        return nodes.value
    }
    // 收集所有有连接的节点ID
    const connectedIds = new Set<number | string>()
    links.value.forEach((l: any) => {
        const sourceId = typeof l.source === 'object' ? l.source.id : l.source
        const targetId = typeof l.target === 'object' ? l.target.id : l.target
        connectedIds.add(sourceId)
        connectedIds.add(targetId)
    })
    return nodes.value.filter((n: any) => connectedIds.has(n.id))
})

// 孤立节点数量统计
const isolatedCount = computed(() => {
    return nodes.value.length - filteredNodes.value.length
})

// 切换显示孤立节点时重新渲染图谱
watch(showIsolatedNodes, () => {
    initGraph()
})

watch(selectedNode, (newVal) => {
    if (newVal) {
        inspectorOpen.value = true
        highlightNode(newVal.id)
        analysisResult.value = '' // Reset analysis
    } else {
        inspectorOpen.value = false
        resetHighlight()
        analysisResult.value = ''
    }
})

function initGraph() {
  if (!containerRef.value) return
  
  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight
  
  if (width === 0 || height === 0) return 

  // Clear previous
  d3.select(containerRef.value).selectAll('*').remove()

  // Create SVG
  svg = d3.select(containerRef.value)
    .append('svg')
    .attr('width', width)
    .attr('height', height)
    .attr('viewBox', [0, 0, width, height])
    .attr('style', 'max-width: 100%; height: auto; display: block;')
    .on('click', () => {
        selectedNode.value = null
    })

  // Define Gradients and Filters
  const defs = svg.append('defs')
  
  // Drop Shadow Filter
  const filter = defs.append('filter')
    .attr('id', 'drop-shadow')
    .attr('height', '130%')
  filter.append('feGaussianBlur')
    .attr('in', 'SourceAlpha')
    .attr('stdDeviation', 3)
    .attr('result', 'blur')
  filter.append('feOffset')
    .attr('in', 'blur')
    .attr('dx', 2)
    .attr('dy', 2)
    .attr('result', 'offsetBlur')
  filter.append('feFlood')
    .attr('flood-color', '#000')
    .attr('flood-opacity', 0.2)
    .attr('result', 'offsetColor')
  filter.append('feComposite')
    .attr('in', 'offsetColor')
    .attr('in2', 'offsetBlur')
    .attr('operator', 'in')
    .attr('result', 'offsetBlur')
  
  const feMerge = filter.append('feMerge')
  feMerge.append('feMergeNode').attr('in', 'offsetBlur')
  feMerge.append('feMergeNode').attr('in', 'SourceGraphic')

  // Create Gradients for each type
  Object.keys(TYPE_GRADIENTS).forEach(type => {
      const gradient = defs.append('linearGradient')
        .attr('id', `grad-${type}`)
        .attr('x1', '0%')
        .attr('y1', '0%')
        .attr('x2', '100%')
        .attr('y2', '100%')
      
      gradient.append('stop')
        .attr('offset', '0%')
        .attr('stop-color', TYPE_GRADIENTS[type][0])
      
      gradient.append('stop')
        .attr('offset', '100%')
        .attr('stop-color', TYPE_GRADIENTS[type][1])
  })

  // Arrow markers for directed links
  const markerTypes = ['LINKS_TO', 'RELATED', 'default']
  markerTypes.forEach(type => {
    defs.append('marker')
      .attr('id', `arrow-${type}`)
      .attr('viewBox', '0 -5 10 10')
      .attr('refX', 25) // 箭头距离节点的距离
      .attr('refY', 0)
      .attr('markerWidth', 6)
      .attr('markerHeight', 6)
      .attr('orient', 'auto')
      .append('path')
      .attr('fill', LINK_COLORS[type] || LINK_COLORS['default'])
      .attr('d', 'M0,-5L10,0L0,5')
  })

  // Add group for zoom
  g = svg.append('g')

  // Zoom behavior
  const zoom = d3.zoom<SVGSVGElement, unknown>()
    .scaleExtent([0.1, 4])
    .on('zoom', (event) => {
      g?.attr('transform', event.transform)
    })
  
  svg.call(zoom)
  svg.on('dblclick.zoom', null)

  // Simulation setup - 使用过滤后的节点
  simulation = d3.forceSimulation(filteredNodes.value)
    .force('link', d3.forceLink(links.value).id((d: any) => d.id).distance(120))
    .force('charge', d3.forceManyBody().strength(-400))
    .force('center', d3.forceCenter(width / 2, height / 2))
    .force('collide', d3.forceCollide().radius(40).iterations(2))

  // Render Links
  const link = g.append('g')
    .attr('class', 'links')
    .selectAll('line')
    .data(links.value)
    .join('line')
    .attr('stroke', (d: any) => LINK_COLORS[d.type] || LINK_COLORS['default']) 
    .attr('stroke-opacity', 0.8)
    .attr('stroke-width', 2.5)
    .attr('marker-end', (d: any) => `url(#arrow-${d.type || 'default'})`)

  // Render Link Labels
  const linkLabels = g.append('g')
    .attr('class', 'link-labels')
    .selectAll('text')
    .data(links.value)
    .join('text')
    .text((d: any) => d.label || (d.type === 'RELATED' ? 'AI关联' : d.type || ''))
    .attr('text-anchor', 'middle')
    .attr('dominant-baseline', 'middle')
    .attr('fill', (d: any) => LINK_COLORS[d.type] || '#6B7280')
    .attr('font-size', '11px')
    .attr('font-weight', '600')
    .style('pointer-events', 'none')
    // Add white background to text for readability using paint-order/stroke
    .style('paint-order', 'stroke')
    .style('stroke', 'white')
    .style('stroke-width', '3px')
    .style('stroke-linecap', 'butt')
    .style('stroke-linejoin', 'miter')

  // Render Nodes Group - 使用过滤后的节点
  const nodeGroup = g.append('g')
    .attr('class', 'nodes')
    .selectAll('g')
    .data(filteredNodes.value)
    .join('g')
    .attr('cursor', 'pointer')
    .call(d3.drag<SVGGElement, any>()
        .on('start', dragstarted)
        .on('drag', dragged)
        .on('end', dragended) as any
    )
    .on('click', (event, d) => {
        event.stopPropagation()
        selectedNode.value = d
    })

  // Node Halo (Selection indicator) - initially invisible
  nodeGroup.append('circle')
    .attr('class', 'halo')
    .attr('r', (d: any) => (d.size ? d.size / 2 : 20) + 8)
    .attr('fill', 'none')
    .attr('stroke', (d: any) => d.style?.fill || '#666')
    .attr('stroke-width', 0)
    .attr('opacity', 0.3)

  // Node Main Circle
  nodeGroup.append('circle')
    .attr('class', 'main-circle')
    .attr('r', (d: any) => d.size ? d.size / 2 : 20)
    .attr('fill', (d: any) => `url(#grad-${TYPE_GRADIENTS[d.type] ? d.type : 'default'})`)
    .attr('stroke', '#fff')
    .attr('stroke-width', 2)
    .style('filter', 'url(#drop-shadow)')


  // Inner Dot (optional, based on design)
  nodeGroup.append('circle')
    .attr('r', 4)
    .attr('fill', (d: any) => d.style?.fill || '#666')

  // Node Label (Below node)
  nodeGroup.append('text')
    .text((d: any) => d.label)
    .attr('dy', (d: any) => (d.size ? d.size / 2 : 20) + 16)
    .attr('text-anchor', 'middle')
    .attr('fill', '#374151') // gray-700
    .attr('font-size', '12px')
    .attr('font-weight', '600')
    .style('pointer-events', 'none')
    .style('text-shadow', '0 2px 4px rgba(255,255,255,0.8)')
    
  // Simulation tick
  simulation.on('tick', () => {
    link
      .attr('x1', (d: any) => d.source.x)
      .attr('y1', (d: any) => d.source.y)
      .attr('x2', (d: any) => d.target.x)
      .attr('y2', (d: any) => d.target.y)

    linkLabels
      .attr('x', (d: any) => (d.source.x + d.target.x) / 2)
      .attr('y', (d: any) => (d.source.y + d.target.y) / 2)

    nodeGroup
      .attr('transform', (d: any) => `translate(${d.x},${d.y})`)
  })
}

// Visual Effects
function highlightNode(nodeId: string) {
    if (!g) return
    
    // Dim all slightly less aggressive (user feedback: "lines disappear")
    g.selectAll('g.nodes g').style('opacity', 0.3)
    g.selectAll('g.links line').style('opacity', 0.3)
    g.selectAll('g.link-labels text').style('opacity', 0.1) // Hide labels of unrelated
    
    // Highlight selected and neighbors
    
    // Show nodes
    g.selectAll('g.nodes g')
     .filter((d: any) => {
         const isConnected = links.value.some((l:any) => 
            (l.source.id === nodeId && l.target.id === d.id) || 
            (l.target.id === nodeId && l.source.id === d.id)
         )
         return d.id === nodeId || isConnected
     })
     .style('opacity', 1)
     
    // Show links
    g.selectAll('g.links line')
     .filter((d: any) => (d.source.id === nodeId || d.target.id === nodeId))
     .style('opacity', 1)
     .attr('stroke', '#60A5FA') // Highlight color
     
    // Show labels for connected links
    g.selectAll('g.link-labels text')
     .filter((d: any) => (d.source.id === nodeId || d.target.id === nodeId))
     .style('opacity', 1)
     
     // Halo effect for selected
     g.selectAll('circle.halo')
      .attr('stroke-width', (d: any) => d.id === nodeId ? 6 : 0)
}

function resetHighlight() {
     if (!g) return
     g.selectAll('g.nodes g').style('opacity', 1)
     g.selectAll('g.links line').style('opacity', 1).attr('stroke', '#E5E7EB')
     g.selectAll('g.link-labels text').style('opacity', 1)
     g.selectAll('circle.halo').attr('stroke-width', 0)
}

// Drag functions
function dragstarted(event: any, d: any) {
  if (!event.active) simulation?.alphaTarget(0.3).restart()
  d.fx = d.x
  d.fy = d.y
}

function dragged(event: any, d: any) {
  d.fx = event.x
  d.fy = event.y
}

function dragended(event: any, d: any) {
  if (!event.active) simulation?.alphaTarget(0)
  d.fx = null
  d.fy = null
}

function navigateToNote(id: string | number) {
    if (!id) return
    router.push(`/notes/${id}`)
}

onMounted(async () => {
  loading.value = true
  // Try to load data from API first, fall back to mock data
  try {
    const data = await graphApi.getGraph()
    console.log('Graph API response:', data)
    
    // 后端返回的是 List<NoteNode>，每个节点的 links 属性包含关联的节点
    // 需要转换为前端需要的 { nodes, links } 扁平格式
    if (Array.isArray(data) && data.length > 0) {
      // 转换节点
      const nodeMap = new Map()
      const processedNodes: any[] = []
      const processedLinks: any[] = []
      
      // 第一遍：收集所有节点
      data.forEach((node: any) => {
        if (!nodeMap.has(node.id)) {
          processedNodes.push({
            id: node.id,
            name: node.title || '无标题',
            label: node.title || '无标题',
            type: 'Note'
          })
          nodeMap.set(node.id, true)
        }
        
        // 处理嵌套的 links（关联节点）
        if (node.links && Array.isArray(node.links)) {
          node.links.forEach((linkedNode: any) => {
            // 添加关联节点到节点列表
            if (!nodeMap.has(linkedNode.id)) {
              processedNodes.push({
                id: linkedNode.id,
                name: linkedNode.title || '无标题',
                label: linkedNode.title || '无标题',
                type: 'Note'
              })
              nodeMap.set(linkedNode.id, true)
            }
            // 添加边
            processedLinks.push({
              source: node.id,
              target: linkedNode.id,
              type: 'LINKS_TO'
            })
          })
        }
        
        // 处理嵌套的 related（AI 关联）
        if (node.related && Array.isArray(node.related)) {
          node.related.forEach((relation: any) => {
            // 新格式: {target: {id, title}, label} 或者直接 {id, title}
            const linkedNode = relation.target || relation
            const label = relation.label || ''
            
            // 添加关联节点到节点列表
            if (linkedNode && linkedNode.id && !nodeMap.has(linkedNode.id)) {
              processedNodes.push({
                id: linkedNode.id,
                name: linkedNode.title || '无标题',
                label: linkedNode.title || '无标题',
                type: 'Note',
                group: 2
              })
              nodeMap.set(linkedNode.id, true)
            }
            // 添加边（带标签）
            if (linkedNode && linkedNode.id) {
              processedLinks.push({
                source: node.id,
                target: linkedNode.id,
                type: 'RELATED',
                label: label
              })
            }
          })
        }
      })
      
      nodes.value = processedNodes
      links.value = processedLinks
      console.log('Processed graph data:', { nodes: processedNodes.length, links: processedLinks.length })
    } else if (data && (data.nodes || data.links)) {
      // 如果后端返回的已经是 { nodes, links } 格式
      nodes.value = data.nodes || []
      links.value = data.links || []
      console.log('Graph loaded from API (flat format):', data)
    } else {
      throw new Error('No graph data from API')
    }
  } catch (error) {
    console.warn('Backend unavailable or error, using mock data:', error)
    if (MOCK_GRAPH_DATA) {
        nodes.value = JSON.parse(JSON.stringify(MOCK_GRAPH_DATA.nodes))
        links.value = JSON.parse(JSON.stringify(MOCK_GRAPH_DATA.edges || []))
    }
  } finally {
      loading.value = false
  }
  
  // Initialize graph after data is loaded
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      initGraph()
    })
    resizeObserver.observe(containerRef.value)
    
    // Initial init
    initGraph()
  }
})

onUnmounted(() => {
  simulation?.stop()
  resizeObserver?.disconnect()
  // Clean up D3 listeners if any specific global ones
})

function refreshLayout() {
    simulation?.alpha(1).restart()
}

function fitView() {
    if (!svg || !g) return
    
    // 省略 D3 zoom 逻辑复用... 实际上我们使用 d3 zoom transform
    // 为了简单，这里还是用 D3 的 transition
    const bounds = (g.node() as SVGGElement).getBBox()
    const parent = (svg.node() as SVGSVGElement).parentElement
    if (!parent) return
    
    const width = parent.clientWidth
    const height = parent.clientHeight
    const fullWidth = width
    const fullHeight = height
    
    const midX = bounds.x + bounds.width / 2
    const midY = bounds.y + bounds.height / 2
    
    if (bounds.width === 0 || bounds.height === 0) return // Empty
    
    const scale = 0.8 / Math.max(bounds.width / width, bounds.height / height)
    const translate = [width / 2 - scale * midX, height / 2 - scale * midY]

    svg.transition().duration(750).call(
        // @ts-ignore
        d3.zoom().transform, // 这种方式可能需要绑定 zoom event
        d3.zoomIdentity.translate(translate[0], translate[1]).scale(scale)
    )
    
    // 重新 restart simulation 也可以辅助布局
    simulation?.alpha(0.3).restart()
}

// AI 自动分析关联 (SSE Stream)
const analyzeRelations = async () => {
    aiAnalyzing.value = true
    analysisProgress.value = { current: 0, total: 100, message: '准备开始分析...' } // default total 100, will update
    
    try {
        // 使用 fetch 实现 SSE 读取，因为原生 EventSource 不支持 Auth Header
        const response = await fetch('/api/graph/analyze/stream', {
            headers: {
                'Authorization': `Bearer ${userStore.token}`
            }
        })
        
        if (!response.ok) throw new Error(response.statusText)
        if (!response.body) throw new Error('No response body')
        
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        
        while (true) {
            const { done, value } = await reader.read()
            if (done) break
            
            buffer += decoder.decode(value, { stream: true })
            const blocks = buffer.split('\n\n')
            buffer = blocks.pop() || '' 
            
            for (const block of blocks) {
                const lines = block.split('\n')
                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        try {
                            const jsonStr = line.slice(5).trim()
                            const data = JSON.parse(jsonStr)
                            
                            if (data.type === 'progress') {
                                analysisProgress.value = data
                            } else if (data.type === 'complete') {
                                message.success(`分析完成，发现 ${data.count} 个新关联！`)
                                
                                // 主动退出循环，确保 finally 执行
                                // 但先重新加载数据
                                const graphData = await graphApi.getGraph()
                                if (Array.isArray(graphData)) {
                                     const nodeMap = new Map()
                                     const processedNodes: any[] = []
                                     const processedLinks: any[] = []
                                     graphData.forEach((node: any) => {
                                        if (!nodeMap.has(node.id)) {
                                          processedNodes.push({
                                            id: node.id,
                                            name: node.title || '无标题',
                                            label: node.title || '无标题',
                                            type: 'Note'
                                          })
                                          nodeMap.set(node.id, true)
                                        }
                                        if (node.links && Array.isArray(node.links)) {
                                          node.links.forEach((linkedNode: any) => {
                                            if (!nodeMap.has(linkedNode.id)) {
                                              processedNodes.push({
                                                id: linkedNode.id,
                                                name: linkedNode.title || '无标题',
                                                label: linkedNode.title || '无标题',
                                                type: 'Note'
                                              })
                                              nodeMap.set(linkedNode.id, true)
                                            }
                                            processedLinks.push({
                                              source: node.id,
                                              target: linkedNode.id,
                                              type: 'LINKS_TO'
                                            })
                                          })
                                        }
                                        // 处理嵌套的 related（AI 关联）
                                        if (node.related && Array.isArray(node.related)) {
                                            node.related.forEach((relation: any) => {
                                                // 新格式: {target: {id, title}, label}
                                                const linkedNode = relation.target || relation
                                                const label = relation.label || ''
                                                
                                                if (linkedNode && linkedNode.id && !nodeMap.has(linkedNode.id)) {
                                                    processedNodes.push({
                                                        id: linkedNode.id,
                                                        name: linkedNode.title || '无标题',
                                                        label: linkedNode.title || '无标题',
                                                        type: 'Note'
                                                    })
                                                    nodeMap.set(linkedNode.id, true)
                                                }
                                                if (linkedNode && linkedNode.id) {
                                                    processedLinks.push({
                                                        source: node.id,
                                                        target: linkedNode.id,
                                                        type: 'RELATED',
                                                        label: label
                                                    })
                                                }
                                            })
                                        }
                                     })
                                     nodes.value = processedNodes
                                     links.value = processedLinks
                                     initGraph()
                                }
                                
                                // 收到完成信号后，不再等待流结束，直接退出
                                return 
                            }
                        } catch (e) {
                            console.warn('SSE parse error:', e)
                        }
                    }
                }
            }
        }
    } catch (e) {
        message.error('AI 分析失败，请稍后重试')
        console.error(e)
    } finally {
        aiAnalyzing.value = false
    }
}

// Draggable Inspector Logic
function startDrag(e: MouseEvent) {
    // Only drag from header
    const target = e.target as HTMLElement
    if (!target.closest('.inspector-header')) return

    isDragging.value = true
    const el = document.getElementById('inspector-panel')
    if (el) {
        const rect = el.getBoundingClientRect()
        // Calculate offset from the top-left corner of the element
        dragOffset.value = {
            x: e.clientX - rect.left,
            y: e.clientY - rect.top
        }
    }
    
    window.addEventListener('mousemove', onDrag)
    window.addEventListener('mouseup', stopDrag)
    e.preventDefault() // Prevent text selection
}

function onDrag(e: MouseEvent) {
    if (!isDragging.value || !containerRef.value) return
    
    // Calculate new position relative to the container
    const containerRect = containerRef.value.getBoundingClientRect()
    
    // Constrain to container bounds (optional, but good UX)
    let newX = e.clientX - containerRect.left - dragOffset.value.x
    let newY = e.clientY - containerRect.top - dragOffset.value.y
    
    // Simple boundary check (prevent moving fully out of screen)
    // newX = Math.max(0, Math.min(newX, containerRect.width - 320))
    // newY = Math.max(0, Math.min(newY, containerRect.height - 50))

    inspectorPos.value = {
        x: newX,
        y: newY,
        width: inspectorPos.value.width,
        height: inspectorPos.value.height
    }
}

function stopDrag() {
    isDragging.value = false
    window.removeEventListener('mousemove', onDrag)
    window.removeEventListener('mouseup', stopDrag)
}

// Resizing Logic
const isResizing = ref(false)

function startResize(e: MouseEvent) {
    isResizing.value = true
    window.addEventListener('mousemove', onResize)
    window.addEventListener('mouseup', stopResize)
    e.preventDefault()
    e.stopPropagation()
}

function onResize(e: MouseEvent) {
    if (!isResizing.value) return
    
    const el = document.getElementById('inspector-panel')
    if (!el) return
    const rect = el.getBoundingClientRect()
    
    // Calculate new width/height
    // We assume resizing from bottom-right handle
    const newWidth = e.clientX - rect.left
    const newHeight = e.clientY - rect.top
    
    // Apply limits
    inspectorPos.value.width = Math.max(300, newWidth)
    inspectorPos.value.height = Math.max(400, newHeight)
}

function stopResize() {
    isResizing.value = false
    window.removeEventListener('mousemove', onResize)
    window.removeEventListener('mouseup', stopResize)
}

// ...

// ...

async function handleAnalyze() {
    if (!selectedNode.value) return
    
    analyzing.value = true
    analysisResult.value = '' // Clear previous result
    analysisTime.value = new Date().toISOString() // Set current time
    
    try {
        // Use summary if available, otherwise label
        const content = selectedNode.value.summary 
            ? `${selectedNode.value.label}: ${selectedNode.value.summary}` 
            : `知识点: ${selectedNode.value.label}`
            
        // Map node type to backend target type
        const targetType = selectedNode.value.type ? selectedNode.value.type.toUpperCase() : 'CONCEPT'
        const targetId = selectedNode.value.id
        
        // Streaming request
        const response = await fetch('/api/ai/analyze/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${userStore.token}`
            },
            body: JSON.stringify({
                content,
                targetType,
                targetId
            })
        })

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        if (!response.body) {
            throw new Error('Response body is null')
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        
        while (true) {
            const { done, value } = await reader.read()
            if (done) break
            
            const chunk = decoder.decode(value, { stream: true })
            const lines = chunk.split('\n\n')
            
            for (const line of lines) {
                if (line.startsWith('data:')) {
                    const data = line.slice(5) // Remove 'data:' prefix
                    // Note: backend sends raw tokens/text directly in data
                    // It doesn't seem to wrap it in JSON based on my AiServiceImpl
                    // let's append it directly.
                    // Actually, SseEmitter sends "data:token\n\n". 
                    // So data is the token string.
                    analysisResult.value += data
                    
                    // Render markdown incrementally
                    // Throttle optimization could be added here if needed
                    if (previewRef.value) {
                         Vditor.preview(previewRef.value, analysisResult.value, {
                            mode: document.documentElement.classList.contains('dark') ? 'dark' : 'light',
                            theme: {
                                current: document.documentElement.classList.contains('dark') ? 'dark' : 'light',
                            },
                            hljs: {
                                style: 'dracula'
                            }
                        })
                    }
                }
            }
        }

    } catch (error) {
        console.error(error)
        message.error('生成解析失败，请稍后重试')
    } finally {
        analyzing.value = false
    }
}
</script>

<template>
  <div class="h-[calc(100vh-140px)] relative bg-gray-50 dark:bg-black rounded-xl overflow-hidden border border-gray-100 dark:border-gray-800">
    
    <!-- Graph Canvas (Full Size) -->
    <div class="absolute inset-0 z-0">
        <div ref="containerRef" class="w-full h-full bg-white dark:bg-gray-900 cursor-move"></div>
        <n-spin v-if="loading" class="absolute inset-0 flex items-center justify-center bg-white/50 z-20" />
    </div>

    <!-- Toolbar (Floating Top-Left) -->
    <div class="absolute top-4 left-4 z-10 flex gap-2 items-center">
       <n-button circle size="small" secondary class="bg-white/80 dark:bg-gray-800/80 backdrop-blur shadow-sm" @click="refreshLayout">
         <template #icon><n-icon :component="RefreshOutline" /></template>
       </n-button>
       <n-button circle size="small" secondary class="bg-white/80 dark:bg-gray-800/80 backdrop-blur shadow-sm" @click="fitView">
         <template #icon><n-icon :component="ExpandOutline" /></template>
       </n-button>
       <div class="w-px h-6 bg-gray-300 dark:bg-gray-700 mx-1"></div>
       <n-button size="small" type="primary" class="bg-gradient-to-r from-blue-500 to-indigo-600 border-none shadow-md" :loading="aiAnalyzing" @click="analyzeRelations">
         <template #icon><n-icon :component="SparklesOutline" /></template>
         {{ aiAnalyzing ? '分析中...' : 'AI 关联分析' }}
       </n-button>
       <div class="w-px h-6 bg-gray-300 dark:bg-gray-700 mx-1"></div>
       <!-- 孤立节点开关 -->
       <div class="flex items-center gap-2 px-3 py-1.5 bg-white/80 dark:bg-gray-800/80 backdrop-blur rounded-lg shadow-sm text-xs">
         <n-switch v-model:value="showIsolatedNodes" size="small" />
         <span class="text-gray-600 dark:text-gray-300 whitespace-nowrap">
           显示孤立节点 <span v-if="isolatedCount > 0" class="text-gray-400">({{ isolatedCount }})</span>
         </span>
       </div>
    </div>

    <!-- AI Progress Bar -->
    <n-collapse-transition :show="aiAnalyzing">
        <div class="absolute top-16 left-4 z-10 w-64 bg-white/90 dark:bg-gray-800/90 backdrop-blur rounded-lg shadow-lg p-3 border border-gray-100 dark:border-gray-700">
            <div class="flex justify-between text-xs mb-1 text-gray-500">
                <span>{{ analysisProgress.message }}</span>
                <span>{{ analysisProgress.current }}/{{ analysisProgress.total }}</span>
            </div>
            <n-progress 
                type="line" 
                :percentage="analysisProgress.total > 0 ? (analysisProgress.current / analysisProgress.total) * 100 : 0" 
                :show-indicator="false"
                status="process"
                processing
                :height="4"
            />
        </div>
    </n-collapse-transition>
    
    <!-- Inspector Panel (Floating Inspector) -->
    <n-collapse-transition :show="inspectorOpen" :duration="0.3">
        <div 
            id="inspector-panel"
            class="absolute z-20 w-80 h-[500px] rounded-2xl border border-gray-200/50 dark:border-gray-700/50 bg-white/80 dark:bg-gray-900/80 backdrop-blur-xl shadow-2xl flex flex-col overflow-hidden transition-shadow duration-300"
            :class="{ 'ring-2 ring-blue-500/50': isDragging }"
            :style="{ 
                left: inspectorPos.x > 0 ? inspectorPos.x + 'px' : undefined, 
                top: inspectorPos.y > 0 ? inspectorPos.y + 'px' : undefined,
                right: inspectorPos.x <= 0 ? '20px' : undefined,
                width: inspectorPos.width + 'px',
                height: inspectorPos.height + 'px'
            }"
        >
             <!-- Draggable Header -->
             <div class="inspector-header h-12 flex items-center justify-between px-4 border-b border-gray-100/50 dark:border-gray-800/50 cursor-move bg-gray-50/50 dark:bg-white/5 select-none"
                  @mousedown="startDrag">
                 <div class="flex items-center gap-2">
                     <span class="w-2 h-2 rounded-full" :style="{ backgroundColor: TYPE_COLORS[selectedNode?.type] || TYPE_COLORS['default'] }"></span>
                     <span class="font-bold text-sm text-gray-700 dark:text-gray-200">属性面板</span>
                 </div>
                 <n-button text size="tiny" @click="selectedNode = null" class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200">
                     <template #icon><n-icon :component="CloseOutline" /></template>
                 </n-button>
             </div>

             <div class="p-6 flex-1 overflow-y-auto custom-scrollbar">
                 <div class="flex justify-between items-start mb-2">
                     <n-tag :bordered="false" size="small" class="rounded-md font-bold px-2 text-white" 
                            :style="{ backgroundColor: TYPE_COLORS[selectedNode?.type] || TYPE_COLORS['default'] }">
                        {{ selectedNode?.type || 'Node' }}
                     </n-tag>
                 </div>
                 
                 <h2 class="text-2xl font-bold mb-6 text-gray-800 dark:text-gray-100 leading-tight">
                     {{ selectedNode?.label }}
                 </h2>
                 
                 <!-- AI Analysis Placeholder -->
                 <div class="mb-8 p-4 bg-gradient-to-br from-purple-50/50 to-indigo-50/50 dark:from-purple-900/20 dark:to-indigo-900/20 rounded-xl border border-purple-100/50 dark:border-purple-800/30">
                     <div class="flex items-center gap-2 mb-2 text-purple-600 dark:text-purple-400 font-bold text-sm">
                         <n-icon :component="SparklesOutline" />
                         AI 知识增强
                         <span v-if="analysisTime" class="ml-auto text-xs text-gray-400 font-normal">
                            {{ new Date(analysisTime).toLocaleString() }}
                         </span>
                     </div>
                     
                     <div v-show="analysisResult" ref="previewRef" class="markdown-preview text-xs text-gray-600 dark:text-gray-300 mb-3 leading-relaxed max-h-80 overflow-y-auto custom-scrollbar p-2 bg-white/50 dark:bg-black/20 rounded">
                     </div>
                     <div v-if="!analysisResult">
                        <p class="text-xs text-gray-500 dark:text-gray-400 mb-3 leading-relaxed">
                             点击下方按钮，请求 AI 分析该知识点在当前项目中的应用场景。
                        </p>
                         <n-button 
                            block 
                            secondary 
                            type="primary" 
                            size="small" 
                            class="bg-white/50 dark:bg-white/5 hover:bg-white dark:hover:bg-white/10 border-none shadow-sm"
                            :loading="analyzing"
                            @click="handleAnalyze"
                        >
                             <template #icon><n-icon :component="SparklesOutline" /></template>
                             {{ analyzing ? '生成中...' : '生成深度解析' }}
                         </n-button>
                     </div>
                 </div>
                 
                 <!-- Relations -->
                 <div class="space-y-6">
                     <div v-if="relatedNodes.upstream.length > 0">
                         <h4 class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-1">
                             <n-icon :component="ArrowBack" /> 上游 ({{ relatedNodes.upstream.length }})
                         </h4>
                         <div class="flex flex-col gap-2">
                             <div 
                                v-for="node in relatedNodes.upstream" 
                                :key="node.id" 
                                class="p-3 bg-gray-50/50 dark:bg-gray-800/50 rounded-lg hover:bg-blue-50 dark:hover:bg-blue-900/20 cursor-pointer transition-colors border border-transparent hover:border-blue-100 dark:hover:border-blue-800 group"
                                @click="selectedNode = node"
                             >
                                 <div class="text-sm font-medium group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">{{ node.label }}</div>
                                 <div class="text-xs text-gray-400">{{ node.type }}</div>
                             </div>
                         </div>
                     </div>
                     
                     <div v-if="relatedNodes.downstream.length > 0">
                         <h4 class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-1">
                             <n-icon :component="ArrowForward" /> 下游 ({{ relatedNodes.downstream.length }})
                         </h4>
                         <div class="flex flex-col gap-2">
                             <div 
                                v-for="node in relatedNodes.downstream" 
                                :key="node.id" 
                                class="p-3 bg-gray-50/50 dark:bg-gray-800/50 rounded-lg hover:bg-blue-50 dark:hover:bg-blue-900/20 cursor-pointer transition-colors border border-transparent hover:border-blue-100 dark:hover:border-blue-800 group"
                                @click="selectedNode = node"
                             >
                                 <div class="text-sm font-medium group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">{{ node.label }}</div>
                                 <div class="text-xs text-gray-400">{{ node.type }}</div>
                             </div>
                         </div>
                     </div>
                 </div>
             </div>
             
             <!-- Footer Action -->
             <div class="p-4 border-t border-gray-100 dark:border-gray-800 bg-white/50 dark:bg-gray-900/50 backdrop-blur relative z-30">
                 <n-button type="primary" block size="large" class="shadow-lg shadow-blue-500/20" @click="navigateToNote(selectedNode?.id)">
                     打开详细笔记
                 </n-button>
             </div>
             <!-- Resize Handle -->
             <div 
                class="absolute bottom-0 right-0 w-5 h-5 cursor-se-resize flex items-center justify-center text-gray-400 hover:text-blue-500 transition-colors z-50 select-none opacity-0 group-hover:opacity-100 hover:opacity-100"
                @mousedown.stop="startResize"
             >
                 <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" class="transform rotate-90"><path d="M22 22H20V20H22V22ZM22 18H20V16H22V18ZM18 22H16V20H18V22ZM18 18H16V16H18V18ZM14 22H12V20H14V22ZM22 14H20V12H22V14Z"/></svg>
             </div>
        </div>
    </n-collapse-transition>
  </div>
</template>

<style scoped>
/* Smooth transitions */
.nodes g, .links line, .link-labels text {
    transition: opacity 0.3s ease;
}
.halo {
    transition: stroke-width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(156, 163, 175, 0.3);
  border-radius: 20px;
}
</style>
