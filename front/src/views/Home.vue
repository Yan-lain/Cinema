<template>
  <div class="home">
    <AnnouncementModal />
    <div class="hero-section">
      <div v-if="carouselMovies.length > 0" class="carousel-container">
        <div class="carousel" :style="{ transform: `translateX(-${currentSlide * 100}%)` }">
          <div
            v-for="movie in carouselMovies"
            :key="movie.id"
            class="carousel-slide"
            @click="goToDetail(movie)"
          >
            <img :src="movie.poster" :alt="movie.title" />
            <div class="carousel-overlay">
              <h3>{{ movie.title }}</h3>
              <p>评分: {{ movie.rating }} | {{ movie.genre }}</p>
              <button class="carousel-btn">立即购票</button>
            </div>
          </div>
        </div>
        
        <button class="carousel-prev" @click="prevSlide">‹</button>
        <button class="carousel-next" @click="nextSlide">›</button>
        
        <div class="carousel-indicators">
          <span
            v-for="(_, index) in carouselMovies"
            :key="index"
            class="indicator"
            :class="{ active: currentSlide === index }"
            @click="goToSlide(index)"
          ></span>
        </div>
      </div>
      <div v-else class="hero-empty">
        <div class="hero-empty-content">
          <span class="hero-empty-icon">🎬</span>
          <h2>暂无热映电影</h2>
          <p>敬请期待精彩影片</p>
        </div>
      </div>
    </div>

    <div class="main-content">
      <div class="left-column">
        <section class="movies-section">
          <h2 class="section-title">🎬 正在热映</h2>
          <div v-if="apiError" class="error-message">
            <span class="error-icon">⚠️</span>
            <span>{{ apiError }}</span>
          </div>
          <div v-else-if="loading" class="loading">加载中...</div>
          <div v-else-if="movies.length === 0" class="empty">暂无电影</div>
          <div v-else class="movies-grid">
            <div
              v-for="movie in movies"
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
        </section>

        <section class="movies-section upcoming-section">
          <h2 class="section-title">🔥 即将上映</h2>
          <div v-if="upcomingMovies.length === 0" class="empty">暂无即将上映的电影</div>
          <div v-else class="movies-grid">
            <div
              v-for="movie in upcomingMovies"
              :key="movie.id"
              class="movie-card"
            >
              <div class="movie-poster" @click="goToDetail(movie)">
                <img :src="movie.poster" :alt="movie.title" />
                <div class="movie-rating">{{ movie.rating }}</div>
              </div>
              <div class="movie-info">
                <h3 @click="goToDetail(movie)">{{ movie.title }}</h3>
                <p class="movie-genre">{{ movie.genre }}</p>
                <p class="movie-director">导演: {{ movie.director }}</p>
                <p class="movie-cast">演员: {{ movie.cast }}</p>
                <p class="movie-duration">{{ movie.duration }}分钟</p>
                <button class="upcoming-btn">即将上映</button>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div class="right-column">
        <section class="ranking-section">
          <h2 class="section-title">🏆 电影排行榜</h2>
          <div v-if="rankingMovies.length > 0" class="ranking-list">
            <div
              v-for="(movie, index) in rankingMovies"
              :key="movie.id"
              class="ranking-item"
              @click="goToDetail(movie)"
            >
              <span class="rank-number" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
              <div class="rank-info">
                <span class="rank-title">{{ movie.title }}</span>
                <span class="rank-rating">⭐ {{ movie.rating }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-ranking">
            <span class="empty-ranking-icon">📊</span>
            <p>暂无排名数据</p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMovieStore } from '../stores/movie'
import { useAuthStore } from '../stores/auth'
import API_BASE_URL from '../api'
import AnnouncementModal from '../components/AnnouncementModal.vue'

const router = useRouter()
const movieStore = useMovieStore()
const authStore = useAuthStore()
const currentSlide = ref(0)
let slideInterval = null

// 有场次安排的电影（从/showing接口获取）
const showingMovies = ref([])
const showingLoading = ref(false)

const movies = computed(() => {
  return [...showingMovies.value]
    .sort((a, b) => parseFloat(b.rating) - parseFloat(a.rating))
    .slice(0, 20)
})

const upcomingMovies = computed(() => {
  return [...movieStore.movies]  
    .filter(m => m.status === 'upcoming')
    .sort((a, b) => new Date(a.releaseDate) - new Date(b.releaseDate))
})

const loading = computed(() => showingLoading.value || movieStore.loading)

const apiError = computed(() => movieStore.error)

const carouselMovies = computed(() => {
  return [...movies.value]
    .sort((a, b) => parseFloat(b.rating) - parseFloat(a.rating))
    .slice(0, 6)
})

const rankingMovies = computed(() => {
  return [...movies.value]
    .sort((a, b) => parseFloat(b.rating) - parseFloat(a.rating))
    .slice(0, 10)
})

