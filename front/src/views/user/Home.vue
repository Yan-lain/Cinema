<template>
  <div class="home">
    <AnnouncementModal />
    
    <div class="hero-section">
      <div v-if="carouselMovies.length > 0" class="carousel-container">
        <div class="carousel-indicators-top">
          <span
            v-for="(_, index) in carouselMovies"
            :key="index"
            class="indicator-top"
            :class="{ active: currentSlide === index }"
            @click="goToSlide(index)"
          ></span>
        </div>
        
        <div class="carousel" :style="{ transform: `translateX(-${currentSlide * 100}%)` }">
          <div
            v-for="movie in carouselMovies"
            :key="movie.id"
            class="carousel-slide"
            @click="goToDetail(movie)"
          >
            <img :src="movie.poster" :alt="movie.title" />
            <div class="carousel-overlay">
              <div class="slide-content">
                <div class="slide-badge">HOT</div>
                <h3>{{ movie.title }}</h3>
                <p class="slide-genre">{{ movie.genre }}</p>
                <div class="slide-meta">
                  <span class="rating">⭐ {{ movie.rating }}</span>
                  <span class="duration">{{ movie.duration }}分钟</span>
                </div>
                <button class="carousel-btn">立即购票</button>
              </div>
            </div>
            <div class="slide-gradient"></div>
          </div>
        </div>
        
        <button class="carousel-prev" @click="prevSlide">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M15 18l-6-6 6-6" />
          </svg>
        </button>
        <button class="carousel-next" @click="nextSlide">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 18l6-6-6-6" />
          </svg>
        </button>
        
        <div class="carousel-indicators-bottom">
          <span
            v-for="(_, index) in carouselMovies"
            :key="index"
            class="indicator-bottom"
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
          <div class="section-header">
            <h2 class="section-title">🎬 正在热映</h2>
            <span class="section-count">{{ movies.length }}部</span>
          </div>
          
          <div v-if="apiError" class="error-message">
            <span class="error-icon">⚠️</span>
            <span>{{ apiError }}</span>
          </div>
          <div v-else-if="loading" class="loading">
            <div class="loading-spinner"></div>
            <span>加载中...</span>
          </div>
          <div v-else-if="movies.length === 0" class="empty">暂无电影</div>
          <div v-else class="movies-grid">
            <div
              v-for="(movie, index) in movies"
              :key="movie.id"
              class="movie-card"
              :style="{ animationDelay: `${index * 50}ms` }"
              @click="goToDetail(movie)"
            >
              <div class="movie-poster-wrapper">
                <div class="movie-poster">
                  <img :src="movie.poster" :alt="movie.title" />
                  <div class="movie-rating">{{ movie.rating }}</div>
                  <div class="movie-overlay"></div>
                </div>
                <button class="watch-btn">▶</button>
              </div>
              <div class="movie-info">
                <h3 class="movie-title">{{ movie.title }}</h3>
                <p class="movie-genre">{{ movie.genre }}</p>
                <div class="movie-footer">
                  <span class="movie-director">{{ movie.director }}</span>
                  <span class="movie-duration">{{ movie.duration }}min</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="movies-section upcoming-section">
          <div class="section-header">
            <h2 class="section-title">🔥 即将上映</h2>
            <span class="section-count">{{ upcomingMovies.length }}部</span>
          </div>
          
          <div v-if="upcomingMovies.length === 0" class="empty">暂无即将上映的电影</div>
          <div v-else class="movies-grid">
            <div
              v-for="(movie, index) in upcomingMovies"
              :key="movie.id"
              class="movie-card upcoming-card"
              :style="{ animationDelay: `${index * 50}ms` }"
              @click="goToDetail(movie)"
            >
              <div class="movie-poster-wrapper">
                <div class="movie-poster">
                  <img :src="movie.poster" :alt="movie.title" />
                  <div class="movie-badge upcoming">即将上映</div>
                  <div class="movie-overlay"></div>
                </div>
              </div>
              <div class="movie-info">
                <h3 class="movie-title">{{ movie.title }}</h3>
                <p class="movie-genre">{{ movie.genre }}</p>
                <div class="movie-footer">
                  <span class="movie-director">{{ movie.director }}</span>
                  <span class="movie-release">{{ formatDate(movie.releaseDate) }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div class="right-column">
        <section class="ranking-section">
          <div class="section-header">
            <h2 class="section-title">🏆 电影排行榜</h2>
          </div>
          
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
              <div class="rank-arrow">→</div>
            </div>
          </div>
          <div v-else class="empty-ranking">
            <span class="empty-ranking-icon">📊</span>
            <p>暂无排名数据</p>
          </div>
        </section>

        <section class="quick-links">
          <h3>快速导航</h3>
          <div class="links-grid">
            <router-link to="/movies" class="quick-link">
              <span class="link-icon">🎞️</span>
              <span>全部电影</span>
            </router-link>
            <router-link to="/cinemas" class="quick-link">
              <span class="link-icon">🏢</span>
              <span>附近影院</span>
            </router-link>
            <router-link to="/profile" class="quick-link">
              <span class="link-icon">👤</span>
              <span>个人中心</span>
            </router-link>
            <router-link to="/orders" class="quick-link">
              <span class="link-icon">📋</span>
              <span>我的订单</span>
            </router-link>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMovieStore } from '@/stores/movie'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'
