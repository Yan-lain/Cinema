<template>
  <div class="orders-page">
    <div class="page-header">
      <h1>我的订单</h1>
    </div>

    <div class="orders-content">
      <div class="orders-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          :class="['tab-btn', { active: activeTab === tab.value }]"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
          <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
        </button>
      </div>

      <div class="orders-list">
        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="filteredOrders.length === 0" class="empty-state">
          <span class="empty-icon">📋</span>
          <p>暂无订单</p>
        </div>

        <div v-else v-for="order in filteredOrders" :key="order.id" class="order-card">
          <div class="order-header">
            <span class="order-id">订单号：{{ order.orderNumber }}</span>
            <div class="status-badges">
              <span :class="['status-badge', order.status]">{{ statusLabels[order.status] }}</span>
              <span v-if="order.payStatus !== 'none'" :class="['status-badge', 'pay-' + order.payStatus]">
                {{ payStatusLabels[order.payStatus] }}
              </span>
              <span v-if="order.refundStatus !== 'none'" :class="['status-badge', 'refund-' + order.refundStatus]">
                {{ refundStatusLabels[order.refundStatus] }}
              </span>
            </div>
          </div>
          
          <div class="order-body">
            <div class="order-info">
              <h3>{{ order.movieTitle }}</h3>
              <div class="info-row">
                <span class="info-label">影院</span>
                <span>{{ order.cinemaName }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">放映厅</span>
                <span>{{ order.hallName }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">场次</span>
                <span>{{ order.showTime }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">座位</span>
                <span>{{ order.seats }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">数量</span>
                <span>{{ order.seatCount }} 张</span>
              </div>
              <div class="info-row">
                <span class="info-label">下单时间</span>
                <span>{{ formatDate(order.createdAt) }}</span>
              </div>
              <div v-if="order.paidAt" class="info-row">
                <span class="info-label">支付时间</span>
                <span>{{ formatDate(order.paidAt) }}</span>
              </div>
              <div v-if="order.refundedAt" class="info-row">
                <span class="info-label">退票时间</span>
                <span>{{ formatDate(order.refundedAt) }}</span>
              </div>
            </div>
            <div class="order-price">
              <span class="price">¥{{ order.totalPrice }}</span>
            </div>
          </div>
          
          <div class="order-actions">
            <button
              v-if="canRefund(order)"
              class="action-btn refund"
              @click="handleRefund(order)"
            >
              申请退票
            </button>
            <button
              v-if="canShowTicket(order)"
              class="action-btn ticket"
              @click="showTicket(order)"
            >
              电子票
            </button>
            <button
              v-if="order.status === 'pending' && order.payStatus === 'unpaid'"
              class="action-btn pay"
              @click="goToPay(order)"
            >
              去支付
            </button>
            <button
              v-if="order.status === 'pending' && order.payStatus === 'unpaid'"
              class="action-btn cancel"
              @click="cancelOrder(order)"
            >
              取消订单
            </button>
            <button
              v-if="order.status === 'cancelled'"
              class="action-btn delete"
              @click="deleteOrder(order)"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showTicketModal" class="ticket-overlay" @click.self="showTicketModal = false">
      <div class="ticket-modal">
        <div class="ticket-header">
          <h2>电子票</h2>
          <button class="close-btn" @click="showTicketModal = false">×</button>
        </div>
        <div class="ticket-content" v-if="currentTicket">
          <div class="ticket-qr">
            <img :src="qrCodeUrl" class="qr-img" alt="二维码" />
            <p class="qr-label">电子票二维码</p>
          </div>
          <div class="ticket-info">
            <div class="info-row">
              <span>电影</span>
              <span>{{ currentTicket.movieTitle }}</span>
            </div>
            <div class="info-row">
              <span>影院</span>
              <span>{{ currentTicket.cinemaName }}</span>
            </div>
            <div class="info-row">
              <span>放映厅</span>
              <span>{{ currentTicket.hallName }}</span>
            </div>
            <div class="info-row">
              <span>场次</span>
              <span>{{ currentTicket.showTime }}</span>
            </div>
            <div class="info-row">
              <span>座位</span>
              <span>{{ currentTicket.seats }}</span>
            </div>
            <div class="info-row">
              <span>数量</span>
              <span>{{ currentTicket.seatCount }} 张</span>
            </div>
            <div class="info-row">
              <span>订单号</span>
              <span>{{ currentTicket.orderNumber }}</span>
            </div>
            <div class="info-row">
              <span>状态</span>
              <span>{{ statusLabels[currentTicket.status] }}</span>
            </div>
          </div>
          <div class="ticket-footer">
            <p>请出示此电子票或出示订单号入场</p>
            <p class="tips">如有疑问，请联系客服</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('all')
const showTicketModal = ref(false)
const currentTicket = ref(null)
const loading = ref(false)
const orders = ref([])
const qrCodeUrl = ref('')

const tabs = ref([
  { label: '全部', value: 'all', count: 0 },
  { label: '待支付', value: 'pending', count: 0 },
  { label: '已支付', value: 'paid', count: 0 },
  { label: '已完成', value: 'completed', count: 0 },
  { label: '已退票', value: 'refunded', count: 0 }
])

const statusLabels = {
  pending: '待支付',
  completed: '已完成',
  cancelled: '已取消',
  refunded: '已退票'
}

const payStatusLabels = {
  unpaid: '未支付',
  paid: '已支付',
  none: ''
}

const refundStatusLabels = {
  none: '',
  pending: '退票处理中',
  refunded: '已退票'
}

onMounted(() => {
  loadOrders()
})

const loadOrders = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) {
    return
  }

  loading.value = true
  try {
    const orderList = await request.get(`/orders/user/${authStore.user.id}`)
    
    const detailedOrders = await Promise.all(
      orderList.map(async (order) => {
        const orderWithDetails = {
          ...order,
          movieTitle: '',
          cinemaName: '',
          hallName: '',
          showTime: '',
          seats: '',
          seatCount: 0,
          scheduleExpired: false
        }

        const schedules = await request.get('/admin/schedules', {
          params: { cinemaId: order.cinemaId }
        })
        const schedule = schedules.find(s => s.id === order.scheduleId)
        if (schedule) {
          orderWithDetails.showTime = formatTime(schedule.showTime)
          const showTime = new Date(schedule.showTime)
          const now = new Date()
          orderWithDetails.scheduleExpired = showTime < now
          const diffMs = showTime - now
          orderWithDetails.minutesUntilShow = Math.max(0, Math.floor(diffMs / (1000 * 60)))

          const movieData = await request.get(`/movies/${schedule.movieId}`)
          orderWithDetails.movieTitle = movieData.title

          const cinemaData = await request.get(`/cinemas/${schedule.cinemaId}`)
          orderWithDetails.cinemaName = cinemaData.name

          const hallData = await request.get(`/halls/${schedule.hallId}`)
          orderWithDetails.hallName = hallData.hallNumber
        }

        const orderSeats = await request.get(`/order-seats/order/${order.id}`)
        const seats = []
        for (const os of orderSeats) {
          const seatData = await request.get(`/seats/${os.seatId}`)
          seats.push(`${String.fromCharCode(64 + seatData.rowNum)}${seatData.colNum}`)
        }
        orderWithDetails.seats = seats.join(', ')
        orderWithDetails.seatCount = seats.length

        return orderWithDetails
      })
    )

    orders.value = detailedOrders
    updateTabCounts()
  } catch (error) {
    console.error('Load orders error:', error)
  } finally {
    loading.value = false
  }
}

const updateTabCounts = () => {
  tabs.value = tabs.value.map(tab => {
    if (tab.value === 'all') {
      return { ...tab, count: orders.value.length }
    } else if (tab.value === 'paid') {
      return { ...tab, count: orders.value.filter(o => o.payStatus === 'paid').length }
    } else if (tab.value === 'refunded') {
      return { ...tab, count: orders.value.filter(o => o.refundStatus === 'refunded').length }
    } else {
      return { ...tab, count: orders.value.filter(o => o.status === tab.value).length }
    }
  })
}

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  if (activeTab.value === 'paid') return orders.value.filter(o => o.payStatus === 'paid')
  if (activeTab.value === 'refunded') return orders.value.filter(o => o.refundStatus === 'refunded')
  return orders.value.filter(o => o.status === activeTab.value)
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const showTicket = (order) => {
  currentTicket.value = order
  qrCodeUrl.value = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(order.orderNumber)}`
  showTicketModal.value = true
}

const canRefund = (order) => {
  // 场次未过期且距离开场超过半小时才能退票
  return order.status === 'completed' && 
         order.payStatus === 'paid' && 
         order.refundStatus === 'none' &&
         order.scheduleExpired === false &&
         (order.minutesUntilShow === undefined || order.minutesUntilShow > 30)
}

const canShowTicket = (order) => {
  return order.status === 'completed' && order.payStatus === 'paid'
}

const handleRefund = async (order) => {
  if (!confirm('确定要申请退票吗？退票金额将原路返回。')) return

  try {
    await request.put(`/orders/${order.id}/refund`)
    alert('退票申请成功，款项将在3-5个工作日内原路返回')
    loadOrders()
  } catch (error) {
    console.error('Refund error:', error)
    alert(error.message || '退票失败')
  }
}

const cancelOrder = async (order) => {
  if (!confirm('确定要取消订单吗？')) return

  try {
    await request.put(`/orders/${order.id}/cancel`)
    alert('订单已取消')
    loadOrders()
  } catch (error) {
    console.error('Cancel order error:', error)
    alert(error.message || '取消订单失败')
  }
}

const deleteOrder = async (order) => {
  if (!confirm('确定要删除这个订单吗？')) return

  try {
    await request.delete(`/orders/${order.id}`)
    alert('订单已删除')
    loadOrders()
  } catch (error) {
    console.error('Delete order error:', error)
    alert(error.message || '删除订单失败')
  }
}

const goToPay = (order) => {
  router.push({ path: '/payment', query: { orderId: order.id.toString() } })
}
</script>

<style scoped>
.orders-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #333;
  margin: 0 0 30px 0;
}

.orders-content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.orders-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 1px solid #eee;
  padding-bottom: 16px;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 8px 20px;
  border: none;
  background: none;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  border-radius: 20px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-btn:hover {
  background: #f5f5f5;
}

.tab-btn.active {
  background: #333;
  color: #fff;
}

.tab-count {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.tab-btn.active .tab-count {
  background: rgba(255, 255, 255, 0.3);
}

.loading-state {
  text-align: center;
  padding: 40px 0;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 60px;
  display: block;
  margin-bottom: 16px;
}

.empty-state p {
  color: #999;
  font-size: 16px;
}

.order-card {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.order-id {
  font-size: 13px;
  color: #999;
}

.status-badges {
  display: flex;
  gap: 8px;
}

.status-badge {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 10px;
}

.status-badge.pending {
  background: #fff3e0;
  color: #ef6c00;
}

.status-badge.completed {
  background: #e8f5e9;
  color: #388e3c;
}

.status-badge.cancelled {
  background: #f5f5f5;
  color: #999;
}

.status-badge.refunded {
  background: #fce4ec;
  color: #c2185b;
}

.status-badge.pay-unpaid {
  background: #fff3e0;
  color: #ef6c00;
}

.status-badge.pay-paid {
  background: #e3f2fd;
  color: #1976d2;
}

.status-badge.refund-pending {
  background: #fff3e0;
  color: #ef6c00;
}

.status-badge.refund-refunded {
  background: #fce4ec;
  color: #c2185b;
}

.order-body {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.order-info {
  flex: 1;
}

.order-info h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #333;
}

.info-row {
  display: flex;
  gap: 10px;
  font-size: 14px;
  color: #666;
  margin-bottom: 6px;
}

.info-label {
  color: #999;
  min-width: 60px;
}

.order-price {
  text-align: right;
  min-width: 100px;
}

.price {
  font-size: 24px;
  font-weight: bold;
  color: #e74c3c;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
  flex-wrap: wrap;
}

.action-btn {
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  border: 1px solid #ddd;
  color: #333;
}

.action-btn:hover {
  background: #f5f5f5;
}

.action-btn.pay {
  background: #667eea;
  border-color: #667eea;
  color: #fff;
}

.action-btn.pay:hover {
  background: #5a6fd6;
}

.action-btn.refund {
  border-color: #ff9800;
  color: #ff9800;
}

.action-btn.refund:hover {
  background: #fff3e0;
}

.action-btn.ticket {
  border-color: #4caf50;
  color: #4caf50;
}

.action-btn.ticket:hover {
  background: #e8f5e9;
}

.action-btn.cancel {
  border-color: #e74c3c;
  color: #e74c3c;
}

.action-btn.cancel:hover {
  background: #fef2f2;
}

.action-btn.delete {
  border-color: #999;
  color: #999;
}

.action-btn.delete:hover {
  background: #f5f5f5;
  color: #666;
  border-color: #666;
}

.ticket-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.ticket-modal {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  overflow: hidden;
}

.ticket-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.ticket-header h2 {
  margin: 0;
  font-size: 18px;
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

.ticket-content {
  padding: 24px;
}

.ticket-qr {
  text-align: center;
  margin-bottom: 24px;
}

.qr-img {
  width: 180px;
  height: 180px;
  border-radius: 12px;
  border: 2px solid #e8e8e8;
}

.qr-label {
  margin: 10px 0 0 0;
  font-size: 13px;
  color: #999;
}

.ticket-info {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
}

.ticket-info .info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  border-bottom: 1px solid #eee;
}

.ticket-info .info-row:last-child {
  border-bottom: none;
}

.ticket-info .info-row span:first-child {
  color: #999;
}

.ticket-info .info-row span:last-child {
  color: #333;
  font-weight: 500;
}

.ticket-footer {
  text-align: center;
}

.ticket-footer p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.ticket-footer .tips {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>
