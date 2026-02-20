<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as d3 from 'd3'
import { useRouter } from 'vue-router'

const props = defineProps({
  data: {
    type: Object as () => { nodes: any[], links: any[] },
    required: true
  },
  height: {
    type: Number,
    default: 600
  },
  width: {
    type: Number,
    default: 800
  }
})

const router = useRouter()
const svgRef = ref<SVGSVGElement | null>(null)
let simulation: d3.Simulation<d3.SimulationNodeDatum, undefined> | null = null

const initGraph = () => {
  if (!svgRef.value || !props.data.nodes.length) return

  const width = props.width
  const height = props.height

  // 清除旧内容
  const svg = d3.select(svgRef.value)
  svg.selectAll('*').remove()

  // 缩放容器
  const g = svg.append('g')
  
  svg.call(d3.zoom<SVGSVGElement, unknown>()
    .scaleExtent([0.1, 8])
    .on('zoom', (event) => {
      g.attr('transform', event.transform)
    }))

  // 力导向模拟
  simulation = d3.forceSimulation(props.data.nodes as d3.SimulationNodeDatum[])
    .force('link', d3.forceLink(props.data.links).id((d: any) => d.id).distance(100))
    .force('charge', d3.forceManyBody().strength(-300))
    .force('center', d3.forceCenter(width / 2, height / 2))
    .force('collide', d3.forceCollide().radius(30))

  // 绘制连线
  const link = g.append('g')
    .attr('stroke', '#999')
    .attr('stroke-opacity', 0.6)
    .selectAll('line')
    .data(props.data.links)
    .join('line')
    .attr('stroke-width', 1.5)

  // 绘制节点
  const node = g.append('g')
    .attr('stroke', '#fff')
    .attr('stroke-width', 1.5)
    .selectAll('circle')
    .data(props.data.nodes)
    .join('circle')
    .attr('r', (d: any) => d.type === 'note' ? 10 : 6)
    .attr('fill', (d: any) => d.type === 'note' ? '#6366f1' : '#10b981')
    .attr('cursor', 'pointer')
    .call(d3.drag<SVGCircleElement, d3.SimulationNodeDatum>()
      .on('start', dragstarted)
      .on('drag', dragged)
      .on('end', dragended))
    .on('click', (event, d: any) => {
      if (d.type === 'note') {
        const id = d.id.split(':')[1]
        router.push(`/notes/${id}`)
      }
    })

  // 节点标签
  const text = g.append('g')
    .selectAll('text')
    .data(props.data.nodes)
    .join('text')
    .text((d: any) => d.label)
    .attr('font-size', '10px')  
    .attr('fill', '#333')
    .attr('dx', 12)
    .attr('dy', 4)

  node.append('title').text((d: any) => d.label)

  // 更新位置
  simulation.on('tick', () => {
    link
      .attr('x1', (d: any) => d.source.x)
      .attr('y1', (d: any) => d.source.y)
      .attr('x2', (d: any) => d.target.x)
      .attr('y2', (d: any) => d.target.y)

    node
      .attr('cx', (d: any) => d.x)
      .attr('cy', (d: any) => d.y)

    text
      .attr('x', (d: any) => d.x)
      .attr('y', (d: any) => d.y)
  })

  // 拖拽函数
  function dragstarted(event: any) {
    if (!event.active) simulation?.alphaTarget(0.3).restart()
    event.subject.fx = event.subject.x
    event.subject.fy = event.subject.y
  }

  function dragged(event: any) {
    event.subject.fx = event.x
    event.subject.fy = event.y
  }

  function dragended(event: any) {
    if (!event.active) simulation?.alphaTarget(0)
    event.subject.fx = null
    event.subject.fy = null
  }
}

watch(() => props.data, () => {
  initGraph()
}, { deep: true })

onMounted(() => {
  initGraph()
})

onUnmounted(() => {
  if (simulation) simulation.stop()
})
</script>

<template>
  <div class="force-graph-container w-full h-full overflow-hidden bg-gray-50 rounded-lg border">
    <svg ref="svgRef" :width="width" :height="height" class="block"></svg>
  </div>
</template>
