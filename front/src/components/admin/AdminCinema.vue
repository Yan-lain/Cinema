<template>
  <div class="admin-cinema">
    <div class="toolbar">
      <div class="search-bar">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索影院名称..."
          @keyup.enter="loadCinemas"
        />
        <button @click="loadCinemas">搜索</button>
      </div>
      <button class="add-btn" @click="openAddModal">
        <span>+</span>
        <span>添加影院</span>
      </button>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>图片</th>
            <th>名称</th>
            <th>城市/区域</th>
            <th>地址</th>
            <th>电话</th>
            <th>营业时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="cinema in cinemas" :key="cinema.id">
            <td>{{ cinema.id }}</td>
            <td>
              <img v-if="cinema.image" :src="cinema.image" class="cinema-thumb" />
              <span v-else class="no-image">无</span>
            </td>
            <td>{{ cinema.name }}</td>
            <td>{{ cinema.city }}/{{ cinema.district }}</td>
            <td>{{ cinema.address }}</td>
            <td>{{ cinema.phone }}</td>
            <td>{{ cinema.businessHours }}</td>
            <td>
              <span :class="['status-badge', getDisplayStatus(cinema).class]">
                {{ getDisplayStatus(cinema).text }}
              </span>
            </td>
            <td>
              <button class="edit-btn" @click="openEditModal(cinema)">编辑</button>
              <button class="delete-btn" @click="deleteCinema(cinema.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="cinemas.length === 0" class="empty-state">
        <span>暂无影院数据</span>
      </div>
    </div>

    <!-- 添加/编辑弹窗 -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <div class="modal-title">
            <span class="breadcrumb">
              <span>影院管理</span>
              <span class="separator">/</span>
              <span>{{ editingCinema ? '编辑影院' : '添加影院' }}</span>
            </span>
          </div>
          <button class="close-btn" @click="closeModal">×</button>
        </div>

        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group">
              <label>影院名称 <span class="required">*</span></label>
              <input v-model="formData.name" type="text" placeholder="请输入影院名称" required />
            </div>

            <div class="form-group">
              <label>所在城市</label>
              <input v-model="formData.city" type="text" placeholder="请输入城市" />
            </div>

            <div class="form-group">
              <label>所在区域</label>
              <input v-model="formData.district" type="text" placeholder="请输入区域" />
            </div>

            <div class="form-group">
              <label>详细地址</label>
              <input v-model="formData.address" type="text" placeholder="请输入详细地址" />
            </div>

            <div class="form-group">
              <label>联系电话</label>
              <input v-model="formData.phone" type="text" placeholder="请输入联系电话" />
            </div>

            <div class="form-group">
              <label>营业时间</label>
              <input v-model="formData.businessHours" type="text" placeholder="如：10:00-22:00" />
            </div>

            <div class="form-group full-width">
              <label>图片URL</label>
              <input v-model="formData.image" type="text" placeholder="请输入图片链接" />
              <div v-if="formData.image" class="image-preview">
                <img :src="formData.image" alt="预览" />
              </div>
            </div>

            <div class="form-group">
              <label>状态</label>
              <select v-model="formData.status">
                <option value="active">营业中</option>
                <option value="inactive">休息中</option>
              </select>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="cancel-btn" @click="closeModal">取消</button>
          <button class="submit-btn" @click="saveCinema">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

const cinemas = ref([])
const searchQuery = ref('')
const showModal = ref(false)
const editingCinema = ref(null)
const formData = reactive({
  name: '',
  city: '',
  district: '',
  address: '',
  phone: '',
  businessHours: '',
  facilities: '',
  image: '',
  status: 'active'
})

const isWithinBusinessHours = (businessHours) => {
  if (!businessHours || !businessHours.includes('-')) {
    return false
  }

  const now = new Date()
  const [start, end] = businessHours.split('-')

  const [startHour, startMin] = start.trim().split(':').map(Number)
  const [endHour, endMin] = end.trim().split(':').map(Number)

  const currentHour = now.getHours()
  const currentMin = now.getMinutes()

  const currentMinutes = currentHour * 60 + currentMin
  const startMinutes = startHour * 60 + startMin
  const endMinutes = endHour * 60 + endMin

  return currentMinutes >= startMinutes && currentMinutes <= endMinutes
}

