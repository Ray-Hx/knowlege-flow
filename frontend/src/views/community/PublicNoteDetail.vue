<template>
  <div class="max-w-4xl mx-auto p-4 w-full">
    <div v-if="loading" class="flex justify-center py-20">
      <n-spin size="large" />
    </div>

    <div v-else-if="!note" class="flex flex-col items-center justify-center py-20 text-gray-400">
      <div class="text-6xl mb-4">🚫</div>
      <p class="text-lg">笔记不存在或未公开</p>
      <n-button class="mt-4" @click="$router.push('/community/feed')">返回社区</n-button>
    </div>

    <div v-else class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <!-- 头部信息 -->
      <div class="p-8 border-b border-gray-50 bg-gradient-to-b from-white to-gray-50/50">
        <div class="flex items-center gap-2 mb-4">
           <n-button size="small" secondary circle @click="$router.back()">
              <template #icon><n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M20 11H7.83l5.59-5.59L12 4l-8 8l8 8l1.41-1.41L7.83 13H20z"/></svg></n-icon></template>
           </n-button>
           <n-tag :bordered="false" type="info" size="small" round>
              {{ note.folderName || '公开笔记' }}
           </n-tag>
        </div>
        
        <h1 class="text-3xl font-bold text-gray-800 mb-6 leading-tight">{{ note.title }}</h1>
        
        <div class="flex items-center justify-between">
            <div class="flex items-center gap-3 cursor-pointer hover:opacity-80 transition-opacity" @click="goToProfile(note.authorId)">
              <n-avatar
                round
                size="medium"
                :src="note.authorAvatar || 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'"
                class="border border-white shadow-sm"
              />
              <div class="flex flex-col">
                 <span class="text-sm font-bold text-gray-700 hover:text-indigo-600 transition-colors">{{ note.authorName || '匿名用户' }}</span>
                 <span class="text-xs text-gray-400">发布于 {{ formatTime(note.createdTime) }} · 更新于 {{ formatTime(note.updatedTime) }}</span>
              </div>
            </div>
            
             <div class="flex items-center gap-4">
                <!-- 浏览量 -->
                <div class="flex items-center gap-1 text-gray-400 text-sm">
                   <n-icon size="18"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5s5 2.24 5 5s-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3s3-1.34 3-3s-1.34-3-3-3z"/></svg></n-icon>
                   <span>{{ note.viewCount || 0 }}</span>
                </div>
                <!-- 评论数 -->
                <div class="flex items-center gap-1 text-gray-400 text-sm">
                   <n-icon size="18"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg></n-icon>
                   <span>{{ comments.length }}</span>
                </div>
                <!-- 点赞按钮 -->
                <n-button 
                   :type="isLiked ? 'error' : 'default'" 
                   size="small" 
                   round
                   :loading="likeLoading"
                   @click="handleLike"
                >
                   <template #icon>
                      <n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="m12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5C2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></n-icon>
                   </template>
                   {{ likeCount }}
                </n-button>
             </div>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="p-8 min-h-[500px]">
        <div id="preview" class="markdown-body"></div>
      </div>

      <!-- 相关推荐区 -->
      <div v-if="relatedNotes && relatedNotes.length > 0" class="p-8 border-t border-gray-50 bg-indigo-50/10">
        <h3 class="text-lg font-bold text-gray-700 mb-6 flex items-center gap-2">
          <n-icon size="20" class="text-orange-400">
             <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2.02c-5.524 0-10 4.476-10 10s4.476 10 10 10s10-4.476 10-10C22 6.55 17.58 2.02 12 2.02zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8s8 3.58 8 8s-3.58 8-8 8zm-2-9h4v4h-4v-4zm0-5h4v4h-4V6z"/></svg>
          </n-icon>
          AI 相关推荐
        </h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div 
            v-for="relNote in relatedNotes" 
            :key="relNote.id"
            class="bg-white p-4 rounded-xl border border-gray-100 shadow-sm cursor-pointer hover:shadow-md hover:border-indigo-100 transition-all group flex flex-col justify-between"
            @click="goToNote(relNote.id)"
          >
            <div>
              <h4 class="font-bold text-gray-800 text-base mb-2 group-hover:text-indigo-600 line-clamp-1">{{ relNote.title }}</h4>
              <p class="text-gray-500 text-xs line-clamp-2 leading-relaxed">{{ relNote.summary || '暂无摘要' }}</p>
            </div>
            <div class="mt-3 pt-3 border-t border-gray-50 flex items-center justify-between text-xs text-gray-400">
               <span class="flex items-center gap-1 hover:text-indigo-600 transition-colors" @click.stop="goToProfile(relNote.authorId)">
                 <n-avatar round size="small" :src="relNote.authorAvatar || 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'" class="w-4 h-4" />
                 {{ relNote.authorName || '匿名用户' }}
               </span>
               <span class="flex items-center gap-3">
                   <span class="flex items-center gap-1">
                     <el-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="14" height="14"><path fill="currentColor" d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5s5 2.24 5 5s-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3s3-1.34 3-3s-1.34-3-3-3z"/></svg></el-icon>
                     {{ relNote.viewCount || 0 }}
                   </span>
                   <span class="flex items-center gap-1">
                     <el-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="14" height="14"><path fill="currentColor" d="m12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5C2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></el-icon>
                     {{ relNote.likeCount || 0 }}
                   </span>
               </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="p-8 border-t border-gray-100 bg-gray-50/30">
        <h3 class="text-lg font-bold text-gray-700 mb-6 flex items-center gap-2">
          <n-icon size="20" class="text-indigo-500">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>
          </n-icon>
          评论 <span class="text-gray-400 text-base font-normal">（{{ comments.length }}）</span>
        </h3>

        <!-- 发表评论输入框 -->
        <div class="mb-8">
          <n-input
            v-model:value="newComment"
            type="textarea"
            :placeholder="replyTo ? `回复 @${replyTo.authorName}：` : '写下你的想法...'"
            :autosize="{ minRows: 2, maxRows: 5 }"
            class="rounded-xl"
          />
          <div class="flex items-center gap-2 mt-2 justify-end">
            <n-button v-if="replyTo" size="small" @click="cancelReply">取消回复</n-button>
            <n-button
              type="primary"
              size="small"
              :loading="submitLoading"
              :disabled="!newComment.trim()"
              @click="submitComment"
            >
              {{ replyTo ? '回复' : '发表评论' }}
            </n-button>
          </div>
        </div>

        <!-- 评论列表 -->
        <n-spin :show="commentsLoading">
          <div v-if="comments.length === 0 && !commentsLoading" class="text-center text-gray-400 py-8">
            <div class="text-4xl mb-2">💬</div>
            <p>暂无评论，来抢楼吧！</p>
          </div>

          <div class="space-y-6">
            <div v-for="comment in comments" :key="comment.id" class="flex gap-3">
              <!-- 头像 -->
              <n-avatar
                round
                size="small"
                :src="comment.authorAvatar || 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'"
              />
              <div class="flex-1">
                <!-- 评论主体 -->
                <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-50">
                  <div class="flex items-center justify-between mb-1">
                    <span class="text-sm font-bold text-gray-700">{{ comment.authorName }}</span>
                    <span class="text-xs text-gray-400">{{ formatTime(comment.createdTime) }}</span>
                  </div>
                  <p class="text-gray-600 text-sm leading-relaxed">{{ comment.content }}</p>
                  <div class="flex items-center gap-3 mt-2">
                    <button
                      class="text-xs text-indigo-400 hover:text-indigo-600 transition-colors"
                      @click="startReply(comment)"
                    >回复</button>
                    <button
                      v-if="isMyComment(comment)"
                      class="text-xs text-red-400 hover:text-red-600 transition-colors"
                      @click="handleDeleteComment(comment.id)"
                    >删除</button>
                  </div>
                </div>

                <!-- 子评论（回复）-->
                <div v-if="comment.replies && comment.replies.length" class="mt-3 ml-4 space-y-3">
                  <div v-for="reply in comment.replies" :key="reply.id" class="flex gap-2">
                    <n-avatar
                      round
                      size="small"
                      :src="reply.authorAvatar || 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'"
                    />
                    <div class="flex-1 bg-gray-50 rounded-xl p-3 border border-gray-100">
                      <div class="flex items-center justify-between mb-1">
                        <span class="text-xs font-bold text-gray-600">{{ reply.authorName }}</span>
                        <span class="text-xs text-gray-400">{{ formatTime(reply.createdTime) }}</span>
                      </div>
                      <p class="text-gray-600 text-sm leading-relaxed">{{ reply.content }}</p>
                      <div class="flex items-center gap-3 mt-2">
                        <button
                          class="text-xs text-indigo-400 hover:text-indigo-600 transition-colors"
                          @click="startReply(comment)"
                        >回复</button>
                        <button
                          v-if="isMyComment(reply)"
                          class="text-xs text-red-400 hover:text-red-600 transition-colors"
                          @click="handleDeleteComment(reply.id)"
                        >删除</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </n-spin>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NSpin, NAvatar, NTag, NButton, NIcon, NInput, useMessage, useDialog } from 'naive-ui'
