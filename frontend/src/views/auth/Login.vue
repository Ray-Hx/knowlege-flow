<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <!-- Background is handled globally by body styles, but we center content here -->
    
    <div class="glass-card w-full max-w-md p-8 shadow-2xl border-t border-white/60 relative overflow-hidden">
      <!-- Decorative Elements -->
      <div class="absolute -top-10 -right-10 w-40 h-40 bg-purple-500/20 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-10 -left-10 w-40 h-40 bg-indigo-500/20 rounded-full blur-3xl"></div>

      <div class="text-center mb-8 relative z-10">
        <h1 class="text-4xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-indigo-600 to-purple-600 mb-2 tracking-tight">KnowledgeFlow</h1>
        <p class="text-gray-500 text-sm font-medium">您的个人知识第二大脑</p>
      </div>
      
      <n-form ref="formRef" :model="model" size="large" :show-label="false">
        <n-form-item path="username">
          <n-input 
            v-model:value="model.username" 
            placeholder="用户名 / 邮箱" 
            class="glass-input !bg-white/50 hover:!bg-white/80 focus:!bg-white/90 transition-all font-medium"
            @keydown.enter.prevent 
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
            placeholder="密码"
            class="glass-input !bg-white/50 hover:!bg-white/80 focus:!bg-white/90 transition-all font-medium"
            @keydown.enter.prevent="handleLogin"
          >
            <template #prefix>
              <n-icon color="#808080" :component="LockClosedOutline" />
            </template>
          </n-input>
        </n-form-item>

        <n-form-item path="captchaCode">
          <div class="flex gap-4 w-full">
            <n-input
              v-model:value="model.captchaCode"
              placeholder="验证码"
              class="glass-input flex-1 !bg-white/50 hover:!bg-white/80 focus:!bg-white/90 transition-all font-medium"
              @keydown.enter.prevent="handleLogin"
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
        
        <div class="flex justify-between items-center mb-6 text-sm">
          <n-checkbox v-model:checked="rememberMe" class="text-gray-600">记住我</n-checkbox>
          <span class="cursor-pointer text-indigo-500 hover:text-indigo-700 transition font-medium">忘记密码?</span>
        </div>

        <n-button 
          type="primary" 
          block 
          size="large" 
          :loading="loading" 
          @click="handleLogin" 
          class="liquid-button h-12 text-lg font-bold shadow-lg shadow-indigo-500/30"
        >
          登 录
        </n-button>
      </n-form>
      
      <div class="mt-8 text-center relative z-10">
<!--        <n-divider class="text-gray-400 text-xs">或者</n-divider>-->
<!--        <div class="flex justify-center gap-4 mt-4">-->
<!--          &lt;!&ndash; Mock Social Login Buttons &ndash;&gt;-->
<!--          <div class="w-10 h-10 rounded-full bg-white/60 flex items-center justify-center cursor-pointer hover:scale-110 transition shadow-sm text-gray-600 hover:text-black">-->
<!--            <n-icon size="20" :component="LogoGithub" />-->
<!--          </div>-->
<!--          <div class="w-10 h-10 rounded-full bg-white/60 flex items-center justify-center cursor-pointer hover:scale-110 transition shadow-sm text-gray-600 hover:text-blue-500">-->
<!--            <n-icon size="20" :component="LogoGoogle" />-->
<!--          </div>-->
<!--        </div>-->
<!--        -->
        <div class="mt-8 text-gray-600 text-sm">
          还没有账号？
          <router-link to="/register" class="text-indigo-600 hover:text-indigo-800 font-bold transition">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NCheckbox, NDivider, NIcon, useMessage } from 'naive-ui'
import { PersonOutline, LockClosedOutline, LogoGithub, LogoGoogle, KeyOutline } from '@vicons/ionicons5'
import { useUserStore } from '@/store/user'
import { authApi } from '@/api/auth'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

const model = ref({
  username: '',
  password: '',
  captchaKey: '',
  captchaCode: ''
})
const rememberMe = ref(false)
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

const handleLogin = async () => {
  if (!model.value.username || !model.value.password || !model.value.captchaCode) {
    message.warning('请输入完整信息')
    return
  }

  loading.value = true
  try {
    const token = await authApi.login(model.value)
    userStore.setToken(token)
    
    // 获取用户信息
    const userInfo = await authApi.getUserInfo()
    userStore.setUserInfo(userInfo)
    
    message.success('登录成功')
    router.push('/')
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
