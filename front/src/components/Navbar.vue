<template>
  <header class="navbar">
    <div class="navbar-container">
      <div class="left-section">
        <button v-if="showBackButton" class="back-btn" @click="goBack">
          ← 返回
        </button>
        <router-link v-else to="/" class="logo">
          🎬 电影院
        </router-link>
      </div>

      <nav class="nav-links">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/movies" class="nav-link">电影</router-link>
        <router-link to="/cinemas" class="nav-link">影院</router-link>
      </nav>

      <div class="search-section">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索电影..."
          class="navbar-search-input"
          @keyup.enter="handleSearch"
        />
        <button class="navbar-search-btn" @click="handleSearch">🔍</button>
      </div>

      <div class="user-area">
        <div class="user-menu" @click="toggleMenu">
          <div class="avatar">
            {{ authStore.isAuthenticated ? (authStore.user?.name?.charAt(0) || 'U') : '👤' }}
          </div>
          <span class="arrow" :class="{ active: showMenu }">▼</span>

          <div v-if="showMenu" class="dropdown-menu">
            <router-link to="/profile" class="menu-item" @click="showMenu = false">
              <span class="menu-icon">👤</span>
              <span>个人中心</span>
            </router-link>
            <!-- <div v-if="authStore.isAuthenticated" class="menu-item" @click="navigateToVip">
              <span class="menu-icon">💎</span>
              <span>会员中心</span>
            </div>
            <div v-else class="menu-item" @click="openLoginModal">
              <span class="menu-icon">💎</span>
              <span>会员中心</span>
            </div> -->
            <div v-if="authStore.isAuthenticated" class="menu-item" @click="navigateToOrders">
              <span class="menu-icon">📋</span>
              <span>我的订单</span>
            </div>
            <div v-else class="menu-item" @click="openLoginModal">
              <span class="menu-icon">📋</span>
              <span>我的订单</span>
            </div>
            <router-link to="/settings" class="menu-item" @click="showMenu = false">
              <span class="menu-icon">⚙️</span>
              <span>设置</span>
            </router-link>
            <div class="menu-divider" v-if="authStore.isAuthenticated"></div>
            <button v-if="authStore.isAuthenticated" class="menu-item logout-item" @click="handleLogout">
              <span class="menu-icon">🚪</span>
              <span>退出登录</span>
            </button>
            <div v-else class="menu-item login-item" @click="openLoginModal">
              <span class="menu-icon">🔑</span>
              <span>登录 / 注册</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <LoginModal v-if="showLoginModal" @close="showLoginModal = false" />
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginModal from './LoginModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const showMenu = ref(false)
const showLoginModal = ref(false)
const searchQuery = ref('')

const showBackButton = computed(() => {
  return route.path !== '/'
})

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const toggleMenu = () => {
  showMenu.value = !showMenu.value
}

const openLoginModal = () => {
  showMenu.value = false
  showLoginModal.value = true
}

const navigateToVip = () => {
  showMenu.value = false
  router.push('/vip')
}

const navigateToOrders = () => {
  showMenu.value = false
  router.push('/orders')
}

const handleLogout = () => {
  authStore.logout()
  showMenu.value = false
  router.push('/')
}

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value } })
  }
}
</script>

<style scoped>
.navbar {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.3);
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.left-section {
  min-width: 120px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  text-decoration: none;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.back-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 20px;
  font-size: 14px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  box-shadow: 0 2px 10px rgba(102, 126, 234, 0.35);
}

.back-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.45);
}

.nav-links {
  display: flex;
  gap: 40px;
}

.search-section {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  padding: 6px 12px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.navbar-search-input {
  border: none;
  background: transparent;
  padding: 8px 12px;
  font-size: 13px;
  width: 180px;
  outline: none;
  color: #fff;
}

.navbar-search-input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.navbar-search-btn {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  padding: 6px;
  color: rgba(255, 255, 255, 0.8);
}

.navbar-search-btn:hover {
  color: #fff;
}

.nav-link {
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 15px;
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.2s;
}

.nav-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.nav-link.router-link-active {
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 6px;
}

.user-area {
  position: relative;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-menu:hover {
  background: rgba(255, 255, 255, 0.1);
}

.avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.4);
}

.arrow {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.6);
  transition: transform 0.2s;
}

.arrow.active {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  min-width: 200px;
  padding: 8px 0;
  z-index: 1000;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 14px;
  transition: background 0.2s;
  cursor: pointer;
  border: none;
  background: none;
  width: 100%;
  text-align: left;
  position: relative;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.menu-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
}

.menu-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 8px 0;
}

.logout-item {
  color: #ff6b6b;
}

.logout-item:hover {
  background: rgba(255, 107, 107, 0.15);
}

.login-item {
  color: #667eea;
}

.login-item:hover {
  background: rgba(102, 126, 234, 0.15);
}
</style>
