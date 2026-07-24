<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { UserRoleLabel } from '@/types/enums'

defineEmits<{
  toggleSidebar: []
}>()

const authStore = useAuthStore()
const router = useRouter()

function handleLogout(): void {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__left">
      <el-icon class="app-header__toggle" @click="$emit('toggleSidebar')">
        <Fold />
      </el-icon>
      <h1 class="app-header__title">云南自然灾害应急协同决策平台</h1>
    </div>
    <div class="app-header__right">
      <span class="app-header__user" v-if="authStore.isLoggedIn">
        <el-icon><User /></el-icon>
        {{ authStore.realName }}
        <el-tag size="small" type="info" class="app-header__role" v-if="authStore.roleName">
          {{ UserRoleLabel[authStore.roleName] }}
        </el-tag>
      </span>
      <el-button text @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        退出登录
      </el-button>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: var(--header-height);
  background-color: var(--color-bg-header);
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  z-index: 10;
}

.app-header__left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.app-header__toggle {
  font-size: 20px;
  cursor: pointer;
  color: var(--color-text-regular);
  transition: color var(--transition-duration);
}

.app-header__toggle:hover {
  color: var(--color-primary);
}

.app-header__title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-primary);
  margin: 0;
}

.app-header__right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.app-header__user {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-base);
  color: var(--color-text-regular);
}

.app-header__role {
  margin-left: var(--spacing-xs);
}
</style>