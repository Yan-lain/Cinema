<template>
  <div class="seat-page">
    <div class="page-header">
      <div class="movie-info">
        <h1>{{ movieTitle }}</h1>
        <p>请选择影院、场次和座位</p>
      </div>
    </div>

    <div class="seat-content">
      <div class="hall-section">
        <!-- 影院信息 -->
        <div class="cinema-info">
          <label>影院：</label>
          <span>{{ selectedCinemaName }}</span>
        </div>

        <!-- 放映厅信息 -->
        <div class="hall-info">
          <label>放映厅：</label>
          <span>{{ selectedHallName }}</span>
        </div>

        <!-- 屏幕 -->
        <div class="screen">屏幕</div>

        <!-- 场次信息 -->
        <div class="schedule-info">
          <label>场次：</label>
          <span>{{ selectedScheduleTime }}</span>
          <span class="schedule-price">¥{{ schedulePrice }}</span>
        </div>

        <!-- 座位图 -->
        <div class="seat-map">
          <div class="row" v-for="row in hallRows" :key="row">
            <span class="row-label">{{ String.fromCharCode(64 + row) }}</span>
            <div class="seats">
              <button
                v-for="col in hallCols"
                :key="col"
                :class="getSeatClass(row, col)"
                :disabled="isSeatDisabled(row, col)"
                @click="toggleSeat(row, col)"
              >
                {{ col }}
              </button>
            </div>
          </div>
        </div>

        <!-- 图例 -->
        <div class="seat-legend">
          <div class="legend-item">
            <span class="seat-icon available"></span>
            <span>可选</span>
          </div>
          <!-- <div class="legend-item">
            <span class="seat-icon selected"></span>
            <span>已选</span>
          </div> -->
          <div class="legend-item">
            <span class="seat-icon sold"></span>
            <span>已售</span>
          </div>
          <!-- <div class="legend-item">
            <span class="seat-icon vip"></span>
            <span>VIP</span>
          </div> -->
        </div>
      </div>

      <div class="order-section">
        <div class="order-card">
          <h3>订单信息</h3>
          <div class="order-info">
            <p><span>电影：</span>{{ movieTitle }}</p>
            <p><span>影院：</span>{{ selectedCinemaName || '未选择' }}</p>
            <p><span>放映厅：</span>{{ selectedHallName || '未选择' }}</p>
            <p><span>场次：</span>{{ selectedSchedule ? formatScheduleTime(selectedSchedule.showTime) : '未选择' }}</p>
            <p><span>座位：</span>{{ selectedSeats.length > 0 ? selectedSeatsLabel : '未选座' }}</p>
            <p><span>数量：</span>{{ selectedSeats.length }} 张</p>
          </div>

          <div class="price-section">
            <div class="price-row">
              <span>票价 ({{ selectedSeats.length }} × ¥{{ schedulePrice }})</span>
              <span>¥{{ totalPrice }}</span>
            </div>
            <div class="price-total">
              <span>总计</span>
              <span>¥{{ totalPrice }}</span>
            </div>
          </div>

          <button
            class="submit-btn"
            :disabled="!canSubmit"
            @click="handleSubmit"
          >
            确认购票
          </button>

          <p class="notice">请在15分钟内完成支付，超时座位将自动释放</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const movieTitle = ref(route.query.title || '未选择电影')
const movieId = ref(parseInt(route.query.movieId) || 0)

// 影院相关
const cinemas = ref([])
const selectedCinemaId = ref('')
const selectedCinemaName = ref('')

// 放映厅相关
const halls = ref([])
const selectedHallId = ref('')
const selectedHallName = ref('')
const hallRows = ref(8)
const hallCols = ref(12)

// 场次相关
const schedules = ref([])
const selectedScheduleId = ref('')
const selectedSchedule = ref(null)
const selectedScheduleTime = ref('')

// 座位相关
const selectedSeats = ref([])
const soldSeats = ref([])
const vipSeats = ref([])

// 价格
const schedulePrice = ref(40)

