<template>
  <div class="movies-page">
    <div class="header-banner">
      <h1>电影</h1>
      <p>探索精彩电影世界</p>
    </div>

    <div class="content-wrapper">
      <div class="filter-bar">
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

        <button class="reset-btn" @click="resetFilters">重置筛选</button>
      </div>

      <div class="result-info">
        共找到 <span class="count">{{ filteredMovies.length }}</span> 部电影
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="filteredMovies.length === 0" class="empty">
        <p>暂无符合条件的电影</p>
        <button class="reset-btn" @click="resetFilters">重置筛选</button>
      </div>
      <div v-else class="movies-grid">
        <div
          v-for="movie in filteredMovies"
          :key="movie.id"
          class="movie-card"
          @click="goToDetail(movie)"
        >
          <div class="movie-poster">
            <img :src="movie.poster" :alt="movie.title" />
            <div class="movie-rating">⭐ {{ movie.rating }}</div>
          </div>
          <div class="movie-info">
            <h3>{{ movie.title }}</h3>
            <p class="movie-genre">{{ movie.genre }}</p>
            <div class="movie-meta">
              <span>{{ movie.duration }}分钟</span>
              <span>{{ movie.director }}</span>
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
import { useAuthStore } from '../stores/auth'
import API_BASE_URL from '../api'

const router = useRouter()
const authStore = useAuthStore()

const movies = ref([])
const loading = ref(true)
const selectedGenre = ref('')
const selectedRating = ref('')
const selectedTime = ref('')
const selectedStatus = ref('showing')

const genreList = ['动作', '喜剧', '剧情', '科幻', '爱情', '动画', '悬疑', '恐怖', '冒险', '奇幻']

const filteredMovies = computed(() => {
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
    const response = await fetch(`${API_BASE_URL}/movies`)
    const data = await response.json()
    if (data.success) {
      movies.value = data.data
    }
  } catch (error) {
    console.error('获取电影列表失败:', error)
  } finally {
    loading.value = false
  }
}

const goToDetail = async (movie) => {
  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      await fetch(`${API_BASE_URL}/browse/add`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: authStore.user.id,
          movieId: movie.id
        })
      })
    } catch (error) {
      console.error('添加浏览记录失败:', error)
    }
  }
  router.push({ path: '/movie', query: { movieId: movie.id } })
}

// const handleBuyTicket = (movie) => {
//   if (movie.status !== 'showing') {
//     alert('该电影暂未上映，无法购票')
//     return
//   }
//   router.push({ path: '/movie', query: { movieId: movie.id } })
// }

onMounted(() => {
  fetchMovies()
})
</script>

<style scoped>
.movies-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.header-banner {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: white;
  padding: 40px 20px;
  text-align: center;
}

.header-banner h1 {
  margin: 0 0 10px 0;
  font-size: 32px;
}

.header-banner p {
  margin: 0;
  opacity: 0.8;
  font-size: 16px;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
}

.filter-bar {
  display: flex;
  gap: 20px;
  align-items: center;
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-group label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.filter-select {
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  background: white;
  cursor: pointer;
  min-width: 120px;
  transition: border-color 0.2s;
}

.filter-select:hover {
  border-color: #667eea;
}

.filter-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.reset-btn {
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: auto;
}

.reset-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.result-info {
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
}

.result-info .count {
  color: #667eea;
  font-weight: bold;
  font-size: 16px;
}

.loading, .empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}

.empty p {
  margin: 0 0 20px 0;
}

.movies-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 25px;
}

.movie-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;
  cursor: pointer;
}

.movie-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.movie-poster {
  position: relative;
  height: 300px;
  overflow: hidden;
}

.movie-poster img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.movie-card:hover .movie-poster img {
  transform: scale(1.05);
}

.movie-rating {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: #ff9800;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
}



.movie-info {
  padding: 15px;
}

.movie-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.movie-genre {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #999;
}

.movie-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.movie-release {
  margin: 0 0 10px 0;
  font-size: 12px;
  color: #666;
}

.buy-btn {
  width: 100%;
  padding: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.buy-btn:hover {
  opacity: 0.9;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    justify-content: space-between;
  }

  .reset-btn {
    margin-left: 0;
    margin-top: 10px;
  }

  .movies-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 15px;
  }

  .movie-poster {
    height: 220px;
  }
}
</style>
