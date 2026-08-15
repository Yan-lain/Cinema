<template>
  <div class="movie-detail-page">
    <div class="movie-header">
      <div class="movie-header-bg"></div>
      <div class="movie-main-content">
        <div class="movie-poster-wrapper">
          <div class="movie-poster">
            <img :src="movie.poster" :alt="movie.title" />
            <div class="poster-overlay"></div>
          </div>
          <div class="movie-rating-badge">{{ movie.rating }}</div>
        </div>
        <div class="movie-info">
          <h1>{{ movie.title }}</h1>
          <div class="movie-meta">
            <span class="meta-item rating">⭐ {{ movie.rating }}</span>
            <span class="meta-item genre">{{ movie.genre }}</span>
            <span class="meta-item duration">🕐 {{ movie.duration }}分钟</span>
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

    <div v-if="movie.status === 'showing'" class="showtimes-section">
      <h2 class="section-title">🎬 选择场次</h2>
      
      <div v-if="showtimesLoaded && cinemaSchedules.length === 0" class="empty-showtimes">
        <span class="empty-icon">📅</span>
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
        <div v-if="comments.length === 0" class="empty-comments">
          <span class="empty-icon">💬</span>
          <p>暂无评论，来抢沙发吧！</p>
        </div>
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

    <LoginModal :show="showLoginModal" @close="showLoginModal = false" />
    
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
import { useAuthStore } from '@/stores/auth'
import LoginModal from '@/components/user/LoginModal.vue'
import request from '@/utils/request'

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
  startExpiredCheck()
})

onUnmounted(() => {
  stopExpiredCheck()
})

