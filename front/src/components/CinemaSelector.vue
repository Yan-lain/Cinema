<template>
  <div class="cinema-selector">
    <div class="selector-header" @click="isExpanded = !isExpanded">
      <span class="selector-icon">🏢</span>
      <span class="selected-cinema">{{ selectedCinema?.name || '选择影院' }}</span>
      <span class="selector-arrow">{{ isExpanded ? '▲' : '▼' }}</span>
    </div>
    
    <div v-if="isExpanded" class="selector-dropdown">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="cinemas.length === 0" class="empty">暂无影院</div>
      <div v-else class="cinema-list">
        <div
          v-for="cinema in cinemas"
          :key="cinema.id"
          :class="['cinema-item', { active: selectedCinema?.id === cinema.id }]"
          @click="selectCinema(cinema)"
        >
          <img 
            v-if="cinema.image" 
            :src="cinema.image" 
            :alt="cinema.name" 
            class="cinema-image"
          />
          <div class="cinema-info">
            <div class="cinema-name">{{ cinema.name }}</div>
            <div class="cinema-address">{{ cinema.address }}</div>
            <div class="cinema-facilities">{{ cinema.facilities }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useAuthStore } from '../stores/auth'
const API_BASE_URL = '/api'

const authStore = useAuthStore()

const emit = defineEmits(['select'])

const cinemas = ref([])
const selectedCinema = ref(null)
const isExpanded = ref(false)
const loading = ref(false)

onMounted(() => {
  loadCinemas()
})

const loadCinemas = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/cinemas`)
    const data = await response.json()
    if (data.success) {
      cinemas.value = data.data
      if (cinemas.value.length > 0 && !selectedCinema.value) {
        selectedCinema.value = cinemas.value[0]
        emit('select', selectedCinema.value)
      }
    }
  } catch (error) {
    console.error('Load cinemas error:', error)
  } finally {
    loading.value = false
  }
}

const selectCinema = (cinema) => {
  selectedCinema.value = cinema
  isExpanded.value = false
  emit('select', cinema)
}

watch(selectedCinema, (newVal) => {
  if (newVal) {
    localStorage.setItem('selectedCinema', JSON.stringify(newVal))
  }
})
</script>

<style scoped>
.cinema-selector {
  position: relative;
  width: 200px;
}

.selector-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.selector-header:hover {
  border-color: #667eea;
}

.selector-icon {
  font-size: 16px;
}

.selected-cinema {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selector-arrow {
  font-size: 12px;
  color: #999;
}

.selector-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  width: 320px;
  background: white;
  border: 1px solid #eee;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  z-index: 100;
  max-height: 400px;
  overflow-y: auto;
}

.loading, .empty {
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.cinema-list {
  display: flex;
  flex-direction: column;
}

.cinema-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f0f0f0;
}

.cinema-item:last-child {
  border-bottom: none;
}

.cinema-item:hover {
  background: #f8f9fa;
}

.cinema-item.active {
  background: #eef2ff;
}

.cinema-image {
  width: 60px;
  height: 45px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.cinema-info {
  flex: 1;
  min-width: 0;
}

.cinema-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.cinema-address {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cinema-facilities {
  font-size: 11px;
  color: #667eea;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>