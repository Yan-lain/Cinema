<template>
  <div class="cinemas-page">
    <div class="header-banner">
      <div class="banner-content">
        <h1>🏢 影院列表</h1>
        <p>选择一家影院，开启您的观影之旅</p>
      </div>
      <div class="banner-decoration"></div>
    </div>

    <div class="cinemas-container">
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      
      <div v-else-if="cinemas.length === 0" class="empty">
        <span class="empty-icon">🏢</span>
        <p>暂无影院信息</p>
      </div>

      <div v-else class="cinemas-list">
        <div
          v-for="(cinema, index) in cinemas"
          :key="cinema.id"
          class="cinema-card"
          :style="{ animationDelay: `${index * 50}ms` }"
          @click="goToDetail(cinema)"
        >
          <div class="cinema-image-wrapper">
            <img 
              :src="cinema.image || 'https://via.placeholder.com/300x200'" 
              :alt="cinema.name" 
              class="cinema-image"
            />
            <div class="cinema-badge" :class="getCinemaStatusClass(cinema)">
              <span class="status-dot"></span>
              {{ getCinemaStatusText(cinema) }}
            </div>
            <div class="cinema-image-overlay"></div>
          </div>
          
          <div class="cinema-info">
            <div class="cinema-header">
              <h2 class="cinema-name">{{ cinema.name }}</h2>
              <span class="cinema-district">{{ cinema.district }}</span>
            </div>
            
            <div class="cinema-details">
              <p class="address">
                <span class="icon">📍</span>
                {{ cinema.address }}
              </p>
              <p v-if="cinema.businessHours" class="business-hours">
                <span class="icon">⏰</span>
                {{ cinema.businessHours }}
              </p>
            </div>

            <div class="cinema-footer">
              <span class="cinema-halls">{{ cinema.hallCount || 0 }} 个影厅</span>
              <span class="cinema-action">查看详情 →</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const cinemas = ref([])
const loading = ref(false)

onMounted(() => {
  loadCinemas()
})

const loadCinemas = async () => {
  loading.value = true
  try {
    cinemas.value = await request.get('/cinemas')
  } catch (error) {
    console.error('加载影院列表失败:', error)
  } finally {
    loading.value = false
  }
}

const goToDetail = (cinema) => {
  router.push({
    path: '/cinema-detail',
    query: { cinemaId: cinema.id.toString() }
  })
}

const getCinemaStatus = (cinema) => {
  if (cinema.status !== 'active') {
    return 'closed'
  }
  
  if (!cinema.businessHours || cinema.businessHours.trim() === '') {
    return 'open'
  }
  
  const hours = cinema.businessHours.split('-')
  if (hours.length !== 2) {
    return 'open'
  }
  
  const [startStr, endStr] = hours
  const now = new Date()
  const currentMinutes = now.getHours() * 60 + now.getMinutes()
  
  const startMatch = startStr.trim().match(/(\d{1,2}):(\d{2})/)
  if (!startMatch) return 'open'
  const startMinutes = parseInt(startMatch[1]) * 60 + parseInt(startMatch[2])
  
  const endMatch = endStr.trim().match(/(\d{1,2}):(\d{2})/)
  if (!endMatch) return 'open'
  const endMinutes = parseInt(endMatch[1]) * 60 + parseInt(endMatch[2])
  
  if (currentMinutes >= startMinutes && currentMinutes <= endMinutes) {
    return 'open'
  } else {
    return 'closed'
  }
}

const getCinemaStatusClass = (cinema) => {
  const status = getCinemaStatus(cinema)
  return status === 'open' ? 'open' : 'closed'
}

const getCinemaStatusText = (cinema) => {
  const status = getCinemaStatus(cinema)
  return status === 'open' ? '营业中' : '休息中'
}
</script>

<style scoped>
.cinemas-page {
  min-height: 100vh;
  background: var(--bg-surface);
}

.header-banner {
  position: relative;
  background: linear-gradient(135deg, var(--color-info) 0%, var(--color-primary) 100%);
  color: var(--text-primary);
  padding: 50px 20px;
  text-align: center;
  overflow: hidden;
}

.banner-content {
  position: relative;
  z-index: 1;
}

.header-banner h1 {
  margin: 0 0 var(--spacing-md) 0;
  font-size: 36px;
  font-weight: bold;
}

.header-banner p {
  margin: 0;
  opacity: 0.9;
  font-size: 16px;
}

.banner-decoration {
  position: absolute;
  bottom: -50px;
  left: -50px;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  filter: blur(40px);
}

.cinemas-container {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--spacing-xl) var(--spacing-lg);
  min-height: 400px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-2xl);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top: 3px solid var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--spacing-md);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading span {
  color: var(--text-muted);
  font-size: 14px;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-2xl);
}

.empty-icon {
  font-size: 80px;
  margin-bottom: var(--spacing-lg);
  animation: float 3s ease-in-out infinite;
}

.empty p {
  color: var(--text-muted);
  font-size: 16px;
  margin: 0;
}

.cinemas-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.cinema-card {
  display: flex;
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-normal);
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
  border: 1px solid var(--border-color);
}

.cinema-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary);
}

.cinema-image-wrapper {
  position: relative;
  width: 220px;
  flex-shrink: 0;
  overflow: hidden;
}

.cinema-image {
  width: 100%;
  height: 160px;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.cinema-card:hover .cinema-image {
  transform: scale(1.1);
}

.cinema-image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to right, transparent, rgba(0, 0, 0, 0.3));
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.cinema-card:hover .cinema-image-overlay {
  opacity: 1;
}

.cinema-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  backdrop-filter: blur(10px);
  z-index: 2;
}

.cinema-badge.open {
  background: rgba(16, 185, 129, 0.9);
  color: var(--text-dark);
}

.cinema-badge.closed {
  background: rgba(156, 163, 175, 0.9);
  color: var(--text-dark);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.cinema-badge.open .status-dot {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.cinema-info {
  flex: 1;
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
}

.cinema-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.cinema-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.cinema-district {
  padding: 4px 12px;
  background: rgba(99, 102, 241, 0.15);
  border-radius: var(--radius-full);
  font-size: 12px;
  color: var(--color-primary-light);
}

.cinema-details {
  flex: 1;
}

.cinema-details p {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin: 0 0 var(--spacing-sm) 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.cinema-details p:last-child {
  margin-bottom: 0;
}

.icon {
  font-size: 14px;
  width: 18px;
}

.cinema-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-color);
}

.cinema-halls {
  font-size: 13px;
  color: var(--text-muted);
}

.cinema-action {
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 500;
  transition: all var(--transition-fast);
}

.cinema-card:hover .cinema-action {
  color: var(--color-primary-light);
  transform: translateX(4px);
}

@media (max-width: 600px) {
  .cinema-card {
    flex-direction: column;
  }
  
  .cinema-image-wrapper {
    width: 100%;
  }
  
  .cinema-image {
    height: 180px;
  }
  
  .cinema-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-sm);
  }

  .header-banner h1 {
    font-size: 28px;
  }
}
</style>