<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import type { UserRoleValue } from '@/types/enums'

defineProps<{
  collapsed: boolean
}>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menuItems = computed(() => {
  const mainRoute = router.options.routes.find((r) => r.path === '/')
  if (!mainRoute?.children) return []
  const role = authStore.roleName
  return mainRoute.children.filter((child) => {
    if (child.meta?.hidden === true) return false
    const roles = child.meta?.roles as UserRoleValue[] | undefined
    if (!roles) return true
    return role ? roles.includes(role) : false
  }).map((child) => {
    if (!child.children) return child
    return {
      ...child,
      children: child.children.filter((sub) => {
        if (sub.meta?.hidden === true) return false
        const subRoles = sub.meta?.roles as UserRoleValue[] | undefined
        if (!subRoles) return true
        return role ? subRoles.includes(role) : false
      }),
    }
  }).filter((child) => {
    if (child.children && child.children.length === 0) return false
    return true
  })
})

const activeMenu = computed(() => {
  return route.path
})

function handleMenuSelect(index: string): void {
  router.push(index)
}
</script>

<template>
  <aside class="app-sidebar" :class="{ 'app-sidebar--collapsed': collapsed }">
    <div class="app-sidebar__logo">
      <el-icon :size="28" color="#fff"><Warning /></el-icon>
      <span v-show="!collapsed" class="app-sidebar__logo-text">应急决策平台</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="collapsed"
      background-color="var(--color-bg-sidebar)"
      text-color="rgba(255, 255, 255, 0.65)"
      active-text-color="#fff"
      @select="handleMenuSelect"
    >
      <template v-for="item in menuItems" :key="item.path">
        <el-sub-menu
          v-if="item.children?.length"
          :index="'/' + item.path"
        >
          <template #title>
            <el-icon><component :is="item.meta?.icon" /></el-icon>
            <span>{{ item.meta?.title }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.path"
            :index="'/' + item.path + '/' + child.path"
          >
            <el-icon><component :is="child.meta?.icon" /></el-icon>
            <span>{{ child.meta?.title }}</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="'/' + item.path">
          <el-icon><component :is="item.meta?.icon" /></el-icon>
          <span>{{ item.meta?.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>
  </aside>
</template>

<style scoped>
.app-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background-color: var(--color-bg-sidebar);
  transition: width var(--transition-duration);
  overflow-y: auto;
  overflow-x: hidden;
  z-index: 100;
}

.app-sidebar--collapsed {
  width: var(--sidebar-collapsed-width);
}

.app-sidebar__logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.app-sidebar__logo-text {
  color: #fff;
  font-size: var(--font-size-lg);
  font-weight: 600;
  white-space: nowrap;
}
</style>
