<script setup lang="ts">
import { NCard, NSpace, NText } from 'naive-ui'
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-100">
    <div class="w-full max-w-md p-4">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-indigo-600 mb-2">KnowledgeFlow</h1>
        <p class="text-gray-500">构建你的第二大脑</p>
      </div>
      <router-view />
    </div>
  </div>
</template>
