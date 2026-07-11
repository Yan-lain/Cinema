<template>
  <div class="admin-cinema-add">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <router-link to="/admin">管理后台</router-link>
      <span class="separator">/</span>
      <router-link to="/admin">影院管理</router-link>
      <span class="separator">/</span>
      <span class="current">{{ isEdit ? '编辑影院' : '添加影院' }}</span>
    </div>

    <div class="content">
      <div class="page-header">
        <h2>{{ isEdit ? '编辑影院' : '添加影院' }}</h2>
        <router-link to="/admin" class="back-btn">← 返回列表</router-link>
      </div>

      <div class="form-container">
        <form @submit.prevent="saveCinema" class="cinema-form">
          <div class="form-group">
            <label>影院名称 <span class="required">*</span></label>
            <input v-model="formData.name" type="text" placeholder="请输入影院名称" required />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>所在城市</label>
              <input v-model="formData.city" type="text" placeholder="请输入城市" />
            </div>
            <div class="form-group">
              <label>所在区域</label>
              <input v-model="formData.district" type="text" placeholder="请输入区域" />
            </div>
          </div>

          <div class="form-group">
            <label>详细地址</label>
            <input v-model="formData.address" type="text" placeholder="请输入详细地址" />
          </div>

          <div class="form-group">
            <label>联系电话</label>
            <input v-model="formData.phone" type="text" placeholder="请输入联系电话" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>营业时间</label>
              <input v-model="formData.businessHours" type="text" placeholder="如：10:00-22:00" />
            </div>
            <div class="form-group">
              <label>设施服务</label>
              <input v-model="formData.facilities" type="text" placeholder="如：IMAX,3D,VIP厅" />
            </div>
          </div>

          <div class="form-group">
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

          <div class="form-actions">
            <router-link to="/admin" class="cancel-btn">取消</router-link>
            <button type="submit" class="submit-btn" :disabled="loading">
              {{ loading ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import API_BASE_URL from '../api'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const isEdit = ref(false)

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

onMounted(() => {
  if (route.query.id) {
    isEdit.value = true
    loadCinemaDetail(route.query.id)
  }
})

const loadCinemaDetail = async (id) => {
  try {
    const response = await fetch(`${API_BASE_URL}/cinemas/${id}`)
    const data = await response.json()
    if (data.success && data.data) {
      Object.assign(formData, data.data)
    }
  } catch (error) {
    console.error('Load cinema detail error:', error)
  }
}

const saveCinema = async () => {
  loading.value = true
  try {
    const method = isEdit.value ? 'PUT' : 'POST'
    const url = isEdit.value 
      ? `${API_BASE_URL}/cinemas/${route.query.id}` 
      : `${API_BASE_URL}/cinemas`
    
    const response = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData)
    })
    const data = await response.json()
    if (data.success) {
      alert(data.message)
      router.push('/admin')
    } else {
      alert(data.message || '操作失败')
    }
  } catch (error) {
    console.error('Save cinema error:', error)
    alert('网络错误')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-cinema-add {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
  color: #666;
  font-size: 14px;
}

.breadcrumb a {
  color: #666;
  transition: color 0.2s;
}

.breadcrumb a:hover {
  color: #667eea;
}

.breadcrumb .separator {
  color: #ccc;
}

.breadcrumb .current {
  color: #333;
  font-weight: 500;
}

.content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 32px;
  border-bottom: 1px solid #eee;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.back-btn {
  color: #666;
  font-size: 14px;
  transition: color 0.2s;
}

.back-btn:hover {
  color: #667eea;
}

.form-container {
  padding: 32px;
}

.cinema-form {
  max-width: 600px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-group {
  margin-bottom: 24px;
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
  padding: 12px 16px;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #eee;
}

.cancel-btn,
.submit-btn {
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
  border: none;
}

.cancel-btn:hover {
  background: #e8e8e8;
}

.submit-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}

.submit-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
