<template>
  <div class="admin-user">
    <div class="filter-section">
      <div class="filter-row">
        <input 
          v-model="searchKeyword" 
          type="text" 
          placeholder="请输入要查询的用户信息"
          @input="applyFilters"
          class="search-input"
        />
        <select v-model="statusFilter" @change="applyFilters" class="status-select">
          <option value="">全部</option>
          <option value="active">正常</option>
          <option value="disabled">禁用</option>
        </select>
      </div>
    </div>
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>电话</th>
            <th>邮箱</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in filteredUsers" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.nickname }}</td>
            <td>{{ user.phone || '-' }}</td>
            <td>{{ user.email || '-' }}</td>
            <td>
              <span :class="['status-badge', user.status]">{{ getStatusText(user.status) }}</span>
            </td>
            <td>{{ formatDate(user.createdAt) }}</td>
            <td class="action-buttons">
              <button class="btn btn-primary" @click="openEditModal(user)">编辑</button>
              <button class="btn btn-warning" @click="openResetModal(user)">修改密码</button>
              <button :class="user.status === 'disabled' ? 'btn btn-success' : 'btn btn-danger'" @click="toggleStatus(user)">
                {{ user.status === 'disabled' ? '启用' : '禁用' }}
              </button>
              <button class="btn btn-outline-danger" @click="deleteUser(user.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <div v-if="users.length === 0" class="empty-state">
        <span>暂无用户数据</span>
      </div>
    </div>

    <!-- 添加/编辑弹窗 -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ editingUser ? '编辑用户' : '添加用户' }}</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label>用户名</label>
            <input v-model="formData.username" type="text" required :disabled="editingUser" />
          </div>
          <div class="form-group" v-if="!editingUser">
            <label>密码</label>
            <input v-model="formData.password" type="password" required />
          </div>
          <div class="form-group">
            <label>昵称</label>
            <input v-model="formData.nickname" type="text" />
          </div>
          <div class="form-group">
            <label>联系电话</label>
            <input v-model="formData.phone" type="text" />
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input v-model="formData.email" type="email" />
          </div>
          <div class="form-group">
            <label>状态</label>
            <select v-model="formData.status">
              <option value="active">正常</option>
              <option value="disabled">禁用</option>
            </select>
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeModal">取消</button>
          <button class="submit-btn" @click="saveUser">保存</button>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="showResetModal" class="modal-overlay" @click="closeResetModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>修改密码</h3>
          <button class="close-btn" @click="closeResetModal">×</button>
        </div>
        
        <div class="modal-body">
          <p>为用户 <strong>{{ resetUser?.username }}</strong> 设置新密码：</p>
          <div class="form-group">
            <label>新密码</label>
            <input v-model="newPassword" type="password" required placeholder="请输入新密码" />
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeResetModal">取消</button>
          <button class="submit-btn" @click="resetPassword">确认修改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
const API_BASE_URL = '/api/admin'

const users = ref([])
const showModal = ref(false)
const showResetModal = ref(false)
const editingUser = ref(null)
const resetUser = ref(null)
const newPassword = ref('')

const searchKeyword = ref('')
const statusFilter = ref('')

const formData = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  role: 'user',
  status: 'active'
})

const formatDate = (date) => {
  if (!date) return '-'
  return date.substring(0, 19).replace('T', ' ')
}

const getStatusText = (status) => {
  switch (status) {
    case 'active': return '正常'
    case 'disabled': return '禁用'
    default: return status
  }
}

const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const matchKeyword = !searchKeyword.value ||
      user.username.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      (user.nickname && user.nickname.toLowerCase().includes(searchKeyword.value.toLowerCase()))
    
    const matchStatus = !statusFilter.value || user.status === statusFilter.value
    
    return matchKeyword && matchStatus
  })
})

const applyFilters = () => {
  // 筛选逻辑已在 computed 中实现
}