const loadMovieData = async () => {
  const id = parseInt(movieId.value)
  if (id) {
    try {
      const data = await request.get(`/movies/${id}`)
      movie.value = {
        ...data,
        rating: data.rating || '0',
        duration: data.duration || '0',
        description: data.description || '',
        director: data.director || '',
        cast: data.cast || '',
        releaseDate: data.releaseDate || null,
        status: data.status || ''
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
      comments.value = await request.get(`/comments/movie/${id}`)
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
    await request.post('/comments', {
      userId: authStore.user.id,
      movieId: parseInt(movieId.value),
      rating: commentRating.value,
      content: commentContent.value.trim()
    })
    alert('评论发表成功')
    commentContent.value = ''
    commentRating.value = 5
    await loadComments()
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
      const data = await request.get('/favorite/list', {
        params: { userId: authStore.user.id, limit: 50 }
      })
      favorites.value = data.map(item => ({
        recordId: item.id,
        id: item.movie_id,
        title: item.title,
        poster: item.poster,
        rating: item.rating
      }))
      return
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
          await request.delete(`/favorite/delete/${item.recordId}`)
          favorites.value = favorites.value.filter(f => f.id !== movie.value.id)
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
        await request.post('/favorite/add', {
          userId: authStore.user.id,
          movieId: movie.value.id
        })
        await loadFavorites()
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
      const data = await request.get('/browse/list', {
        params: { userId: authStore.user.id, limit: 20 }
      })
      browseHistory.value = data.map(item => ({
        id: item.movieId,
        title: item.title,
        poster: item.poster,
        rating: item.rating,
        viewedAt: item.createdAt
      }))
      return
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
      await request.post('/browse/add', {
        userId: authStore.user.id,
        movieId: movie.value.id
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

const loadShowtimes = async () => {
  const id = parseInt(movieId.value)
  if (!id) return
  
  try {
    const cinemas = await request.get('/cinemas')
    const cinemaMap = new Map()
    
    for (const cinema of cinemas) {
      const schedules = await request.get('/admin/schedules', {
        params: { cinemaId: cinema.id }
      })
      
      const movieSchedules = schedules.filter(s => s.movieId === id && s.status === 'available')
      
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
        
        cinemaMap.set(cinema.id, {
          cinema: cinema,
          halls: Array.from(hallsMap.values())
        })
      }
    }
    
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

const showWarningModal = ref(false)
const selectedSchedule = ref(null)
let checkExpiredInterval = null

const getMinutesUntilShow = (showTime) => {
  const now = new Date()
  const show = new Date(showTime)
  const diff = show - now
  return Math.floor(diff / 60000)
}

const isScheduleExpired = (showTime) => {
  return getMinutesUntilShow(showTime) < 0
}

const isScheduleNear = (showTime) => {
  const minutes = getMinutesUntilShow(showTime)
  return minutes >= 0 && minutes < 30
}

const getScheduleStatusClass = (showTime) => {
  if (isScheduleExpired(showTime)) return 'expired'
  if (isScheduleNear(showTime)) return 'near'
  return ''
}

const goToSeatSelection = (schedule) => {
  const minutesUntil = getMinutesUntilShow(schedule.showTime)
  
  if (minutesUntil < 0) {
    alert('该场次已过期，请选择其他场次')
    return
  }
  
  if (minutesUntil < 30) {
    selectedSchedule.value = schedule
    showWarningModal.value = true
    return
  }
  
  navigateToSeatSelection(schedule)
}

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

const closeWarningModal = () => {
  showWarningModal.value = false
  selectedSchedule.value = null
}

const confirmPurchase = () => {
  if (selectedSchedule.value) {
    navigateToSeatSelection(selectedSchedule.value)
  }
  closeWarningModal()
}

const startExpiredCheck = () => {
  checkExpiredInterval = setInterval(() => {
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
    
    if (hasExpired) {
      console.log('检测到过期场次，重新加载...')
      loadShowtimes()
    }
  }, 60000)
}

const stopExpiredCheck = () => {
  if (checkExpiredInterval) {
    clearInterval(checkExpiredInterval)
    checkExpiredInterval = null
  }
}
</script>

<style scoped>
.movie-detail-page {
  min-height: 100vh;
  background: var(--bg-surface);
}

.movie-header {
  position: relative;
  margin-bottom: var(--spacing-xl);
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  margin: 0 var(--spacing-lg) var(--spacing-xl);
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.movie-header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 200px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  opacity: 0.1;
}

.movie-main-content {
  display: flex;
  gap: var(--spacing-xl);
  position: relative;
  z-index: 1;
}

.movie-poster-wrapper {
  position: relative;
  flex-shrink: 0;
  width: 280px;
}

.movie-poster {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.movie-poster img {
  width: 100%;
  display: block;
}

.poster-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.3) 0%, transparent 100%);
}

.movie-rating-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-light));
  color: var(--text-dark);
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: bold;
  z-index: 2;
}

.movie-info {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.movie-info h1 {
  font-size: 32px;
  color: var(--text-primary);
  margin: 0 0 var(--spacing-md) 0;
}

.movie-meta {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  flex-wrap: wrap;
  align-items: center;
}

.meta-item {
  padding: 6px 14px;
  background: var(--bg-secondary);
  border-radius: var(--radius-full);
  font-size: 14px;
  color: var(--text-secondary);
}

.meta-item.rating {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(245, 158, 11, 0.1));
  color: var(--color-accent-light);
  font-weight: bold;
}

.meta-item.genre {
  background: rgba(99, 102, 241, 0.15);
  color: var(--color-primary-light);
}

.movie-detail-row {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
  font-size: 14px;
}

.movie-detail-row .label {
  color: var(--text-muted);
  flex-shrink: 0;
}

.movie-detail-row .value {
  color: var(--text-secondary);
}

.movie-desc-container {
  position: relative;
  z-index: 1;
}

.movie-desc {
  font-size: 15px;
  color: var(--text-secondary);
  line-height: 1.8;
  margin: var(--spacing-lg) 0 var(--spacing-xl) 0;
  padding: var(--spacing-lg);
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
}

.movie-actions {
  display: flex;
  gap: var(--spacing-md);
  margin-top: var(--spacing-lg);
}

.action-btn {
  padding: 12px 24px;
  border: 2px solid var(--border-color);
  background: var(--bg-secondary);
  border-radius: var(--radius-full);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--text-secondary);
}

.action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.action-btn.active {
  border-color: var(--color-error);
  color: var(--color-error);
  background: rgba(239, 68, 68, 0.1);
}

.section-title {
  margin: 0 0 var(--spacing-lg) 0;
  font-size: 22px;
  color: var(--text-primary);
}

.comments-section {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.comment-form {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
  border: 1px solid var(--border-color);
}

.rating-input {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.rating-input label {
  font-size: 15px;
  color: var(--text-secondary);
}

.stars {
  display: flex;
  gap: var(--spacing-xs);
}

.star {
  font-size: 28px;
  color: var(--border-color);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.star.active {
  color: var(--color-accent);
}

.star:hover {
  transform: scale(1.2);
}

.comment-textarea {
  width: 100%;
  min-height: 100px;
  padding: var(--spacing-md);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  font-size: 14px;
  color: var(--text-primary);
  resize: vertical;
  box-sizing: border-box;
  margin-bottom: var(--spacing-md);
  font-family: inherit;
}

.comment-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.comment-textarea::placeholder {
  color: var(--text-muted);
}

.submit-btn {
  padding: 12px 32px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
}

.login-prompt {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
  text-align: center;
  border: 1px solid var(--border-color);
}

.login-prompt p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 15px;
}

.login-link {
  color: var(--color-primary-light);
  cursor: pointer;
  font-weight: 600;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.empty-comments {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-2xl);
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-color);
}

.empty-comments .empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
}

.empty-comments p {
  margin: 0;
  color: var(--text-muted);
}

.comment-item {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  border: 1px solid var(--border-color);
}

.comment-header {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
  align-items: center;
}

.comment-user {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
}

.comment-rating {
  color: var(--color-accent);
  font-weight: bold;
  font-size: 13px;
}

.comment-date {
  color: var(--text-muted);
  font-size: 13px;
}

.comment-content {
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: 14px;
}

.showtimes-section {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.empty-showtimes {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-2xl);
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-color);
}