import AnnouncementModal from '@/components/user/AnnouncementModal.vue'

const router = useRouter()
const movieStore = useMovieStore()
const authStore = useAuthStore()
const currentSlide = ref(0)
let slideInterval = null

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
    showingMovies.value = await request.get('/movies/showing')
  } catch (error) {
    console.error('Fetch showing movies error:', error)
    showingMovies.value = []
  } finally {
    showingLoading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

onMounted(() => {
  movieStore.fetchMoviesPage(0, 20)
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
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: var(--bg-surface);
}

.hero-section {
  position: relative;
  height: 500px;
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
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
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
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  padding: 0 10%;
}

.slide-content {
  max-width: 500px;
  animation: fadeInUp 0.6s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.slide-badge {
  display: inline-block;
  padding: 6px 16px;
  background: linear-gradient(135deg, var(--color-error), var(--color-accent));
  color: var(--text-primary);
  font-size: 12px;
  font-weight: bold;
  border-radius: var(--radius-full);
  margin-bottom: var(--spacing-lg);
  text-transform: uppercase;
  letter-spacing: 2px;
}

.slide-content h3 {
  margin: 0 0 var(--spacing-md) 0;
  font-size: 42px;
  font-weight: bold;
  color: var(--text-primary);
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.5);
}

.slide-genre {
  margin: 0 0 var(--spacing-md) 0;
  font-size: 16px;
  color: var(--text-secondary);
}

.slide-meta {
  display: flex;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.slide-meta .rating {
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: bold;
  color: var(--color-accent-light);
}

.slide-meta .duration {
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-full);
  font-size: 14px;
  color: var(--text-secondary);
}

.carousel-btn {
  padding: 14px 32px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.4);
}

.carousel-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.6);
}

.slide-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to right, rgba(0, 0, 0, 0.7) 0%, transparent 50%);
}

.carousel-prev,
.carousel-next {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: var(--text-primary);
  width: 50px;
  height: 50px;
  border-radius: 50%;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}

.carousel-prev:hover,
.carousel-next:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-50%) scale(1.1);
}

.carousel-prev {
  left: 30px;
}

.carousel-next {
  right: 30px;
}

.carousel-prev svg,
.carousel-next svg {
  width: 24px;
  height: 24px;
}

.carousel-indicators-top {
  position: absolute;
  top: 20px;
  right: 30px;
  display: flex;
  gap: 8px;
  z-index: 10;
}

.indicator-top {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.indicator-top.active {
  background: var(--color-primary);
  width: 24px;
  border-radius: var(--radius-full);
}

.carousel-indicators-bottom {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
}

.indicator-bottom {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.indicator-bottom.active {
  background: var(--text-primary);
  border-color: var(--text-primary);
  transform: scale(1.2);
}

.main-content {
  display: flex;
  gap: var(--spacing-xl);
  padding: var(--spacing-xl) var(--spacing-lg);
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
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-xl);
  border: 1px solid var(--border-color);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-xl);
}

