<template>
  <div v-if="showModal && announcement" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <div class="modal-header">
        <h3>{{ announcement.title }}</h3>
        <button class="close-btn" @click="closeModal">×</button>
      </div>
      <div class="modal-body">
        <p>{{ announcement.content }}</p>
      </div>
      <div class="modal-footer">
        <label class="checkbox-label">
          <input type="checkbox" v-model="dontShowToday" />
          今日不再显示
        </label>
        <button class="confirm-btn" @click="closeModal">我知道了</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import API_BASE_URL from '../api'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const showModal = ref(false)
const announcement = ref(null)
const dontShowToday = ref(false)

const getStorageKey = () => {
  const userId = authStore.user?.id || 'guest'
  return `announcementHiddenDate_${userId}`
}

const getLatestAnnouncement = async () => {
  try {
    const today = new Date().toISOString().split('T')[0]
    const storageKey = getStorageKey()
    const hiddenDate = localStorage.getItem(storageKey)
    
    if (hiddenDate === today) {
      return
    }
    
    const response = await fetch(`${API_BASE_URL}/admin/announcements/latest`)
    const data = await response.json()
    if (data.success && data.data) {
      announcement.value = data.data
      showModal.value = true
    }
  } catch (error) {
    console.error('获取公告失败:', error)
  }
}

const closeModal = () => {
  showModal.value = false
  if (dontShowToday.value) {
    const today = new Date().toISOString().split('T')[0]
    const storageKey = getStorageKey()
    localStorage.setItem(storageKey, today)
  }
}

onMounted(() => {
  getLatestAnnouncement()
})

defineExpose({
  refresh: getLatestAnnouncement
})
</script>

<style scoped>
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
  z-index: 2000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: #ffffff;
  border-radius: 8px;
  width: 38vw;
  height: 38vh;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.3s ease;
  display: flex;
  flex-direction: column;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  background: #ffffff;
}

.modal-header h3 {
  margin: 0;
  color: #000000;
  font-size: 16px;
}

.close-btn {
  background: none;
  border: none;
  color: #000000;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.close-btn:hover {
  background: #f5f5f5;
}

.modal-body {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
}

.modal-body p {
  margin: 0;
  color: #000000;
  line-height: 1.6;
  font-size: 14px;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid #eee;
  background: #ffffff;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #000000;
  cursor: pointer;
}

.checkbox-label input {
  cursor: pointer;
}

.confirm-btn {
  padding: 8px 20px;
  background: #000000;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: opacity 0.2s;
}

.confirm-btn:hover {
  opacity: 0.8;
}
</style>
