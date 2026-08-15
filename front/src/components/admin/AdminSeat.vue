<template>
  <div class="admin-seat">
    <div class="header-section">
      <div class="hall-info">
        <h2>{{ currentHall?.hallNumber }} - {{ getCinemaName(currentHall?.cinemaId) }}</h2>
        <p>座位总数：{{ seats.length }}</p>
      </div>
      <div class="actions">
        <button class="back-btn" @click="goBack">返回放映厅管理</button>
        <button class="generate-btn" @click="openGenerateModal">批量生成座位</button>
      </div>
    </div>
    
    <div class="seat-layout">
      <div class="screen">银幕</div>
      <div class="seats-grid">
        <div 
          v-for="seat in seats" 
          :key="seat.id"
          class="seat-item"
          :class="{ 
            'selected': selectedSeats.includes(seat.id)
          }"
          @click="toggleSelect(seat.id)"
        >
          <span class="seat-label">{{ seat.rowNum }}-{{ seat.colNum }}</span>
        </div>
      </div>
    </div>
    
    <div class="toolbar">
      <div class="legend">
        <div class="legend-item">
          <span class="legend-color available"></span>
          <span>可用</span>
        </div>
        <div class="legend-item">
          <span class="legend-color damaged"></span>
          <span>损坏</span>
        </div>
        <div class="legend-item">
          <span class="legend-color selected"></span>
          <span>已选</span>
        </div>
      </div>
      <div class="batch-actions" v-if="selectedSeats.length > 0">
        <span>已选择 {{ selectedSeats.length }} 个座位</span>

        <button class="batch-btn" @click="clearSelection">取消选择</button>
      </div>
    </div>
    
    <!-- 批量生成弹窗 -->
    <div v-if="showGenerateModal" class="modal-overlay" @click="closeGenerateModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>批量生成座位</h3>
          <button class="close-btn" @click="closeGenerateModal">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label>行数 *</label>
            <input v-model="generateForm.rows" type="number" required min="1" max="50" />
          </div>
          <div class="form-group">
            <label>列数 *</label>
            <input v-model="generateForm.cols" type="number" required min="1" max="30" />
          </div>
          <div class="form-group">
            <label>起始行号</label>
            <input v-model="generateForm.startRow" type="number" min="1" max="50" />
          </div>
          <div class="form-group">
            <label>起始列号</label>
            <input v-model="generateForm.startCol" type="number" min="1" max="30" />
          </div>
          <div class="warning">
            <p>⚠️ 注意：此操作将在当前放映厅创建 {{ generateForm.rows * generateForm.cols }} 个座位</p>
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeGenerateModal">取消</button>
          <button class="submit-btn" @click="generateSeats">生成</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
/**
 * 【修改原因】api.js 使用 export default 默认导出，不能使用命名导入语法
 * 【变更前】import { API_BASE_URL } from '@/api'
 * 【变更后】import request from '@/utils/request'
 * 【涉及文件】src/components/admin/AdminSeat.vue
 * 【潜在影响】无，只是修复导入语法错误
 */
import request from '@/utils/request'

const seats = ref([])
const cinemas = ref([])
const showGenerateModal = ref(false)
const selectedSeats = ref([])

const currentHall = ref(null)

const generateForm = reactive({
  rows: 10,
  cols: 8,
  startRow: 1,
  startCol: 1
})

const availableCount = computed(() => seats.value.length)

const getCinemaName = (cinemaId) => {
  const cinema = cinemas.value.find(c => c.id === cinemaId)
  return cinema ? cinema.name : '未知影院'
}

const loadSeats = async () => {
  if (!currentHall.value?.id) return
  
  try {
    const response = await request.get('/' + 'seats/hall/${currentHall.value.id}`')
    const data = await response.json()
    if (data.code === 200) {
      seats.value = data.data
    }
  } catch (error) {
    console.error('Load seats error:', error)
  }
}

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

const toggleSelect = (seatId) => {
  const index = selectedSeats.value.indexOf(seatId)
  if (index > -1) {
    selectedSeats.value.splice(index, 1)
  } else {
    selectedSeats.value.push(seatId)
  }
}

const clearSelection = () => {
  selectedSeats.value = []
}



const openGenerateModal = () => {
  generateForm.rows = 10
  generateForm.cols = 8
  generateForm.startRow = 1
  generateForm.startCol = 1
  showGenerateModal.value = true
}

const closeGenerateModal = () => {
  showGenerateModal.value = false
}

const generateSeats = async () => {
  if (!generateForm.rows || !generateForm.cols) {
    alert('请填写行数和列数')
    return
  }
  
  try {
    const response = await request.post('/' + 'seats/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        hallId: currentHall.value.id,
        rows: generateForm.rows,
        cols: generateForm.cols,
        startRow: generateForm.startRow || 1,
        startCol: generateForm.startCol || 1
      })
    })
    const data = await response.json()
    if (data.code === 200) {
      alert(data.message)
      closeGenerateModal()
      loadSeats()
    } else {
      alert(data.message || '生成失败')
    }
  } catch (error) {
    console.error('Generate seats error:', error)
    alert('网络错误')
  }
}

const goBack = () => {
  localStorage.removeItem('currentHall')
  window.location.href = '/admin'
}

onMounted(() => {
  const hallStr = localStorage.getItem('currentHall')
  if (hallStr) {
    currentHall.value = JSON.parse(hallStr)
  }
  loadCinemas()
  loadSeats()
})
</script>

<style scoped>
.admin-seat {
  padding: 20px;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.hall-info h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
}

.hall-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.back-btn, .generate-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  margin-left: 12px;
}

.back-btn {
  background: #f5f5f5;
  color: #666;
}

.generate-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.seat-layout {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  padding: 30px;
  margin-bottom: 20px;
}

.screen {
  text-align: center;
  padding: 12px;
  background: #333;
  color: #fff;
  border-radius: 8px 8px 0 0;
  font-weight: 500;
  margin-bottom: 30px;
}

.seats-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.seat-item {
  width: 40px;
  height: 40px;
  background: #d4edda;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.seat-item:hover {
  transform: scale(1.1);
}

.seat-item.damaged {
  background: #f8d7da;
}

.seat-item.selected {
  background: #667eea;
  color: white;
}

.seat-label {
  font-size: 11px;
  font-weight: 500;
  color: #333;
}

.seat-item.selected .seat-label {
  color: white;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.legend {
  display: flex;
  gap: 24px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.legend-color {
  width: 20px;
  height: 20px;
  border-radius: 4px;
}

.legend-color.available {
  background: #d4edda;
}

.legend-color.damaged {
  background: #f8d7da;
}

.legend-color.selected {
  background: #667eea;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-actions span {
  font-size: 14px;
  color: #666;
}

.batch-btn {
  padding: 8px 16px;
  background: #e3f2fd;
  color: #1976d2;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.batch-btn.danger {
  background: #ffebee;
  color: #c62828;
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
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 450px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 20px;
  color: #666;
  cursor: pointer;
}

.modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-group input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.warning {
  padding: 12px;
  background: #fff3e0;
  border-radius: 8px;
  margin-top: 16px;
}

.warning p {
  margin: 0;
  color: #f57c00;
  font-size: 13px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #eee;
}

.cancel-btn {
  padding: 10px 24px;
  background: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.submit-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
</style>