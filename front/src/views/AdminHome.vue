<template>
  <div class="admin-container">
    <aside class="sidebar">
      <div class="logo">
        <h2>影院管理</h2>
      </div>
      <nav class="menu">
        <div 
          v-for="item in menuItems" 
          :key="item.id"
          class="menu-item"
          :class="{ active: currentMenu === item.id }"
          @click="currentMenu = item.id"
        >
          <span class="icon">{{ item.icon }}</span>
          <span class="text">{{ item.label }}</span>
        </div>
      </nav>
      <div class="logout-btn" @click="handleLogout">
        <span>退出登录</span>
      </div>
    </aside>
    
    <main class="main-content">
      <header class="header">
        <h1>{{ currentMenuName }}</h1>
        <div class="user-info">
          <span>欢迎你{{ admin?.nickname }}</span>
        </div>
      </header>
      
      <div class="content-area">
        <AdminMovie v-if="currentMenu === 'movies'" />
        <AdminCinema v-if="currentMenu === 'cinemas'" />
        <AdminHall v-if="currentMenu === 'halls'" />
        <AdminSchedule v-if="currentMenu === 'schedule'" />
        <AdminOrder v-if="currentMenu === 'orders'" />
        <AdminUser v-if="currentMenu === 'users'" />
        <AdminAnnouncement v-if="currentMenu === 'announcement'" />
        <AdminDashboard v-if="currentMenu === 'dashboard'" />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AdminMovie from '../components/AdminMovie.vue'
import AdminSchedule from '../components/AdminSchedule.vue'
import AdminAnnouncement from '../components/AdminAnnouncement.vue'
import AdminDashboard from '../components/AdminDashboard.vue'
import AdminCinema from '../components/AdminCinema.vue'
import AdminHall from '../components/AdminHall.vue'
import AdminUser from '../components/AdminUser.vue'
import AdminOrder from '../components/AdminOrder.vue'

const router = useRouter()
const currentMenu = ref('dashboard')
const admin = ref(null)

const menuItems = [
  { id: 'dashboard', label: '控制台', icon: '📊' },
  { id: 'movies', label: '电影管理', icon: '🎬' },
  { id: 'cinemas', label: '影院管理', icon: '🏢' },
  { id: 'halls', label: '放映厅管理', icon: '🎭' },
  { id: 'schedule', label: '排片管理', icon: '⏰' },
  { id: 'orders', label: '订单管理', icon: '📋' },
  { id: 'users', label: '用户管理', icon: '👥' },
  { id: 'announcement', label: '公告管理', icon: '📢' }
]

const currentMenuName = computed(() => {
  const item = menuItems.find(i => i.id === currentMenu.value)
  return item ? item.label : ''
})

const handleLogout = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin')
  router.push('/admin/login')
}

onMounted(() => {
  const adminStr = localStorage.getItem('admin')
  if (adminStr) {
    admin.value = JSON.parse(adminStr)
  } else {
    router.push('/admin/login')
  }
})
</script>

<style scoped>
.admin-container {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
}

.logo {
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.menu {
  flex: 1;
  padding: 10px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
  margin-bottom: 4px;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.menu-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.icon {
  font-size: 18px;
}

.text {
  font-size: 14px;
}

.logout-btn {
  padding: 12px 16px;
  margin: 10px;
  background: rgba(255, 107, 107, 0.2);
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: background 0.3s;
}

.logout-btn:hover {
  background: rgba(255, 107, 107, 0.3);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 30px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.header h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.user-info {
  color: #666;
  font-size: 14px;
}

.content-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>