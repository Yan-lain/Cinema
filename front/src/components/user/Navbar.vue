<template>
  <header class="navbar">
    <div class="navbar-container">
      <div class="left-section">
        <button v-if="showBackButton" class="back-btn" @click="goBack">
          <span class="back-icon">←</span>
          <span>返回</span>
        </button>
        <router-link v-else to="/" class="logo">
          <span class="logo-icon">🎬</span>
          <span class="logo-text">光影影院</span>
        </router-link>
      </div>

      <nav class="nav-links">
        <router-link to="/" class="nav-link" :class="{ active: route.path === '/' }">
          <span class="nav-icon">🏠</span>
          <span>首页</span>
        </router-link>
        <router-link to="/movies" class="nav-link" :class="{ active: route.path === '/movies' }">
          <span class="nav-icon">🎞️</span>
          <span>电影</span>
        </router-link>
        <router-link to="/cinemas" class="nav-link" :class="{ active: route.path === '/cinemas' }">
          <span class="nav-icon">🏢</span>
          <span>影院</span>
        </router-link>
      </nav>

      <div class="search-section">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索电影..."
          class="navbar-search-input"
          @keyup.enter="handleSearch"
        />
        <button class="navbar-search-btn" @click="handleSearch">
          <span>🔍</span>
        </button>
      </div>

      <div class="user-area">
        <div class="user-menu" @click="toggleMenu">
          <div class="avatar" :class="{ animated: showMenu }">
            {{ authStore.isAuthenticated ? (authStore.user?.name?.charAt(0) || 'U') : '👤' }}
          </div>
          <span class="arrow" :class="{ active: showMenu }">▼</span>

          <Transition name="dropdown">
            <div v-if="showMenu" class="dropdown-menu">
              <router-link to="/profile" class="menu-item" @click="showMenu = false">
                <span class="menu-icon">👤</span>
                <span>个人中心</span>
              </router-link>
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
          </Transition>
        </div>
      </div>

      <button class="mobile-menu-btn" @click="toggleMobileMenu">
        <span class="hamburger"></span>
        <span class="hamburger"></span>
        <span class="hamburger"></span>
      </button>
    </div>

    <Transition name="slide-down">
      <div v-if="mobileMenuOpen" class="mobile-menu">
        <nav class="mobile-nav-links">
          <router-link to="/" class="mobile-nav-link" @click="mobileMenuOpen = false">
            <span class="nav-icon">🏠</span>
            <span>首页</span>
          </router-link>
          <router-link to="/movies" class="mobile-nav-link" @click="mobileMenuOpen = false">
            <span class="nav-icon">🎞️</span>
            <span>电影</span>
          </router-link>
          <router-link to="/cinemas" class="mobile-nav-link" @click="mobileMenuOpen = false">
            <span class="nav-icon">🏢</span>
            <span>影院</span>
          </router-link>
        </nav>
        <div class="mobile-search">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索电影..."
            class="mobile-search-input"
            @keyup.enter="handleSearch"
          />
          <button class="mobile-search-btn" @click="handleSearch">🔍</button>
        </div>
        <div class="mobile-user-info">
          <div v-if="authStore.isAuthenticated" class="mobile-user-item" @click="toggleMobileMenu(); navigateToOrders()">
            <span class="menu-icon">📋</span>
            <span>我的订单</span>
          </div>
          <div v-else class="mobile-user-item" @click="openLoginModal(); toggleMobileMenu()">
            <span class="menu-icon">🔑</span>
            <span>登录 / 注册</span>
          </div>
        </div>
      </div>
    </Transition>

    <LoginModal :show="showLoginModal" @close="showLoginModal = false" />
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginModal from '@/components/user/LoginModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const showMenu = ref(false)
const showLoginModal = ref(false)
const searchQuery = ref('')
const mobileMenuOpen = ref(false)

const showBackButton = computed(() => {
  return route.path !== '/' && !route.path.startsWith('/admin')
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

const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

const openLoginModal = () => {
  showMenu.value = false
  mobileMenuOpen.value = false
  showLoginModal.value = true
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
    searchQuery.value = ''
    mobileMenuOpen.value = false
  }
}

const handleClickOutside = (event) => {
  const target = event.target
  if (!target.closest('.user-menu') && !target.closest('.dropdown-menu')) {
    showMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.navbar {
  background: linear-gradient(135deg, var(--bg-secondary) 0%, var(--bg-primary) 100%);
  box-shadow: var(--shadow-md);
  position: sticky;
  top: 0;
  z-index: 100;
  transition: all var(--transition-normal);
}

.navbar-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--spacing-lg);
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.left-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  min-width: 140px;
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 20px;
  font-weight: bold;
  color: var(--text-primary);
  text-decoration: none;
  transition: transform var(--transition-fast);
}

.logo:hover {
  transform: scale(1.05);
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  background: linear-gradient(135deg, var(--color-primary-light), var(--color-secondary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-full);
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-weight: 500;
}

.back-btn:hover {
  background: var(--color-primary-dark);
  transform: translateX(-4px);
}

.nav-links {
  display: flex;
  gap: var(--spacing-xl);
}

.nav-link {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 15px;
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  position: relative;
}

.nav-link:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.1);
}

