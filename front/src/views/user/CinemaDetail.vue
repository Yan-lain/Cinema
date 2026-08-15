<template>
  <div class="cinema-detail-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- 影院信息 -->
    <div v-else-if="cinema" class="content">
      <!-- 影院头部 -->
      <div class="cinema-header">
        <div class="cinema-image-wrapper">
          <img :src="cinema.image || 'https://via.placeholder.com/400x200'" :alt="cinema.name" class="cinema-image" />
          <div class="cinema-badge" :class="getCinemaStatusClass(cinema)">
            <span class="status-icon">{{ getCinemaStatusIcon(cinema) }}</span>
            {{ getCinemaStatusText(cinema) }}
          </div>
        </div>
        
        <div class="cinema-basic">
          <h1 class="cinema-name">{{ cinema.name }}</h1>
          <span class="cinema-district">{{ cinema.district }}</span>
        </div>
      </div>

      <!-- 详细地址 -->
      <div class="address-section">
        <div class="address-icon">📍</div>
        <div class="address-content">
          <span class="address-label">详细地址</span>
          <p class="address-text">{{ cinema.address }}</p>
        </div>
      </div>

      <!-- 场次安排 -->
      <div class="schedule-section">
        <div class="section-header">
          <h2 class="section-title">🎬 场次安排</h2>
        </div>

        <!-- 日期选择 -->
        <div class="date-tabs">
          <button
            v-for="date in availableDates"
            :key="date.dateStr"
            :class="['date-tab', { active: selectedDate === date.dateStr }]"
            @click="selectedDate = date.dateStr"
          >
            <span class="date-day">{{ date.day }}</span>
            <span class="date-week">{{ date.week }}</span>
          </button>
        </div>

        <!-- 电影列表 -->
        <div v-if="moviesWithSchedules.length === 0" class="empty-schedule">
          <span class="empty-icon">🎥</span>
          <p>当日暂无排片</p>
        </div>

        <div v-else class="movies-list">
          <div v-for="movie in moviesWithSchedules" :key="movie.id" class="movie-card">
            <div class="movie-poster-wrapper">
              <img :src="movie.poster || 'https://via.placeholder.com/100x150'" :alt="movie.title" class="movie-poster" />
            </div>
            
            <div class="movie-info">
              <h3 class="movie-title">{{ movie.title }}</h3>
              <p class="movie-meta">{{ movie.duration }} | {{ movie.genre }}</p>
              <div class="movie-rating">
                <span class="star">★</span>
                <span>{{ movie.rating }}</span>
              </div>
            </div>

            <div class="schedules-wrapper">
              <div class="schedules">
                <button
                  v-for="schedule in movie.schedules"
                  :key="schedule.id"
                  class="schedule-btn"
                  @click="selectSchedule(schedule)"
                >
                  <span class="schedule-time">{{ formatTime(schedule.showTime) }}</span>
                  <span class="schedule-hall">{{ schedule.hallNumber }}</span>
                  <span class="schedule-price">¥{{ schedule.price }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else class="error">
      <span class="error-icon">❌</span>
      <p>获取影院信息失败</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()

const cinema = ref(null)
const loading = ref(false)
const selectedDate = ref('')

// 日期列表
const availableDates = computed(() => {
  const dates = []
  const today = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  
  for (let i = 0; i < 7; i++) {
    const date = new Date(today)
    date.setDate(today.getDate() + i)
    
    // 使用本地时间格式，避免时区问题
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    
    dates.push({
      dateStr: `${year}-${month}-${day}`,
      day: `${date.getMonth() + 1}/${date.getDate()}`,
      week: weekDays[date.getDay()]
    })
  }
  
  return dates
})

// 电影列表（带场次）
const moviesWithSchedules = ref([])

onMounted(() => {
  // 默认选中今天
  if (availableDates.value.length > 0) {
    selectedDate.value = availableDates.value[0].dateStr
  }
  
  loadCinema()
  loadSchedules()
})

// 监听日期变化
import { watch } from 'vue'
watch(selectedDate, () => {
  loadSchedules()
})

const loadCinema = async () => {
  loading.value = true
  const cinemaId = route.query.cinemaId
  
  try {
    cinema.value = await request.get(`/cinemas/${cinemaId}`)
  } catch (error) {
    console.error('Load cinema error:', error)
  } finally {
    loading.value = false
  }
}

const loadSchedules = async () => {
  const cinemaId = route.query.cinemaId
  
  try {
    const schedules = await request.get('/admin/schedules', {
      params: { cinemaId }
    })
    console.log('当前选中日期:', selectedDate.value)
    console.log('后端返回的场次总数:', schedules?.length || 0)
    
    const filteredSchedules = schedules.filter(s => {
      if (s.status !== 'available') return false
      const scheduleDatePart = s.showTime.substring(0, 10)
      return scheduleDatePart === selectedDate.value
    })
    
    const movieMap = new Map()
    
    for (const schedule of filteredSchedules) {
      const movieId = schedule.movieId
      
      if (!movieMap.has(movieId)) {
        const movieData = await request.get(`/movies/${movieId}`)
        movieMap.set(movieId, {
          ...movieData,
          schedules: []
        })
      }
      
      const movieEntry = movieMap.get(movieId)
      if (movieEntry) {
        movieEntry.schedules.push({
          ...schedule,
          hallNumber: schedule.hallNumber || `厅${schedule.hallId}`
        })
      }
    }
    
    moviesWithSchedules.value = Array.from(movieMap.values())
  } catch (error) {
    console.error('Load schedules error:', error)
  }
}

const formatTime = (datetime) => {
  return datetime.substring(11, 16)
}

const selectSchedule = (schedule) => {
  // 确保获取到正确的电影标题（可能在movie对象中或schedule对象中）
  let title = '未知电影'
  if (schedule.movieTitle) {
    title = schedule.movieTitle
  } else if (schedule.movie && schedule.movie.title) {
    title = schedule.movie.title
  }
  
  router.push({
    path: '/seat',
    query: { 
      cinemaId: route.query.cinemaId,
      scheduleId: schedule.id.toString(),
      movieId: schedule.movieId.toString(),
      title: title,
      hallId: schedule.hallId.toString()
    }
  })
}

const goBack = () => {
  router.back()
}

const getCinemaStatus = (cinema) => {
  if (!cinema) return 'closed'
  
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
.cinema-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.back-bar {
  background: white;
  padding: 12px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: transparent;
  border: none;
  font-size: 16px;
  color: #333;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}

.back-btn:hover {
  background: #f5f5f5;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
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

.content {
  padding: 20px;
}

/* 影院头部 */
.cinema-header {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 20px;
}

.cinema-image-wrapper {
  position: relative;
}

.cinema-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.cinema-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  color: white;
  display: flex;
  align-items: center;
  gap: 6px;
}

.cinema-badge.open {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.cinema-badge.closed {
  background: #999;
}

.status-icon {
  font-size: 10px;
}

.cinema-basic {
  padding: 20px;
}

.cinema-name {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
}

.cinema-district {
  padding: 4px 12px;
  background: #f0f4ff;
  border-radius: 12px;
  font-size: 14px;
  color: #667eea;
}

/* 地址部分 */
.address-section {
  display: flex;
  gap: 12px;
  background: white;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.address-icon {
  font-size: 24px;
}

.address-content {
  flex: 1;
}

.address-label {
  font-size: 12px;
  color: #999;
}

.address-text {
  margin: 4px 0 0 0;
  font-size: 15px;
  color: #333;
  line-height: 1.5;
}

/* 场次部分 */
.schedule-section {
  background: white;
  border-radius: 16px;
  overflow: hidden;
}

.section-header {
  padding: 20px 20px 0 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

/* 日期选择 */
.date-tabs {
  display: flex;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  overflow-x: auto;
}

.date-tab {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 16px;
  background: #f8f9fa;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.date-tab:hover {
  background: #e9ecef;
}

.date-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.date-tab.active .date-day,
.date-tab.active .date-week {
  color: white;
}

.date-day {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.date-week {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.date-tab.active .date-week {
  color: rgba(255, 255, 255, 0.8);
}

/* 空状态 */
.empty-schedule {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-schedule p {
  color: #999;
  font-size: 16px;
  margin: 0;
}

/* 电影列表 */
.movies-list {
  padding: 20px;
}

.movie-card {
  display: flex;
  padding-bottom: 20px;
  border-bottom: 1px solid #f5f5f5;
}

.movie-card:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.movie-poster-wrapper {
  flex-shrink: 0;
  width: 80px;
}

.movie-poster {
  width: 100%;
  height: 110px;
  object-fit: cover;
  border-radius: 8px;
}

.movie-info {
  flex: 1;
  padding: 0 16px;
}

.movie-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.movie-meta {
  font-size: 13px;
  color: #999;
  margin: 0 0 8px 0;
}

.movie-rating {
  display: flex;
  align-items: center;
  gap: 4px;
}

.star {
  color: #f59e0b;
  font-size: 14px;
}

.movie-rating span:last-child {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.schedules-wrapper {
  flex-shrink: 0;
}

.schedules {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.schedule-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 14px;
  background: #f8f9fa;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.schedule-btn:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.schedule-btn:hover .schedule-time,
.schedule-btn:hover .schedule-hall,
.schedule-btn:hover .schedule-price {
  color: white;
}

.schedule-time {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.schedule-hall {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.schedule-price {
  font-size: 13px;
  font-weight: 600;
  color: #ef4444;
  margin-top: 4px;
}

.schedule-btn:hover .schedule-price {
  color: white;
}

/* 错误状态 */
.error {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100px 20px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error p {
  color: #999;
  font-size: 16px;
}

@media (max-width: 600px) {
  .cinema-image {
    height: 160px;
  }
  
  .cinema-name {
    font-size: 20px;
  }
  
  .movie-card {
    flex-direction: column;
    gap: 12px;
  }
  
  .movie-info {
    padding: 0;
  }
  
  .movie-poster-wrapper {
    width: 100%;
    height: 180px;
  }
  
  .movie-poster {
    width: 100%;
    height: 100%;
  }
  
  .schedules {
    flex-direction: row;
    flex-wrap: wrap;
  }
  
  .schedule-btn {
    flex-direction: row;
    gap: 8px;
    padding: 10px 16px;
  }
  
  .schedule-hall {
    margin-top: 0;
  }
}
</style>
