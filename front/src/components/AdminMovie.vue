<template>
  <div class="admin-movie">
    <div class="toolbar">
      <div class="toolbar-left">
        <input
          type="text"
          v-model="searchKeyword"
          placeholder="搜索电影..."
          class="search-input"
        />        
        <select v-model="filterStatus" class="filter-select">
          <option value="">全部状态</option>
          <option value="showing">上映中</option>
          <option value="upcoming">即将上映</option>
          <option value="classic">经典影片</option>
        </select>
        <select v-model="filterGenre" class="filter-select">
          <option value="">全部类型</option>
          <option v-for="genre in genreList" :key="genre" :value="genre">{{ genre }}</option>
        </select>
        <select v-model="sortBy" class="filter-select">
          <option value="rating-desc">评分从高到低</option>
          <option value="rating-asc">评分从低到高</option>
          <option value="release-desc">上映时间最新</option>
          <option value="release-asc">上映时间最早</option>
        </select>
      </div>
      <button class="add-btn" @click="showAddModal = true">+ 添加电影</button>
    </div>

    <div class="movie-table">
      <table>
        <thead>
          <tr>
            <th>封面</th>
            <th>电影名称</th>
            <th>类型</th>
            <th>评分</th>
            <th>时长</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="movie in filteredMovies" :key="movie.id">
            <td><img :src="movie.poster" :alt="movie.title" class="poster-small" /></td>
            <td>{{ movie.title }}</td>
            <td>{{ movie.genre }}</td>
            <td>{{ movie.rating }}</td>
            <td>{{ movie.duration }}分钟</td>
            <td>
              <span class="status-badge" :class="movie.status">
                {{ getStatusText(movie.status) }}
              </span>
            </td>
            <td>
              <button class="edit-btn" @click="editMovie(movie)">编辑</button>
              <button class="status-btn" @click="toggleStatus(movie)">
                {{ movie.status === 'showing' ? '下架' : '上架' }}
              </button>
              <button class="delete-btn" @click="deleteMovie(movie.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    

    <div v-if="filteredMovies.length === 0" class="empty-state">
      <p>暂无电影数据</p>
    </div>

    <div v-if="showAddModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h3>{{ editingMovie ? '编辑电影' : '添加电影' }}</h3>
        <form @submit.prevent="saveMovie">
          <div class="form-row">
            <div class="form-group">
              <label>电影名称</label>
              <input type="text" v-model="formData.title" required />
            </div>
            <div class="form-group">
              <label>海报URL</label>
              <input type="text" v-model="formData.poster" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>类型</label>
              <input type="text" v-model="formData.genre" />
            </div>
            <div class="form-group">
              <label>评分</label>
              <input type="number" step="0.1" v-model="formData.rating" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>时长(分钟)</label>
              <input type="number" v-model="formData.duration" />
            </div>
            <div class="form-group">
              <label>上映日期</label>
              <input type="datetime-local" v-model="formData.releaseDate" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>导演</label>
              <input type="text" v-model="formData.director" />
            </div>
            <div class="form-group">
              <label>演员</label>
              <input type="text" v-model="formData.cast" />
            </div>
            <div class="form-group">
              <label>状态</label>
              <!-- 美化状态选择框 -->
              <select v-model="formData.status" class="status-select">
                <option value="showing">上映中</option>
                <option value="upcoming">即将上映</option>
                <option value="classic">经典影片</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label>简介</label>
            <textarea v-model="formData.description" rows="3"></textarea>
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
import { ref, computed, onMounted } from 'vue'
import API_BASE_URL from '../api'

const movies = ref([])
const searchKeyword = ref('')
const filterGenre = ref('')
const filterStatus = ref('')
const sortBy = ref('rating-desc')
const showAddModal = ref(false)
const editingMovie = ref(null)
const formData = ref({
  title: '',
  poster: '',
  description: '',
  genre: '',
  duration: '',
  rating: '',
  releaseDate: '',
  director: '',
  cast: '',
  status: 'showing'
})

const genreList = computed(() => {
  const genres = new Set()
  movies.value.forEach(m => {
    if (m.genre) {
      m.genre.split('/').forEach(g => genres.add(g.trim()))
    }
  })
  return Array.from(genres).sort()
})

