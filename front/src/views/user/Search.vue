<template>
  <div class="search-page">
    <div class="search-container">
      <div v-if="loading" class="loading">搜索中...</div>
      <div v-else-if="searchResults.length === 0 && hasSearched" class="empty">
        <p>未找到相关电影</p>
        <p class="empty-hint">尝试使用其他关键词搜索</p>
      </div>
      <div v-else-if="searchResults.length > 0" class="results-card">
        <div class="results-header">
          <h2>搜索结果</h2>
          <span class="results-count">共找到 {{ searchResults.length }} 部电影</span>
        </div>
        <div class="movies-grid">
          <div
            v-for="movie in searchResults"
            :key="movie.id"
            class="movie-card"
            @click="goToDetail(movie)"
          >
            <div class="movie-poster">
              <img :src="movie.poster" :alt="movie.title" />
              <div class="movie-rating">{{ movie.rating }}</div>
            </div>
            <div class="movie-info">
              <h3>{{ movie.title }}</h3>
              <p class="movie-genre">{{ movie.genre }}</p>
              <p class="movie-director">导演: {{ movie.director }}</p>
              <p class="movie-cast">演员: {{ movie.cast }}</p>
              <p class="movie-duration">{{ movie.duration }}分钟</p>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="hint">
        <div class="hint-icon">🔍</div>
        <p>输入关键词搜索电影</p>
        <p class="hint-example">例如：霸王别姬、陈凯歌、张国荣</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMovieStore } from '@/stores/movie'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const movieStore = useMovieStore()
const authStore = useAuthStore()
const searchQuery = ref('')
const hasSearched = ref(false)

const loading = computed(() => movieStore.loading)
const searchResults = ref([])

const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    return
  }
  hasSearched.value = true
  await movieStore.fetchMovies()
  const keyword = searchQuery.value.toLowerCase()
  searchResults.value = movieStore.movies.filter(m =>
    m.status === 'showing' &&
    m.title.toLowerCase().includes(keyword) ||
    (m.director && m.director.toLowerCase().includes(keyword)) ||
    (m.cast && m.cast.toLowerCase().includes(keyword))
  )
}

const goToDetail = async (movie) => {
  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      await request.post('/browse/add', {
        userId: authStore.user.id,
        movieId: movie.id
      })
    } catch (error) {
      console.error('Add browse history error:', error)
    }
  }
  router.push({ path: '/movie', query: { movieId: movie.id } })
}

const handleBuyTicket = async (movie) => {
  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      await request.post('/browse/add', {
        userId: authStore.user.id,
        movieId: movie.id
      })
    } catch (error) {
      console.error('Add browse history error:', error)
    }
  }
  router.push({ path: '/movie', query: { movieId: movie.id } })
}

onMounted(async () => {
  await performSearch()
})

watch(() => route.query.q, async (newQuery) => {
  if (newQuery) {
    await performSearch()
  } else {
    searchResults.value = []
    hasSearched.value = false
  }
})

const performSearch = async () => {
  const query = route.query.q
  if (query) {
    searchQuery.value = query
    hasSearched.value = true
    await movieStore.fetchMovies()
    const keyword = query.toLowerCase()
    searchResults.value = movieStore.movies.filter(m =>
      m.status === 'showing' &&
      m.title.toLowerCase().includes(keyword) ||
      (m.director && m.director.toLowerCase().includes(keyword)) ||
      (m.cast && m.cast.toLowerCase().includes(keyword))
    )
  }
}
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  padding: 40px 20px;
}

.search-container {
  max-width: 1400px;
  margin: 0 auto;
}

.results-card {
  background: white;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  padding: 30px;
  margin-bottom: 30px;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.results-header h2 {
  margin: 0;
  font-size: 22px;
  color: #333;
  font-weight: 600;
}

.results-count {
  font-size: 14px;
  color: #999;
  background: #f8f9fa;
  padding: 8px 16px;
  border-radius: 20px;
}

.movies-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 28px;
  padding: 0 10px;
}

.movie-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid #f0f0f0;
}

.movie-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.12);
  border-color: #e8e8e8;
}

.movie-poster {
  position: relative;
  height: 320px;
  overflow: hidden;
}

.movie-poster img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.movie-card:hover .movie-poster img {
  transform: scale(1.08);
}

.movie-rating {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.75);
  color: #ffd700;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 4px;
}

.movie-rating::before {
  content: '⭐';
}

.movie-info {
  padding: 20px;
}

.movie-info h3 {
  margin: 0 0 10px 0;
  font-size: 17px;
  color: #333;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.movie-genre {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  padding: 4px 10px;
  border-radius: 12px;
  display: inline-block;
}

.movie-director, .movie-cast {
  margin: 0 0 4px 0;
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.movie-duration {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #999;
}

.loading, .empty, .hint {
  text-align: center;
  padding: 80px 40px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  margin: 0 auto;
  max-width: 500px;
}

.loading {
  font-size: 18px;
  color: #667eea;
}

.empty p {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #666;
}

.empty-hint {
  font-size: 14px !important;
  color: #999 !important;
}

.hint-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.hint p {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #666;
}

.hint-example {
  font-size: 14px !important;
  color: #999 !important;
}

@media (max-width: 1200px) {
  .movies-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 20px;
    padding: 0;
  }

  .movie-poster {
    height: 280px;
  }
}

@media (max-width: 768px) {
  .search-page {
    padding: 20px 15px;
  }

  .results-card {
    padding: 20px;
  }

  .results-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .movies-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 15px;
  }

  .movie-poster {
    height: 220px;
  }

  .movie-info {
    padding: 15px;
  }

  .movie-info h3 {
    font-size: 15px;
  }

  .loading, .empty, .hint {
    padding: 50px 20px;
  }
}
</style>
