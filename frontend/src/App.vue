<script setup lang="ts">
import { ref } from 'vue'
import { NConfigProvider, NMessageProvider, NDialogProvider, NNotificationProvider, NLoadingBarProvider } from 'naive-ui'
import { zhCN, dateZhCN } from 'naive-ui'
import GlobalSearchModal from '@/components/common/GlobalSearchModal.vue'
import ArticleViewerModal from '@/components/common/ArticleViewerModal.vue'

const articleViewerRef = ref<InstanceType<typeof ArticleViewerModal> | null>(null)

const handleOpenArticle = (id: number) => {
  articleViewerRef.value?.open(id)
}
</script>

<template>
  <n-config-provider :locale="zhCN" :date-locale="dateZhCN">
    <n-loading-bar-provider>
      <n-message-provider>
        <n-notification-provider>
          <n-dialog-provider>
            <router-view />
            <GlobalSearchModal @open-article="handleOpenArticle" />
            <ArticleViewerModal ref="articleViewerRef" />
          </n-dialog-provider>
        </n-notification-provider>
      </n-message-provider>
    </n-loading-bar-provider>
  </n-config-provider>
</template>

<style>
/* Global styles are already imported in main.ts via style.css */
</style>
