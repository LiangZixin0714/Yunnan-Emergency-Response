<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login({ username: loginForm.username, password: loginForm.password })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/home'
    router.push(redirect)
  } catch {
    // Error handled by Axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-page__card">
      <div class="login-page__header">
        <el-icon :size="48" color="var(--color-primary)"><Warning /></el-icon>
        <h1 class="login-page__title">云南自然灾害应急协同决策平台</h1>
        <p class="login-page__subtitle">Yunnan Emergency Response Platform</p>
      </div>
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-page__btn"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
        <el-form-item>
          <div style="width: 100%; text-align: center">
            <el-button link type="primary" @click="router.push('/register')">没有账号？立即注册</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a73e8 0%, #0d47a1 50%, #01579b 100%);
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
  top: -200px;
  right: -100px;
}

.login-page::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 50%;
  bottom: -150px;
  left: -100px;
}

.login-page__card {
  width: 420px;
  padding: 48px 40px;
  background: #fff;
  border-radius: var(--border-radius-lg);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.2);
  z-index: 1;
}

.login-page__header {
  text-align: center;
  margin-bottom: 36px;
}

.login-page__title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 16px 0 4px;
}

.login-page__subtitle {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  letter-spacing: 1px;
}

.login-page__btn {
  width: 100%;
  font-size: var(--font-size-lg);
}
</style>