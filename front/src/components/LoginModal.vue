<template>
  <div class="auth-overlay" @click.self="handleClose">
    <div class="auth-window">
      <div class="window-header">
        <h2>{{ getTitle }}</h2>
        <button class="close-btn" @click="handleClose">×</button>
      </div>

      <div class="window-body">
        <template v-if="mode === 'login'">
          <form @submit.prevent="handleLogin" class="auth-form">
            <div class="form-group">
              <input
                v-model="loginForm.username"
                type="text"
                placeholder="用户名"
              />
            </div>
            <div class="form-group">
              <input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
              />
            </div>

            <div v-if="error" class="error-message">{{ error }}</div>

            <button type="submit" class="submit-btn" :disabled="loading">
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </form>

          <div class="auth-footer">
            <a @click.prevent="mode = 'register'" href="#">注册新账号</a>
            <a @click.prevent="mode = 'forgot'" href="#">忘记密码？</a>
          </div>
        </template>

        <template v-else-if="mode === 'register'">
          <form @submit.prevent="handleRegister" class="auth-form">
            <div class="form-group">
              <input
                v-model="registerForm.username"
                type="text"
                placeholder="用户名"
              />
            </div>
            <div class="form-group">
              <input
                v-model="registerForm.email"
                type="email"
                placeholder="邮箱（必填）"
              />
            </div>
            <div class="form-group">
              <input
                v-model="registerForm.password"
                type="password"
                placeholder="密码"
              />
            </div>
            <div class="form-group">
              <input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="确认密码"
              />
            </div>
            <div class="form-group code-group">
              <input
                v-model="registerForm.code"
                type="text"
                placeholder="验证码"
              />
              <button type="button" class="code-btn" @click="sendRegisterCode" :disabled="codeDisabled">
                {{ codeButtonText }}
              </button>
            </div>

            <div v-if="error" class="error-message">{{ error }}</div>

            <button type="submit" class="submit-btn" :disabled="loading">
              {{ loading ? '注册中...' : '注册' }}
            </button>
          </form>

          <div class="auth-footer">
            <a @click.prevent="mode = 'login'" href="#">已有账号？去登录</a>
          </div>
        </template>

        <template v-else-if="mode === 'forgot'">
          <!-- 第一步：输入邮箱和验证码 -->
          <template v-if="!forgotCodeVerified">
            <form @submit.prevent="verifyForgotCode" class="auth-form">
              <div class="form-group">
                <input
                  v-model="forgotForm.email"
                  type="email"
                  placeholder="请输入注册邮箱"
                />
              </div>
              <div class="form-group code-group">
                <input
                  v-model="forgotForm.code"
                  type="text"
                  placeholder="验证码"
                />
                <button type="button" class="code-btn" @click="sendForgotCode" :disabled="forgotCodeDisabled">
                  {{ forgotCodeButtonText }}
                </button>
              </div>

              <div v-if="error" class="error-message">{{ error }}</div>

              <button type="submit" class="submit-btn" :disabled="loading">
                {{ loading ? '验证中...' : '验证验证码' }}
              </button>
            </form>
          </template>

          <!-- 第二步：输入新密码 -->
          <template v-else>
            <form @submit.prevent="handleForgotPassword" class="auth-form">
              <div class="form-group">
                <input
                  v-model="forgotForm.email"
                  type="email"
                  placeholder="注册邮箱"
                  disabled
                />
              </div>
              <div class="form-group">
                <input
                  v-model="forgotForm.newPassword"
                  type="password"
                  placeholder="新密码"
                />
              </div>
              <div class="form-group">
                <input
                  v-model="forgotForm.confirmPassword"
                  type="password"
                  placeholder="确认新密码"
                />
              </div>

              <div v-if="error" class="error-message">{{ error }}</div>

              <button type="submit" class="submit-btn" :disabled="loading">
                {{ loading ? '处理中...' : '重置密码' }}
              </button>
              
              <button type="button" class="submit-btn secondary" @click="resetForgotStep">
                返回重新验证
              </button>
            </form>
          </template>

          <div class="auth-footer">
            <a @click.prevent="mode = 'login'" href="#">返回登录</a>
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
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import API_BASE_URL from '../api'

const emit = defineEmits(['close'])
const authStore = useAuthStore()

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

// 注册验证码相关
const codeDisabled = ref(false)
const codeButtonText = ref('获取验证码')
let codeTimer = null