const filteredMovies = computed(() => {
  let result = [...movies.value]

  if (searchKeyword.value) {
    result = result.filter(m =>
      m.title.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }

  if (filterGenre.value) {
    result = result.filter(m =>
      m.genre && m.genre.split('/').some(g => g.trim() === filterGenre.value)
    )
  }

  if (filterStatus.value) {
    result = result.filter(m => m.status === filterStatus.value)
  }

  result.sort((a, b) => {
    switch (sortBy.value) {
      case 'rating-desc':
        return (b.rating || 0) - (a.rating || 0)
      case 'rating-asc':
        return (a.rating || 0) - (b.rating || 0)
      case 'release-desc':
        return new Date(b.releaseDate) - new Date(a.releaseDate)
      case 'release-asc':
        return new Date(a.releaseDate) - new Date(b.releaseDate)
      default:
        return 0
    }
  })

  return result
})

const getStatusText = (status) => {
  const map = {
    showing: '上映中',
    upcoming: '即将上映',
    classic: '经典影片'
  }
  return map[status] || status
}

const loadMovies = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/admin/movies`)
    const data = await response.json()
    if (data.success) {
      movies.value = data.data
    }
  } catch (error) {
    console.error('加载电影失败:', error)
  }
}

const addMovie = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/admin/movies`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData.value)
    })
    const data = await response.json()
    if (data.success) {
      movies.value.push(data.data)
      closeModal()
      alert('添加成功')
    } else {
      alert(data.message)
    }
  } catch (error) {
    alert('添加失败')
  }
}

const updateMovie = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/admin/movies/${editingMovie.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...formData.value, id: editingMovie.value.id })
    })
    const data = await response.json()
    if (data.success) {
      const index = movies.value.findIndex(m => m.id === editingMovie.value.id)
      if (index !== -1) {
        movies.value[index] = data.data
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

const saveMovie = () => {
  if (editingMovie.value) {
    updateMovie()
  } else {
    addMovie()
  }
}

const editMovie = (movie) => {
  editingMovie.value = movie
  formData.value = {
    title: movie.title,
    poster: movie.poster,
    description: movie.description,
    genre: movie.genre,
    duration: movie.duration,
    rating: movie.rating,
    releaseDate: movie.releaseDate?.replace('T', ' ').substring(0, 16) || '',
    director: movie.director,
    cast: movie.cast
  }
  showAddModal.value = true
}

const toggleStatus = async (movie) => {
  const newStatus = movie.status === 'showing' ? 'classic' : 'showing'
  try {
    const response = await fetch(`${API_BASE_URL}/admin/movies/${movie.id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    })
    const data = await response.json()
    if (data.success) {
      movie.status = newStatus
      alert('状态更新成功')
    } else {
      alert(data.message)
    }
  } catch (error) {
    alert('状态更新失败')
  }
}

const deleteMovie = async (id) => {
  if (!confirm('确定要删除这部电影吗？')) return
  try {
    const response = await fetch(`${API_BASE_URL}/admin/movies/${id}`, {
      method: 'DELETE'
    })
    const data = await response.json()
    if (data.success) {
      movies.value = movies.value.filter(m => m.id !== id)
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
  editingMovie.value = null
  formData.value = {
    title: '',
    poster: '',
    description: '',
    genre: '',
    duration: '',
    rating: '',
    releaseDate: '',
    director: '',
    cast: ''
  }
}

onMounted(() => {
  loadMovies()
})
</script>

<style scoped>
.admin-movie {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  width: 200px;
  font-size: 14px;
}

.filter-select {
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
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

.status-select {
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
}

.add-btn:hover {
  opacity: 0.9;
}
.status-btn:hover {
  opacity: 0.9;
}


.movie-table {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f8f9fa;
}

th, td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

th {
  font-weight: 600;
  color: #666;
}

.poster-small {
  width: 40px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}

.status-badge.showing {
  background: #d4edda;
  color: #155724;
}

.status-badge.upcoming {
  background: #fff3cd;
  color: #856404;
}

.status-badge.classic {
  background: #e2e3e5;
  color: #383d41;
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
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin: 0 0 20px 0;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
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