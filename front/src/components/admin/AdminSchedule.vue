<template>
  <div class="admin-schedule">
    <div class="toolbar">
      <div class="filter-section">
        <select v-model="filterCinemaId" @change="loadSchedules">
          <option value="">全部影院</option>
          <option v-for="cinema in cinemas" :key="cinema.id" :value="cinema.id">
            {{ cinema.name }}
          </option>
        </select>
        <select v-model="filterStatus" @change="loadSchedules">
          <option value="">全部状态</option>
          <option value="available">可购票</option>
          <option value="expired">已过期</option>
        </select>
        <input 
          type="text" 
          v-model="searchKeyword" 
          placeholder="搜索电影名..."
          @keyup.enter="loadSchedules"
          class="search-input"
        />
      </div>
      <div class="btn-group">
        <button class="add-btn" @click="showAddModal = true">+ 添加场次</button>
      </div>
    </div>
    
    <div class="schedule-table">
      <table>
        <thead>
          <tr>
            <th>影院</th>
            <th>电影名称</th>
            <th>放映厅</th>
            <th>放映时间</th>
            <th>结束时间</th>
            <th>票价</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="schedule in schedules" :key="schedule.id">
            <td>{{ getCinemaName(schedule.cinemaId) }}</td>
            <td>{{ getMovieTitle(schedule.movieId) }}</td>
            <td>{{ getHallNumber(schedule.hallId) }}</td>
            <td>{{ formatTime(schedule.showTime) }}</td>
            <td>{{ formatTime(schedule.endTime) }}</td>
            <td>¥{{ schedule.price }}</td>
            <td>
              <span class="status-badge" :class="schedule.status">
                {{ schedule.status === 'available' ? '可购票' : '已过期' }}
              </span>
            </td>
            <td>
              <button 
                class="edit-btn" 
                :class="{ 'view-btn': schedule.status === 'expired' }"
                @click="schedule.status === 'expired' ? viewSchedule(schedule) : editSchedule(schedule)"
              >
                {{ schedule.status === 'expired' ? '查看' : '编辑' }}
              </button>
              <button class="delete-btn" @click="deleteSchedule(schedule.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div v-if="schedules.length === 0" class="empty-state">
      <p>暂无场次数据</p>
    </div>
    
    <!-- 查看场次详情弹窗 -->
    <div v-if="showViewModal" class="modal-overlay" @click.self="closeViewModal">
      <div class="modal-content">
        <h3>场次详情</h3>
        <div class="detail-group">
          <span class="detail-label">影院：</span>
          <span class="detail-value">{{ getCinemaName(viewingSchedule?.cinemaId) }}</span>
        </div>
        <div class="detail-group">
          <span class="detail-label">电影：</span>
          <span class="detail-value">{{ getMovieTitle(viewingSchedule?.movieId) }}</span>
        </div>
        <div class="detail-group">
          <span class="detail-label">放映厅：</span>
          <span class="detail-value">{{ getHallNumber(viewingSchedule?.hallId) }}</span>
        </div>
        <div class="detail-group">
          <span class="detail-label">放映时间：</span>
          <span class="detail-value">{{ formatTime(viewingSchedule?.showTime) }}</span>
        </div>
        <div class="detail-group">
          <span class="detail-label">结束时间：</span>
          <span class="detail-value">{{ formatTime(viewingSchedule?.endTime) }}</span>
        </div>
        <div class="detail-group">
          <span class="detail-label">票价：</span>
          <span class="detail-value">¥{{ viewingSchedule?.price }}</span>
        </div>
        <div class="detail-group">
          <span class="detail-label">状态：</span>
          <span :class="['detail-value', 'status-badge', viewingSchedule?.status]">
            {{ viewingSchedule?.status === 'available' ? '可购票' : '已过期' }}
          </span>
        </div>
        <div class="modal-footer">
          <button type="button" class="submit-btn" @click="closeViewModal">关闭</button>
        </div>
      </div>
    </div>
    
    <!-- 添加/编辑弹窗 -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content schedule-modal">
        <h3>{{ editingSchedule ? '编辑场次' : '添加场次' }}</h3>
        <form @submit.prevent="saveSchedule">
          <!-- 选择影院 -->
          <div class="form-group">
            <label>选择影院</label>
            <select v-model="formData.cinemaId" required @change="handleCinemaChange">
              <option value="">请选择影院</option>
              <option v-for="cinema in cinemas" :key="cinema.id" :value="cinema.id">
                {{ cinema.name }}
              </option>
            </select>
          </div>
          
          <!-- 影院信息展示 -->
          <div v-if="selectedCinema" class="cinema-info">
            <div class="info-row">
              <span class="info-label">营业时间：</span>
              <span class="info-value">{{ selectedCinema.businessHours || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">状态：</span>
              <span :class="['info-value', 'status-badge', getCinemaStatus(selectedCinema)]">
                {{ getCinemaStatus(selectedCinema) === 'open' ? '营业中' : '休息中' }}
              </span>
            </div>
          </div>

          <!-- 选择电影 -->
          <div class="form-group">
            <label>选择电影</label>
            <select v-model="formData.movieId" required @change="handleMovieChange">
              <option value="">请选择电影</option>
              <option v-for="movie in movies" :key="movie.id" :value="movie.id">
                {{ movie.title }} | {{ movie.duration }}分钟
              </option>
            </select>
          </div>
          
          <!-- 电影信息 -->
          <div v-if="selectedMovie" class="movie-info">
            <div class="info-row">
              <span class="info-label">影片时长：</span>
              <span class="info-value">{{ selectedMovie.duration }} 分钟</span>
            </div>
            <div class="info-row">
              <span class="info-label">结束缓冲：</span>
              <span class="info-value">20 分钟</span>
            </div>
            <div class="info-row">
              <span class="info-label">排片周期：</span>
              <span class="info-value">{{ getCycleMinutes() }} 分钟</span>
            </div>
          </div>
          
          <!-- 选择放映厅 -->
          <div class="form-group">
            <label>选择放映厅</label>
            <select v-model="formData.hallId" required @change="handleHallChange">
              <option value="">请选择放映厅</option>
              <option v-for="hall in filteredHalls" :key="hall.id" :value="hall.id">
                {{ hall.hallNumber }}
              </option>
            </select>
          </div>

          <!-- 放映日期 -->
          <div class="form-group">
            <label>放映日期</label>
            <input type="date" v-model="formData.showDate" required @change="handleDateChange" />
          </div>
          
          <!-- 开始时间（下拉选择） -->
          <div class="form-group">
            <label>开始时间</label>
            <select v-model="formData.startTime" required @change="selectStartTime(formData.startTime)">
              <option value="">请选择开始时间</option>
              <option 
                v-for="time in availableTimes" 
                :key="time.value" 
                :value="time.value"
              >
                {{ time.label }}
              </option>
            </select>
          </div>
          
          <!-- 结束时间（自动计算） -->
          <div v-if="formData.startTime" class="form-group">
            <label>结束时间（自动计算）</label>
            <input 
              type="text" 
              :value="endTime" 
              class="readonly-input"
              readonly
            />
          </div>
          
          <!-- 冲突提示 -->
          <div v-if="conflictMessage" class="conflict-warning">
            ⚠️ {{ conflictMessage }}
          </div>
          
          <!-- 票价 -->
          <div class="form-group">
            <label>票价</label>
            <input type="number" step="0.01" v-model="formData.price" required />
          </div>
          
          <div class="modal-footer">
            <button type="button" class="cancel-btn" @click="closeModal">取消</button>
            <button type="submit" class="submit-btn" :disabled="!!conflictMessage">保存</button>
          </div>
        </form>
      </div>
    </div>
    
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
/**
 * 【修改原因】api.js 使用 export default 默认导出，不能使用命名导入语法
 * 【变更前】import { API_BASE_URL } from '@/api'
 * 【变更后】import request from '@/utils/request'
 * 【涉及文件】src/components/admin/AdminSchedule.vue
 * 【潜在影响】无，只是修复导入语法错误
 */
import request from '@/utils/request'

const schedules = ref([])
const movies = ref([])
const halls = ref([])
const cinemas = ref([])
const showAddModal = ref(false)
const showViewModal = ref(false)
const editingSchedule = ref(null)
const viewingSchedule = ref(null)
const filterCinemaId = ref('')
const filterStatus = ref('')
const searchKeyword = ref('')

// 已占用的时间段
const occupiedSlots = ref([])

// 冲突消息
const conflictMessage = ref('')

// 表单数据
const formData = ref({
  cinemaId: '',
  movieId: '',
  hallId: '',
  showDate: '',
  startTime: '',
  endTime: '',
  price: ''
})

// 计算属性
const selectedCinema = computed(() => {
  if (!formData.value.cinemaId) return null
  return cinemas.value.find(c => c.id == formData.value.cinemaId)
})

const selectedMovie = computed(() => {
  if (!formData.value.movieId) return null
  return movies.value.find(m => m.id == formData.value.movieId)
})

const filteredHalls = computed(() => {
  if (!formData.value.cinemaId) return halls.value
  return halls.value.filter(h => h.cinemaId === parseInt(formData.value.cinemaId))
})

// 获取排片周期（影片时长 + 20分钟缓冲，向上取整到半小时）
const getCycleMinutes = () => {
  if (!selectedMovie.value) return 0
  const totalMinutes = selectedMovie.value.duration + 20
  // 向上取整到半小时
  return Math.ceil(totalMinutes / 30) * 30
}

// 计算结束时间
const endTime = computed(() => {
  if (!formData.value.startTime || !selectedMovie.value) return '--:--'
  
  const [hours, minutes] = formData.value.startTime.split(':').map(Number)
  const totalMinutes = hours * 60 + minutes + getCycleMinutes()
  
  const endHours = Math.floor(totalMinutes / 60)
  const endMinutes = totalMinutes % 60
  
  return `${String(endHours).padStart(2, '0')}:${String(endMinutes).padStart(2, '0')}`
})

// 生成可用时间列表（根据影片时长+缓冲计算周期）
const availableTimes = computed(() => {
  const times = []
  const openTime = selectedCinema.value?.businessHours ? 
    selectedCinema.value.businessHours.split('-')[0].trim() : '09:00'
  const closeTime = selectedCinema.value?.businessHours ? 
    selectedCinema.value.businessHours.split('-')[1].trim() : '23:00'
  
  const [openHour, openMin] = openTime.split(':').map(Number)
  const [closeHour, closeMin] = closeTime.split(':').map(Number)
  
  // 获取当前时间（用于判断是否需要从当前时间之后开始）
  const now = new Date()
  const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  const isToday = formData.value.showDate === todayStr
  
  // 获取当前时间（如果是今天，用于过滤已过时间）
  let cutoffHour = openHour
  let cutoffMin = openMin
  if (isToday) {
    // 如果是今天，计算当前时间之后的15分钟倍数作为起始点
    let currentHour = now.getHours()
    let currentMin = now.getMinutes()
    
    // 向上取整到最近的15分钟
    const remainder = currentMin % 15
    if (remainder !== 0) {
      currentMin = currentMin + (15 - remainder)
      if (currentMin >= 60) {
        currentMin = 0
        currentHour++
      }
    }
    
    // 确保不早于营业时间
    const currentMinutes = currentHour * 60 + currentMin
    const openMinutes = openHour * 60 + openMin
    
    if (currentMinutes > openMinutes) {
      cutoffHour = currentHour
      cutoffMin = currentMin
    }
  }
  
  // 计算排片周期（影片时长+20分钟缓冲）
  const cycleMinutes = getCycleMinutes()
  
  // 生成时间列表：从营业时间开始，按周期生成
  let currentHour = openHour
  let currentMin = openMin
  
  while (true) {
    const currentMinutes = currentHour * 60 + currentMin
    const closeMinutes = closeHour * 60 + closeMin
    
    // 检查是否超过闭店时间（留出足够时间放完电影）
    if (currentMinutes + cycleMinutes > closeMinutes) {
      break
    }
    
    const timeValue = `${String(currentHour).padStart(2, '0')}:${String(currentMin).padStart(2, '0')}`
    
    // 检查是否在当前时间之前（如果是今天）
    const cutoffMinutes = cutoffHour * 60 + cutoffMin
    if (currentMinutes < cutoffMinutes) {
      // 已过时间，跳过
      currentMin += 15
      if (currentMin >= 60) {
        currentMin = 0
        currentHour++
      }
      continue
    }
    
    // 检查是否与已占用场次冲突
    let hasConflict = false
    if (occupiedSlots.value.length > 0) {
      hasConflict = occupiedSlots.value.some(slot => {
        const slotStart = slot.startTime
        const slotEnd = slot.endTime
        
        // 检查时间段是否重叠
        const [slotStartHour, slotStartMin] = slotStart.split(':').map(Number)
        const [slotEndHour, slotEndMin] = slotEnd.split(':').map(Number)
        const [currHour, currMin] = timeValue.split(':').map(Number)
        
        const slotStartMinutes = slotStartHour * 60 + slotStartMin
        const slotEndMinutes = slotEndHour * 60 + slotEndMin
        const currMinutes = currHour * 60 + currMin
        const currEndMinutes = currMinutes + cycleMinutes
        
        return !(currEndMinutes <= slotStartMinutes || currMinutes >= slotEndMinutes)
      })
    }
    
    // 如果没有冲突，才添加到列表
    if (!hasConflict) {
      times.push({
        value: timeValue,
        label: timeValue,
        disabled: false,
        conflict: false
      })
    }
    
    // 增加15分钟
    currentMin += 15
    if (currentMin >= 60) {
      currentMin = 0
      currentHour++
    }
  }
  
  return times
})

// 获取影院状态（根据营业时间）
const getCinemaStatus = (cinema) => {
  if (!cinema || !cinema.businessHours) return 'closed'
  
  const now = new Date()
  const currentHour = now.getHours()
  const currentMin = now.getMinutes()
  const currentMinutes = currentHour * 60 + currentMin
  
  const [openTime, closeTime] = cinema.businessHours.split('-')
  const [openHour, openMin] = openTime.trim().split(':').map(Number)
  const [closeHour, closeMin] = closeTime.trim().split(':').map(Number)
  
  const openMinutes = openHour * 60 + openMin
  const closeMinutes = closeHour * 60 + closeMin
  
  return (currentMinutes >= openMinutes && currentMinutes <= closeMinutes) ? 'open' : 'closed'
}

// 获取影院名称
const getCinemaName = (id) => {
  const cinema = cinemas.value.find(c => c.id === id)
  return cinema ? cinema.name : '未知影院'
}

// 获取电影名称
const getMovieTitle = (id) => {
  const movie = movies.value.find(m => m.id === id)
  return movie ? movie.title : '未知电影'
}

// 获取放映厅编号
const getHallNumber = (id) => {
  const hall = halls.value.find(h => h.id === id)
  return hall ? hall.hallNumber : '未知厅'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

// 加载影院列表
const loadCinemas = async () => {
  try {
    const response = await request.get('/admin/cinemas')
    cinemas.value = response || []
  } catch (error) {
    console.error('加载影院失败:', error)
  }
}

// 加载电影列表
const loadMovies = async () => {
  try {
    const response = await request.get('/admin/movies')
    movies.value = response || []
  } catch (error) {
    console.error('加载电影失败:', error)
  }
}

// 加载放映厅列表
const loadHalls = async () => {
  try {
    const response = await request.get('/admin/halls')
    halls.value = response || []
  } catch (error) {
    console.error('加载放映厅失败:', error)
  }
}

// 加载场次列表
const loadSchedules = async () => {
  try {
    let url = '/admin/schedules'
    const params = []
    if (filterCinemaId.value) params.push(`cinemaId=${filterCinemaId.value}`)
    if (filterStatus.value) params.push(`status=${filterStatus.value}`)
    if (searchKeyword.value) params.push(`keyword=${encodeURIComponent(searchKeyword.value)}`)
    if (params.length > 0) url += '?' + params.join('&')
    const response = await request.get(url)
    schedules.value = response || []
  } catch (error) {
    console.error('加载场次失败:', error)
  }
}

// 加载影厅已占用时间段
const loadOccupiedSlots = async () => {
  if (!formData.value.hallId || !formData.value.showDate) {
    occupiedSlots.value = []
    return
  }
  
  try {
    const response = await request.get(`/admin/schedule/occupied?hallId=${formData.value.hallId}&date=${formData.value.showDate}`)
  
    occupiedSlots.value = response || []
  } catch (error) {
    console.error('加载已占用场次失败:', error)
    occupiedSlots.value = []
  }
}

// 检查排片冲突
const checkConflict = async () => {
  if (!formData.value.hallId || !formData.value.showDate || !formData.value.startTime) {
    conflictMessage.value = ''
    return
  }
  
  try {
    const response = await request.post('/admin/schedule/conflict-check', {
      hallId: formData.value.hallId,
      date: formData.value.showDate,
      startTime: formData.value.startTime,
      duration: selectedMovie.value?.duration || 120
    })
    if (response?.conflict) {
      conflictMessage.value = response.message || '排片冲突'
    } else {
      conflictMessage.value = ''
    }
  } catch (error) {
    console.error('冲突检测失败:', error)
    conflictMessage.value = ''
  }
}

// 处理影院变化
const handleCinemaChange = () => {
  formData.value.hallId = ''
  formData.value.startTime = ''
  occupiedSlots.value = []
  conflictMessage.value = ''
}

// 处理电影变化
const handleMovieChange = () => {
  formData.value.startTime = ''
  conflictMessage.value = ''
}

// 处理影厅变化
const handleHallChange = async () => {
  formData.value.startTime = ''
  conflictMessage.value = ''
  await loadOccupiedSlots()
}

// 处理日期变化
const handleDateChange = async () => {
  formData.value.startTime = ''
  conflictMessage.value = ''
  await loadOccupiedSlots()
}

// 选择开始时间
const selectStartTime = (time) => {
  formData.value.startTime = time
  checkConflict()
}

// 添加场次
const addSchedule = async () => {
  if (conflictMessage.value) {
    alert(conflictMessage.value)
    return
  }
  
  try {
    const scheduleData = {
      cinemaId: formData.value.cinemaId,
      movieId: formData.value.movieId,
      hallId: formData.value.hallId,
      showTime: `${formData.value.showDate}T${formData.value.startTime}:00`,
      price: formData.value.price
    }
    
    const data = await request.post('/admin/schedules', scheduleData)
    schedules.value.push(data)
    closeModal()
    alert('添加成功')
  } catch (error) {
    alert(error.message || '添加失败')
  }
}

// 更新场次
const updateSchedule = async () => {
  if (conflictMessage.value) {
    alert(conflictMessage.value)
    return
  }
  
  try {
    const scheduleData = {
      id: editingSchedule.value.id,
      cinemaId: formData.value.cinemaId,
      movieId: formData.value.movieId,
      hallId: formData.value.hallId,
      showTime: `${formData.value.showDate}T${formData.value.startTime}:00`,
      price: formData.value.price
    }
    
    const data = await request.put(`/admin/schedules/${editingSchedule.value.id}`, scheduleData)
    const index = schedules.value.findIndex(s => s.id === editingSchedule.value.id)
    if (index !== -1) {
      schedules.value[index] = data
    }
    closeModal()
    alert('更新成功')
  } catch (error) {
    alert('更新失败')
  }
}

// 保存场次
const saveSchedule = () => {
  if (editingSchedule.value) {
    updateSchedule()
  } else {
    addSchedule()
  }
}

// 查看场次
const viewSchedule = (schedule) => {
  viewingSchedule.value = schedule
  showViewModal.value = true
}

// 编辑场次
const editSchedule = (schedule) => {
  editingSchedule.value = schedule
  const showTime = schedule.showTime || ''
  formData.value = {
    cinemaId: schedule.cinemaId,
    movieId: schedule.movieId,
    hallId: schedule.hallId,
    showDate: showTime.substring(0, 10) || '',
    startTime: showTime.substring(11, 16) || '',
    price: schedule.price
  }
  showAddModal.value = true
}

// 关闭查看弹窗
const closeViewModal = () => {
  showViewModal.value = false
  viewingSchedule.value = null
}



// 删除场次
const deleteSchedule = async (id) => {
  if (!confirm('确定要删除这场放映吗？')) return
  try {
    await request.delete(`/admin/schedules/${id}`)
    schedules.value = schedules.value.filter(s => s.id !== id)
    alert('删除成功')
  } catch (error) {
    alert(error.message || '删除失败')
  }
}

// 关闭弹窗
const closeModal = () => {
  showAddModal.value = false
  editingSchedule.value = null
  formData.value = {
    cinemaId: '',
    movieId: '',
    hallId: '',
    showDate: '',
    startTime: '',
    price: ''
  }
  occupiedSlots.value = []
  conflictMessage.value = ''
}

// 初始化
onMounted(() => {
  loadCinemas()
  loadMovies()
  loadHalls()
  loadSchedules()
})
</script>

<style scoped>
.admin-schedule {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-section select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  min-width: 140px;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  min-width: 180px;
}

.search-input.small {
  min-width: 150px;
  margin-bottom: 8px;
}

.btn-group {
  display: flex;
  gap: 10px;
}

.add-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.add-btn:hover {
  opacity: 0.9;
}

.schedule-table {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f8f9fa;
}

th, td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #eee;
  color: #1f2937;
}

th {
  font-weight: 600;
  color: #666;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.available {
  background: #d4edda;
  color: #155724;
}

.status-badge.expired {
  background: #f8d7da;
  color: #721c24;
}

.status-badge.open {
  background: #d4edda;
  color: #155724;
}

.status-badge.closed {
  background: #f8d7da;
  color: #721c24;
}

.edit-btn {
  padding: 6px 12px;
  background: #667eea;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  margin-right: 8px;
}

.view-btn {
  background: #6c757d;
}

.delete-btn {
  padding: 6px 12px;
  background: #dc3545;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
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

.modal-content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  max-width: 500px;
  width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
}

.schedule-modal {
  max-width: 600px;
}

.batch-modal {
  max-width: 800px;
}

.modal-content h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #333;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}

.form-group label {
  font-size: 14px;
  color: #666;
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.readonly-input {
  background: #f8f9fa;
  color: #666;
}

.optional-hint {
  font-weight: normal;
  font-size: 12px;
  color: #999;
}

.time-picker-container {
  margin-top: 8px;
}

.conflict-warning {
  background: #fff3cd;
  border: 1px solid #ffeeba;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 16px;
  color: #721c24;
  font-size: 13px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.cancel-btn {
  padding: 10px 20px;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  background: #fff;
}

.submit-btn {
  padding: 10px 20px;
  background: #667eea;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.detail-group {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.detail-label {
  font-weight: 600;
  color: #666;
  min-width: 80px;
}

.detail-value {
  color: #333;
}

.form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.form-row .form-group {
  flex: 1;
}

.form-row .form-group.wide {
  flex: 1 1 100%;
}

/* 影院信息展示 */
.cinema-info {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  display: flex;
  gap: 20px;
}

.movie-info {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  align-items: center;
}

.info-label {
  font-size: 13px;
  color: #666;
  min-width: 80px;
}

.info-value {
  font-size: 13px;
  color: #333;
}

.info-value.status-badge.open {
  background: #d4edda;
  color: #155724;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.info-value.status-badge.closed {
  background: #f8d7da;
  color: #721c24;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
</style>