<template>
  <div class="movies-page">
    <div class="header-banner">
      <div class="banner-content">
        <h1>🎬 电影</h1>
        <p>探索精彩电影世界</p>
      </div>
      <div class="banner-decoration"></div>
    </div>

    <div class="content-wrapper">
      <div class="filter-bar">
        <div class="filter-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            class="filter-tab"
            :class="{ active: selectedStatus === tab.value }"
            @click="selectedStatus = tab.value"
          >
            <span>{{ tab.icon }}</span>
            <span>{{ tab.label }}</span>
          </button>
        </div>

        <div class="filter-group-row">
          <div class="filter-group">
            <label>类型：</label>
            <select v-model="selectedGenre" class="filter-select">
              <option value="">全部类型</option>
              <option v-for="genre in genreList" :key="genre" :value="genre">{{ genre }}</option>
            </select>
          </div>

          <div class="filter-group">
            <label>评分：</label>
            <select v-model="selectedRating" class="filter-select">
              <option value="">全部评分</option>
              <option value="9">9分以上</option>
              <option value="8">8分以上</option>
              <option value="7">7分以上</option>
              <option value="6">6分以上</option>
            </select>
          </div>

          <div class="filter-group">
            <label>上映时间：</label>
            <select v-model="selectedTime" class="filter-select">
              <option value="">全部时间</option>
              <option value="2024">2024年</option>
              <option value="2023">2023年</option>
              <option value="2022">2022年</option>
              <option value="older">更早</option>
            </select>
          </div>

          <button class="reset-btn" @click="resetFilters">
            <span>↺</span>
            <span>重置筛选</span>
          </button>
        </div>
      </div>

      <div class="result-info">
        共找到 <span class="count">{{ filteredMovies.length }}</span> 部电影
      </div>

      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      <div v-else-if="filteredMovies.length === 0" class="empty">
        <span class="empty-icon">📭</span>
        <p>暂无符合条件的电影</p>
        <button class="reset-btn" @click="resetFilters">重置筛选</button>
      </div>
      <div v-else class="movies-grid">
        <div
          v-for="(movie, index) in filteredMovies"
          :key="movie.id"
          class="movie-card"
          :style="{ animationDelay: `${index * 50}ms` }"
          @click="goToDetail(movie)"
        >
          <div class="movie-poster-wrapper">
            <div class="movie-poster">
              <img :src="movie.poster" :alt="movie.title" />
              <div class="movie-rating">{{ movie.rating }}</div>
              <div v-if="movie.status === 'upcoming'" class="movie-badge upcoming">即将上映</div>
              <div v-else class="movie-badge showing">热映中</div>
              <div class="movie-overlay"></div>
            </div>
            <button class="watch-btn">▶</button>
          </div>
          <div class="movie-info">
            <h3>{{ movie.title }}</h3>
            <p class="movie-genre">{{ movie.genre }}</p>
            <div class="movie-meta">
              <span class="meta-item">{{ movie.duration }}分钟</span>
              <span class="meta-divider">|</span>
              <span class="meta-item">{{ movie.director }}</span>
            </div>
            <p class="movie-release" v-if="movie.releaseDate">
              上映：{{ formatDate(movie.releaseDate) }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const router = useRouter()
const authStore = useAuthStore()

const movies = ref([])
const loading = ref(true)
const selectedGenre = ref('')
const selectedRating = ref('')
const selectedTime = ref('')
const selectedStatus = ref('')

const statusTabs = [ 
  { value: '', label: '全部', icon: '📋' },
  { value: 'showing', label: '热映中', icon: '🔥' },
  { value: 'upcoming', label: '即将上映', icon: '🎬' }
 
]

const genreList = ['动作', '喜剧', '剧情', '科幻', '爱情', '动画', '悬疑', '恐怖', '冒险', '奇幻']

const filteredMovies = computed(() => {
  //...是数组的扩展运算符，用于创建一个新数组，包含原始数组的所有元素
  let result = [...movies.value]

  if (selectedStatus.value) {
    result = result.filter(m => m.status === selectedStatus.value)
  }

  if (selectedGenre.value) {
    result = result.filter(m => m.genre && m.genre.includes(selectedGenre.value))
  }

  if (selectedRating.value) {
    const minRating = parseFloat(selectedRating.value)
    result = result.filter(m => parseFloat(m.rating) >= minRating)
  }

  if (selectedTime.value) {
    result = result.filter(m => {
      if (!m.releaseDate) return false
      const year = new Date(m.releaseDate).getFullYear()
      if (selectedTime.value === 'older') {
        return year < 2022
      }
      return year === parseInt(selectedTime.value)
    })
  }

  return result
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const resetFilters = () => {
  selectedGenre.value = ''
  selectedRating.value = ''
  selectedTime.value = ''
}

const fetchMovies = async () => {
  loading.value = true
  try {
    movies.value = await request.get('/movies')
    console.log(movies.value)
  } catch (error) {
    console.error('获取电影列表失败:', error)
  } finally {
    loading.value = false
  }
}

const goToDetail = async (movie) => {
  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      await request.post('/browse/add', {
        userId: authStore.user.id,
        movieId: movie.id
      })
    } catch (error) {
      console.error('添加浏览记录失败:', error)
    }
  }
  router.push({ path: '/movie', query: { movieId: movie.id } })
}

onMounted(() => {
  fetchMovies()
})
</script>

<style scoped>
.movies-page {
  min-height: 100vh;
  background: var(--bg-surface);
}

.header-banner {
  position: relative;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
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
  top: -50px;
  right: -50px;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  filter: blur(40px);
}

.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--spacing-xl) var(--spacing-lg);
}

