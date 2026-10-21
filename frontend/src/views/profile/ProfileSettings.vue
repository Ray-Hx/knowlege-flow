<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
    NCard, NForm, NFormItem, NInput, NButton, NAvatar, NUpload, NText,
    useMessage, UploadFileInfo
} from 'naive-ui'
import { 
    PersonOutline, MailOutline, LogoGithub, CreateOutline, 
    LockClosedOutline, CameraOutline, SaveOutline, ArrowBack
} from '@vicons/ionicons5'
import { userApi, UpdateProfile, UserProfile } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const profile = ref<UserProfile | null>(null)

const formModel = ref<UpdateProfile>({
    email: '',
    bio: '',
    githubId: '',
    learningGoals: ''
})

const passwordModel = ref({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

const fetchProfile = async () => {
    loading.value = true
    try {
        const data = await userApi.getProfile()
        profile.value = data
        formModel.value = {
            email: data.email,
            bio: data.bio,
            githubId: data.githubId,
            learningGoals: data.learningGoals
        }
    } catch (error: any) {
        message.error(error.message || '获取个人资料失败')
    } finally {
        loading.value = false
    }
}

const handleUpdateProfile = async () => {
    saving.value = true
    try {
        const updatedProfile = await userApi.updateProfile(formModel.value)
        message.success('个人资料已更新')
        profile.value = updatedProfile
        // Sync to user store
        userStore.setUserInfo({ ...userStore.userInfo, ...updatedProfile })
    } catch (error: any) {
        message.error(error.message || '更新失败')
    } finally {
        saving.value = false
    }
}

const handleChangePassword = async () => {
    if (passwordModel.value.newPassword !== passwordModel.value.confirmPassword) {
        message.error('两次输入的密码不一致')
        return
    }
    
    try {
        await userApi.changePassword({
            oldPassword: passwordModel.value.oldPassword,
            newPassword: passwordModel.value.newPassword
        })
        message.success('密码修改成功')
        passwordModel.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    } catch (error: any) {
        message.error(error.message || '密码修改失败')
    }
}

const handleAvatarUpload = async (options: { file: UploadFileInfo }) => {
    if (!options.file.file) return
    
    try {
        message.loading('上传头像中...', { duration: 0 })
        const avatarUrl = await userApi.uploadAvatar(options.file.file)
        message.destroyAll()
        message.success('头像上传成功')
        if (profile.value) profile.value.avatar = avatarUrl
        // Sync to user store
        userStore.setUserInfo({ ...userStore.userInfo, avatar: avatarUrl })
    } catch (error: any) {
        message.destroyAll()
        message.error(error.message || '上传头像失败')
    }
}

onMounted(() => {
    fetchProfile()
})
</script>

<template>
    <div class="profile-settings p-4 md:p-8 max-w-4xl mx-auto">
        <n-card class="glass-card mb-6" title="个人主页设置" :segmented="{ content: true }">
            <template #header-extra>
                <n-button quaternary circle @click="router.back()">
                    <template #icon><ArrowBack /></template>
                </n-button>
            </template>
            
            <div class="flex flex-col md:flex-row gap-8 items-start">
                <!-- Avatar Section -->
                <div class="flex flex-col items-center gap-4 w-full md:w-1/3">
                    <n-avatar
                        round
                        :size="150"
                        :src="profile?.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
                        class="border-4 border-white shadow-xl"
                    />
                    <n-upload
                        @change="handleAvatarUpload"
                        :show-file-list="false"
                        accept="image/*"
                    >
                        <n-button icon-placement="left" secondary round>
                            <template #icon><n-icon :component="CameraOutline" /></template>
                            更换头像
                        </n-button>
                    </n-upload>
                    <n-text depth="3" class="text-xs text-center">建议尺寸 200x200px，支持 JPG、PNG 格式</n-text>
                </div>

                <!-- Profile Form -->
                <div class="flex-1 w-full">
                    <n-form
                        ref="formRef"
                        :model="formModel"
                        label-placement="top"
                        size="large"
                    >
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
                            <n-form-item label="用户名">
                                <n-input :value="profile?.username" disabled placeholder="用户名不可修改">
                                    <template #prefix><n-icon :component="PersonOutline" /></template>
                                </n-input>
                            </n-form-item>
                            <n-form-item label="电子邮箱">
                                <n-input v-model:value="formModel.email" placeholder="email@example.com">
                                    <template #prefix><n-icon :component="MailOutline" /></template>
                                </n-input>
                            </n-form-item>
                            <n-form-item label="GitHub ID" class="md:col-span-2">
                                <n-input v-model:value="formModel.githubId" placeholder="GitHub 用户名">
                                    <template #prefix><n-icon :component="LogoGithub" /></template>
                                </n-input>
                            </n-form-item>
                            <n-form-item label="个人简介" class="md:col-span-2">
                                <n-input
                                    v-model:value="formModel.bio"
                                    type="textarea"
                                    placeholder="介绍一下你自己..."
                                    :autosize="{ minRows: 2, maxRows: 4 }"
                                >
                                    <template #prefix><n-icon :component="CreateOutline" /></template>
                                </n-input>
                            </n-form-item>
                            <n-form-item label="学习目标" class="md:col-span-2">
                                <n-input
                                    v-model:value="formModel.learningGoals"
                                    type="textarea"
                                    placeholder="你目前正在学习什么？"
                                    :autosize="{ minRows: 2, maxRows: 4 }"
                                />
                            </n-form-item>
                        </div>
                        <div class="flex justify-end mt-4">
                            <n-button
                                type="primary"
                                round
                                size="large"
                                :loading="saving"
                                @click="handleUpdateProfile"
                            >
                                <template #icon><n-icon :component="SaveOutline" /></template>
                                保存修改
                            </n-button>
                        </div>
                    </n-form>
                </div>
            </div>
        </n-card>

        <n-card class="glass-card" title="安全设置">
            <n-form label-placement="top">
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <n-form-item label="旧密码">
                        <n-input
                            v-model:value="passwordModel.oldPassword"
                            type="password"
                            show-password-on="click"
                            placeholder="输入当前密码"
                        >
                            <template #prefix><n-icon :component="LockClosedOutline" /></template>
                        </n-input>
                    </n-form-item>
                    <n-form-item label="新密码">
                        <n-input
                            v-model:value="passwordModel.newPassword"
                            type="password"
                            show-password-on="click"
                            placeholder="设置新密码"
                        >
                            <template #prefix><n-icon :component="LockClosedOutline" /></template>
                        </n-input>
                    </n-form-item>
                    <n-form-item label="确认新密码">
                        <n-input
                            v-model:value="passwordModel.confirmPassword"
                            type="password"
                            show-password-on="click"
                            placeholder="再次输入新密码"
                        >
                            <template #prefix><n-icon :component="LockClosedOutline" /></template>
                        </n-input>
                    </n-form-item>
                </div>
                <div class="flex justify-end mt-2">
                    <n-button round type="warning" @click="handleChangePassword">修改密码</n-button>
                </div>
            </n-form>
        </n-card>
    </div>
</template>

<style scoped>
.glass-card {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-radius: 1.5rem;
}
</style>
