<template>
  <div class="settings-page">
    <div class="page-header">
      <h1>设置</h1>
    </div>
    <div class="settings-content">
      <div class="settings-section">
        <h3>账号设置</h3>
        <div class="settings-list">
          <div class="settings-item">
            <div class="item-info">
              <span class="item-label">头像</span>
            </div>
            <div class="item-action">
              <div class="avatar-preview" @click="authStore.isAuthenticated && openAvatarUpload">
                <img v-if="authStore.user?.avatar && authStore.user.avatar.startsWith('http')" 
                     :src="authStore.user.avatar" 
                     alt="头像" 
                     class="avatar-img" />
                <span v-else>{{ authStore.user?.avatar || '👤' }}</span>
              </div>
              <input type="file" 
                     id="avatar-upload" 
                     class="avatar-upload-input" 
                     accept="image/*" 
                     @change="handleAvatarUpload" />
              <button v-if="authStore.isAuthenticated" class="edit-btn" @click="triggerAvatarUpload">上传</button>
              <button v-else class="edit-btn login-btn" @click="openLoginModal">登录</button>
            </div>
          </div>
          <div class="settings-item" @click="authStore.isAuthenticated && openEditModal('name')">
            <div class="item-info">
              <span class="item-label">昵称</span>
              <span class="item-value">{{ authStore.user?.nickname || '未设置' }}</span>
            </div>
            <button v-if="authStore.isAuthenticated" class="edit-btn">修改</button>
            <button v-else class="edit-btn login-btn" @click="openLoginModal">登录</button>
          </div>
          <div class="settings-item" @click="authStore.isAuthenticated && openEditModal('email')">
            <div class="item-info">
              <span class="item-label">邮箱</span>
              <span class="item-value">{{ authStore.user?.email || '未设置' }}</span>
            </div>
            <button v-if="authStore.isAuthenticated" class="edit-btn">修改</button>
            <button v-else class="edit-btn login-btn" @click="openLoginModal">登录</button>
          </div>
          <div class="settings-item" @click="authStore.isAuthenticated && openEditModal('phone')">
            <div class="item-info">
              <span class="item-label">手机号</span>
              <span class="item-value">{{ authStore.user?.phone || '未绑定' }}</span>
            </div>
            <button v-if="authStore.isAuthenticated" class="edit-btn">绑定</button>
            <button v-else class="edit-btn login-btn" @click="openLoginModal">登录</button>
          </div>
        </div>
      </div>

      <div class="settings-section">
        <h3>偏好设置</h3>
        <div class="settings-list">
          <div class="settings-item">
            <div class="item-info">
              <span class="item-label">消息通知</span>
              <span class="item-desc">接收订单和优惠信息推送</span>
            </div>
            <label class="switch">
              <input type="checkbox" v-model="notifications" />
              <span class="slider"></span>
            </label>
          </div>
          <div class="settings-item">
            <div class="item-info">
              <span class="item-label">邮件通知</span>
              <span class="item-desc">接收活动邮件</span>
            </div>
            <label class="switch">
              <input type="checkbox" v-model="emailNotifications" />
              <span class="slider"></span>
            </label>
          </div>
        </div>
      </div>

      <div class="settings-section">
        <h3>其他</h3>
        <div class="settings-list">
          <div class="settings-item">
            <div class="item-info">
              <span class="item-label">清除缓存</span>
              <span class="item-desc">释放存储空间</span>
            </div>
            <button class="edit-btn" @click="clearCache">清除</button>
          </div>
          <div class="settings-item">
            <div class="item-info">
              <span class="item-label">关于我们</span>
            </div>
            <button class="edit-btn">查看</button>
          </div>
        </div>
      </div>

      <div v-if="authStore.isAuthenticated" class="logout-section">
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
      <div v-else class="login-section">
        <button class="login-page-btn" @click="openLoginModal">登录 / 注册</button>
      </div>
    </div>

    <LoginModal v-if="showLoginModal" @close="showLoginModal = false" />

    <div v-if="showEditModal" class="edit-overlay" @click.self="showEditModal = false">
      <div class="edit-modal">
        <div class="modal-header">
          <h3>{{ editTitles[editType] }}</h3>
          <button class="close-btn" @click="showEditModal = false">×</button>
        </div>
        <div class="modal-body">
          <template v-if="editType === 'avatar'">
            <div class="avatar-select">
              <button
                v-for="avatar in avatars"
                :key="avatar"
                :class="['avatar-option', { active: editValue === avatar }]"
                @click="editValue = avatar"
              >
                {{ avatar }}
              </button>
            </div>
          </template>
          <template v-else>
            <input
              v-model="editValue"
              :type="editType === 'phone' ? 'tel' : 'text'"
              :placeholder="editPlaceholders[editType]"
              class="edit-input"
            />
          </template>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showEditModal = false">取消</button>
          <button class="save-btn" @click="handleSave">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import LoginModal from '../components/LoginModal.vue'
import API_BASE_URL from '../api'

