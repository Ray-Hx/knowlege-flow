<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import { useUserStore } from '@/store/user'
import { login } from '@/api/auth'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

const model = ref({
  username: '',
  password: ''
})

const loading = ref(false)

async function handleLogin() {
  if (!model.value.username || !model.value.password) {
    message.warning('请输入用户名和密码')
    return
  }
  
  loading.value = true
  try {
    const res = await login(model.value)
    if (res.code === 200 && res.data) {
      userStore.setToken(res.data)
      message.success('登录成功')
      router.push('/')
    } else {
      message.error(res.message || '登录失败')
    }
  } catch (error: any) {
    console.error('Login failed:', error)
    // Error message already shown by axios interceptor
  } finally {
    loading.value = false
  }
}
</script>


<template>
  <div class="h-screen w-full flex items-center justify-center bg-gray-100 dark:bg-gray-900">
    <n-card class="w-96 shadow-lg rounded-xl">
      <template #header>
        <div class="text-center text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-500 to-teal-400">
          KnowledgeFlow
        </div>
      </template>
      <n-form>
        <n-form-item label="用户名">
          <n-input v-model:value="model.username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item label="密码">
          <n-input
            v-model:value="model.password"
            type="password"
            show-password-on="mousedown"
            placeholder="请输入密码"
            @keydown.enter="handleLogin"
          />
        </n-form-item>
        <n-button type="primary" block :loading="loading" @click="handleLogin">
          登录
        </n-button>
      </n-form>
    </n-card>
  </div>
</template>
