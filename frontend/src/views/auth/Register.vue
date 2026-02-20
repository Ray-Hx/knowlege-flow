<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <!-- Background is handled globally by body styles -->
    
    <div class="glass-card w-full max-w-md p-8 shadow-2xl border-t border-white/60 relative overflow-hidden">
      <!-- Decorative Elements -->
      <div class="absolute -top-10 -right-10 w-40 h-40 bg-indigo-500/20 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-10 -left-10 w-40 h-40 bg-purple-500/20 rounded-full blur-3xl"></div>

      <div class="text-center mb-8 relative z-10">
        <h1 class="text-3xl font-bold text-gray-800 mb-2 tracking-tight">创建新账号</h1>
        <p class="text-gray-500 text-sm font-medium">加入 KnowledgeFlow，开启知识之旅</p>
      </div>
      
      <n-form ref="formRef" :model="model" size="large" :show-label="false">
        <n-form-item path="username">
          <n-input 
            v-model:value="model.username" 
            placeholder="用户名" 
            class="glass-input !bg-white/50 hover:!bg-white/80 focus:!bg-white/90 transition-all font-medium"
          >
            <template #prefix>
              <n-icon color="#808080" :component="PersonOutline" />
            </template>
          </n-input>
        </n-form-item>
        
        <n-form-item path="password">
          <n-input
            v-model:value="model.password"
            type="password"
            show-password-on="click"
            placeholder="设置密码"
            class="glass-input !bg-white/50 hover:!bg-white/80 focus:!bg-white/90 transition-all font-medium"
          >
            <template #prefix>
              <n-icon color="#808080" :component="LockClosedOutline" />
            </template>
          </n-input>
        </n-form-item>

        <n-form-item path="confirmPassword">
          <n-input
            v-model:value="model.confirmPassword"
            type="password"
            show-password-on="click"
            placeholder="确认密码"
            class="glass-input !bg-white/50 hover:!bg-white/80 focus:!bg-white/90 transition-all font-medium"
          >
            <template #prefix>
              <n-icon color="#808080" :component="CheckmarkCircleOutline" />
            </template>
          </n-input>
        </n-form-item>

        <n-form-item path="email">
          <n-input 
            v-model:value="model.email" 
            placeholder="邮箱" 
            class="glass-input !bg-white/50 hover:!bg-white/80 focus:!bg-white/90 transition-all font-medium"
          >
            <template #prefix>
              <n-icon color="#808080" :component="MailOutline" />
            </template>
          </n-input>
        </n-form-item>

        <n-form-item path="captchaCode">
          <div class="flex gap-4 w-full">
            <n-input
              v-model:value="model.captchaCode"
              placeholder="验证码"
              class="glass-input flex-1 !bg-white/50 hover:!bg-white/80 focus:!bg-white/90 transition-all font-medium"
              @keydown.enter.prevent="handleRegister"
            >
              <template #prefix>
                <n-icon color="#808080" :component="KeyOutline" />
              </template>
            </n-input>
            <div 
              class="h-[46px] w-[120px] rounded cursor-pointer overflow-hidden shadow-sm border border-white/40 hover:opacity-80 transition"
              @click="fetchCaptcha"
              title="点击刷新验证码"
            >
              <img 
                v-if="captchaImage" 
                :src="captchaImage" 
                alt="验证码" 
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full bg-gray-100 animate-pulse"></div>
            </div>
          </div>
        </n-form-item>
        
        <div class="mb-6">
          <n-button 
            type="primary" 
            block 
            size="large" 
            @click="handleRegister" 
            class="liquid-button h-12 text-lg font-bold shadow-lg shadow-indigo-500/30"
          >
            立即注册
          </n-button>
        </div>
      </n-form>
      
      <div class="text-center relative z-10 mt-4">
        <div class="text-gray-600 text-sm">
          已经有账号？
          <router-link to="/login" class="text-indigo-600 hover:text-indigo-800 font-bold transition">返回登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NIcon, useMessage } from 'naive-ui'
import { PersonOutline, LockClosedOutline, CheckmarkCircleOutline, MailOutline, KeyOutline } from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()

const model = ref({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  captchaKey: '',
  captchaCode: ''
})

import { authApi } from '@/api/auth'

const loading = ref(false)
const captchaImage = ref('')

const fetchCaptcha = async () => {
  try {
    const { captchaKey, captchaImage: img } = await authApi.getCaptcha()
    model.value.captchaKey = captchaKey
    captchaImage.value = img
  } catch (error) {
    console.error('Failed to fetch captcha:', error)
  }
}

onMounted(() => {
  fetchCaptcha()
})

const handleRegister = async () => {
  if (!model.value.username || !model.value.password || !model.value.email || !model.value.captchaCode) {
    message.warning('请填写完整信息')
    return
  }
  if (model.value.password !== model.value.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  
  loading.value = true
  try {
    await authApi.register({
      username: model.value.username,
      password: model.value.password,
      email: model.value.email,
      captchaKey: model.value.captchaKey,
      captchaCode: model.value.captchaCode
    })
    message.success('注册成功，请登录')
    router.push('/login')
  } catch (error: any) {
    console.error(error)
    // Refresh captcha on failure
    fetchCaptcha()
    model.value.captchaCode = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.glass-input :deep(.n-input__input-el) {
  height: 46px;
}
.glass-input :deep(.n-input-wrapper) {
  padding-left: 12px;
}
</style>
