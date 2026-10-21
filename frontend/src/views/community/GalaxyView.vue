<template>
  <div class="galaxy-container relative w-full h-screen bg-black overflow-hidden">
    <!-- 顶部导航 -->
    <div class="absolute top-4 left-4 z-10 flex items-center gap-4">
      <n-button circle color="rgba(255,255,255,0.2)" text-color="#fff" @click="goBack">
        <template #icon>
          <n-icon><ArrowBack /></n-icon>
        </template>
      </n-button>
      <h1 class="text-white text-xl font-light tracking-widest flex items-center gap-2">
        <span class="text-indigo-400">✧</span> 知识星空
      </h1>
    </div>

    <!-- 右侧操作栏 -->
    <div class="absolute top-4 right-4 z-10 flex items-center gap-2 bg-black/40 backdrop-blur-md rounded-full p-1 border border-white/10">
      <n-button 
        circle 
        quaternary 
        :type="autoRotate ? 'primary' : 'default'"
        text-color="#fff" 
        @click="autoRotate = !autoRotate"
        title="自动漫游"
      >
        <template #icon>
          <n-icon><PlanetOutline /></n-icon>
        </template>
      </n-button>
      <n-button 
        circle 
        quaternary 
        text-color="#fff" 
        @click="resetCamera"
        title="复位视角"
      >
        <template #icon>
          <n-icon><ScanOutline /></n-icon>
        </template>
      </n-button>
    </div>

    <!-- 3D 渲染容器 -->
    <div ref="graphContainer" class="w-full h-full cursor-move"></div>

    <!-- 加载与空状态 -->
    <div v-if="loading" class="absolute inset-0 z-20 flex flex-col items-center justify-center bg-black/60 backdrop-blur-sm">
      <n-spin size="large" stroke="#818cf8" />
      <p class="mt-4 text-indigo-200 tracking-widest animate-pulse">正在跃迁至知识星图...</p>
    </div>

    <!-- 底部信息卡片 (Hover) -->
    <transition name="fade-slide">
      <div 
        v-if="hoverNode" 
        class="absolute bottom-8 left-1/2 transform -translate-x-1/2 z-10 min-w-[300px] max-w-[400px] 
               bg-black/70 backdrop-blur-xl border border-indigo-500/30 rounded-2xl p-4 shadow-[0_0_30px_rgba(99,102,241,0.2)]"
      >
        <div class="flex items-start justify-between">
          <h3 class="text-white font-medium text-lg leading-snug">{{ hoverNode.title }}</h3>
          <n-tag v-if="hoverNode.isPublic" size="small" type="success" :bordered="false" class="ml-2 !bg-green-500/20 !text-green-300">公开</n-tag>
        </div>
        <div class="flex gap-2 mt-3 text-xs text-indigo-300">
          <span class="flex items-center gap-1"><n-icon><GitNetworkOutline/></n-icon> 引用: {{ hoverNode.degree || 0 }}</span>
        </div>
        <div class="mt-2 text-xs text-gray-400">
          点击穿越至该节点
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, shallowRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NSpin, NTag } from 'naive-ui'
import { ArrowBack, PlanetOutline, ScanOutline, GitNetworkOutline } from '@vicons/ionicons5'
import ForceGraph3D from '3d-force-graph'
import * as THREE from 'three'
import { getGlobalGraph } from '@/api/community'

interface GalaxyNode {
  id: number
  title: string
  userId: number
  val: number
  color: string
  degree: number
  isPublic?: boolean
  x?: number
  y?: number
  z?: number
}

interface GalaxyLink {
  source: number | string
  target: number | string
  type: string
  label?: string
}

const router = useRouter()
const graphContainer = ref<HTMLElement | null>(null)
const loading = ref(true)
const hoverNode = ref<any>(null)
const autoRotate = ref(true)

// Graph 实例保存为 shallowRef 避免 Vue 响应式代理大量 3D 对产生的性能问题
const Graph = shallowRef<any>(null)
let rotationAngle = 0
let animationFrameId: number

const goBack = () => {
  router.back()
}

const resetCamera = () => {
  if (Graph.value) {
    Graph.value.cameraPosition(
      { x: 0, y: 0, z: 800 }, // new position
      { x: 0, y: 0, z: 0 },   // lookAt
      2000                    // ms transition duration
    )
  }
}

