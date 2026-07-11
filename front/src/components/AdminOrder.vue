<template>
  <div class="admin-order">
    <div class="toolbar">
      <div class="search-bar">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索订单号..."
          @keyup.enter="loadOrders"
        />
        <button @click="loadOrders">搜索</button>
      </div>
      <div class="filter-section">
        <select v-model="filterStatus" @change="loadOrders">
          <option value="">全部状态</option>
          <option value="pending">待付款</option>
        
          <option value="completed">已完成</option>
    
          <option value="cancelled">已取消</option>
        </select>
      </div>
    </div>
    
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>用户ID</th>
            <th>用户昵称</th>
            <th>场次ID</th>
            <th>电影名称</th>
            <th>放映厅</th>
            <th>座位</th>
            <th>总金额</th>
            <th>状态</th>
            <th>支付状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td>{{ order.orderNumber }}</td>
            <td>{{ order.userId }}</td>
            <td>{{ getUserNickname(order.userId) }}</td>
            <td>{{ order.scheduleId }}</td>
            <td>{{ getMovieTitle(order.scheduleId) }}</td>
            <td>{{ getHallInfo(order.scheduleId) }}</td>
            <td>{{ getSeats(order.id) }}</td>
            <td>¥{{ order.totalPrice }}</td>
            <td>
              <span :class="['status-badge', order.status]">{{ getStatusText(order.status) }}</span>
            </td>
            <td>
              <span :class="['pay-badge', order.payStatus]">{{ getPayStatusText(order.payStatus) }}</span>
            </td>
            <td>{{ formatDate(order.createdAt) }}</td>
            <td>
              <button class="detail-btn" @click="viewDetail(order)">详情</button>
              <button 
                v-if="order.status === 'paid'" 
                class="refund-btn" 
                @click="handleRefund(order)"
              >
                退款
              </button>
              <button class="delete-btn" @click="deleteOrder(order.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <div v-if="orders.length === 0" class="empty-state">
        <span>暂无订单数据</span>
      </div>
    </div>

    <!-- 订单详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content detail-modal" @click.stop>
        <div class="modal-header">
          <h3>订单详情</h3>
          <button class="close-btn" @click="closeDetailModal">×</button>
        </div>
        
        <div v-if="selectedOrder" class="modal-body">
          <div class="detail-section">
            <h4>基本信息</h4>
            <div class="detail-row">
              <span class="label">订单号：</span>
              <span class="value">{{ selectedOrder.orderNumber }}</span>
            </div>
            <div class="detail-row">
              <span class="label">用户ID：</span>
              <span class="value">{{ selectedOrder.userId }}</span>
            </div>
            <div class="detail-row">
              <span class="label">用户昵称：</span>
              <span class="value">{{ getUserNickname(selectedOrder.userId) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">场次ID：</span>
              <span class="value">{{ selectedOrder.scheduleId }}</span>
            </div>
            <div class="detail-row">
              <span class="label">电影名称：</span>
              <span class="value">{{ getMovieTitle(selectedOrder.scheduleId) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">放映厅：</span>
              <span class="value">{{ getHallInfo(selectedOrder.scheduleId) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">放映时间：</span>
              <span class="value">{{ getShowTime(selectedOrder.scheduleId) }}</span>
            </div>
          </div>
          
          <div class="detail-section">
            <h4>座位信息</h4>
            <div class="seats-display">
              <span v-for="seat in selectedSeats" :key="seat" class="seat-tag">{{ seat }}</span>
            </div>
          </div>
          
          <div class="detail-section">
            <h4>支付信息</h4>
            <div class="detail-row">
              <span class="label">总金额：</span>
              <span class="value price">¥{{ selectedOrder.totalPrice }}</span>
            </div>
            <div class="detail-row">
              <span class="label">订单状态：</span>
              <span :class="['value', 'status-text', selectedOrder.status]">{{ getStatusText(selectedOrder.status) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">支付状态：</span>
              <span :class="['value', 'status-text', selectedOrder.payStatus]">{{ getPayStatusText(selectedOrder.payStatus) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">创建时间：</span>
              <span class="value">{{ formatDate(selectedOrder.createdAt) }}</span>
            </div>
            <div class="detail-row" v-if="selectedOrder.paidAt">
              <span class="label">支付时间：</span>
              <span class="value">{{ formatDate(selectedOrder.paidAt) }}</span>
            </div>
            <div class="detail-row" v-if="selectedOrder.refundedAt">
              <span class="label">退款时间：</span>
              <span class="value">{{ formatDate(selectedOrder.refundedAt) }}</span>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeDetailModal">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const API_BASE_URL = '/api/admin'

const orders = ref([])
const searchQuery = ref('')
const filterStatus = ref('')
const showDetailModal = ref(false)
const selectedOrder = ref(null)
const selectedSeats = ref([])

const users = ref([])
const movies = ref([])
const schedules = ref([])
const halls = ref([])
const orderSeats = ref([])
const seats = ref([])

const formatDate = (date) => {
  if (!date) return '-'
  return date.substring(0, 19).replace('T', ' ')
}

const getStatusText = (status) => {
  const map = {
    'pending': '待付款',
    'paid': '已付款',
    'completed': '已完成',
    'refunded': '已退款',
    'cancelled': '已取消'
  }
  return map[status] || status
}

const getPayStatusText = (status) => {
  const map = {
    'unpaid': '未支付',
    'paid': '已支付',
    'refunded': '已退款'
  }
  return map[status] || status
}

const getUserNickname = (userId) => {
  const user = users.value.find(u => u.id === userId)
  return user ? user.nickname : '未知用户'
}

const getMovieTitle = (scheduleId) => {
  const schedule = schedules.value.find(s => s.id === scheduleId)
  if (!schedule) return '未知电影'
  const movie = movies.value.find(m => m.id === schedule.movieId)
  return movie ? movie.title : '未知电影'
}

const getHallInfo = (scheduleId) => {
  const schedule = schedules.value.find(s => s.id === scheduleId)
  if (!schedule) return '未知'
  const hall = halls.value.find(h => h.id === schedule.hallId)
  return hall ? hall.hallNumber : '未知厅'
}

const getShowTime = (scheduleId) => {
  const schedule = schedules.value.find(s => s.id === scheduleId)
  return schedule ? formatDate(schedule.showTime) : '-'
}

const getSeats = (orderId) => {
  const orderSeatList = orderSeats.value.filter(s => s.orderId === orderId)
  if (orderSeatList.length === 0) return '-'
  
  return orderSeatList.map(orderSeat => {
    const seat = seats.value.find(s => s.id === orderSeat.seatId)
    if (seat) {
      return `${seat.rowNum}排${seat.colNum}座`
    }
    return `座位${orderSeat.seatId}`
  }).join(', ')
}

const loadOrders = async () => {
  try {
    let url = `${API_BASE_URL}/orders`
    const params = []
    if (searchQuery.value.trim()) {
      params.push(`q=${encodeURIComponent(searchQuery.value)}`)
    }
    if (filterStatus.value) {
      params.push(`status=${filterStatus.value}`)
    }
    if (params.length > 0) {
      url += '?' + params.join('&')
    }
    
    const response = await fetch(url)
    const data = await response.json()
    if (data.success) {
      orders.value = data.data
    }
  } catch (error) {
    console.error('Load orders error:', error)
  }
}

const loadRelatedData = async () => {
  try {
    const [usersRes, moviesRes, schedulesRes, hallsRes, orderSeatsRes, seatsRes] = await Promise.all([
      fetch('/api/admin/users'),
      fetch('/api/movies'),
      fetch('/api/admin/schedules'),
      fetch('/api/halls'),
      fetch('/api/order-seats'),
      fetch('/api/seats')
    ])
    
    const usersData = await usersRes.json()
    const moviesData = await moviesRes.json()
    const schedulesData = await schedulesRes.json()
    const hallsData = await hallsRes.json()
    const orderSeatsData = await orderSeatsRes.json()
    const seatsData = await seatsRes.json()
    
    if (usersData.success) users.value = usersData.data
    if (moviesData.success) movies.value = moviesData.data
    if (schedulesData.success) schedules.value = schedulesData.data
    if (hallsData.success) halls.value = hallsData.data
    if (orderSeatsData.success) orderSeats.value = orderSeatsData.data
    if (seatsData.success) seats.value = seatsData.data
  } catch (error) {
    console.error('Load related data error:', error)
  }
}

const viewDetail = async (order) => {
  selectedOrder.value = order
  selectedSeats.value = orderSeats.value
    .filter(s => s.orderId === order.id)
    .map(s => `${s.row}排${s.col}座`)
  showDetailModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
  selectedSeats.value = []
}

const handleRefund = async (order) => {
  if (!confirm(`确定要对订单 ${order.orderNumber} 进行退款吗？`)) return
  
  try {
    const response = await fetch(`${API_BASE_URL}/orders/${order.id}/refund`, {
      method: 'PUT'
    })
    const data = await response.json()
    if (data.success) {
      alert(data.message)
      loadOrders()
    } else {
      alert(data.message || '退款失败')
    }
  } catch (error) {
    console.error('Refund error:', error)
    alert('网络错误')
  }
}

const deleteOrder = async (id) => {
  if (!confirm('确定要删除这个订单吗？')) return
  
  try {
    const response = await fetch(`${API_BASE_URL}/orders/${id}`, {
      method: 'DELETE'
    })
    const data = await response.json()
    if (data.success) {
      alert(data.message)
      loadOrders()
    } else {
      alert(data.message || '删除失败')
    }
  } catch (error) {
    console.error('Delete order error:', error)
    alert('网络错误')
  }
}

onMounted(() => {
  loadRelatedData()
  loadOrders()
})
</script>

<style scoped>
.admin-order {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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

.filter-section select {
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
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

.status-badge, .pay-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.pending, .pay-badge.unpaid {
  background: #fff3e0;
  color: #f57c00;
}

.status-badge.paid, .pay-badge.paid {
  background: #d4edda;
  color: #155724;
}

.status-badge.completed {
  background: #e3f2fd;
  color: #1976d2;
}

.status-badge.refunded, .pay-badge.refunded {
  background: #f8d7da;
  color: #721c24;
}

.status-badge.cancelled {
  background: #e2e3e5;
  color: #383d41;
}

.detail-btn, .refund-btn, .delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  margin-right: 6px;
}

.detail-btn {
  background: #e3f2fd;
  color: #1976d2;
}

.refund-btn {
  background: #fff3e0;
  color: #f57c00;
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
}

.detail-modal {
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
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
  overflow-y: auto;
  flex: 1;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
}

.detail-row .label {
  width: 120px;
  font-weight: 500;
  color: #666;
}

.detail-row .value {
  flex: 1;
  color: #333;
}

.detail-row .value.price {
  color: #e74c3c;
  font-weight: 600;
  font-size: 18px;
}

.detail-row .value.status-text.pending,
.detail-row .value.status-text.unpaid {
  color: #f57c00;
}

.detail-row .value.status-text.paid {
  color: #155724;
}

.detail-row .value.status-text.completed {
  color: #1976d2;
}

.detail-row .value.status-text.refunded {
  color: #721c24;
}

.seats-display {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.seat-tag {
  padding: 6px 12px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 13px;
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
</style>