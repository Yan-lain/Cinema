<template>
  <Transition name="modal">
    <div v-if="showModal" class="auth-overlay" @click.self="handleClose">
      <div class="auth-window">
        <div class="window-header">
          <div class="header-decoration"></div>
          <h2>{{ getTitle }}</h2>
        </div>

        <div class="window-body">
          <template v-if="mode === 'login'">
            <form @submit.prevent="handleLogin" class="auth-form">
              <div class="form-group">
                <div class="input-wrapper">
                  <span class="input-icon">👤</span>
                  <input
                    v-model="loginForm.username"
                    type="text"
                    placeholder="用户名"
                  />
                </div>
              </div>
              <div class="form-group">
                <div class="input-wrapper">
                  <span class="input-icon">🔒</span>
                  <input
                    v-model="loginForm.password"
                    type="password"
                    placeholder="密码"
                  />
                </div>
              </div>

              <div v-if="error" class="error-message">
                <span class="error-icon">⚠️</span>
                {{ error }}
              </div>

              <button type="submit" class="submit-btn" :disabled="loading">
                <span v-if="loading" class="btn-spinner"></span>
                {{ loading ? '登录中...' : '登录' }}
              </button>
            </form>

            <div class="auth-footer">
              <span @click.prevent="mode = 'register'" class="footer-link">注册新账号</span>
              <span class="footer-divider">|</span>
              <span @click.prevent="mode = 'forgot'" class="footer-link">忘记密码？</span>
            </div>
          </template>

          <template v-else-if="mode === 'register'">
            <form @submit.prevent="handleRegister" class="auth-form">
              <div class="form-group">
                <div class="input-wrapper">
                  <span class="input-icon">👤</span>
                  <input
                    v-model="registerForm.username"
                    type="text"
                    placeholder="用户名"
                  />
                </div>
              </div>
              <div class="form-group">
                <div class="input-wrapper">
                  <span class="input-icon">📧</span>
                  <input
                    v-model="registerForm.email"
                    type="email"
                    placeholder="邮箱（必填）"
                  />
                </div>
              </div>
              <div class="form-group">
                <div class="input-wrapper">
                  <span class="input-icon">🔒</span>
                  <input
                    v-model="registerForm.password"
                    type="password"
                    placeholder="密码"
                  />
                </div>
              </div>
              <div class="form-group">
                <div class="input-wrapper">
                  <span class="input-icon">🔑</span>
                  <input
                    v-model="registerForm.confirmPassword"
                    type="password"
                    placeholder="确认密码"
                  />
                </div>
              </div>
              <div class="form-group code-group">
                <div class="input-wrapper">
                  <span class="input-icon">✉️</span>
                  <input
                    v-model="registerForm.code"
                    type="text"
                    placeholder="验证码"
                  />
                </div>
                <button type="button" class="code-btn" @click="sendRegisterCode" :disabled="codeDisabled">
                  {{ codeButtonText }}
                </button>
              </div>

              <div v-if="error" class="error-message">
                <span class="error-icon">⚠️</span>
                {{ error }}
              </div>

              <button type="submit" class="submit-btn" :disabled="loading">
                <span v-if="loading" class="btn-spinner"></span>
                {{ loading ? '注册中...' : '注册' }}
              </button>
            </form>

            <div class="auth-footer">
              <span @click.prevent="mode = 'login'" class="footer-link">已有账号？去登录</span>
            </div>
          </template>

          <template v-else-if="mode === 'forgot'">
            <template v-if="!forgotCodeVerified">
              <form @submit.prevent="verifyForgotCode" class="auth-form">
                <div class="form-group">
                  <div class="input-wrapper">
                    <span class="input-icon">📧</span>
                    <input
                      v-model="forgotForm.email"
                      type="email"
                      placeholder="请输入注册邮箱"
                    />
                  </div>
                </div>
                <div class="form-group code-group">
                  <div class="input-wrapper">
                    <span class="input-icon">✉️</span>
                    <input
                      v-model="forgotForm.code"
                      type="text"
                      placeholder="验证码"
                    />
                  </div>
                  <button type="button" class="code-btn" @click="sendForgotCode" :disabled="forgotCodeDisabled">
                    {{ forgotCodeButtonText }}
                  </button>
                </div>

                <div v-if="error" class="error-message">
                  <span class="error-icon">⚠️</span>
                  {{ error }}
                </div>

                <button type="submit" class="submit-btn" :disabled="loading">
                  <span v-if="loading" class="btn-spinner"></span>
                  {{ loading ? '验证中...' : '验证验证码' }}
                </button>
              </form>
            </template>

            <template v-else>
              <form @submit.prevent="handleForgotPassword" class="auth-form">
                <div class="form-group">
                  <div class="input-wrapper">
                    <span class="input-icon">📧</span>
                    <input
                      v-model="forgotForm.email"
                      type="email"
                      placeholder="注册邮箱"
                      disabled
                    />
                  </div>
                </div>
                <div class="form-group">
                  <div class="input-wrapper">
                    <span class="input-icon">🔒</span>
                    <input
                      v-model="forgotForm.newPassword"
                      type="password"
                      placeholder="新密码"
                    />
                  </div>
                </div>
                <div class="form-group">
                  <div class="input-wrapper">
                    <span class="input-icon">🔑</span>
                    <input
                      v-model="forgotForm.confirmPassword"
                      type="password"
                      placeholder="确认新密码"
                    />
                  </div>
                </div>

                <div v-if="error" class="error-message">
                  <span class="error-icon">⚠️</span>
                  {{ error }}
                </div>

                <button type="submit" class="submit-btn" :disabled="loading">
                  <span v-if="loading" class="btn-spinner"></span>
                  {{ loading ? '处理中...' : '重置密码' }}
                </button>
                
                <button type="button" class="submit-btn secondary" @click="resetForgotStep">
                  返回重新验证
                </button>
              </form>
            </template>

            <div class="auth-footer">
              <span @click.prevent="mode = 'login'" class="footer-link">返回登录</span>
            </div>
          </template>

          <template v-else-if="mode === 'success'">
            <div class="success-message">
              <div class="success-icon">✓</div>
              <p>{{ successMessage }}</p>
              <button class="submit-btn" @click="handleClose">确定</button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])
