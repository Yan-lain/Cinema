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
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const showModal = ref(false)// 是否显示弹窗 当前false是不显示
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
    
    //console.log('公告检查:', { today, storageKey, hiddenDate })
    
    if (hiddenDate === today) {
      console.log('今日已隐藏，跳过显示')
      return
    }
    
    const data = await request.get('/admin/announcements/latest')
    if (data) {
      announcement.value = data
      showModal.value = true
    } else {
      console.log('公告数据为空')
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
  background: rgba(0, 0, 0, 0.7);
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
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  width: 90%;
  max-width: 480px;
  max-height: 70vh;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-color);
  animation: slideUp 0.3s ease;
  display: flex;
  flex-direction: column;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
}

.modal-header h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: var(--text-primary);
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.modal-body {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
}

.modal-body p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
  font-size: 15px;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-secondary);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
}

.checkbox-label input {
  cursor: pointer;
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
}

.confirm-btn {
  padding: 10px 28px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  color: var(--text-primary);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow);
}
</style>