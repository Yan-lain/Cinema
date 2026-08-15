<script setup>
import { ref, onMounted, computed } from 'vue'
import Navbar from '@/components/user/Navbar.vue'
import { useAuthStore } from '@/stores/auth'
import { useRoute } from 'vue-router'

const authStore = useAuthStore()
const route = useRoute()

// 显示导航栏
// 非管理员路由才显示导航栏
const showNavbar = computed(() => {
  return !route.path.startsWith('/admin')
})

onMounted(() => {
  authStore.checkAuth()
})
</script>

<template>
  <div id="app">
    <Navbar v-if="showNavbar" />
    <router-view></router-view>
  </div>
</template>

<style scoped>
#app {
  min-height: 100vh;
}
</style>