onMounted(async () => {
  // 确保认证状态被正确加载
  authStore.checkAuth()
  
  console.log('=== 选座页面加载开始 ===')
  console.log('URL参数:', route.query)
  
  // 如果有scheduleId，优先获取场次详情（包含电影信息）
  if (route.query.scheduleId) {
    console.log('获取场次详情:', route.query.scheduleId)
    await fetchScheduleDetail(route.query.scheduleId)
    console.log('获取场次详情完成，电影标题:', movieTitle.value)
  }
  
  // 如果后端没有返回电影标题，尝试从URL获取
  if (!movieTitle.value || movieTitle.value === '未知电影') {
    if (route.query.title && route.query.title !== '未知电影') {
      movieTitle.value = route.query.title
      console.log('从URL获取电影标题:', movieTitle.value)
    }
  }
  
  // 如果有电影ID，设置电影ID
  if (route.query.movieId) {
    movieId.value = parseInt(route.query.movieId)
  }
  
  // 加载影院列表
  console.log('加载影院列表...')
  await loadCinemas()
  
  // 如果有cinemaId，设置影院并继续加载
  if (route.query.cinemaId) {
    selectedCinemaId.value = route.query.cinemaId
    const cinema = cinemas.value.find(c => c.id === parseInt(route.query.cinemaId))
    if (cinema) {
      selectedCinemaName.value = cinema.name + ' - ' + cinema.address
      console.log('设置影院:', selectedCinemaName.value)
    }
    
    // 加载放映厅
    await loadHalls()
    
    // 如果有hallId，设置放映厅
    if (route.query.hallId) {
      selectedHallId.value = route.query.hallId
      const hall = halls.value.find(h => h.id === parseInt(route.query.hallId))
      if (hall) {
        selectedHallName.value = hall.hallNumber + ' - ' + hall.rows + '排' + hall.cols + '座'
        hallRows.value = hall.rows
        hallCols.value = hall.cols
        console.log('设置放映厅:', selectedHallName.value)
      }
    }
    
    // 加载场次（需要movieId）
    if (movieId.value) {
      await loadSchedules()
      
      // 如果有scheduleId，设置场次并加载座位
      if (route.query.scheduleId) {
        selectedScheduleId.value = route.query.scheduleId
        const schedule = schedules.value.find(s => s.id === parseInt(route.query.scheduleId))
        if (schedule) {
          selectSchedule(schedule)
          selectedScheduleTime.value = formatScheduleTime(schedule.showTime)
          schedulePrice.value = parseFloat(schedule.price) || 40
        }
        
        // 加载座位
        await loadSeats()
      }
    }
  }
  
  console.log('=== 选座页面加载完成 ===')
  console.log('当前状态 - 电影标题:', movieTitle.value, ', 放映厅:', selectedHallName.value, ', 行数:', hallRows.value, ', 列数:', hallCols.value)
})

const loadCinemas = async () => {
  try {
    const response = await request.get('/' + 'cinemas`')
    const data = await response.json()
    if (data.code === 200) {
      cinemas.value = data.data
    }
  } catch (error) {
    console.error('Load cinemas error:', error)
  }
}

const loadHalls = async () => {
  halls.value = []
  selectedHallId.value = ''
  schedules.value = []
  selectedScheduleId.value = ''
  selectedSchedule.value = null
  
  if (!selectedCinemaId.value) return
  
  try {
    const response = await request.get('/' + 'halls?cinemaId=${selectedCinemaId.value}`')
    const data = await response.json()
    if (data.code === 200) {
      halls.value = data.data
    }
  } catch (error) {
    console.error('Load halls error:', error)
  }
}

const loadSchedules = async () => {
  schedules.value = []
  selectedScheduleId.value = ''
  selectedSchedule.value = null
  
  if (!selectedHallId.value || !movieId.value) return
  
  try {
    const response = await request.get('/' + 'admin/schedules?cinemaId=${selectedCinemaId.value}`')
    const data = await response.json()
    if (data.code === 200) {
      schedules.value = data.data.filter(s => s.movieId === movieId.value && s.status === 'available')
    }
  } catch (error) {
    console.error('Load schedules error:', error)
  }
}

const selectSchedule = (schedule) => {
  selectedScheduleId.value = schedule.id
  selectedSchedule.value = schedule
  schedulePrice.value = parseFloat(schedule.price) || 40
  
  console.log('=== 选择场次 ===')
  console.log('schedule:', schedule)
  console.log('scheduleId:', schedule.id)
  console.log('hallId:', schedule.hallId)
  
  // 更新放映厅信息
  const hall = halls.value.find(h => h.id === schedule.hallId)
  console.log('找到的放映厅:', hall)
  if (hall) {
    hallRows.value = hall.rows || 8
    hallCols.value = hall.cols || 12
    console.log('放映厅行数:', hallRows.value, '列数:', hallCols.value)
  }
  
  // 加载该场次的座位信息
  loadSeats()
}