// 忘记密码验证码相关
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
      emit('close')
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
    if (registerForm.password.length < 6) {
      throw new Error('密码至少6位')
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
      emit('close')
    } else {
      error.value = result.message
    }
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

// 发送注册验证码
const sendRegisterCode = async () => {
  if (!registerForm.email) {
    error.value = '请先填写邮箱'
    return
  }
  
  // 简单的邮箱格式验证
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(registerForm.email)) {
    error.value = '请输入有效的邮箱地址'
    return
  }

  codeDisabled.value = true
  codeButtonText.value = '发送中...'

  try {
    const response = await fetch(`${API_BASE_URL}/auth/sendCode`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email: registerForm.email })
    })
    
    const data = await response.json()
    if (data.success) {
      // 开始倒计时
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
    } else {
      error.value = data.message
      codeDisabled.value = false
      codeButtonText.value = '获取验证码'
    }
  } catch (err) {
    error.value = '发送失败，请重试'
    codeDisabled.value = false
    codeButtonText.value = '获取验证码'
  }
}

// 发送忘记密码验证码
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
    const response = await fetch(`${API_BASE_URL}/auth/sendCode`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email: forgotForm.email })
    })
    
    const data = await response.json()
    if (data.success) {
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
    } else {
      error.value = data.message
      forgotCodeDisabled.value = false
      forgotCodeButtonText.value = '获取验证码'
    }
  } catch (err) {
    error.value = '发送失败，请重试'
    forgotCodeDisabled.value = false
    forgotCodeButtonText.value = '获取验证码'
  }
}

// 忘记密码 - 第一步：验证验证码
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

    const response = await fetch(`${API_BASE_URL}/auth/verifyCode`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        email: forgotForm.email,
        code: forgotForm.code
      })
    })
    
    const data = await response.json()
    if (data.success) {
      forgotCodeVerified.value = true
      error.value = ''
    } else {
      error.value = data.message
    }
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

// 重置忘记密码步骤
const resetForgotStep = () => {
  forgotCodeVerified.value = false
  forgotForm.code = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
  error.value = ''
}

// 忘记密码 - 第二步：重置密码
const handleForgotPassword = async () => {
  error.value = ''
  loading.value = true

  try {
    if (!forgotForm.newPassword) {
      throw new Error('请填写新密码')
    }
    if (forgotForm.newPassword.length < 6) {
      throw new Error('密码至少6位')
    }
    if (!forgotForm.confirmPassword) {
      throw new Error('请填写确认密码')
    }
    if (forgotForm.newPassword !== forgotForm.confirmPassword) {
      throw new Error('两次输入的密码不一致')
    }

    // 使用用户之前输入的验证码进行密码重置
    // 验证码在有效期内（5分钟）可以重复使用
    const response = await fetch(`${API_BASE_URL}/auth/forgotPassword`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        email: forgotForm.email,
        code: forgotForm.code,
        newPassword: forgotForm.newPassword
      })
    })
    
    const data = await response.json()
    if (data.success) {
      mode.value = 'success'
      successMessage.value = '密码重置成功，请重新登录'
    } else {
      error.value = data.message
      // 重置步骤，让用户重新验证
      forgotCodeVerified.value = false
    }
  } catch (err) {
    error.value = err.message
    forgotCodeVerified.value = false
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
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.auth-window {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  overflow: hidden;
}

.window-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.window-header h2 {
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

.window-body {
  padding: 24px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.code-group {
  display: flex;
  gap: 10px;
}

.code-group input {
  flex: 1;
}

.code-btn {
  padding: 12px 20px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  white-space: nowrap;
}

.code-btn:hover:not(:disabled) {
  background: #eee;
}

.code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  color: #e74c3c;
  font-size: 13px;
  text-align: center;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 15px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.submit-btn:hover {
  opacity: 0.9;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-btn.secondary {
  background: #f5f5f5;
  color: #666;
  margin-top: 10px;
}

.submit-btn.secondary:hover {
  background: #eee;
}

.auth-footer {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 16px;
}

.auth-footer a {
  color: #667eea;
  font-size: 13px;
  text-decoration: none;
  cursor: pointer;
}

.success-message {
  text-align: center;
  padding: 20px;
}

.success-icon {
  width: 60px;
  height: 60px;
  background: #e8f5e9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 30px;
  color: #4caf50;
}

.success-message p {
  margin: 0 0 20px 0;
  font-size: 15px;
  color: #333;
}
</style>
