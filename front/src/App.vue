<script setup>
import { ref, onMounted, computed } from 'vue'
import Navbar from './components/Navbar.vue'
import { useAuthStore } from './stores/auth'
import { useRoute } from 'vue-router'

const authStore = useAuthStore()
const route = useRoute()

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

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f5f5f5;
  color: #333;
  line-height: 1.5;
}

#app {
  min-height: 100vh;
}

a {
  text-decoration: none;
  color: inherit;
}
</style>