const initGraph = async () => {
  if (!graphContainer.value) return
  
  try {
    loading.value = true
    const nodesData = await getGlobalGraph(300)
    
    // 构造 nodes 和 links 数据
    const nodes: GalaxyNode[] = []
    const links: GalaxyLink[] = []
    const nodeMap = new Map<number, GalaxyNode>()

    nodesData.forEach(n => {
      const node = { 
        id: n.id, 
        title: n.title, 
        userId: n.userId, 
        val: 1, // 控制大小，可根据引用次数计算
        color: '#6366f1', // 默认 indigo
        degree: 0 
      }
      nodes.push(node)
      nodeMap.set(n.id, node)
    })

    nodesData.forEach(n => {
      // 物理链接
      if (n.links) {
        n.links.forEach((l: any) => {
          if (nodeMap.has(l.id)) {
            links.push({ source: n.id, target: l.id, type: 'link' })
            nodeMap.get(n.id)!.degree++
            nodeMap.get(l.id)!.degree++
            // 增加大小
            nodeMap.get(l.id)!.val += 0.5
          }
        })
      }
      // AI 语义链接
      if (n.related) {
        n.related.forEach((r: any) => {
          if (r.target && nodeMap.has(r.target.id)) {
            links.push({ source: n.id, target: r.target.id, type: 'ai', label: r.label })
          }
        })
      }
    })

    // 根据度数调整颜色
    nodes.forEach(n => {
      if (n.degree > 10) n.color = '#f59e0b' // amber
      else if (n.degree > 5) n.color = '#ec4899' // pink
      else if (n.degree > 2) n.color = '#8b5cf6' // purple
    })

    const graphData = { nodes, links }

    Graph.value = ForceGraph3D()(graphContainer.value)
      .backgroundColor('#000000')
      .nodeRelSize(4)
      .nodeVal('val')
      .nodeColor('color')
      .nodeOpacity(0.9)
      .nodeResolution(32)
      .linkColor((link: any) => link.type === 'ai' ? 'rgba(99, 102, 241, 0.4)' : 'rgba(255, 255, 255, 0.6)')
      .linkWidth((link: any) => link.type === 'ai' ? 0.5 : 1)
      .linkDirectionalParticles((link: any) => link.type === 'link' ? 2 : 0) // 实线链接加流动粒子
      .linkDirectionalParticleWidth(1.5)
      .linkDirectionalParticleSpeed(0.005)
      // 交互
      .onNodeHover((node: any) => {
        hoverNode.value = node
        document.body.style.cursor = node ? 'pointer' : 'default'
      })
      .onNodeClick((node: any) => {
        // 飞船视角穿梭到目标星球
        const distance = 80
        const distRatio = 1 + distance/Math.hypot(node.x, node.y, node.z)
        Graph.value.cameraPosition(
          { x: node.x * distRatio, y: node.y * distRatio, z: node.z * distRatio }, // new position
          node, // lookAt ({ x, y, z })
          2000  // ms transition duration
        )
        // 延迟跳转或打开浮层查阅
        setTimeout(() => {
            router.push(`/community/note/${node.id}`)
        }, 1500)
      })
      .graphData(graphData)

    // 添加一些星空背景点缀效果
    const scene = Graph.value.scene()
    const starGeo = new THREE.BufferGeometry()
    const starCount = 3000
    const starArr = new Float32Array(starCount * 3)
    for(let i=0;i<starCount*3;i++) {
      starArr[i] = (Math.random() - 0.5) * 4000
    }
    starGeo.setAttribute('position', new THREE.BufferAttribute(starArr, 3))
    const starMat = new THREE.PointsMaterial({color: 0x888888, size: 1, transparent: true, opacity: 0.5})
    const stars = new THREE.Points(starGeo, starMat)
    scene.add(stars)

    // 自动漫游动画循环
    const animate = () => {
      if (autoRotate.value && Graph.value) {
        // 让相机围绕中心缓慢旋转
        const distance = 800
        Graph.value.cameraPosition({
          x: distance * Math.sin(rotationAngle),
          z: distance * Math.cos(rotationAngle)
        })
        rotationAngle += Math.PI / 1500
      }
      animationFrameId = requestAnimationFrame(animate)
    }
    animate()

  } catch (error) {
    console.error('加载星空图谱失败', error)
  } finally {
    loading.value = false
  }
}

watch(autoRotate, (val) => {
  if (!val && Graph.value) {
     // 停止旋转时的最后位置，如果需要干预可在这里设置
  }
})

// 监听容器大小改变
const handleResize = () => {
  if (Graph.value && graphContainer.value) {
    Graph.value
      .width(graphContainer.value.clientWidth)
      .height(graphContainer.value.clientHeight)
  }
}

onMounted(() => {
  initGraph()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  if (Graph.value) {
    Graph.value._destructor()
  }
  window.removeEventListener('resize', handleResize)
  document.body.style.cursor = 'default'
})
</script>

<style scoped>
.galaxy-container {
  /* 屏蔽滚动条和溢出 */
  touch-action: none;
}
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translate(-50%, 20px);
}
</style>
