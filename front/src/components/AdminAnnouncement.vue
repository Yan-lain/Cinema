<template>
  <div class="admin-announcement">
    <div class="toolbar">
      <button class="add-btn" @click="showAddModal = true">+ 发布公告</button>
    </div>
    
    <div class="announcement-list">
      <div v-for="announcement in announcements" :key="announcement.id" class="announcement-card">
        <div class="card-header">
          <h3>{{ announcement.title }}</h3>
          <span class="status-badge" :class="announcement.status">
            {{ announcement.status === 'published' ? '已发布' : '草稿' }}
          </span>
        </div>
        <p class="card-content">{{ announcement.content }}</p>
        <div class="card-footer">
          <span class="publish-time">发布时间：{{ formatTime(announcement.publishedAt) }}</span>
          <div class="actions">
            <button class="edit-btn" @click="editAnnouncement(announcement)">编辑</button>
            <button 
              class="status-btn" 
              @click="toggleStatus(announcement)"
            >
              {{ announcement.status === 'published' ? '设为草稿' : '发布' }}
            </button>
            <button class="delete-btn" @click="deleteAnnouncement(announcement.id)">删除</button>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="announcements.length === 0" class="empty-state">
      <p>暂无公告数据</p>
    </div>
    
    <!-- 添加/编辑弹窗 -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h3>{{ editingAnnouncement ? '编辑公告' : '发布公告' }}</h3>
        <form @submit.prevent="saveAnnouncement">
          <div class="form-group">
            <label>公告标题</label>
            <input type="text" v-model="formData.title" required />
          </div>
          <div class="form-group">
            <label>公告内容</label>
            <textarea v-model="formData.content" rows="5" required></textarea>
          </div>
          <div class="modal-footer">
            <button type="button" class="cancel-btn" @click="closeModal">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import API_BASE_URL from '../api'

const announcements = ref([])
const showAddModal = ref(false)
const editingAnnouncement = ref(null)
const formData = ref({
  title: '',
  content: ''
})

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

const loadAnnouncements = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/admin/announcements`)
    const data = await response.json()
    if (data.success) {
      announcements.value = data.data
    }
  } catch (error) {
    console.error('加载公告失败:', error)
  }
}

const addAnnouncement = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/admin/announcements`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData.value)
    })
    const data = await response.json()
    if (data.success) {
      announcements.value.unshift(data.data)
      closeModal()
      alert('发布成功')
    } else {
      alert(data.message)
    }
  } catch (error) {
    alert('发布失败')
  }
}

const updateAnnouncement = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/admin/announcements/${editingAnnouncement.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...formData.value, id: editingAnnouncement.value.id })
    })
    const data = await response.json()
    if (data.success) {
      const index = announcements.value.findIndex(a => a.id === editingAnnouncement.value.id)
      if (index !== -1) {
        announcements.value[index] = data.data
      }
      closeModal()
      alert('更新成功')
    } else {
      alert(data.message)
    }
  } catch (error) {
    alert('更新失败')
  }
}

const saveAnnouncement = () => {
  if (editingAnnouncement.value) {
    updateAnnouncement()
  } else {
    addAnnouncement()
  }
}

const editAnnouncement = (announcement) => {
  editingAnnouncement.value = announcement
  formData.value = {
    title: announcement.title,
    content: announcement.content
  }
  showAddModal.value = true
}

const toggleStatus = async (announcement) => {
  const newStatus = announcement.status === 'published' ? 'draft' : 'published'
  try {
    const response = await fetch(`${API_BASE_URL}/admin/announcements/${announcement.id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    })
    const data = await response.json()
    if (data.success) {
      announcement.status = newStatus
      alert('状态更新成功')
    } else {
      alert(data.message)
    }
  } catch (error) {
    alert('状态更新失败')
  }
}

const deleteAnnouncement = async (id) => {
  if (!confirm('确定要删除这篇公告吗？')) return
  try {
    const response = await fetch(`${API_BASE_URL}/admin/announcements/${id}`, {
      method: 'DELETE'
    })
    const data = await response.json()
    if (data.success) {
      announcements.value = announcements.value.filter(a => a.id !== id)
      alert('删除成功')
    } else {
      alert(data.message)
    }
  } catch (error) {
    alert('删除失败')
  }
}

const closeModal = () => {
  showAddModal.value = false
  editingAnnouncement.value = null
  formData.value = {
    title: '',
    content: ''
  }
}

onMounted(() => {
  loadAnnouncements()
})
</script>

<style scoped>
.admin-announcement {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
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

.announcement-list {
  display: grid;
  gap: 16px;
}

.announcement-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}

.status-badge.published {
  background: #d4edda;
  color: #155724;
}

.status-badge.draft {
  background: #fff3cd;
  color: #856404;
}

.card-content {
  color: #666;
  margin: 0 0 12px 0;
  line-height: 1.6;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.publish-time {
  font-size: 12px;
  color: #999;
}

.edit-btn, .status-btn, .delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  margin-right: 4px;
}

.edit-btn {
  background: #17a2b8;
  color: #fff;
}

.status-btn {
  background: #ffc107;
  color: #333;
}

.delete-btn {
  background: #dc3545;
  color: #fff;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
}

.modal-content h3 {
  margin: 0 0 20px 0;
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
.form-group textarea {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
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
</style>