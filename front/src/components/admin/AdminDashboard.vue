<template>
  <div class="dashboard">
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon movies">🎬</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.movies }}</div>
          <div class="stat-label">电影总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon showing">🎟️</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.showing }}</div>
          <div class="stat-label">上映中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon cinemas">🏢</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.cinemas }}</div>
          <div class="stat-label">影院数量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon schedules">⏰</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.schedules }}</div>
          <div class="stat-label">排片场次</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon users">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.users }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon admins">👤</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.admins }}</div>
          <div class="stat-label">管理员</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon halls">🎭</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.halls }}</div>
          <div class="stat-label">放映厅</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon announcements">📢</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.announcements }}</div>
          <div class="stat-label">公告数量</div>
        </div>
      </div>
    </div>
    
    <div class="recent-section">
      <h3>最近上映电影</h3>
      <div class="movie-list">
        <div v-for="movie in recentMovies" :key="movie.id" class="movie-item">
          <img :src="movie.poster" :alt="movie.title" class="movie-poster" />
          <div class="movie-info">
            <h4>{{ movie.title }}</h4>
            <p class="genre">{{ movie.genre }}</p>
            <p class="rating">评分：{{ movie.rating }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="recent-section">
      <h3>今日排片</h3>
      <div class="schedule-list">
        <div v-for="schedule in todaySchedules" :key="schedule.id" class="schedule-item">
          <div class="schedule-movie">{{ getMovieTitle(schedule.movieId) }}</div>
          <div class="schedule-hall">{{ getHallNumber(schedule.hallId) }}</div>
          <div class="schedule-time">{{ formatTime(schedule.showTime) }}</div>
          <div class="schedule-price">¥{{ schedule.price }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
/**
 * 【修改原因】api.js 使用 export default 默认导出，不能使用命名导入语法
 * 【变更前】import { API_BASE_URL } from '@/api'
 * 【变更后】import API_BASE_URL from '@/api'
 * 【涉及文件】src/components/admin/AdminDashboard.vue
 * 【潜在影响】无，只是修复导入语法错误
 */
import request from '@/utils/request'

const stats = ref({
  movies: 0,
  showing: 0,
  cinemas: 0,
  schedules: 0,
  users: 0,
  admins: 0,
  halls: 0,
  announcements: 0
})

const recentMovies = ref([])
const todaySchedules = ref([])
const movies = ref([])
const halls = ref([])

const getMovieTitle = (id) => {
  const movie = movies.value.find(m => m.id === id)
  return movie ? movie.title : '未知电影'
}

const getHallNumber = (id) => {
  const hall = halls.value.find(h => h.id === id)
  return hall ? hall.hallNumber : '未知厅'
}

const formatTime = (time) => {
  if (!time) return ''
  return time.substring(11, 16)
}

/**
 * 加载仪表盘统计数据
 *
 * 【数据契约说明】
 * request.js 的响应拦截器已经将 ApiResponse 解包，成功时直接返回 data 字段里的内容，
 * 失败时（code !== 200 或网络异常）由拦截器抛出 Error 进入 catch。
 * 因此这里拿到的 moviesData / schedulesData 等已经是真实的 List，
 * 无需再判断 .code，也无需再取 .data。
 */
const loadStats = async () => {
  try {
    // 使用 Promise.all 并发请求，减少加载时间
    const [
      moviesData,
      schedulesData,
      announcementsData,
      cinemasData,
      usersData,
      hallsData
    ] = await Promise.all([
      request.get('/admin/movies'),
      request.get('/admin/schedules'),
      request.get('/admin/announcements'),
      request.get('/admin/cinemas'),
      request.get('/admin/users'),
      request.get('/admin/halls')
    ])

    // 电影统计 & 最近上映
    // 后端字段：status 为 'showing' 表示正在上映
    stats.value.movies = moviesData?.length ?? 0
    stats.value.showing = moviesData?.filter(m => m.status === 'showing').length ?? 0
    movies.value = moviesData ?? []
    recentMovies.value = (moviesData ?? [])
      .slice()
      .sort((a, b) => new Date(b.releaseDate) - new Date(a.releaseDate))
      .slice(0, 5)

    // 排片统计 & 今日排片
    stats.value.schedules = schedulesData?.length ?? 0
    const today = new Date().toISOString().split('T')[0]
    todaySchedules.value = (schedulesData ?? [])
      .filter(s => s.showTime && s.showTime.startsWith(today))
      .sort((a, b) => a.showTime.localeCompare(b.showTime))
      .slice(0, 10)

    // 公告、影院、放映厅统计
    stats.value.announcements = announcementsData?.length ?? 0
    stats.value.cinemas = cinemasData?.length ?? 0
    stats.value.halls = hallsData?.length ?? 0
    halls.value = hallsData ?? []

    // 用户统计：管理员接口返回全部用户，按 role 字段分别计数
    const users = usersData ?? []
    stats.value.users = users.filter(u => u.role === 'user').length
    stats.value.admins = users.filter(u => u.role === 'admin').length
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

onMounted(() => {
  // 初始化统计数据统计数据
  loadStats()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.movies {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.showing {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-icon.cinemas {
  background: linear-gradient(135deg, #434343 0%, #000000 100%);
}

.stat-icon.schedules {
  background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
}

.stat-icon.users {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.admins {
  background: linear-gradient(135deg, #96c93d 0%, #00b09b 100%);
}

.stat-icon.halls {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stat-icon.announcements {
  background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #999;
}

.recent-section {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.recent-section h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #333;
}

.movie-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.movie-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
}

.movie-poster {
  width: 60px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
}

.movie-info {
  flex: 1;
}

.movie-info h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: #333;
}

.genre {
  margin: 0 0 4px 0;
  font-size: 12px;
  color: #666;
}

.rating {
  margin: 0;
  font-size: 12px;
  color: #ff9800;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.schedule-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
}

.schedule-movie {
  flex: 2;
  font-size: 14px;
  color: #333;
}

.schedule-hall {
  flex: 1;
  font-size: 14px;
  color: #666;
}

.schedule-time {
  flex: 1;
  font-size: 14px;
  color: #666;
}

.schedule-price {
  flex: 1;
  font-size: 14px;
  color: #e74c3c;
  font-weight: 500;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .schedule-item {
    flex-wrap: wrap;
  }
}
</style>