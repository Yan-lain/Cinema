<template>
  <div class="refund-page">
    <div class="refund-card">
      <!-- 退票成功状态 -->
      <div v-if="refundStatus === 'success'" class="status-card success">
        <div class="status-icon">✓</div>
        <h2>退票成功</h2>
        <p class="order-number">订单号：{{ orderInfo.orderNumber }}</p>
        <div class="refund-amount">
          <span class="amount-label">退款金额</span>
          <span class="amount-value">¥{{ orderInfo.totalPrice }}</span>
        </div>
        <p class="refund-tips">款项将在3-5个工作日内原路返回</p>
        <div class="order-summary">
          <div class="summary-row">
            <span>电影</span>
            <span>{{ orderInfo.movieTitle }}</span>
          </div>
          <div class="summary-row">
            <span>场次</span>
            <span>{{ orderInfo.showTime }}</span>
          </div>
          <div class="summary-row">
            <span>座位</span>
            <span>{{ orderInfo.seats }}</span>
          </div>
        </div>
        <button class="action-btn" @click="goToOrders">查看订单</button>
      </div>

      <!-- 退票失败状态 -->
      <div v-else-if="refundStatus === 'failed'" class="status-card failed">
        <div class="status-icon">✗</div>
        <h2>退票失败</h2>
        <p class="error-message">{{ errorMessage }}</p>
        <div class="order-summary">
          <div class="summary-row">
            <span>订单号</span>
            <span>{{ orderInfo.orderNumber }}</span>
          </div>
        </div>
        <button class="action-btn retry" @click="retryRefund">重新申请退票</button>
        <button class="action-btn cancel" @click="goToOrders">返回订单列表</button>
      </div>

      <!-- 退票处理中 -->
      <div v-else-if="refundStatus === 'processing'" class="status-card processing">
        <div class="loading-spinner"></div>
        <h2>处理中...</h2>
        <p>请稍候，正在处理您的退票请求</p>
      </div>

      <!-- 默认退票申请页面 -->
      <div v-else>
        <div class="refund-header">
          <div class="refund-icon">🎫</div>
          <h2>申请退票</h2>
          <p>请确认以下订单信息</p>
        </div>

        <div class="order-details">
          <div class="detail-row">
            <span>订单号</span>
            <span>{{ orderInfo.orderNumber }}</span>
          </div>
          <div class="detail-row">
            <span>电影</span>
            <span>{{ orderInfo.movieTitle }}</span>
          </div>
          <div class="detail-row">
            <span>影院</span>
            <span>{{ orderInfo.cinemaName }}</span>
          </div>
          <div class="detail-row">
            <span>放映厅</span>
            <span>{{ orderInfo.hallName }}</span>
          </div>
          <div class="detail-row">
            <span>场次</span>
            <span>{{ orderInfo.showTime }}</span>
          </div>
          <div class="detail-row">
            <span>座位</span>
            <span>{{ orderInfo.seats }}</span>
          </div>
          <div class="detail-row">
            <span>数量</span>
            <span>{{ orderInfo.seatCount }} 张</span>
          </div>
          <div class="detail-row total">
            <span>退款金额</span>
            <span>¥{{ orderInfo.totalPrice }}</span>
          </div>
        </div>

        <div class="refund-reason">
          <h3>退票原因</h3>
          <div class="reason-options">
            <div
              v-for="reason in refundReasons"
              :key="reason.id"
              :class="['reason', { active: selectedReason === reason.id }]"
              @click="selectedReason = reason.id"
            >
              <span class="reason-icon">{{ reason.icon }}</span>
              <span class="reason-text">{{ reason.text }}</span>
            </div>
          </div>
        </div>

        <div class="refund-notes">
          <textarea
            v-model="refundNotes"
            placeholder="请输入补充说明（选填）"
            rows="3"
          ></textarea>
        </div>

        <div class="actions">
          <button class="refund-btn" @click="handleRefund" :disabled="processing">
            {{ processing ? '处理中...' : '确认退票' }}
          </button>
          <button class="cancel-btn" @click="goToOrders">取消</button>
        </div>

        <p class="notice">退票后款项将在3-5个工作日内原路返回</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const API_BASE_URL = '/api'

const route = useRoute()
const router = useRouter()

const orderId = ref(route.query.orderId)
const orderInfo = ref({
  orderNumber: '',
  movieTitle: '',
  cinemaName: '',
  hallName: '',
  showTime: '',
  seats: '',
  seatCount: 0,
  totalPrice: 0
})
const selectedReason = ref('schedule')
const refundNotes = ref('')
const processing = ref(false)
const refundStatus = ref('') // '', 'processing', 'success', 'failed'
const errorMessage = ref('')

const refundReasons = [
  { id: 'schedule', icon: '📅', text: '行程变更' },
  { id: 'illness', icon: '🤒', text: '身体不适' },
  { id: 'movie', icon: '🎬', text: '不想看了' },
  { id: 'other', icon: '📝', text: '其他原因' }
]

onMounted(() => {
  loadOrderInfo()
})