const loadSeats = async () => {
  soldSeats.value = []
  vipSeats.value = []
  
  console.log('=== 加载座位 ===')
  console.log('selectedScheduleId:', selectedScheduleId.value)
  
  if (!selectedScheduleId.value) {
    console.log('selectedScheduleId为空，返回')
    return
  }
  
  try {
    const url = `${API_BASE_URL}/seats/schedule/${selectedScheduleId.value}`
    console.log('请求URL:', url)
    
    const response = await fetch(url)
    console.log('响应状态:', response.status)
    
    const data = await response.json()
    console.log('响应数据:', data)
    
    if (data.code === 200 && data.data) {
      console.log('座位数据长度:', data.data.length)
      if (data.data.length > 0) {
        console.log('第一个座位:', data.data[0])
      }
      
      soldSeats.value = data.data.filter(s => s.status === 'sold' || s.status === 'locked').map(s => [s.row, s.col])
      console.log('已售/已锁定座位:', soldSeats.value)
      
      // 从实际座位数据中获取行数和列数
      const rows = [...new Set(data.data.map(s => s.row))]
      const cols = [...new Set(data.data.map(s => s.col))]
      hallRows.value = rows.length
      hallCols.value = cols.length
      console.log('从座位数据获取的行数:', hallRows.value, '列数:', hallCols.value)
    } else {
      console.log('没有座位数据，保持默认值')
    }
  } catch (error) {
    console.error('Load seats error:', error)
  }
}

// 获取场次详情，用于补充电影信息
const fetchScheduleDetail = async (scheduleId) => {
  try {
    const response = await request.get('/' + 'admin/schedules/${scheduleId}`')
    const data = await response.json()
    if (data.code === 200 && data.data) {
      const schedule = data.data
      console.log('获取到场次详情:', schedule)
      
      // 设置电影信息（优先使用后端返回的电影标题）
      if (schedule.movieId) {
        movieId.value = schedule.movieId
        console.log('设置电影ID:', movieId.value)
      }
      // 总是尝试设置后端返回的电影标题（不管之前是什么值）
      if (schedule.movieTitle && schedule.movieTitle !== '未知电影') {
        movieTitle.value = schedule.movieTitle
        console.log('从后端获取电影标题:', movieTitle.value)
      } else if (schedule.movieTitle === '未知电影') {
        console.log('后端返回电影标题为"未知电影"，可能电影表中没有对应数据')
      } else {
        console.log('后端未返回movieTitle字段:', schedule)
      }
      
      // 设置放映厅信息
      if (!route.query.hallId && schedule.hallId) {
        selectedHallId.value = schedule.hallId.toString()
      }
      
      // 设置放映厅的行数和列数
      if (schedule.rows) {
        hallRows.value = schedule.rows
      }
      if (schedule.cols) {
        hallCols.value = schedule.cols
      }
      
      // 设置放映厅名称
      if (schedule.hallNumber) {
        selectedHallName.value = schedule.hallNumber + ' - ' + (schedule.rows || hallRows.value) + '排' + (schedule.cols || hallCols.value) + '座'
      }
      
      // 设置场次时间和价格
      if (schedule.showTime) {
        selectedScheduleTime.value = formatScheduleTime(schedule.showTime)
      }
      if (schedule.price) {
        schedulePrice.value = parseFloat(schedule.price) || 40
      }
    }
  } catch (error) {
    console.error('Fetch schedule detail error:', error)
  }
}

const generateDefaultVipSeats = () => {
  vipSeats.value = []
  // 默认VIP座位：中间区域
  for (let row = 2; row <= 4; row++) {
    for (let col = Math.floor(hallCols.value / 2) - 1; col <= Math.floor(hallCols.value / 2); col++) {
      vipSeats.value.push([row, col])
    }
  }
}

const isSeatDisabled = (row, col) => {
  return soldSeats.value.some(([r, c]) => r === row && c === col)
}

const isVipSeat = (row, col) => {
  return vipSeats.value.some(([r, c]) => r === row && c === col)
}

const isSeatSelected = (row, col) => {
  return selectedSeats.value.some(([r, c]) => r === row && c === col)
}

const getSeatClass = (row, col) => {
  if (isSeatDisabled(row, col)) return 'seat sold'
  if (isSeatSelected(row, col)) return 'seat selected'
  if (isVipSeat(row, col)) return 'seat vip'
  return 'seat available'
}

const toggleSeat = (row, col) => {
  if (isSeatDisabled(row, col)) return

  const index = selectedSeats.value.findIndex(([r, c]) => r === row && c === col)
  if (index > -1) {
    selectedSeats.value.splice(index, 1)
  } else {
    if (selectedSeats.value.length >= 6) {
      alert('最多只能选择6个座位')
      return
    }
    selectedSeats.value.push([row, col])
  }
}