.filter-bar {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  border: 1px solid var(--border-color);
}

.filter-tabs {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--bg-secondary);
  border: none;
  border-radius: var(--radius-lg);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.filter-tab:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.filter-tab.active {
  background: var(--color-primary);
  color: var(--text-primary);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.filter-group-row {
  display: flex;
  gap: var(--spacing-lg);
  align-items: center;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.filter-group label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.filter-select {
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
  min-width: 120px;
  transition: all var(--transition-fast);
}

.filter-select:hover {
  border-color: var(--color-primary);
}

.filter-select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.filter-select option {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.reset-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-left: auto;
}

.reset-btn:hover {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--text-primary);
}

.result-info {
  margin-bottom: var(--spacing-lg);
  font-size: 14px;
  color: var(--text-secondary);
}

.result-info .count {
  color: var(--color-primary-light);
  font-weight: bold;
  font-size: 18px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
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
  font-size: 64px;
  margin-bottom: var(--spacing-lg);
}

.empty p {
  margin: 0 0 var(--spacing-lg) 0;
  color: var(--text-muted);
  font-size: 16px;
}

.movies-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--spacing-lg);
}

.movie-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-normal);
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
  border: 1px solid var(--border-color);
}

.movie-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary);
}

.movie-poster-wrapper {
  position: relative;
  overflow: hidden;
}

.movie-poster {
  position: relative;
}

.movie-poster img {
  width: 100%;
  height: 280px;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.movie-card:hover .movie-poster img {
  transform: scale(1.1);
}

.movie-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8) 0%, transparent 50%);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.movie-card:hover .movie-overlay {
  opacity: 1;
}

.movie-rating {
  position: absolute;
  top: 10px;
  right: 10px;
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-light));
  color: var(--text-dark);
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: bold;
  z-index: 2;
}

.movie-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  z-index: 2;
}

.movie-badge.upcoming {
  background: linear-gradient(135deg, var(--color-success), #34d399);
  color: var(--text-dark);
}

.movie-badge.showing {
  background: linear-gradient(135deg, var(--color-error), var(--color-accent));
  color: var(--text-primary);
}

.watch-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) scale(0);
  width: 50px;
  height: 50px;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  border-radius: 50%;
  font-size: 20px;
  color: var(--color-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
}

.movie-card:hover .watch-btn {
  transform: translate(-50%, -50%) scale(1);
}

.watch-btn:hover {
  background: var(--color-primary);
  color: var(--text-primary);
}

.movie-info {
  padding: var(--spacing-md);
}

.movie-info h3 {
  margin: 0 0 var(--spacing-xs) 0;
  font-size: 15px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.movie-genre {
  margin: 0 0 var(--spacing-sm) 0;
  font-size: 13px;
  color: var(--color-primary-light);
}

.movie-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: var(--spacing-xs);
}

.meta-divider {
  color: var(--border-color);
}

.movie-release {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
}

@media (max-width: 900px) {
  .filter-group-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    justify-content: space-between;
  }

  .reset-btn {
    margin-left: 0;
    margin-top: var(--spacing-md);
  }

  .movies-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: var(--spacing-md);
  }

  .movie-poster img {
    height: 220px;
  }
}

@media (max-width: 600px) {
  .header-banner h1 {
    font-size: 28px;
  }

  .filter-tabs {
    flex-wrap: wrap;
  }

  .filter-tab {
    flex: 1;
    min-width: 80px;
  }
}
</style>