const toggleStatus = async (user) => {
  const newStatus = user.status === 'disabled' ? 'active' : 'disabled'
  const action = newStatus === 'disabled' ? '禁用' : '启用'
  
  if (!confirm(`确定要${action}用户 ${user.username} 吗？`)) return
  
  try {
    const response = await fetch(`${API_BASE_URL}/users/${user.id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    })
    const data = await response.json()
    if (data.success) {
      alert(data.message)
      loadUsers()
    } else {
      alert(data.message || '操作失败')
    }
  } catch (error) {
    console.error('Toggle status error:', error)
    alert('网络错误')
  }
}

const loadUsers = async () => {
  try {
    const url = `${API_BASE_URL}/users?role=user`
    const response = await fetch(url)
    const data = await response.json()
    if (data.success) {
      users.value = data.data
    }
  } catch (error) {
    console.error('Load users error:', error)
  }
}

const openAddModal = () => {
  editingUser.value = null
  resetForm()
  showModal.value = true
}

const openEditModal = (user) => {
  editingUser.value = user
  formData.username = user.username
  formData.nickname = user.nickname
  formData.phone = user.phone || ''
  formData.email = user.email || ''
  formData.status = user.status
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingUser.value = null
  resetForm()
}

const resetForm = () => {
  formData.username = ''
  formData.password = ''
  formData.nickname = ''
  formData.phone = ''
  formData.email = ''
  formData.role = 'user'
  formData.status = 'active'
}

const saveUser = async () => {
  try {
    const method = editingUser.value ? 'PUT' : 'POST'
    const url = editingUser.value 
      ? `${API_BASE_URL}/users/${editingUser.value.id}` 
      : `${API_BASE_URL}/users`
    
    const body = editingUser.value 
      ? { ...formData, role: 'user' } 
      : { ...formData, role: 'user' }
    
    const response = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const data = await response.json()
    if (data.success) {
      alert(data.message)
      closeModal()
      loadUsers()
    } else {
      alert(data.message || '操作失败')
    }
  } catch (error) {
    console.error('Save user error:', error)
    alert('网络错误')
  }
}

const openResetModal = (user) => {
  resetUser.value = user
  newPassword.value = ''
  showResetModal.value = true
}

const closeResetModal = () => {
  showResetModal.value = false
  resetUser.value = null
  newPassword.value = ''
}

const resetPassword = async () => {
  if (!newPassword.value.trim()) {
    alert('请输入新密码')
    return
  }
  
  try {
    const response = await fetch(`${API_BASE_URL}/users/${resetUser.value.id}/password`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ password: newPassword.value })
    })
    const data = await response.json()
    if (data.success) {
      alert(data.message)
      closeResetModal()
    } else {
      alert(data.message || '重置失败')
    }
  } catch (error) {
    console.error('Reset password error:', error)
    alert('网络错误')
  }
}

const deleteUser = async (id) => {
  if (!confirm('确定要删除这个用户吗？')) return
  
  try {
    const response = await fetch(`${API_BASE_URL}/users/${id}`, {
      method: 'DELETE'
    })
    const data = await response.json()
    if (data.success) {
      alert(data.message)
      loadUsers()
    } else {
      alert(data.message || '删除失败')
    }
  } catch (error) {
    console.error('Delete user error:', error)
    alert('网络错误')
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.admin-user {
  padding: 20px;
}

.filter-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  max-width: 300px;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

.status-select {
  margin-left: auto;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  min-width: 120px;
  box-sizing: border-box;
}

.search-input:focus,
.status-select:focus {
  outline: none;
  border-color: #667eea;
}

.btn-reset {
  padding: 10px 20px;
  background: #f5f5f5;
  color: #666;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-reset:hover {
  background: #eee;
  border-color: #ccc;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
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

.action-buttons {
  white-space: nowrap;
}

.btn {
  padding: 6px 14px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  margin-right: 8px;
  transition: all 0.2s;
}

.btn-primary {
  background: #e3f2fd;
  color: #1976d2;
}

.btn-primary:hover {
  background: #bbdefb;
}

.btn-warning {
  background: #fff3e0;
  color: #f57c00;
}

.btn-warning:hover {
  background: #ffe0b2;
}

.btn-danger {
  background: #ffebee;
  color: #c62828;
}

.btn-danger:hover {
  background: #ffcdd2;
}

.btn-success {
  background: #d4edda;
  color: #155724;
}

.btn-success:hover {
  background: #c3e6cb;
}

.btn-outline-danger {
  background: transparent;
  color: #c62828;
  border: 1px solid #ffcdd2;
}

.btn-outline-danger:hover {
  background: #ffebee;
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

.form-group input:disabled {
  background: #f5f5f5;
  color: #999;
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