<template>
  <div class="profile-page">
    <div class="page-header">
      <h1>个人中心</h1>
    </div>
    <div class="profile-content">
      <div class="profile-card">
        <div class="avatar">
          {{ userAvatar }}
        </div>
        <div class="info">
          <h2>{{ authStore.user?.nickname || authStore.user?.username || '游客' }}</h2>
          <p class="user-id">ID: {{ authStore.user?.id || '未登录' }}</p>
        </div>
      </div>

      <div class="stats-grid">
        <div class="stat-item">
          <span class="stat-value">{{ orderCount }}</span>
          <span class="stat-label">电影订单</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ favoriteCount }}</span>
          <span class="stat-label">我的收藏</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ browseCount }}</span>
          <span class="stat-label">浏览记录</span>
        </div>      
      </div>

      <div class="info-section">
        <h3 class="section-title">账户信息</h3>
        <div class="info-list">
          <div class="info-row">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ authStore.user?.username || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">昵称</span>
            <span class="info-value">{{ authStore.user?.nickname || '未设置' }}</span>
            <button class="edit-btn" @click="showEditNickname = true">修改</button>
          </div>
          <div class="info-row">
            <span class="info-label">手机号</span>
            <span class="info-value">{{ formatPhone(authStore.user?.phone) }}</span>
            <button class="edit-btn" @click="openBindPhone">
              {{ authStore.user?.phone ? '更换' : '绑定' }}
            </button>
          </div>
          <div class="info-row">
            <span class="info-label">邮箱</span>
            <span class="info-value">{{ authStore.user?.email || '未绑定' }}</span>
            <button class="edit-btn" @click="openBindEmail">
              {{ authStore.user?.email ? '更换' : '绑定' }}
            </button>
          </div>
        </div>
      </div>

      <div class="menu-list">
        <div v-if="authStore.isAuthenticated" class="menu-item" @click="navigateToOrders">
          <span class="icon">📋</span>
          <span>我的订单</span>
          <span class="badge" v-if="orderCount > 0">{{ orderCount }}</span>
          <span class="arrow">›</span>
        </div>
        <div v-else class="menu-item" @click="openLoginModal">
          <span class="icon">📋</span>
          <span>我的订单</span>
          <span class="arrow">›</span>
        </div>
        <div class="menu-item" @click="navigateToVip">
          <span class="icon">💎</span>
          <span>会员中心</span>
          <span class="arrow">›</span>
        </div>
        <div class="menu-item" @click="showFavorites = true">
          <span class="icon">❤️</span>
          <span>我的收藏</span>
          <span class="arrow">›</span>
        </div> 
         <div class="menu-item" @click="showMyComments = true">
          <span class="icon">💬</span>
          <span>我的评论</span>
          <span class="arrow">›</span>
        </div>
        <div class="menu-item" @click="showBrowseHistory = true">
          <span class="icon">📜</span>
          <span>浏览记录</span>
          <span class="arrow">›</span>
        </div>
      
        <router-link to="/settings" class="menu-item">
          <span class="icon">⚙️</span>
          <span>设置</span>
          <span class="arrow">›</span>
        </router-link>
      </div>
    </div>

    <LoginModal v-if="showLoginModal" @close="showLoginModal = false" />

    <div v-if="showEditNickname" class="modal-overlay" @click.self="showEditNickname = false">
      <div class="modal-content small">
        <div class="modal-header">
          <h3>修改昵称</h3>
          <button class="close-btn" @click="showEditNickname = false">×</button>
        </div>
        <div class="modal-body">
          <input v-model="newNickname" type="text" placeholder="请输入新昵称" class="input-field" />
          <button class="submit-btn" @click="updateNickname" :disabled="updating">
            {{ updating ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showBindPhone" class="modal-overlay" @click.self="showBindPhone = false">
      <div class="modal-content small">
        <div class="modal-header">
          <h3>{{ authStore.user?.phone ? '更换手机号' : '绑定手机号' }}</h3>
          <button class="close-btn" @click="showBindPhone = false">×</button>
        </div>
        <div class="modal-body">
          <input v-model="newPhone" type="tel" placeholder="请输入手机号" class="input-field" />
          <button class="submit-btn" @click="bindPhone" :disabled="updating">
            {{ updating ? '绑定中...' : '绑定' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showBindEmail" class="modal-overlay" @click.self="showBindEmail = false">
      <div class="modal-content small">
        <div class="modal-header">
          <h3>{{ authStore.user?.email ? '更换邮箱' : '绑定邮箱' }}</h3>
          <button class="close-btn" @click="showBindEmail = false">×</button>
        </div>
        <div class="modal-body">
          <input v-model="newEmail" type="email" placeholder="请输入邮箱" class="input-field" />
          <button class="submit-btn" @click="bindEmail" :disabled="updating">
            {{ updating ? '绑定中...' : '绑定' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showFavorites" class="modal-overlay" @click.self="showFavorites = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>我的收藏</h3>
          <button class="close-btn" @click="showFavorites = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="favorites.length === 0" class="empty-tip">
            暂无收藏
          </div>
          <div v-else class="item-list">
            <div
              v-for="item in favorites"
              :key="item.recordId || item.id"
              class="list-item"
              @click="goToMovie(item)"
            >
              <img :src="item.poster" :alt="item.title" class="item-poster" />
              <div class="item-info">
                <span class="item-title">{{ item.title }}</span>
                <span class="item-rating">⭐ {{ item.rating }}</span>
                <span class="item-time">{{ formatTime(item.addedAt) }}</span>
              </div>
              <button class="remove-btn" @click.stop="removeFavorite(item)">×</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showBrowseHistory" class="modal-overlay" @click.self="showBrowseHistory = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>浏览记录</h3>
          <button class="close-btn" @click="showBrowseHistory = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="browseHistory.length === 0" class="empty-tip">
            暂无浏览记录
          </div>
          <div v-else class="item-list">
            <div
              v-for="item in browseHistory"
              :key="item.recordId || item.id + item.viewedAt"
              class="list-item"
              @click="goToMovie(item)"
            >
              <img :src="item.poster" :alt="item.title" class="item-poster" />
              <div class="item-info">
                <span class="item-title">{{ item.title }}</span>
                <span class="item-rating">⭐ {{ item.rating }}</span>
                <span class="item-time">{{ formatTime(item.viewedAt) }}</span>
              </div>
              <button class="remove-btn" @click.stop="removeBrowseHistory(item)">×</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showMyComments" class="modal-overlay" @click.self="showMyComments = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>我的评论</h3>
          <button class="close-btn" @click="showMyComments = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="myComments.length === 0" class="empty-tip">
            暂无评论
          </div>
          <div v-else class="comment-list">
            <div
              v-for="comment in myComments"
              :key="comment.id"
              class="comment-item"
            >
              <div class="comment-header">
                <span class="comment-rating">⭐ {{ comment.rating }}</span>
                <span class="comment-date">{{ formatCommentDate(comment.createdAt) }}</span>
                <button class="remove-btn" @click.stop="deleteComment(comment.id)">×</button>
              </div>
              <div class="comment-content" @click="goToMovieById(comment.movieId)">{{ comment.content }}</div>
              <div class="comment-movie-id">电影ID: {{ comment.movieId }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginModal from '@/components/user/LoginModal.vue'
import request from '@/utils/request'

const router = useRouter()
const authStore = useAuthStore()
const showLoginModal = ref(false)
const showFavorites = ref(false)
const showBrowseHistory = ref(false)
const showMyComments = ref(false)
const showEditNickname = ref(false)
const showBindPhone = ref(false)
const showBindEmail = ref(false)
const updating = ref(false)

const favorites = ref([])
const browseHistory = ref([])
const myComments = ref([])
const orders = ref([])
const browseCount = ref(0)
const favoriteCount = ref(0)
const commentCount = ref(0)

const newNickname = ref('')
const newPhone = ref('')
const newEmail = ref('')

onMounted(() => {
  loadUserData()
})

const loadUserData = () => {
  loadFavorites()
  loadBrowseHistory()
  loadOrders()
  loadUserInfo()
  loadMyComments()
}

const userAvatar = computed(() => {
  if (authStore.user?.nickname) {
    return authStore.user.nickname.charAt(0).toUpperCase()
  }
  if (authStore.user?.username) {
    return authStore.user.username.charAt(0).toUpperCase()
  }
  return '👤'
})

const getStorageKey = (key) => {
  const userId = authStore.user?.id || 'guest'
  return `${key}_${userId}`
}

const loadFavorites = async () => {
  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      const data = await request.get('/favorite/list', {
        params: { userId: authStore.user.id, limit: 50 }
      })
      favorites.value = data.map(item => ({
        recordId: item.id,
        id: item.movie_id,
        title: item.title,
        poster: item.poster,
        rating: item.rating,
        genre: item.genre,
        duration: item.duration,
        addedAt: item.created_at
      }))
      favoriteCount.value = favorites.value.length
      return
    } catch (error) {
      console.error('Load favorites from server error:', error)
    }
  }

  const stored = localStorage.getItem(getStorageKey('movieFavorites'))
  favorites.value = stored ? JSON.parse(stored) : []
  favoriteCount.value = favorites.value.length
}

const saveFavorites = () => {
  localStorage.setItem(getStorageKey('movieFavorites'), JSON.stringify(favorites.value))
}

const removeFavorite = async (item) => {
  if (authStore.isAuthenticated && item.recordId) {
    try {
      await request.delete(`/favorite/delete/${item.recordId}`)
      favorites.value = favorites.value.filter(f => f.recordId !== item.recordId)
      favoriteCount.value = Math.max(0, favoriteCount.value - 1)
    } catch (error) {
      console.error('Remove favorite error:', error)
    }
  } else {
    favorites.value = favorites.value.filter(f => f.id !== item.id)
    favoriteCount.value = Math.max(0, favoriteCount.value - 1)
    saveFavorites()
  }
}

const loadBrowseHistory = async () => {
  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      const data = await request.get('/browse/list', {
        params: { userId: authStore.user.id, limit: 20 }
      })
      browseHistory.value = data.map(item => ({
        recordId: item.id,
        id: item.movie_id,
        title: item.title,
        poster: item.poster,
        rating: item.rating,
        genre: item.genre,
        duration: item.duration,
        viewedAt: item.created_at,
        username: item.username,
        nickname: item.nickname,
        phone: item.phone
      }))
      browseCount.value = browseHistory.value.length
      return
    } catch (error) {
      console.error('Load browse history from server error:', error)
    }
  }

  const stored = localStorage.getItem(getStorageKey('browseHistory'))
  browseHistory.value = stored ? JSON.parse(stored) : []
  browseCount.value = browseHistory.value.length
}

const removeBrowseHistory = async (item) => {
  if (authStore.isAuthenticated && item.recordId) {
    try {
      await request.delete(`/browse/delete/${item.recordId}`)
      browseHistory.value = browseHistory.value.filter(h => h.recordId !== item.recordId)
      browseCount.value = Math.max(0, browseCount.value - 1)
    } catch (error) {
      console.error('Delete browse history error:', error)
    }
  } else {
    browseHistory.value = browseHistory.value.filter(h => h.id !== item.id)
    browseCount.value = Math.max(0, browseCount.value - 1)
    saveBrowseHistory()
  }
}

const saveBrowseHistory = () => {
  localStorage.setItem(getStorageKey('browseHistory'), JSON.stringify(browseHistory.value))
}

const loadMyComments = async () => {
  if (authStore.isAuthenticated && authStore.user?.id) {
    try {
      myComments.value = await request.get(`/comments/user/${authStore.user.id}`)
      commentCount.value = myComments.value.length
    } catch (error) {
      console.error('Load my comments error:', error)
    }
  }
}

const deleteComment = async (commentId) => {
  if (!authStore.isAuthenticated || !authStore.user?.id) {
    alert('请先登录')
    return
  }
  
  if (!confirm('确定要删除这条评论吗？')) {
    return
  }
  
  try {
    await request.delete(`/comments/${commentId}`, {
      params: { userId: authStore.user.id }
    })
    myComments.value = myComments.value.filter(c => c.id !== commentId)
    commentCount.value = Math.max(0, commentCount.value - 1)
    alert('删除成功')
  } catch (error) {
    console.error('Delete comment error:', error)
    alert(error.message || '删除失败')
  }
}

const loadOrders = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) {
    orders.value = []
    return
  }
  
  try {
    orders.value = await request.get(`/orders/user/${authStore.user.id}`)
    return
  } catch (error) {
    console.error('Load orders from server error:', error)
  }
  
  const stored = localStorage.getItem(getStorageKey('orders'))
  orders.value = stored ? JSON.parse(stored) : []
}

const loadUserInfo = async () => {
  if (!authStore.user?.id) return
  try {
    const data = await request.get('/auth/userinfo', {
      params: { userId: authStore.user.id }
    })
    authStore.user = data
    localStorage.setItem('user', JSON.stringify(data))
  } catch (error) {
    console.error('Load user info error:', error)
  }
}

const orderCount = computed(() => orders.value.length)

const openLoginModal = () => {
  showLoginModal.value = true
}

const navigateToOrders = () => {
  router.push('/orders')
}

// const navigateToVip = () => {
//   router.push('/vip')
// }

const goToMovie = (item) => {
  showFavorites.value = false
  showBrowseHistory.value = false
  router.push({ path: '/movie', query: { movieId: item.id } })
}

const goToMovieById = (movieId) => {
  showMyComments.value = false
  router.push({ path: '/movie', query: { movieId: movieId } })
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${Math.floor(diff / 86400000)}天前`
}

const formatCommentDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatPhone = (phone) => {
  if (!phone) return '未绑定'
  if (phone.length >= 7) {
    return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
  }
  return phone
}

const openBindPhone = () => {
  newPhone.value = authStore.user?.phone || ''
  showBindPhone.value = true
}

const openBindEmail = () => {
  newEmail.value = authStore.user?.email || ''
  showBindEmail.value = true
}

const updateNickname = async () => {
  if (!newNickname.value.trim()) {
    alert('请输入昵称')
    return
  }
  updating.value = true
  try {
    const result = await authStore.updateUser({
      id: authStore.user.id,
      nickname: newNickname.value
    })
    if (result.success) {
      await authStore.loadUser()
      showEditNickname.value = false
      newNickname.value = ''
    } else {
      alert(result.message)
    }
  } finally {
    updating.value = false
  }
}

const bindPhone = async () => {
  if (!newPhone.value.trim() || !/^1[3-9]\d{9}$/.test(newPhone.value)) {
    alert('请输入正确的手机号')
    return
  }
  updating.value = true
  try {
    const result = await authStore.updateUser({
      id: authStore.user.id,
      phone: newPhone.value
    })
    if (result.success) {
      await authStore.loadUser()
      showBindPhone.value = false
      newPhone.value = ''
    } else {
      alert(result.message)
    }
  } finally {
    updating.value = false
  }
}

const bindEmail = async () => {
  if (!newEmail.value.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newEmail.value)) {
    alert('请输入正确的邮箱')
    return
  }
  updating.value = true
  try {
    const result = await authStore.updateUser({
      id: authStore.user.id,
      email: newEmail.value
    })
    if (result.success) {
      await authStore.loadUser()
      showBindEmail.value = false
      newEmail.value = ''
    } else {
      alert(result.message)
    }
  } finally {
    updating.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 40px 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #333;
  margin: 0 0 30px 0;
}

.profile-content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
  margin-bottom: 24px;
}

.avatar {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
  font-weight: bold;
}

.info h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
  color: #333;
}

.user-id {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #666;
}

.info-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  color: #333;
  margin: 0 0 16px 0;
  font-weight: 600;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-label {
  width: 60px;
  font-size: 14px;
  color: #999;
}

.info-value {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.edit-btn {
  padding: 4px 12px;
  background: #f5f5f5;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  color: #667eea;
  cursor: pointer;
}

.edit-btn:hover {
  background: #eee;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  text-decoration: none;
  color: inherit;
}

.menu-item:hover {
  background: #f0f0f0;
}

.menu-item .icon {
  font-size: 18px;
}

.menu-item span:nth-child(2) {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.menu-item .badge {
  background: #e74c3c;
  color: #fff;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.menu-item .arrow {
  font-size: 18px;
  color: #ccc;
}

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
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.modal-content.small {
  max-width: 360px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.input-field {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
  margin-bottom: 12px;
}

.input-field:focus {
  outline: none;
  border-color: #667eea;
}

.modal-body .submit-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.modal-body .submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.empty-tip {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
}

.list-item:hover {
  background: #f0f0f0;
}

.item-poster {
  width: 50px;
  height: 70px;
  object-fit: cover;
  border-radius: 4px;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-title {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.item-rating {
  font-size: 12px;
  color: #ffb800;
}

.item-time {
  font-size: 12px;
  color: #999;
}

.remove-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  border: none;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}

.remove-btn:hover {
  background: rgba(0, 0, 0, 0.2);
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  position: relative;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.comment-header .remove-btn {
  position: static;
  margin-left: auto;
}

.comment-rating {
  color: #ffb800;
  font-weight: bold;
  font-size: 14px;
}

.comment-date {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

.comment-content {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  margin-bottom: 8px;
}

.comment-movie-id {
  font-size: 12px;
  color: #999;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
