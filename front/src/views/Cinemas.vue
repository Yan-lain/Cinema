<template>
  <div class="cinemas-page">
    <div class="page-header">
      <h1>🎬 影院列表</h1>
      <p>选择一家影院，开启您的观影之旅</p>
    </div>

    <div class="cinemas-container">
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>
      
      <div v-else-if="cinemas.length === 0" class="empty">
        <span class="empty-icon">🏢</span>
        <p>暂无影院信息</p>
      </div>

      <div v-else class="cinemas-list">
        <div
          v-for="cinema in cinemas"
          :key="cinema.id"
          class="cinema-card"
          @click="goToDetail(cinema)"
        >
          <div class="cinema-image-wrapper">
            <img 
              :src="cinema.image || 'https://via.placeholder.com/300x200'" 
              :alt="cinema.name" 
              class="cinema-image"
            />
            <div class="cinema-badge" :class="getCinemaStatusClass(cinema)">
              <span class="status-icon">{{ getCinemaStatusIcon(cinema) }}</span>
              {{ getCinemaStatusText(cinema) }}
            </div>
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

const API_BASE_URL = '/api'

const router = useRouter()

const cinemas = ref([])
const loading = ref(false)

onMounted(() => {
  loadCinemas()
})

const loadCinemas = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/cinemas`)
    const data = await response.json()
    if (data.success) {
      cinemas.value = data.data
    }
  } catch (error) {
    console.error('Load cinemas error:', error)
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
  // 如果影院状态不是 active，直接返回休息中
  if (cinema.status !== 'active') {
    return 'closed'
  }
  
  // 如果没有设置营业时间，默认营业中
  if (!cinema.businessHours || cinema.businessHours.trim() === '') {
    return 'open'
  }
  
  // 解析营业时间（格式：09:00-22:00）
  const hours = cinema.businessHours.split('-')
  if (hours.length !== 2) {
    return 'open'
  }
  
  const [startStr, endStr] = hours
  const now = new Date()
  const currentMinutes = now.getHours() * 60 + now.getMinutes()
  
  // 解析开始时间
  const startMatch = startStr.trim().match(/(\d{1,2}):(\d{2})/)
  if (!startMatch) return 'open'
  const startMinutes = parseInt(startMatch[1]) * 60 + parseInt(startMatch[2])
  
  // 解析结束时间
  const endMatch = endStr.trim().match(/(\d{1,2}):(\d{2})/)
  if (!endMatch) return 'open'
  const endMinutes = parseInt(endMatch[1]) * 60 + parseInt(endMatch[2])
  
  // 判断当前时间是否在营业范围内
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

const getCinemaStatusIcon = (cinema) => {
  const status = getCinemaStatus(cinema)
  return status === 'open' ? '●' : '○'
}

const getCinemaStatusText = (cinema) => {
  const status = getCinemaStatus(cinema)
  return status === 'open' ? '营业中' : '休息中'
}
</script>

<style scoped>
.cinemas-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-header h1 {
  font-size: 36px;
  color: #333;
  margin: 0 0 12px 0;
}

.page-header p {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.cinemas-container {
  min-height: 400px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading span {
  color: #666;
  font-size: 14px;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty p {
  color: #999;
  font-size: 16px;
}

.cinemas-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.cinema-card {
  display: flex;
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s;
}

.cinema-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.cinema-image-wrapper {
  position: relative;
  width: 220px;
  flex-shrink: 0;
}

.cinema-image {
  width: 100%;
  height: 160px;
  object-fit: cover;
}

.cinema-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.cinema-badge.open {
  background: rgba(255, 255, 255, 0.95);
  color: #10b981;
}

.cinema-badge.closed {
  background: rgba(255, 255, 255, 0.95);
  color: #9ca3af;
}

.status-icon {
  font-size: 8px;
}

.cinema-badge.open .status-icon {
  color: #10b981;
}

.cinema-badge.closed .status-icon {
  color: #9ca3af;
}

.cinema-info {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.cinema-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.cinema-name {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.cinema-district {
  padding: 4px 10px;
  background: #f0f4ff;
  border-radius: 12px;
  font-size: 12px;
  color: #667eea;
}

.cinema-details {
  flex: 1;
}

.cinema-details p {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
}

.cinema-details p:last-child {
  margin-bottom: 0;
}

.icon {
  font-size: 14px;
  width: 18px;
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
    gap: 8px;
  }
}
</style>