.section-title {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
}

.section-count {
  padding: 4px 12px;
  background: rgba(99, 102, 241, 0.15);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--color-primary-light);
  font-weight: 500;
}

.movies-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: var(--spacing-lg);
}

.movie-card {
  background: var(--bg-secondary);
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
  height: 260px;
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

.movie-title {
  margin: 0 0 var(--spacing-xs) 0;
  font-size: 15px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.movie-genre {
  margin: 0 0 var(--spacing-sm) 0;
  font-size: 12px;
  color: var(--color-primary-light);
}

.movie-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.movie-director {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}

.movie-duration {
  font-size: 11px;
  color: var(--text-muted);
}

.movie-release {
  font-size: 11px;
  color: var(--color-success);
}

.upcoming-card {
  opacity: 0.85;
}

.upcoming-card:hover {
  opacity: 1;
}

.ranking-section {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  border: 1px solid var(--border-color);
  position: sticky;
  top: 90px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.ranking-item:hover {
  background: var(--bg-card-hover);
  transform: translateX(4px);
}

.rank-number {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: bold;
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-muted);
}

.rank-number.rank-1 {
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-light));
  color: var(--text-dark);
}

.rank-number.rank-2 {
  background: linear-gradient(135deg, #9ca3af, #d1d5db);
  color: var(--text-dark);
}

.rank-number.rank-3 {
  background: linear-gradient(135deg, #d97706, #f59e0b);
  color: var(--text-dark);
}

.rank-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.rank-title {
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-rating {
  font-size: 12px;
  color: var(--color-accent-light);
}

.rank-arrow {
  font-size: 14px;
  color: var(--text-muted);
  transition: color var(--transition-fast);
}

.ranking-item:hover .rank-arrow {
  color: var(--color-primary);
}

.quick-links {
  margin-top: var(--spacing-xl);
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  border: 1px solid var(--border-color);
}

.quick-links h3 {
  margin: 0 0 var(--spacing-lg) 0;
  font-size: 16px;
  color: var(--text-primary);
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-md);
}

.quick-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-lg);
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  text-decoration: none;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.quick-link:hover {
  background: var(--color-primary);
  color: var(--text-primary);
  transform: translateY(-2px);
}

.link-icon {
  font-size: 24px;
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

.error-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  padding: var(--spacing-xl);
  background: rgba(239, 68, 68, 0.1);
  border-radius: var(--radius-lg);
  color: var(--color-error);
  margin-bottom: var(--spacing-xl);
}

.error-icon {
  font-size: 20px;
}

.empty {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--text-muted);
}

.empty-ranking {
  text-align: center;
  padding: var(--spacing-xl);
  color: var(--text-muted);
}

.empty-ranking-icon {
  font-size: 32px;
  display: block;
  margin-bottom: var(--spacing-sm);
}

.empty-ranking p {
  margin: 0;
  font-size: 14px;
}

.hero-empty {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-empty-content {
  text-align: center;
}

.hero-empty-icon {
  font-size: 80px;
  display: block;
  margin-bottom: var(--spacing-xl);
  animation: float 3s ease-in-out infinite;
}

.hero-empty h2 {
  margin: 0 0 var(--spacing-sm) 0;
  font-size: 28px;
  color: var(--text-primary);
}

.hero-empty p {
  margin: 0;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
}

@media (max-width: 900px) {
  .main-content {
    flex-direction: column;
  }

  .right-column {
    width: 100%;
  }

  .ranking-section {
    position: static;
  }

  .slide-content h3 {
    font-size: 28px;
  }

  .carousel-overlay {
    padding: 0 var(--spacing-lg);
  }

  .movies-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}

@media (max-width: 600px) {
  .hero-section {
    height: 350px;
  }

  .slide-content h3 {
    font-size: 22px;
  }

  .carousel-prev,
  .carousel-next {
    width: 40px;
    height: 40px;
  }

  .carousel-prev {
    left: 15px;
  }

  .carousel-next {
    right: 15px;
  }
}
</style>