const getDisplayStatus = (cinema) => {
  if (cinema.status !== 'active') {
    return { text: '休息中', class: 'inactive' }
  }

  const isOpen = isWithinBusinessHours(cinema.businessHours)
  return isOpen
    ? { text: '营业中', class: 'open' }
    : { text: '休息中', class: 'closed' }
}

onMounted(() => {
  loadCinemas()
  setInterval(() => {
    location.reload()
  }, 60000)
})

const loadCinemas = async () => {
  try {
    const params = {}
    if (searchQuery.value.trim()) {
      params.name = searchQuery.value.trim()
    }
    cinemas.value = await request.get('/admin/cinemas', { params })
  } catch (error) {
    console.error('Load cinemas error:', error)
  }
}

const openAddModal = () => {
  editingCinema.value = null
  resetForm()
  showModal.value = true
}

const openEditModal = (cinema) => {
  editingCinema.value = cinema
  formData.name = cinema.name
  formData.city = cinema.city
  formData.district = cinema.district
  formData.address = cinema.address
  formData.phone = cinema.phone
  formData.businessHours = cinema.businessHours
  formData.facilities = cinema.facilities || ''
  formData.image = cinema.image || ''
  formData.status = cinema.status
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingCinema.value = null
  resetForm()
}

const resetForm = () => {
  formData.name = ''
  formData.city = ''
  formData.district = ''
  formData.address = ''
  formData.phone = ''
  formData.businessHours = ''
  formData.facilities = ''
  formData.image = ''
  formData.status = 'active'
}

const saveCinema = async () => {
  try {
    if (editingCinema.value) {
      await request.put(`/admin/cinemas/${editingCinema.value.id}`, formData)
    } else {
      await request.post('/admin/cinemas', formData)
    }
    alert('操作成功')
    closeModal()
    loadCinemas()
  } catch (error) {
    console.error('Save cinema error:', error)
    alert(error.message || '操作失败')
  }
}

const deleteCinema = async (id) => {
  if (!confirm('确定要删除这个影院吗？')) return

  try {
    await request.delete(`/admin/cinemas/${id}`)
    alert('删除成功')
    loadCinemas()
  } catch (error) {
    console.error('Delete cinema error:', error)
    alert(error.message || '删除失败')
  }
}
</script>

<style scoped>
.admin-cinema {
  padding: 20px;
  height: 100%;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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

.search-bar {
  display: flex;
  gap: 10px;
}

.search-bar input {
  flex: 1;
  max-width: 300px;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}

.search-bar button {
  padding: 10px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
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

.cinema-thumb {
  width: 40px;
  height: 30px;
  object-fit: cover;
  border-radius: 4px;
}

.no-image {
  color: #999;
  font-size: 12px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active,
.status-badge.open {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-badge.inactive {
  background: #ffebee;
  color: #c62828;
}

.status-badge.closed {
  background: #fff3e0;
  color: #e65100;
}

.edit-btn, .delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  margin-right: 6px;
}

.edit-btn {
  background: #e3f2fd;
  color: #1976d2;
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
  max-width: 600px;
  max-height: 90vh;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.modal-title .breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.modal-title .breadcrumb .separator {
  opacity: 0.7;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  color: white;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.modal-body {
  padding: 24px;
  max-height: calc(90vh - 140px);
  overflow-y: auto;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-group {
  margin-bottom: 0;
}

.form-group.full-width {
  grid-column: span 2;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-group .required {
  color: #e74c3c;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.image-preview {
  margin-top: 12px;
}

.image-preview img {
  max-width: 200px;
  border-radius: 8px;
  border: 1px solid #eee;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #eee;
}

.cancel-btn {
  padding: 12px 28px;
  background: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.cancel-btn:hover {
  background: #e8e8e8;
}

.submit-btn {
  padding: 12px 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.submit-btn:hover {
  opacity: 0.9;
}
</style>
