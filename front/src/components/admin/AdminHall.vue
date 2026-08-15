<template>
  <div class="admin-hall">
    <div class="toolbar">
      <div class="filter-section">
        <label>选择影院：</label>
        <select v-model="selectedCinema" @change="loadHalls">
          <option value="">全部影院</option>
          <option v-for="cinema in cinemas" :key="cinema.id" :value="cinema.id">{{ cinema.name }}</option>
        </select>
      </div>
      <button class="add-btn" @click="openAddModal">+ 添加放映厅</button>
    </div>
    
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>影院名称</th>
            <th>厅号</th>
            <th>座位数</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="hall in halls" :key="hall.id">
            <td>{{ hall.id }}</td>
            <td>{{ getCinemaName(hall.cinemaId) }}</td>
            <td>{{ hall.hallNumber }}</td>
            <td>{{ getSeatCount(hall) }}</td>
            <td>
              <span :class="['status-badge', hall.status]">{{ hall.status === 'active' ? '启用' : '停用' }}</span>
            </td>
            <td>{{ formatDate(hall.createdAt) }}</td>
            <td>
              <button class="edit-btn" @click="openEditModal(hall)">编辑</button>
              <!-- <button class="seat-btn" @click="manageSeats(hall)">座位管理</button> -->
              <button 
                class="status-btn" 
                :class="{ active: hall.status === 'active' }"
                @click="toggleStatus(hall)"
              >
                {{ hall.status === 'active' ? '停用' : '启用' }}
              </button>
              <button class="delete-btn" @click="deleteHall(hall.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <div v-if="halls.length === 0" class="empty-state">
        <span>暂无放映厅数据</span>
      </div>
    </div>

    <!-- 添加/编辑弹窗 -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ editingHall ? '编辑放映厅' : '添加放映厅' }}</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label>所属影院 *</label>
            <select v-model="formData.cinemaId" required>
              <option value="">请选择影院</option>
              <option v-for="cinema in cinemas" :key="cinema.id" :value="cinema.id">{{ cinema.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>厅号 *</label>
            <input v-model="formData.hallNumber" type="text" required placeholder="如：1号厅" />
          </div>
          <div class="form-group">
            <label>行数 *</label>
            <input v-model="formData.rows" type="number" required min="1" max="50" />
          </div>
          <div class="form-group">
            <label>列数 *</label>
            <input v-model="formData.cols" type="number" required min="1" max="30" />
          </div>
          <div class="form-group">
            <label>状态</label>
            <select v-model="formData.status">
              <option value="active">启用</option>
              <option value="disabled">停用</option>
            </select>
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeModal">取消</button>
          <button class="submit-btn" @click="saveHall">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
/**
 * 【修改原因】api.js 使用 export default 默认导出，不能使用命名导入语法
 * 【变更前】import { API_BASE_URL } from '@/api'
 * 【变更后】import request from '@/utils/request'
 * 【涉及文件】src/components/admin/AdminHall.vue
 * 【潜在影响】无，只是修复导入语法错误
 */
import request from '@/utils/request'

const halls = ref([])
const cinemas = ref([])
const showModal = ref(false)
const editingHall = ref(null)
const selectedCinema = ref('')

const formData = reactive({
  cinemaId: '',
  hallNumber: '',
  rows: 10,
  cols: 8,
  status: 'active'
})

const formatDate = (date) => {
  if (!date) return '-'
  return date.substring(0, 19).replace('T', ' ')
}

const getCinemaName = (cinemaId) => {
  const cinema = cinemas.value.find(c => c.id === parseInt(cinemaId))
  return cinema ? cinema.name : '未知影院'
}

const getSeatCount = (hall) => {
  const rows = hall.rows || 0
  const cols = hall.cols || 0
  return rows * cols
}

const loadHalls = async () => {
  try {
    let url = '/admin/halls'
    if (selectedCinema.value && selectedCinema.value !== '') {
      url += '?cinemaId=${parseInt(selectedCinema.value)}'
    }
    const response = await request.get(url)
      halls.value = response || []
  } catch (error) {
    console.error('加载放映厅数据失败:', error)
  }
}

const loadCinemas = async () => {
  try {
    const response = await request.get('/admin/cinemas')
      cinemas.value = response || []
    
  } catch (error) {
    console.error('加载影院数据失败:', error)
  }
}

const openAddModal = () => {
  editingHall.value = null
  resetForm()
  showModal.value = true
}

const openEditModal = (hall) => {
  editingHall.value = hall
  formData.cinemaId = hall.cinemaId.toString()
  formData.hallNumber = hall.hallNumber
  formData.rows = hall.rows || 10
  formData.cols = hall.cols || 8
  formData.status = hall.status
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingHall.value = null
  resetForm()
}

const resetForm = () => {
  formData.cinemaId = ''
  formData.hallNumber = ''
  formData.rows = 10
  formData.cols = 8
  formData.status = 'active'
}

const saveHall = async () => {
  if (!formData.cinemaId || !formData.hallNumber || !formData.rows || !formData.cols) {
    alert('请填写必填项')
    return
  }
  
  try {
    const method = editingHall.value ? 'PUT' : 'POST'
    const url = editingHall.value 
      ? `/admin/halls/${editingHall.value.id}` 
      : `/admin/halls`
    
    const body = {
      cinemaId: parseInt(formData.cinemaId),
      hallNumber: formData.hallNumber,
      rows: parseInt(formData.rows),
      cols: parseInt(formData.cols),
      status: formData.status
    }
    
    const response = await request(url, {
      method,
      body
    })
    const data = await response.json()
    if (data.code === 200) {
      alert(data.message)
      closeModal()
      loadHalls()
    } else {
      alert(data.message || '操作失败')
    }
  } catch (error) {
    console.error('Save hall error:', error)
    alert('网络错误')
  }
}

const manageSeats = (hall) => {
  localStorage.setItem('currentHall', JSON.stringify(hall))
  window.location.href = '/admin/seats'
}

const toggleStatus = async (hall) => {
  const newStatus = hall.status === 'active' ? 'disabled' : 'active'
  try {
    await request.put(`/admin/halls/${hall.id}/status`, { status: newStatus })
    alert('状态更新成功')
    loadHalls()
  } catch (error) {
    console.error('Toggle status error:', error)
    alert(error.message || '操作失败')
  }
}

const deleteHall = async (id) => {
  if (!confirm('确定要删除这个放映厅吗？删除后相关的排片和座位信息也会被删除。')) return
  
  try {
    await request.delete(`/admin/halls/${id}`)
    alert('删除成功')
    loadHalls()
  } catch (error) {
    console.error('Delete hall error:', error)
    alert(error.message || '删除失败')
  }
}

onMounted(() => {
  loadCinemas()
  loadHalls()
})
</script>

<style scoped>
.admin-hall {
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
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}

.add-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.add-btn:hover {
  opacity: 0.9;
}

.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #eee;
  color: #1f2937;
}

.data-table th {
  background: #f8f9fa;
  font-weight: 600;
  color: #333;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background: #d4edda;
  color: #155724;
}

.status-badge.disabled {
  background: #f8d7da;
  color: #721c24;
}

.edit-btn, .seat-btn, .status-btn, .delete-btn {
  padding: 6px 10px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  margin-right: 4px;
}

.edit-btn {
  background: #e3f2fd;
  color: #1976d2;
}

.seat-btn {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-btn {
  background: #fff3e0;
  color: #f57c00;
}

.status-btn.active {
  background: #f8d7da;
  color: #721c24;
}

.delete-btn {
  background: #ffebee;
  color: #c62828;
}

.empty-state {
  padding: 60px;
  text-align: center;
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

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #667eea;
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