.nav-link.active {
  color: var(--text-primary);
  background: rgba(99, 102, 241, 0.2);
}

.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background: var(--color-primary);
  border-radius: var(--radius-full);
}

.nav-icon {
  font-size: 16px;
}

.search-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-full);
  border: 1px solid var(--border-color);
  transition: all var(--transition-fast);
}

.search-section:focus-within {
  background: rgba(255, 255, 255, 0.12);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.navbar-search-input {
  border: none;
  background: transparent;
  padding: var(--spacing-sm);
  font-size: 13px;
  width: 200px;
  outline: none;
  color: var(--text-primary);
}

.navbar-search-input::placeholder {
  color: var(--text-muted);
}

.navbar-search-btn {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
  padding: var(--spacing-xs);
  color: var(--text-muted);
  transition: color var(--transition-fast);
}

.navbar-search-btn:hover {
  color: var(--text-primary);
}

.user-area {
  position: relative;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  cursor: pointer;
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-lg);
  transition: background var(--transition-fast);
}

.user-menu:hover {
  background: rgba(255, 255, 255, 0.1);
}

.avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--color-secondary), var(--color-primary));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
  transition: all var(--transition-fast);
}

.avatar.animated {
  transform: scale(1.1);
}

.arrow {
  font-size: 10px;
  color: var(--text-muted);
  transition: transform var(--transition-fast);
}

.arrow.active {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + var(--spacing-sm));
  right: 0;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  min-width: 220px;
  padding: var(--spacing-xs) 0;
  z-index: 1000;
}

.dropdown-enter-active {
  animation: dropdownIn 0.2s ease;
}

.dropdown-leave-active {
  animation: dropdownIn 0.2s ease reverse;
}

@keyframes dropdownIn {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.menu-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  transition: all var(--transition-fast);
  cursor: pointer;
  border: none;
  background: none;
  width: 100%;
  text-align: left;
}

.menu-item:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.menu-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
}

.menu-divider {
  height: 1px;
  background: var(--border-color);
  margin: var(--spacing-xs) 0;
}

.logout-item {
  color: var(--color-error);
}

.logout-item:hover {
  background: rgba(239, 68, 68, 0.1);
}

.login-item {
  color: var(--color-primary);
}

.login-item:hover {
  background: rgba(99, 102, 241, 0.1);
}

.mobile-menu-btn {
  display: none;
  flex-direction: column;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--spacing-sm);
}

.hamburger {
  width: 24px;
  height: 2px;
  background: var(--text-primary);
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);
}

.mobile-menu-btn:hover .hamburger {
  background: var(--color-primary-light);
}

.mobile-menu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-lg);
}

.slide-down-enter-active {
  animation: slideDown 0.3s ease;
}

.slide-down-leave-active {
  animation: slideDown 0.3s ease reverse;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mobile-nav-links {
  display: flex;
  flex-direction: column;
}

.mobile-nav-link {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 16px;
  transition: background var(--transition-fast);
  border-bottom: 1px solid var(--border-color);
}

.mobile-nav-link:last-child {
  border-bottom: none;
}

.mobile-nav-link:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.mobile-search {
  display: flex;
  gap: var(--spacing-xs);
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
}

.mobile-search-input {
  flex: 1;
  padding: var(--spacing-md);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  color: var(--text-primary);
  font-size: 14px;
}

.mobile-search-input::placeholder {
  color: var(--text-muted);
}

.mobile-search-btn {
  padding: var(--spacing-md);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-lg);
  color: var(--text-primary);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.mobile-search-btn:hover {
  background: var(--color-primary-dark);
}

.mobile-user-info {
  display: flex;
  flex-direction: column;
  padding: var(--spacing-sm);
}

.mobile-user-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: background var(--transition-fast);
  border-radius: var(--radius-md);
}

.mobile-user-item:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

@media (max-width: 900px) {
  .navbar-container {
    padding: 0 var(--spacing-md);
    height: 64px;
  }

  .nav-links {
    display: none;
  }

  .search-section {
    display: none;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .logo-text {
    display: none;
  }
}

@media (max-width: 480px) {
  .left-section {
    min-width: 80px;
  }

  .back-btn span:last-child {
    display: none;
  }
}
</style>