const router = useRouter()
const authStore = useAuthStore()

const showModal = ref(false)

watch(() => props.show, (val) => {
  showModal.value = val
})

const mode = ref('login')
const loading = ref(false)
const error = ref('')
const successMessage = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  code: ''
})

const forgotForm = reactive({
  email: '',
  newPassword: '',
  confirmPassword: '',
  code: ''
})

const codeDisabled = ref(false)
const codeButtonText = ref('获取验证码')
let codeTimer = null

const forgotCodeDisabled = ref(false)
const forgotCodeButtonText = ref('获取验证码')
let forgotCodeTimer = null
const forgotCodeVerified = ref(false)

const getTitle = computed(() => {
  const titles = {
    login: '登录',
    register: '注册',
    forgot: '忘记密码',
    success: '提示'
  }
  return titles[mode.value] || '登录'
})

const handleClose = () => {
  showModal.value = false
  emit('close')
}

const handleLogin = async () => {
  error.value = ''
  loading.value = true

  try {
    if (!loginForm.username || !loginForm.password) {
      throw new Error('请填写用户名和密码')
    }

    const result = await authStore.login(loginForm.username, loginForm.password)
    if (result.success) {
      handleClose()
      if (result.isAdmin) {
        router.push('/admin')
      } else {
        authStore.loadUser()
      }
    } else {
      error.value = result.message
    }
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  error.value = ''
  loading.value = true

  try {
    if (!registerForm.username) {
      throw new Error('请填写用户名')
    }
    if (!registerForm.email) {
      throw new Error('请填写邮箱')
    }
    if (!registerForm.password) {
      throw new Error('请填写密码')
    }
    const passwordError = validatePasswordStrength(registerForm.password)
    if (passwordError) {
      throw new Error(passwordError)
    }
    if (!registerForm.confirmPassword) {
      throw new Error('请填写确认密码')
    }
    if (registerForm.password !== registerForm.confirmPassword) {
      throw new Error('两次输入的密码不一致')
    }
    if (!registerForm.code) {
      throw new Error('请填写验证码')
    }

    const result = await authStore.register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      code: registerForm.code
    })

    if (result.success) {
      handleClose()
      authStore.loadUser()
    } else {
      error.value = result.message
    }
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const sendRegisterCode = async () => {
  if (!registerForm.email) {
    error.value = '请先填写邮箱'
    return
  }
  
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(registerForm.email)) {
    error.value = '请输入有效的邮箱地址'
    return
  }

  codeDisabled.value = true
  codeButtonText.value = '发送中...'

  try {
    await request.post('/auth/sendCode', { email: registerForm.email })
    let count = 60
    codeButtonText.value = `${count}秒后重发`
    codeTimer = setInterval(() => {
      count--
      if (count <= 0) {
        clearInterval(codeTimer)
        codeDisabled.value = false
        codeButtonText.value = '获取验证码'
      } else {
        codeButtonText.value = `${count}秒后重发`
      }
    }, 1000)
  } catch (err) {
    error.value = err.message || '发送失败，请重试'
    codeDisabled.value = false
    codeButtonText.value = '获取验证码'
  }
}

const sendForgotCode = async () => {
  if (!forgotForm.email) {
    error.value = '请先填写邮箱'
    return
  }
  
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(forgotForm.email)) {
    error.value = '请输入有效的邮箱地址'
    return
  }

  forgotCodeDisabled.value = true
  forgotCodeButtonText.value = '发送中...'

  try {
    await request.post('/auth/sendCode', { email: forgotForm.email })
    let count = 60
    forgotCodeButtonText.value = `${count}秒后重发`
    forgotCodeTimer = setInterval(() => {
      count--
      if (count <= 0) {
        clearInterval(forgotCodeTimer)
        forgotCodeDisabled.value = false
        forgotCodeButtonText.value = '获取验证码'
      } else {
        forgotCodeButtonText.value = `${count}秒后重发`
      }
    }, 1000)
  } catch (err) {
    error.value = err.message || '发送失败，请重试'
    forgotCodeDisabled.value = false
    forgotCodeButtonText.value = '获取验证码'
  }
}