const loadOrderInfo = async () => {
  if (!orderId.value) {
    alert('订单ID不能为空')
    router.push('/orders')
    return
  }

  try {
    const response = await fetch(`${API_BASE_URL}/orders/${orderId.value}`)
    const data = await response.json()
    if (data.success && data.data) {
      const order = data.data
      orderInfo.value.orderNumber = order.orderNumber
      orderInfo.value.totalPrice = order.totalPrice
      
      // 获取场次信息
      const scheduleResponse = await fetch(`${API_BASE_URL}/admin/schedules`)
      const scheduleData = await scheduleResponse.json()
      if (scheduleData.success) {
        const schedule = scheduleData.data.find(s => s.id === order.scheduleId)
        if (schedule) {
          orderInfo.value.showTime = formatTime(schedule.showTime)
          
          // 获取电影信息
          const movieResponse = await fetch(`${API_BASE_URL}/movies/${schedule.movieId}`)
          const movieData = await movieResponse.json()
          if (movieData.success) {
            orderInfo.value.movieTitle = movieData.data.title
          }
          
          // 获取影院信息
          const cinemaResponse = await fetch(`${API_BASE_URL}/cinemas/${schedule.cinemaId}`)
          const cinemaData = await cinemaResponse.json()
          if (cinemaData.success) {
            orderInfo.value.cinemaName = cinemaData.data.name
          }
          
          // 获取放映厅信息
          const hallResponse = await fetch(`${API_BASE_URL}/halls/${schedule.hallId}`)
          const hallData = await hallResponse.json()
          if (hallData.success) {
            orderInfo.value.hallName = hallData.data.hallNumber
          }
        }
      }
      
      // 获取座位信息
      const orderSeatResponse = await fetch(`${API_BASE_URL}/order-seats/order/${orderId.value}`)
      const orderSeatData = await orderSeatResponse.json()
      if (orderSeatData.success) {
        const seatIds = orderSeatData.data.map(os => os.seatId)
        const seats = []
        for (const seatId of seatIds) {
          const seatResponse = await fetch(`${API_BASE_URL}/seats/${seatId}`)
          const seatData = await seatResponse.json()
          if (seatData.success) {
            const seat = seatData.data
            seats.push(`${String.fromCharCode(64 + seat.rowNum)}${seat.colNum}`)
          }
        }
        orderInfo.value.seats = seats.join(', ')
        orderInfo.value.seatCount = seats.length
      }
    } else {
      alert('订单不存在')
      router.push('/orders')
    }
  } catch (error) {
    console.error('Load order error:', error)
    alert('加载订单信息失败')
  }
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

const handleRefund = async () => {
  if (processing.value) return
  processing.value = true
  refundStatus.value = 'processing'

  try {
    const response = await fetch(`${API_BASE_URL}/orders/${orderId.value}/refund`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        reason: selectedReason.value,
        notes: refundNotes.value
      })
    })
    const data = await response.json()
    
    if (data.success) {
      refundStatus.value = 'success'
    } else {
      refundStatus.value = 'failed'
      errorMessage.value = data.message || '退票失败'
    }
  } catch (error) {
    console.error('Refund error:', error)
    refundStatus.value = 'failed'
    errorMessage.value = '网络错误，请稍后重试'
  }
  
  processing.value = false
}

const retryRefund = () => {
  refundStatus.value = ''
  errorMessage.value = ''
}

const goToOrders = () => {
  router.push('/orders')
}
</script>

<style scoped>
.refund-page {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.refund-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  width: 100%;
  max-width: 480px;
  padding: 40px;
}

.status-card {
  text-align: center;
  padding: 20px 0;
}

.status-card.success {
  color: #155724;
}

.status-card.failed {
  color: #721c24;
}

.status-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  font-size: 40px;
}

.status-card.success .status-icon {
  background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%);
}

.status-card.failed .status-icon {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
}

.status-card h2 {
  font-size: 24px;
  margin: 0 0 10px 0;
}

.status-card p {
  font-size: 14px;
  margin: 5px 0;
}

.order-number {
  color: #666;
}

.error-message {
  color: #dc3545;
  font-weight: 500;
}

.refund-amount {
  margin: 20px 0;
}

.amount-label {
  display: block;
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}

.amount-value {
  font-size: 36px;
  font-weight: bold;
  color: #e74c3c;
}

.refund-tips {
  color: #666;
  margin-bottom: 20px !important;
}

.order-summary {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  margin: 20px 0;
  text-align: left;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
  border-bottom: 1px solid #eee;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-row span:first-child {
  color: #999;
}

.action-btn {
  padding: 14px 32px;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  margin: 10px 5px;
  transition: all 0.2s;
}

.action-btn.retry {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.action-btn.cancel {
  background: #f5f5f5;
  color: #666;
}

.action-btn:hover {
  transform: translateY(-2px);
}

.refund-header {
  text-align: center;
  margin-bottom: 30px;
}

.refund-header .refund-icon {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  font-size: 36px;
  color: white;
}

.refund-header h2 {
  font-size: 24px;
  color: #333;
  margin: 0 0 8px 0;
}

.refund-header p {
  color: #999;
  font-size: 14px;
  margin: 0;
}

.order-details {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
  color: #666;
  border-bottom: 1px solid #eee;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-row.total {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  padding-top: 16px;
}

.detail-row.total span:last-child {
  color: #e74c3c;
}

.detail-row span:first-child {
  color: #999;
}

.refund-reason h3 {
  font-size: 16px;
  color: #333;
  margin: 0 0 16px 0;
}

.reason-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.reason {
  flex: calc(50% - 5px);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border: 2px solid #eee;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.reason:hover {
  border-color: #ff6b6b;
}

.reason.active {
  border-color: #ff6b6b;
  background: #fff5f5;
}

.reason-icon {
  font-size: 20px;
}

.reason-text {
  font-size: 14px;
  color: #333;
}

.refund-notes textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #eee;
  border-radius: 10px;
  font-size: 14px;
  resize: none;
  box-sizing: border-box;
  margin-bottom: 24px;
}

.refund-notes textarea:focus {
  outline: none;
  border-color: #ff6b6b;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.refund-btn {
  padding: 16px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.refund-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.4);
}

.refund-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-btn {
  padding: 14px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 10px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: #f5f5f5;
}

.notice {
  font-size: 12px;
  color: #999;
  text-align: center;
  margin: 20px 0 0 0;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #ff6b6b;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
