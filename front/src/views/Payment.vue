<template>
  <div class="payment-page">
    <div class="payment-card">
      <!-- 支付成功状态 -->
      <div v-if="paymentStatus === 'success'" class="status-card success">
        <div class="status-icon">✓</div>
        <h2>支付成功</h2>
        <p class="order-number">订单号：{{ orderInfo.orderNumber }}</p>
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

      <!-- 支付失败状态 -->
      <div v-else-if="paymentStatus === 'failed'" class="status-card failed">
        <div class="status-icon">✗</div>
        <h2>支付失败</h2>
        <p class="error-message">{{ errorMessage }}</p>
        <button class="action-btn retry" @click="retryPayment">重新支付</button>
        <button class="action-btn cancel" @click="goToHome">返回首页</button>
      </div>

      <!-- 支付处理中 -->
      <div v-else-if="paymentStatus === 'processing'" class="status-card processing">
        <div class="loading-spinner"></div>
        <h2>支付处理中...</h2>
        <p>请稍候，正在处理您的支付请求</p>
      </div>

      <!-- 默认支付页面 -->
      <div v-else>
        <div class="success-header">
          <div class="success-icon">📋</div>
          <h2>确认支付</h2>
          <p>订单号：{{ orderInfo.orderNumber }}</p>
        </div>

        <div class="order-details">
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
            <span>应付金额</span>
            <span>¥{{ orderInfo.totalPrice }}</span>
          </div>
        </div>

        <div class="payment-methods">
          <h3>选择支付方式</h3>
          <div class="methods">
            <div
              v-for="method in paymentMethods"
              :key="method.id"
              :class="['method', { active: selectedMethod === method.id }]"
              @click="selectedMethod = method.id"
            >
              <span class="method-name">{{ method.name }}</span>
            </div>
          </div>
        </div>

        <div class="actions">
          <button class="pay-btn" @click="showPayConfirmModal = true" :disabled="processing">
            {{ processing ? '支付中...' : `确认支付 ¥${orderInfo.totalPrice}` }}
          </button>
          <button class="cancel-btn" @click="handleCancel">取消订单</button>
        </div>

        <p class="notice">请在15分钟内完成支付，超时订单将自动取消</p>
      </div>
    </div>

    <!-- 支付确认弹窗 -->
    <div v-if="showPayConfirmModal" class="modal-overlay" @click="showPayConfirmModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>确认支付</h3>
          <span class="modal-close" @click="showPayConfirmModal = false">×</span>
        </div>
        <div class="modal-body">
          <p>您即将支付以下订单：</p>
          <div class="confirm-order-info">
            <div class="confirm-row">
              <span>电影</span>
              <span>{{ orderInfo.movieTitle }}</span>
            </div>
            <div class="confirm-row">
              <span>场次</span>
              <span>{{ orderInfo.showTime }}</span>
            </div>
            <div class="confirm-row">
              <span>座位</span>
              <span>{{ orderInfo.seats }}</span>
            </div>
            <div class="confirm-row total">
              <span>支付金额</span>
              <span>¥{{ orderInfo.totalPrice }}</span>
            </div>
          </div>
          <p class="confirm-notice">确认支付后，款项将立即扣除</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="showPayConfirmModal = false">取消</button>
          <button class="modal-btn confirm" @click="confirmPay">确认支付</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const API_BASE_URL = '/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

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
const selectedMethod = ref('wechat')
const processing = ref(false)
const paymentStatus = ref('') // '', 'processing', 'success', 'failed'
const errorMessage = ref('')
const showPayConfirmModal = ref(false)

const paymentMethods = [
  { id: 'wechat', name: '微信支付'},
  { id: 'alipay', name: '支付宝'},
  { id: 'bank', name: '银行卡'}
  // vip
]

onMounted(() => {
  loadOrderInfo()
})

const loadOrderInfo = async () => {
  if (!orderId.value) {
    alert('订单ID不能为空')
    router.push('/')
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
      router.push('/')
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

const confirmPay = () => {
  showPayConfirmModal.value = false
  handlePay()
}

const handlePay = async () => {
  if (processing.value) return
  processing.value = true
  paymentStatus.value = 'processing'

  try {
    const response = await fetch(`${API_BASE_URL}/orders/${orderId.value}/pay`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        paymentMethod: selectedMethod.value
      })
    })
    const data = await response.json()
    
    if (data.success) {
      paymentStatus.value = 'success'
    } else {
      paymentStatus.value = 'failed'
      errorMessage.value = data.message || '支付失败'
    }
  } catch (error) {
    console.error('Payment error:', error)
    paymentStatus.value = 'failed'
    errorMessage.value = '网络错误，请稍后重试'
  }
  
  processing.value = false
}

const handleCancel = async () => {
  if (!confirm('确定要取消订单吗？')) return

  try {
    const response = await fetch(`${API_BASE_URL}/orders/${orderId.value}/cancel`, {
      method: 'PUT'
    })
    const data = await response.json()
    if (data.success) {
      alert('订单已取消')
      router.push('/')
    } else {
      alert('取消订单失败: ' + data.message)
    }
  } catch (error) {
    console.error('Cancel order error:', error)
    alert('取消订单失败')
  }
}

const retryPayment = () => {
  paymentStatus.value = ''
  errorMessage.value = ''
}

const goToOrders = () => {
  router.push('/orders')
}

const goToHome = () => {
  router.push('/')
}
</script>

<style scoped>
.payment-page {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.payment-card {
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

.success-header {
  text-align: center;
  margin-bottom: 30px;
}

.success-header .success-icon {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  font-size: 36px;
  color: white;
}

.success-header h2 {
  font-size: 24px;
  color: #333;
  margin: 0 0 8px 0;
}

.success-header p {
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

.detail-row span:first-child {
  color: #999;
}

.payment-methods h3 {
  font-size: 16px;
  color: #333;
  margin: 0 0 16px 0;
}

.methods {
  display: flex;
  gap: 12px;
  margin-bottom: 30px;
}

.method {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border: 2px solid #eee;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.method:hover {
  border-color: #667eea;
}

.method.active {
  border-color: #667eea;
  background: #f8f5ff;
}

.method-icon {
  font-size: 28px;
}

.method-name {
  font-size: 13px;
  color: #333;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pay-btn {
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.pay-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.pay-btn:disabled {
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
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  overflow: hidden;
  animation: slideUp 0.3s ease;
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
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.modal-close {
  font-size: 28px;
  color: #999;
  cursor: pointer;
  line-height: 1;
  transition: color 0.2s;
}

.modal-close:hover {
  color: #333;
}

.modal-body {
  padding: 24px;
}

.modal-body p {
  font-size: 14px;
  color: #666;
  margin: 0 0 16px 0;
}

.confirm-order-info {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.confirm-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
  color: #666;
  border-bottom: 1px solid #eee;
}

.confirm-row:last-child {
  border-bottom: none;
}

.confirm-row span:first-child {
  color: #999;
}

.confirm-row.total {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.confirm-notice {
  font-size: 12px;
  color: #999 !important;
  text-align: center;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #eee;
}

.modal-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-btn.cancel {
  background: #f5f5f5;
  color: #666;
}

.modal-btn.cancel:hover {
  background: #eee;
}

.modal-btn.confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.modal-btn.confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}
</style>
