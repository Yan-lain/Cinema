<template>
  <div class="movie-detail-page">
    <div class="movie-header">
      <div class="movie-main-content">
        <div class="movie-poster">
          <img :src="movie.poster" :alt="movie.title" />
        </div>
        <div class="movie-info">
          <h1>{{ movie.title }}</h1>
          <div class="movie-meta">
            <span class="rating">⭐ {{ movie.rating }}</span>
            <span class="genre">{{ movie.genre }}</span>
            <span class="duration">🕐 {{ movie.duration }}分钟</span>
          </div>
          <div class="movie-detail-row">
            <span class="label">导演:</span>
            <span class="value">{{ movie.director }}</span>
          </div>
          <div class="movie-detail-row">
            <span class="label">演员:</span>
            <span class="value">{{ movie.cast }}</span>
          </div>
          <div class="movie-detail-row">
            <span class="label">上映日期:</span>
            <span class="value">{{ formatDate(movie.releaseDate) }}</span>
          </div>
          <div class="movie-actions">
            <button
              :class="['action-btn', isFavorite ? 'active' : '']"
              @click="toggleFavorite"
            >
              {{ isFavorite ? '❤️ 已收藏' : '🤍 收藏' }}
            </button>
          </div>
        </div>
      </div>
      <div class="movie-desc-container">
        <p class="movie-desc">{{ movie.description }}</p>
      </div>
    </div>

    <!-- 放映场次 -->
      <div v-if="movie.status === 'showing'" class="showtimes-section">
        <h2 class="section-title">🎬 选择场次</h2>
        
        <div v-if="showtimesLoaded && cinemaSchedules.length === 0" class="empty-showtimes">
          <p>暂无排片信息</p>
        </div>
        
        <div v-for="cinemaSchedule in cinemaSchedules" :key="cinemaSchedule.cinema.id" class="cinema-card">
          <div class="cinema-header">
            <h3>{{ cinemaSchedule.cinema.name }}</h3>
            <p class="cinema-address">{{ cinemaSchedule.cinema.address }}</p>
          </div>
          
          <div class="halls-grid">
            <div v-for="hallSchedule in cinemaSchedule.halls" :key="hallSchedule.hall.id" class="hall-card">
              <div class="hall-header">
                <span class="hall-name">{{ hallSchedule.hall.hallNumber }}</span>
                <span class="hall-seats">{{ hallSchedule.hall.rows }}排{{ hallSchedule.hall.cols }}座</span>
              </div>
              <div class="showtimes-row">
                <button
                  v-for="schedule in hallSchedule.schedules"
                  :key="schedule.id"
                  :class="['showtime-btn', getScheduleStatusClass(schedule.showTime)]"
                  @click="goToSeatSelection(schedule)"
                >
                  <span class="time">{{ formatShowTime(schedule.showTime) }}</span>
                  <span class="price">¥{{ schedule.price }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="movie.status === 'showing'" class="comments-section">
        <h2 class="section-title">💬 评论区</h2>

      <div v-if="authStore.isAuthenticated" class="comment-form">
        <div class="rating-input">
          <label>评分:</label>
          <div class="stars">
            <span
              v-for="i in 5"
              :key="i"
              :class="['star', { active: i <= commentRating }]"
              @click="commentRating = i"
            >★</span>
          </div>
        </div>
        <textarea
          v-model="commentContent"
          placeholder="写下你的评论..."
          class="comment-textarea"
        ></textarea>
        <button class="submit-btn" @click="submitComment">发表评论</button>
      </div>
      <div v-else class="login-prompt">
        <p>请先 <span @click="showLoginModal = true" class="login-link">登录</span> 后发表评论</p>
      </div>

      <div class="comments-list">
        <div v-if="comments.length === 0" class="empty-comments">暂无评论，来抢沙发吧！</div>
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <span class="comment-user">{{ comment.username }}</span>
            <span class="comment-rating">⭐ {{ comment.rating }}</span>
            <span class="comment-date">{{ formatCommentDate(comment.createdAt) }}</span>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
        </div>
      </div>
    </div>

    <LoginModal v-if="showLoginModal" @close="showLoginModal = false" />
    
    <!-- 时间警告弹窗 -->
    <div v-if="showWarningModal" class="modal-overlay" @click="closeWarningModal">
      <div class="warning-modal" @click.stop>
        <div class="modal-icon">⚠️</div>
        <h3>距离电影开场时间不足半小时</h3>
        <p>请慎重考虑是否继续购票</p>
        <div class="modal-buttons">
          <button class="modal-btn cancel" @click="closeWarningModal">取消</button>
          <button class="modal-btn confirm" @click="confirmPurchase">继续购票</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginModal from '../components/LoginModal.vue'
const API_BASE_URL = '/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const showLoginModal = ref(false)
const showtimesLoaded = ref(false)
const cinemaSchedules = ref([])

const movieId = computed(() => route.query.movieId)

const movie = ref({
  id: null,
  title: '加载中...',
  poster: '',
  rating: '0',
  genre: '',
  duration: '',
  description: '',
  director: '',
  cast: '',
  releaseDate: null,
  status: ''
})

const favorites = ref([])
const browseHistory = ref([])
const comments = ref([])
const commentRating = ref(5)
const commentContent = ref('')

onMounted(() => {
  loadMovieData()
  loadFavorites()
  loadBrowseHistory()
  addToBrowseHistory()
  loadComments()
  loadShowtimes()
})

const loadMovieData = async () => {
  const id = parseInt(movieId.value)
  if (id) {
    try {
      const response = await fetch(`${API_BASE_URL}/movies/${id}`)
      const data = await response.json()
      if (data.success && data.data) {
        movie.value = {
          ...data.data,
          rating: data.data.rating || '0',
          duration: data.data.duration || '0',
          description: data.data.description || '',
          director: data.data.director || '',
          cast: data.data.cast || '',
          releaseDate: data.data.releaseDate || null,
          status: data.data.status || ''
        }
      }
    } catch (error) {
      console.error('Load movie data error:', error)
    }
  }
}

const loadComments = async () => {
  const id = parseInt(movieId.value)
  if (id) {
    try {
      const response = await fetch(`${API_BASE_URL}/comments/movie/${id}`)
      const data = await response.json()
      if (data.success) {
        comments.value = data.data
      }
    } catch (error) {
      console.error('Load comments error:', error)
    }
  }
}

const submitComment = async () => {
  if (!commentContent.value.trim()) {
    alert('请输入评论内容')
    return
  }
  if (!authStore.isAuthenticated || !authStore.user?.id) {
    showLoginModal.value = true
    return
  }

  try {
    const response = await fetch(`${API_BASE_URL}/comments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId: authStore.user.id,
        movieId: parseInt(movieId.value),
        rating: commentRating.value,
        content: commentContent.value.trim()
      })
    })
    const data = await response.json()
    if (data.success) {
      alert('评论发表成功')
      commentContent.value = ''
      commentRating.value = 5
      await loadComments()
    } else {
      alert('评论失败: ' + (data.message || '未知错误'))
    }
  } catch (error) {
    console.error('Submit comment error:', error)
    alert('网络错误: ' + error.message)
  }
}

const formatDate = (date) => {
  if (!date) return '未知'
  if (typeof date === 'string') {
    return date.split('T')[0]
  }
  return date
}

const formatCommentDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStorageKey = (key) => {
  const userId = authStore.user?.id || 'guest'
  return `${key}_${userId}`
}

const loadFavorites = async () => {
  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      const response = await fetch(`${API_BASE_URL}/favorite/list?userId=${authStore.user.id}&limit=50`)
      const data = await response.json()
      if (data.success) {
        favorites.value = data.data.map(item => ({
          recordId: item.id,
          id: item.movie_id,
          title: item.title,
          poster: item.poster,
          rating: item.rating
        }))
        return
      }
    } catch (error) {
      console.error('Load favorites from server error:', error)
    }
  }

  const stored = localStorage.getItem(getStorageKey('movieFavorites'))
  favorites.value = stored ? JSON.parse(stored) : []
}

const saveFavorites = () => {
  localStorage.setItem(getStorageKey('movieFavorites'), JSON.stringify(favorites.value))
}

const isFavorite = computed(() => {
  return favorites.value.some(f => f.id === movie.value.id)
})

const toggleFavorite = async () => {
  if (!authStore.isAuthenticated) {
    showLoginModal.value = true
    return
  }

  if (isFavorite.value) {
    const item = favorites.value.find(f => f.id === movie.value.id)
    if (item) {
      if (item.recordId) {
        try {
          const response = await fetch(`${API_BASE_URL}/favorite/delete/${item.recordId}`, {
            method: 'DELETE'
          })
          const data = await response.json()
          if (data.success) {
            favorites.value = favorites.value.filter(f => f.id !== movie.value.id)
          }
        } catch (error) {
          console.error('Remove favorite error:', error)
        }
      } else {
        favorites.value = favorites.value.filter(f => f.id !== movie.value.id)
        saveFavorites()
      }
    }
  } else {
    if (authStore.user?.id) {
      try {
        const response = await fetch(`${API_BASE_URL}/favorite/add`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userId: authStore.user.id,
            movieId: movie.value.id
          })
        })
        const data = await response.json()
        if (data.success) {
          await loadFavorites()
        }
      } catch (error) {
        console.error('Add favorite error:', error)
      }
    } else {
      favorites.value.push({
        id: movie.value.id,
        title: movie.value.title,
        poster: movie.value.poster,
        rating: movie.value.rating,
        addedAt: new Date().toISOString()
      })
      saveFavorites()
    }
  }
}

const loadBrowseHistory = async () => {
  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      const response = await fetch(`${API_BASE_URL}/browse/list?userId=${authStore.user.id}&limit=20`)
      const data = await response.json()
      if (data.success) {
        browseHistory.value = data.data.map(item => ({
          id: item.movieId,
          title: item.title,
          poster: item.poster,
          rating: item.rating,
          viewedAt: item.createdAt
        }))
        return
      }
    } catch (error) {
      console.error('Load browse history from server error:', error)
    }
  }

  const stored = localStorage.getItem(getStorageKey('browseHistory'))
  browseHistory.value = stored ? JSON.parse(stored) : []
}

const saveBrowseHistory = () => {
  localStorage.setItem(getStorageKey('browseHistory'), JSON.stringify(browseHistory.value))
}

const addToBrowseHistory = async () => {
  if (!movie.value.id) return

  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      await fetch(`${API_BASE_URL}/browse/add`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId: authStore.user.id,
          movieId: movie.value.id
        })
      })
    } catch (error) {
      console.error('Add browse history to server error:', error)
    }
  }

  const existing = browseHistory.value.findIndex(h => h.id === movie.value.id)
  if (existing > -1) {
    browseHistory.value.splice(existing, 1)
  }

  browseHistory.value.unshift({
    id: movie.value.id,
    title: movie.value.title,
    poster: movie.value.poster,
    rating: movie.value.rating,
    viewedAt: new Date().toISOString()
  })

  if (browseHistory.value.length > 20) {
    browseHistory.value = browseHistory.value.slice(0, 20)
  }

  saveBrowseHistory()
}

const handleBuyTicket = () => {
  router.push({
    path: '/seat',
    query: { movieId: movie.value.id, title: movie.value.title }
  })
}

const loadShowtimes = async () => {
  const id = parseInt(movieId.value)
  if (!id) return
  
  try {
    // 获取所有影院
    const cinemasResponse = await fetch(`${API_BASE_URL}/cinemas`)
    const cinemasData = await cinemasResponse.json()
    if (!cinemasData.success) return
    
    const cinemas = cinemasData.data
    // 使用 Map 去重，确保每个影院只显示一次
    const cinemaMap = new Map()
    
    // 为每个影院获取场次
    for (const cinema of cinemas) {
      const schedulesResponse = await fetch(`${API_BASE_URL}/admin/schedules?cinemaId=${cinema.id}`)
      const schedulesData = await schedulesResponse.json()
      
      if (schedulesData.success) {
        // 过滤当前电影的场次并按放映厅分组
        const movieSchedules = schedulesData.data.filter(s => s.movieId === id && s.status === 'available')
        
        if (movieSchedules.length > 0) {
          const hallsMap = new Map()
          
          for (const schedule of movieSchedules) {
            if (!hallsMap.has(schedule.hallId)) {
              hallsMap.set(schedule.hallId, {
                hall: { id: schedule.hallId, hallNumber: schedule.hallNumber, rows: schedule.rows, cols: schedule.cols },
                schedules: []
              })
            }
            hallsMap.get(schedule.hallId).schedules.push(schedule)
          }
          
          // 使用影院ID作为key去重
          cinemaMap.set(cinema.id, {
            cinema: cinema,
            halls: Array.from(hallsMap.values())
          })
        }
      }
    }
    
    // 将去重后的结果赋值给 cinemaSchedules
    cinemaSchedules.value = Array.from(cinemaMap.values())
  } catch (error) {
    console.error('Load showtimes error:', error)
  } finally {
    showtimesLoaded.value = true
  }
}

const formatShowTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 弹窗状态
const showWarningModal = ref(false)
const selectedSchedule = ref(null)
let checkExpiredInterval = null

// 计算距离开场的剩余时间（分钟）
const getMinutesUntilShow = (showTime) => {
  const now = new Date()
  const show = new Date(showTime)
  const diff = show - now
  return Math.floor(diff / 60000)
}

// 检查场次是否已过期
const isScheduleExpired = (showTime) => {
  return getMinutesUntilShow(showTime) < 0
}

// 检查场次是否即将开始（少于30分钟）
const isScheduleNear = (showTime) => {
  const minutes = getMinutesUntilShow(showTime)
  return minutes >= 0 && minutes < 30
}

// 获取场次状态样式
const getScheduleStatusClass = (showTime) => {
  if (isScheduleExpired(showTime)) return 'expired'
  if (isScheduleNear(showTime)) return 'near'
  return ''
}

// 购票前检查时间
const goToSeatSelection = (schedule) => {
  const minutesUntil = getMinutesUntilShow(schedule.showTime)
  
  // 如果已经过期，提示用户
  if (minutesUntil < 0) {
    alert('该场次已过期，请选择其他场次')
    return
  }
  
  // 如果不足30分钟，显示确认弹窗
  if (minutesUntil < 30) {
    selectedSchedule.value = schedule
    showWarningModal.value = true
    return
  }
  
  // 正常跳转
  navigateToSeatSelection(schedule)
}

// 确认跳转选座页面
const navigateToSeatSelection = (schedule) => {
  router.push({
    path: '/seat',
    query: { 
      movieId: movie.value.id, 
      title: movie.value.title,
      cinemaId: schedule.cinemaId,
      hallId: schedule.hallId,
      scheduleId: schedule.id
    }
  })
}

// 关闭警告弹窗
const closeWarningModal = () => {
  showWarningModal.value = false
  selectedSchedule.value = null
}

// 确认购票（即使时间不足30分钟）
const confirmPurchase = () => {
  if (selectedSchedule.value) {
    navigateToSeatSelection(selectedSchedule.value)
  }
  closeWarningModal()
}

// 定时检查场次是否过期
const startExpiredCheck = () => {
  checkExpiredInterval = setInterval(() => {
    // 遍历所有场次，检查是否有过期的
    let hasExpired = false
    
    for (const cinemaSchedule of cinemaSchedules.value) {
      for (const hallSchedule of cinemaSchedule.halls) {
        for (const schedule of hallSchedule.schedules) {
          if (isScheduleExpired(schedule.showTime)) {
            hasExpired = true
            break
          }
        }
        if (hasExpired) break
      }
      if (hasExpired) break
    }
    
    // 如果有场次过期，重新加载场次数据
    if (hasExpired) {
      console.log('检测到过期场次，重新加载...')
      loadShowtimes()
    }
  }, 60000) // 每分钟检查一次
}

// 停止定时检查
const stopExpiredCheck = () => {
  if (checkExpiredInterval) {
    clearInterval(checkExpiredInterval)
    checkExpiredInterval = null
  }
}

// 在组件挂载时启动定时检查
onMounted(() => {
  loadMovieData()
  loadFavorites()
  loadBrowseHistory()
  addToBrowseHistory()
  loadComments()
  loadShowtimes()
  startExpiredCheck()
})

onUnmounted(() => {
  stopExpiredCheck()
})
</script>

<style scoped>
.movie-detail-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 30px 20px;
}

.movie-header {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 40px;
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.movie-main-content {
  display: flex;
  gap: 40px;
}

.movie-poster {
  flex-shrink: 0;
  width: 280px;
}

.movie-poster img {
  width: 100%;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}

.movie-info {
  flex: 1;
  min-width: 0;
}

.movie-desc-container {
  width: 100%;
}

.movie-info h1 {
  font-size: 32px;
  color: #333;
  margin: 0 0 16px 0;
}

.movie-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
}

.movie-meta span {
  padding: 6px 14px;
  background: #f5f5f5;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
}

.movie-meta .rating {
  background: linear-gradient(135deg, #ffd700, #ffb800);
  color: #333;
  font-weight: bold;
}

.movie-detail-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
}

.movie-detail-row .label {
  color: #999;
  flex-shrink: 0;
}

.movie-detail-row .value {
  color: #666;
}

.movie-desc {
  font-size: 15px;
  color: #666;
  line-height: 1.8;
  margin: 16px 0 24px 0;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 10px;
}

.movie-actions {
  display: flex;
  gap: 16px;
}

.action-btn {
  padding: 14px 28px;
  border: 2px solid #ddd;
  background: white;
  border-radius: 30px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
}

.action-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.action-btn:not(.primary):hover {
  border-color: #667eea;
  color: #667eea;
}

.action-btn.active {
  border-color: #e74c3c;
  color: #e74c3c;
  background: #fef2f2;
}

.comments-section {
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.section-title {
  margin: 0 0 24px 0;
  font-size: 22px;
  color: #333;
}

.comment-form {
  margin-bottom: 30px;
  padding-bottom: 30px;
  border-bottom: 1px solid #eee;
}

.rating-input {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.rating-input label {
  font-size: 15px;
  color: #666;
}

.stars {
  display: flex;
  gap: 4px;
}

.star {
  font-size: 28px;
  color: #ddd;
  cursor: pointer;
  transition: color 0.2s;
}

.star.active {
  color: #ffd700;
}

.comment-textarea {
  width: 100%;
  min-height: 100px;
  padding: 14px;
  border: 2px solid #eee;
  border-radius: 10px;
  font-size: 15px;
  resize: vertical;
  box-sizing: border-box;
  margin-bottom: 16px;
}

.comment-textarea:focus {
  outline: none;
  border-color: #667eea;
}

.submit-btn {
  padding: 12px 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 24px;
  color: white;
  font-size: 15px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.login-prompt {
  margin-bottom: 30px;
  padding-bottom: 30px;
  border-bottom: 1px solid #eee;
  text-align: center;
}

.login-prompt p {
  margin: 0;
  color: #666;
  font-size: 15px;
}

.login-link {
  color: #667eea;
  cursor: pointer;
  font-weight: 600;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.empty-comments {
  text-align: center;
  color: #999;
  padding: 40px;
}

.comment-item {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
}

.comment-header {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  align-items: center;
}

.comment-user {
  font-weight: 600;
  color: #333;
  font-size: 15px;
}

.comment-rating {
  color: #ffd700;
  font-weight: bold;
}

.comment-date {
  color: #999;
  font-size: 13px;
}

.comment-content {
  color: #666;
  line-height: 1.7;
  font-size: 14px;
}

/* 放映场次样式 */
.showtimes-section {
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  margin-bottom: 30px;
}

.empty-showtimes {
  text-align: center;
  padding: 40px;
  color: #999;
}

.cinema-card {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.cinema-card:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.cinema-header h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #333;
}

.cinema-address {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #999;
}

.halls-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.hall-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px;
}

.hall-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.hall-name {
  font-weight: 600;
  color: #333;
  font-size: 15px;
}

.hall-seats {
  font-size: 13px;
  color: #999;
}

.showtimes-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.showtime-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 16px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 80px;
}

.showtime-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.showtime-btn .time {
  font-size: 14px;
  font-weight: 500;
}

.showtime-btn .price {
  font-size: 12px;
  margin-top: 4px;
  opacity: 0.8;
}

/* 场次状态样式 */
.showtime-btn.near {
  border-color: #f59e0b;
  background: #fffbeb;
}

.showtime-btn.near:hover {
  border-color: #d97706;
  color: #d97706;
}

.showtime-btn.expired {
  border-color: #ddd;
  background: #f5f5f5;
  cursor: not-allowed;
  opacity: 0.6;
}

.showtime-btn.expired:hover {
  border-color: #ddd;
  color: inherit;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.warning-modal {
  background: white;
  border-radius: 16px;
  padding: 32px;
  max-width: 400px;
  width: 90%;
  text-align: center;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.warning-modal h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #333;
}

.warning-modal p {
  margin: 0 0 24px 0;
  color: #666;
  font-size: 14px;
}

.modal-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.modal-btn {
  padding: 12px 28px;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.modal-btn.cancel {
  background: #f5f5f5;
  color: #666;
}

.modal-btn.cancel:hover {
  background: #eee;
}

.modal-btn.confirm {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.modal-btn.confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
}

@media (max-width: 768px) {
  .movie-header {
    flex-direction: column;
    align-items: center;
  }

  .movie-poster {
    width: 200px;
  }

  .movie-info {
    text-align: center;
  }

  .movie-meta {
    justify-content: center;
  }

  .movie-actions {
    justify-content: center;
  }

  .movie-detail-row {
    justify-content: center;
  }
}
</style>