.empty-showtimes .empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
}

.empty-showtimes p {
  margin: 0;
  color: var(--text-muted);
}

.cinema-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
  border: 1px solid var(--border-color);
}

.cinema-header h3 {
  margin: 0 0 var(--spacing-xs) 0;
  font-size: 18px;
  color: var(--text-primary);
}

.cinema-address {
  margin: 0 0 var(--spacing-lg) 0;
  font-size: 14px;
  color: var(--text-muted);
}

.halls-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-md);
}

.hall-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
}

.hall-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.hall-name {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
}

.hall-seats {
  font-size: 12px;
  color: var(--text-muted);
}

.showtimes-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.showtime-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  min-width: 90px;
}

.showtime-btn:hover:not(.expired) {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(99, 102, 241, 0.1);
}

.showtime-btn .time {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.showtime-btn .price {
  font-size: 13px;
  margin-top: var(--spacing-xs);
  color: var(--color-secondary);
  font-weight: 600;
}

.showtime-btn.near {
  border-color: var(--color-accent);
  background: rgba(245, 158, 11, 0.1);
}

.showtime-btn.near:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.showtime-btn.expired {
  border-color: var(--border-color);
  background: var(--bg-secondary);
  cursor: not-allowed;
  opacity: 0.5;
}

.showtime-btn.expired:hover {
  border-color: var(--border-color);
  color: inherit;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.warning-modal {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  max-width: 400px;
  width: 90%;
  text-align: center;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-color);
}

.modal-icon {
  font-size: 56px;
  margin-bottom: var(--spacing-md);
}

.warning-modal h3 {
  margin: 0 0 var(--spacing-sm) 0;
  font-size: 18px;
  color: var(--text-primary);
}

.warning-modal p {
  margin: 0 0 var(--spacing-xl) 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.modal-buttons {
  display: flex;
  gap: var(--spacing-md);
  justify-content: center;
}

.modal-btn {
  padding: 12px 28px;
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
}

.modal-btn.cancel {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

.modal-btn.cancel:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.modal-btn.confirm {
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
  color: var(--text-dark);
}

.modal-btn.confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
}

@media (max-width: 768px) {
  .movie-main-content {
    flex-direction: column;
    align-items: center;
  }

  .movie-poster-wrapper {
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

  .movie-info h1 {
    font-size: 24px;
  }
}
</style>