const verifyForgotCode = async () => {
  error.value = ''
  loading.value = true

  try {
    if (!forgotForm.email) {
      throw new Error('请填写邮箱')
    }
    if (!forgotForm.code) {
      throw new Error('请填写验证码')
    }

    await request.post('/auth/verifyCode', {
      email: forgotForm.email,
      code: forgotForm.code
    })
    forgotCodeVerified.value = true
    error.value = ''
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const resetForgotStep = () => {
  forgotCodeVerified.value = false
  forgotForm.code = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
  error.value = ''
}

/**
 * 验证密码强度是否符合要求
 * 要求：至少包含大小写字母、数字和特殊字符，长度至少8位
 */
const validatePasswordStrength = (password) => {
  if (password.length < 8) {
    return '密码长度至少8位'
  }
  if (!/(?=.*[a-z])/.test(password)) {
    return '密码需要包含小写字母'
  }
  if (!/(?=.*[A-Z])/.test(password)) {
    return '密码需要包含大写字母'
  }
  if (!/(?=.*\d)/.test(password)) {
    return '密码需要包含数字'
  }
  if (!/(?=.*[@$!%*?&])/.test(password)) {
    return '密码需要包含特殊字符(@$!%*?&)'
  }
  return null
}

const handleForgotPassword = async () => {
  error.value = ''
  loading.value = true

  try {
    if (!forgotForm.newPassword) {
      throw new Error('请填写新密码')
    }
    const passwordError = validatePasswordStrength(forgotForm.newPassword)
    if (passwordError) {
      throw new Error(passwordError)
    }
    if (!forgotForm.confirmPassword) {
      throw new Error('请填写确认密码')
    }
    if (forgotForm.newPassword !== forgotForm.confirmPassword) {
      throw new Error('两次输入的密码不一致')
    }

    await request.post('/auth/forgotPassword', {
      email: forgotForm.email,
      code: forgotForm.code,
      newPassword: forgotForm.newPassword
    })
    mode.value = 'success'
    successMessage.value = '密码重置成功，请重新登录'
    //关闭弹窗
    closeModal()
  } catch (err) {
    error.value = err.message
    // 只有验证码相关错误才重置验证码状态
    if (err.message.includes('验证码')) {
      forgotCodeVerified.value = false
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.auth-window {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  width: 90%;
  max-width: 420px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-lg);
}

.window-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  position: relative;
  overflow: hidden;
}

.header-decoration {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 100px;
  height: 100px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  filter: blur(20px);
}

.window-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
  font-weight: 600;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  transition: all var(--transition-fast);
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.close-btn svg {
  width: 18px;
  height: 18px;
}

.window-body {
  padding: var(--spacing-xl);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.form-group {
  width: 100%;
}

.input-wrapper {
  display: flex;
  align-items: center;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 0 var(--spacing-md);
  transition: all var(--transition-fast);
}

.input-wrapper:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.input-icon {
  font-size: 16px;
  margin-right: var(--spacing-sm);
}

.form-group input {
  width: 100%;
  padding: var(--spacing-md);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--text-primary);
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
}

.form-group input::placeholder {
  color: var(--text-muted);
}

.form-group input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.code-group {
  display: flex;
  gap: var(--spacing-sm);
}

.code-group .input-wrapper {
  flex: 1;
}

.code-btn {
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-fast);
}

.code-btn:hover:not(:disabled) {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--text-primary);
}

.code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.error-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
  color: var(--color-error);
  font-size: 13px;
  padding: var(--spacing-sm);
  background: rgba(239, 68, 68, 0.1);
  border-radius: var(--radius-sm);
}

.error-icon {
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  padding: var(--spacing-md);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  border: none;
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-btn.secondary {
  background: var(--bg-secondary);
  color: var(--text-secondary);
  margin-top: var(--spacing-xs);
}

.submit-btn.secondary:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
  box-shadow: none;
  transform: none;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid var(--text-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.auth-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-md);
  margin-top: var(--spacing-lg);
}

.footer-link {
  color: var(--color-primary-light);
  font-size: 13px;
  cursor: pointer;
  transition: color var(--transition-fast);
}

.footer-link:hover {
  color: var(--color-secondary);
}

.footer-divider {
  color: var(--border-color);
}

.success-message {
  text-align: center;
  padding: var(--spacing-xl) 0;
}

.success-icon {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, var(--color-success), #34d399);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--spacing-lg);
  font-size: 36px;
  color: var(--text-dark);
  animation: scaleIn 0.4s ease;
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.5);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.success-message p {
  margin: 0 0 var(--spacing-lg) 0;
  font-size: 16px;
  color: var(--text-primary);
}

.modal-enter-active,
.modal-leave-active {
  transition: all var(--transition-normal);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .auth-window,
.modal-leave-to .auth-window {
  transform: scale(0.9) translateY(20px);
}
</style>