const authStore = useAuthStore()
const router = useRouter()
const showLoginModal = ref(false)
const showEditModal = ref(false)
const editType = ref('')
const editValue = ref('')
const notifications = ref(true)
const emailNotifications = ref(false)

const avatars = ['👤', '😀', '😍', '🤗', '😎', '🤩', '🥳', '🤓', '😸', '🦊', '🐱', '🐶']
const editTitles = {
  name: '修改昵称',
  email: '修改邮箱',
  phone: '绑定手机',
  avatar: '选择头像'
}
const editPlaceholders = {
  name: '请输入昵称',
  email: '请输入邮箱',
  phone: '请输入手机号'
}

const openLoginModal = () => {
  showLoginModal.value = true
}

const triggerAvatarUpload = () => {
  document.getElementById('avatar-upload')?.click()
}

const openAvatarUpload = () => {
  triggerAvatarUpload()
}

const handleAvatarUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  
  // 检查文件大小（限制在2MB以内）
  if (file.size > 2 * 1024 * 1024) {
    alert('头像文件不能超过2MB')
    return
  }
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }
  
  const reader = new FileReader()
  reader.onload = async (e) => {
    const base64Image = e.target?.result?.toString()
    
    try {
      const response = await fetch(`${API_BASE_URL}/auth/uploadAvatar`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId: authStore.user?.id,
          avatar: base64Image
        })
      })
      
      const data = await response.json()
      if (data.success) {
        authStore.user = data.data
        localStorage.setItem('user', JSON.stringify(data.data))
        alert('头像上传成功')
      } else {
        alert('上传失败：' + data.message)
      }
    } catch (error) {
      console.error('Avatar upload error:', error)
      alert('上传失败，请重试')
    }
  }
  reader.readAsDataURL(file)
  
  // 重置文件输入
  event.target.value = ''
}

const openEditModal = (type) => {
  editType.value = type
  if (type === 'avatar') {
    editValue.value = authStore.user?.avatar || '👤'
  } else if (type === 'name') {
    editValue.value = authStore.user?.name || ''
  } else if (type === 'email') {
    editValue.value = authStore.user?.email || ''
  } else if (type === 'phone') {
    editValue.value = authStore.user?.phone || ''
  }
  showEditModal.value = true
}

const handleSave = async () => {
  const updateData = {
    id: authStore.user?.id
  }
  updateData[editType.value] = editValue.value

  const result = await authStore.updateUser(updateData)
  showEditModal.value = false
  if (result.success) {
    alert('修改成功')
  } else {
    alert('修改失败：' + result.message)
  }
}

const clearCache = () => {
  alert('缓存已清除')
}

const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
    authStore.logout()
    router.push('/')
  }
}
</script>

<style scoped>
.settings-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 40px 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #333;
  margin: 0 0 30px 0;
}

.settings-content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.settings-section {
  margin-bottom: 32px;
}

.settings-section:last-child {
  margin-bottom: 0;
}

.settings-section h3 {
  font-size: 16px;
  color: #333;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.settings-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px;
  border-radius: 8px;
  transition: background 0.2s;
  cursor: pointer;
}

.settings-item:hover {
  background: #f8f8f8;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-label {
  font-size: 15px;
  color: #333;
}

.item-value {
  font-size: 13px;
  color: #999;
}

.item-desc {
  font-size: 12px;
  color: #bbb;
}

.item-action {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-preview {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload-input {
  display: none;
}

.edit-btn {
  padding: 6px 16px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.login-btn {
  border-color: #667eea;
  color: #667eea;
}

.login-btn:hover {
  background: #f0f0ff;
  color: #667eea;
}

.switch {
  position: relative;
  width: 44px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: 0.3s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #667eea;
}

input:checked + .slider:before {
  transform: translateX(20px);
}

.logout-section,
.login-section {
  padding-top: 24px;
  border-top: 1px solid #eee;
}

.logout-btn,
.login-page-btn {
  width: 100%;
  padding: 14px;
  background: #fff;
  border: 1px solid #e74c3c;
  color: #e74c3c;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #fef2f2;
}

.login-page-btn {
  border-color: #667eea;
  color: #667eea;
}

.login-page-btn:hover {
  background: #f0f0ff;
}

.edit-overlay {
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

.edit-modal {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 360px;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 17px;
  color: #333;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}

.modal-body {
  padding: 24px;
}

.edit-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  box-sizing: border-box;
}

.edit-input:focus {
  border-color: #667eea;
}

.avatar-select {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.avatar-option {
  width: 48px;
  height: 48px;
  border: 2px solid #eee;
  background: #f8f8f8;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.avatar-option:hover {
  border-color: #667eea;
}

.avatar-option.active {
  border-color: #667eea;
  background: #f0f0ff;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px 24px;
}

.cancel-btn,
.save-btn {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: #fff;
  border: 1px solid #ddd;
  color: #666;
}

.cancel-btn:hover {
  background: #f5f5f5;
}

.save-btn {
  background: #667eea;
  border: none;
  color: #fff;
}

.save-btn:hover {
  background: #5a6fd6;
}
</style>