const fetchShowingMovies = async () => {
  showingLoading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/movies/showing`)
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    const data = await response.json()
    if (data.success) {
      showingMovies.value = data.data
    }
  } catch (error) {
    console.error('Fetch showing movies error:', error)
    showingMovies.value = []
  } finally {
    showingLoading.value = false
  }
}

onMounted(() => {
  movieStore.fetchMovies()
  fetchShowingMovies()
  startAutoSlide()
})

onUnmounted(() => {
  if (slideInterval) {
    clearInterval(slideInterval)
  }
})

const startAutoSlide = () => {
  slideInterval = setInterval(() => {
    nextSlide()
  }, 5000)
}

const nextSlide = () => {
  if (carouselMovies.value.length > 0) {
    currentSlide.value = (currentSlide.value + 1) % carouselMovies.value.length
  }
}

const prevSlide = () => {
  if (carouselMovies.value.length > 0) {
    currentSlide.value = (currentSlide.value - 1 + carouselMovies.value.length) % carouselMovies.value.length
  }
}

const goToSlide = (index) => {
  currentSlide.value = index
}

const goToDetail = async (movie) => {
  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      await fetch(`${API_BASE_URL}/browse/add`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId: authStore.user.id,
          movieId: movie.id
        })
      })
    } catch (error) {
      console.error('Add browse history error:', error)
    }
  }
  router.push({ path: '/movie', query: { movieId: movie.id } })
}


</script>

<style scoped>
.home {
  min-height: 100vh;
  background: #f5f5f5;
}

.hero-section {
  position: relative;
  min-height: 400px;
  text-align: center;
  color: white;
  overflow: hidden;
}

.carousel-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.carousel {
  display: flex;
  transition: transform 0.5s ease-in-out;
  width: 100%;
  height: 100%;
}

.carousel-slide {
  min-width: 100%;
  position: relative;
  height: 100%;
}

.carousel-slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  padding: 40px 30px;
  text-align: left;
}

.carousel-overlay h3 {
  margin: 0 0 10px 0;
  font-size: 24px;
}

.carousel-overlay p {
  margin: 0 0 15px 0;
  font-size: 14px;
  opacity: 0.9;
}

.carousel-btn {
  background: #ff6b6b;
  border: none;
  color: white;
  padding: 10px 24px;
  border-radius: 25px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.carousel-btn:hover {
  background: #ee5a5a;
}

.carousel-prev,
.carousel-next {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.5);
  border: none;
  color: white;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  transition: background 0.3s;
}

.carousel-prev {
  left: 15px;
}

.carousel-next {
  right: 15px;
}

.carousel-prev:hover,
.carousel-next:hover {
  background: rgba(0, 0, 0, 0.8);
}

.carousel-indicators {
  position: absolute;
  bottom: 15px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
}

.indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background 0.3s;
}

.indicator.active {
  background: white;
}

.main-content {
  display: flex;
  gap: 30px;
  padding: 30px;
  max-width: 1400px;
  margin: 0 auto;
}

.left-column {
  flex: 1;
}

.right-column {
  width: 320px;
  flex-shrink: 0;
}

.movies-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #333;
}

.movies-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.movie-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.movie-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.movie-poster {
  position: relative;
  cursor: pointer;
}

.movie-poster img {
  width: 100%;
  height: 250px;
  object-fit: cover;
}

.movie-rating {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: #ff9800;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.movie-info {
  padding: 12px;
}

.movie-info h3 {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #333;
  cursor: pointer;
}

.movie-info h3:hover {
  color: #667eea;
}

.movie-genre {
  margin: 0 0 4px 0;
  font-size: 12px;
  color: #999;
}

.movie-director, .movie-cast {
  margin: 0 0 2px 0;
  font-size: 11px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.movie-duration {
  margin: 0 0 10px 0;
  font-size: 12px;
  color: #999;
}

.movie-release {
  margin: 0;
  font-size: 12px;
  color: #ff9800;
}

.upcoming-section {
  margin-top: 20px;
}

.view-detail {
  width: 100%;
  padding: 8px;
  text-align: center;
  color: #667eea;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s;
}

.view-detail:hover {
  color: #764ba2;
}

.upcoming-btn {
  width: 100%;
  padding: 8px;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 13px;
  cursor: default;
}

.ranking-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  position: sticky;
  top: 20px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.ranking-item:hover {
  background: #f8f9fa;
}

.rank-number {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: bold;
  background: #eee;
  color: #666;
}

.rank-number.rank-1 {
  background: #ffd700;
  color: #8b6914;
}

.rank-number.rank-2 {
  background: #c0c0c0;
  color: #5a5a5a;
}

.rank-number.rank-3 {
  background: #cd7f32;
  color: #5a3a1a;
}

.rank-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.rank-title {
  font-size: 14px;
  color: #333;
}

.rank-rating {
  font-size: 12px;
  color: #ff9800;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

.error-message {
  text-align: center;
  padding: 30px;
  color: #e74c3c;
  background: #ffebee;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.error-icon {
  font-size: 20px;
}

.hero-empty {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-empty-content {
  text-align: center;
}

.hero-empty-icon {
  font-size: 64px;
  display: block;
  margin-bottom: 16px;
}

.hero-empty h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #fff;
}

.hero-empty p {
  margin: 0;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
}

.empty-ranking {
  text-align: center;
  padding: 30px;
  color: #999;
}

.empty-ranking-icon {
  font-size: 32px;
  display: block;
  margin-bottom: 8px;
}

.empty-ranking p {
  margin: 0;
  font-size: 14px;
}

@media (max-width: 900px) {
  .main-content {
    flex-direction: column;
  }

  .right-column {
    width: 100%;
  }
}
</style>