import { getPublicNoteDetail, toggleLike, getComments, addComment, deleteComment, getRelatedPublicNotes } from '@/api/community'
import type { NoteComment } from '@/api/community'
import type { NoteDetail } from '@/api/note'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import dayjs from 'dayjs'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()

const loading = ref(true)
const note = ref<NoteDetail | null>(null)
const isLiked = ref(false)
const likeCount = ref(0)
const likeLoading = ref(false)
const relatedNotes = ref<any[]>([])

// 评论相关
const comments = ref<NoteComment[]>([])
const commentsLoading = ref(false)
const newComment = ref('')
const submitLoading = ref(false)
const replyTo = ref<NoteComment | null>(null)

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY年MM月DD日')
}

const goToProfile = (userId?: number) => {
  if (userId) {
    router.push(`/profile/${userId}`)
  }
}

const isMyComment = (comment: NoteComment) => {
  return userStore.userInfo && comment.userId === userStore.userInfo.id
}

const startReply = (parent: NoteComment) => {
  replyTo.value = parent
  newComment.value = ''
}

const cancelReply = () => {
  replyTo.value = null
  newComment.value = ''
}

const submitComment = async () => {
  if (!newComment.value.trim()) return
  const noteId = Number(route.params.id)
  
  if (!userStore.userInfo) {
    message.warning('请先登录后再评论')
    return
  }

  submitLoading.value = true
  try {
    await addComment(noteId, newComment.value.trim(), replyTo.value?.id)
    newComment.value = ''
    replyTo.value = null
    message.success('评论发表成功')
    await loadComments(noteId)
  } catch (e: any) {
    message.error(e.message || '评论失败，请稍后重试')
  } finally {
    submitLoading.value = false
  }
}