const selectedSeatsLabel = computed(() => {
  return selectedSeats.value
    .map(([row, col]) => `${String.fromCharCode(64 + row)}${col}`)
    .sort()
    .join(', ')
})

const totalPrice = computed(() => {
  return selectedSeats.value.length * schedulePrice.value
})

const canSubmit = computed(() => {
  const result = authStore.isAuthenticated && 
         selectedCinemaId.value && 
         selectedHallId.value && 
         selectedScheduleId.value && 
         selectedSeats.value.length > 0
  
  return result
})

const getSelectedCinemaName = () => {
  const cinema = cinemas.value.find(c => c.id === parseInt(selectedCinemaId.value))
  return cinema ? cinema.name : ''
}

const getSelectedHallName = () => {
  const hall = halls.value.find(h => h.id === parseInt(selectedHallId.value))
  return hall ? hall.hallNumber : ''
}

const formatScheduleTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
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

const handleSubmit = async () => {
  if (!authStore.isAuthenticated) {
    alert('请先登录')
    return
  }

  const orderData = {
    userId: authStore.user.id,
    movieId: movieId.value,
    cinemaId: parseInt(selectedCinemaId.value),
    scheduleId: parseInt(selectedScheduleId.value),
    seats: selectedSeats.value.map(([row, col]) => ({ row, col })),
    totalPrice: totalPrice.value,
    seatCount: selectedSeats.value.length
  }

  try {
    const data = await request.post('/orders', orderData)
    router.push({
      path: '/payment',
      query: { orderId: data.id.toString() }
    })
  } catch (error) {
    console.error('Create order error:', error)
    alert('创建订单失败: ' + (error.message || '未知错误'))
  }
}
</script>

<style scoped>
.seat-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
}

.page-header {
  margin-bottom: 30px;
}

.movie-info h1 {
  font-size: 28px;
  color: #333;
  margin: 0 0 8px 0;
}

.movie-info p {
  color: #666;
  font-size: 15px;
  margin: 0;
}

.seat-content {
  display: flex;
  gap: 30px;
}

.hall-section {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.cinema-info,
.hall-info {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.cinema-info label,
.hall-info label {
  color: #666;
  font-weight: 500;
  min-width: 40px;
}

.cinema-info span,
.hall-info span {
  color: #333;
  font-weight: 500;
}

.screen {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 30px;
  font-weight: 500;
}

.schedule-info {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.schedule-info label {
  color: #666;
  font-weight: 500;
  min-width: 40px;
}

.schedule-info span {
  color: #333;
  font-weight: 500;
}

.schedule-info .schedule-price {
  color: #e74c3c;
  font-size: 16px;
  font-weight: 600;
  margin-left: auto;
}

.seat-map {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}

.row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.row-label {
  width: 30px;
  text-align: center;
  font-weight: 600;
  color: #666;
  font-size: 14px;
}

.seats {
  display: flex;
  gap: 6px;
}

.seat {
  width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.seat.available:hover {
  border-color: #667eea;
  background: #f0f0ff;
}

.seat.selected {
  background: #667eea;
  border-color: #667eea;
  color: #fff;
}

.seat.sold {
  background: #eee;
  color: #999;
  cursor: not-allowed;
}

.seat.vip {
  background: linear-gradient(135deg, #ffd700, #ffb800);
  border-color: #ffd700;
}

.seat-legend {
  display: flex;
  gap: 24px;
  justify-content: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.seat-icon {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  border: 1px solid #ddd;
}

.seat-icon.available {
  background: #fff;
}

.seat-icon.selected {
  background: #667eea;
  border-color: #667eea;
}

.seat-icon.sold {
  background: #eee;
}

.seat-icon.vip {
  background: linear-gradient(135deg, #ffd700, #ffb800);
  border-color: #ffd700;
}

.order-section {
  width: 320px;
}

.order-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 90px;
}

.order-card h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 20px 0;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.order-info {
  margin-bottom: 20px;
}

.order-info p {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #666;
}

.order-info p span {
  color: #999;
}

.price-section {
  padding: 16px 0;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
  color: #666;
}

.price-total {
  display: flex;
  justify-content: space-between;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-top: 10px;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.notice {
  font-size: 12px;
  color: #999;
  text-align: center;
  margin: 16px 0 0 0;
}

@media (max-width: 900px) {
  .seat-content {
    flex-direction: column;
  }

  .order-section {
    width: 100%;
  }

  .order-card {
    position: static;
  }
}
</style>