<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import AppSidebar from './components/AppSidebar.vue'

const route = useRoute()
const sidebarCollapsed = ref(false)
const contentRef = ref<HTMLElement>()

function toggleSidebar(): void {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

watch(() => route.path, () => {
  if (contentRef.value) {
    contentRef.value.scrollTop = 0
  }
})
</script>

<template>
  <div class="main-layout">
    <AppSidebar :collapsed="sidebarCollapsed" />
    <div class="main-layout__right" :class="{ 'main-layout__right--collapsed': sidebarCollapsed }">
      <AppHeader @toggle-sidebar="toggleSidebar" />
      <main ref="contentRef" class="main-layout__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.main-layout__right {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-left: var(--sidebar-width);
  transition: margin-left var(--transition-duration);
}

.main-layout__right--collapsed {
  margin-left: var(--sidebar-collapsed-width);
}

.main-layout__content {
  flex: 1;
  overflow-y: auto;
  background-color: var(--color-bg-page);
  padding: var(--spacing-lg);
}
</style>