const handleDeleteComment = (commentId: number) => {
  const noteId = Number(route.params.id)
  dialog.warning({
    title: '确认删除',
    content: '确定要删除这条评论吗？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteComment(noteId, commentId)
        message.success('评论已删除')
        await loadComments(noteId)
      } catch (e: any) {
        message.error(e.message || '删除失败')
      }
    }
  })
}

const loadComments = async (noteId: number) => {
  commentsLoading.value = true
  try {
    comments.value = await getComments(noteId)
  } catch (e) {
    // 静默失败，不影响主流程
  } finally {
    commentsLoading.value = false
  }
}

const handleLike = async () => {
    if (!note.value) return
    
    try {
        likeLoading.value = true
        const result = await toggleLike(note.value.id)
        isLiked.value = result.liked
        likeCount.value = result.likeCount
        message.success(result.liked ? '点赞成功' : '已取消点赞')
    } catch (e: any) {
        message.error(e.message || '操作失败，请登录后重试')
    } finally {
        likeLoading.value = false
    }
}

const goToNote = (id: number) => {
    router.push(`/community/note/${id}`).then(() => {
        // 重载组件
        loadNote()
        window.scrollTo({ top: 0, behavior: 'smooth' })
    })
}

const loadNote = async () => {
    const id = Number(route.params.id)
    if (!id) return
    
    try {
        const data = await getPublicNoteDetail(id)
        note.value = data
        isLiked.value = data.isLiked || false
        likeCount.value = data.likeCount || 0
        
        // 先关闭 loading，让 v-else 块中的 #preview div 出现在 DOM 中
        loading.value = false

        // 等待 DOM 更新后再渲染 Markdown
        await nextTick()
        if (data.contentMd) {
            Vditor.preview(document.getElementById('preview') as HTMLDivElement, data.contentMd, {
                cdn: 'https://cdn.jsdelivr.net/npm/vditor',
                mode: 'light',
                theme: {
                    current: 'light'
                },
                 transform(html: string) {
                    html = html.replace(/\[\[!article:(\d+)(?:\|([^\]]+))?\]\]/g, (_, id, title) => {
                        const displayTitle = title ? title : `文章引用 #${id}`
                        return `<span class="article-ref-badge" style="cursor:pointer" onclick="window.open('/#/content?keyword=' + encodeURIComponent('${title || ''}'), '_blank')">📰 ${displayTitle}</span>`
                    })
                    html = html.replace(/\[\[([^\]!][^\]]*)\]\]/g, (_, title) => {
                        return `<a class="note-ref-badge" href="/#/notes?keyword=${encodeURIComponent(title)}" target="_blank" style="text-decoration: none;">📝 ${title}</a>`
                    })
                    return html
                }
            })
        }
        
        // 并行加载评论和推荐
        await Promise.all([
          loadComments(id),
          loadRelatedNotes(id)
        ])
    } catch (e: any) {
        message.error(e.message || '加载笔记失败')
        loading.value = false
    }
}

const loadRelatedNotes = async (id: number) => {
    try {
        relatedNotes.value = await getRelatedPublicNotes(id, 4)
    } catch (e) {
        console.error('获取相关推荐失败', e)
    }
}

onMounted(() => {
    loadNote()
})
</script>

<style scoped>
/* 继承 Vditor 全局样式 */
</style>
