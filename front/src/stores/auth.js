import { defineStore } from 'pinia'
import API_BASE_URL from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || null,
    isAuthenticated: !!localStorage.getItem('token')
  }),

  getters: {
    getUser: (state) => state.user,
    isLoggedIn: (state) => state.isAuthenticated
  },

  actions: {
    async login(username, password) {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ username, password })
        })
        const data = await response.json()
        if (data.success) {
          if (data.data.role === 'admin') {
            return { success: false, message: '网络错误，请稍后重试' }
          }
          this.user = data.data
          this.token = `user_${data.data.id}`
          this.isAuthenticated = true
          localStorage.setItem('token', this.token)
          localStorage.setItem('user', JSON.stringify(data.data))
          return { success: true, message: data.message }
        } else {
          return { success: false, message: data.message }
        }
      } catch (error) {
        console.error('Login error:', error)
        return { success: false, message: '网络错误，请稍后重试' }
      }
    },

    async register(userData) {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(userData)
        })
        const data = await response.json()
        if (data.success) {
          this.user = data.data
          this.token = `user_${data.data.id}`
          this.isAuthenticated = true
          localStorage.setItem('token', this.token)
          localStorage.setItem('user', JSON.stringify(data.data))
          return { success: true, message: data.message }
        } else {
          return { success: false, message: data.message }
        }
      } catch (error) {
        console.error('Register error:', error)
        return { success: false, message: '网络错误，请稍后重试' }
      }
    },

    logout() {
      this.user = null
      this.token = null
      this.isAuthenticated = false
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },

    async updateUser(userData) {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/update`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(userData)
        })
        const data = await response.json()
        if (data.success) {
          this.user = data.data
          localStorage.setItem('user', JSON.stringify(data.data))
          return { success: true, message: data.message }
        } else {
          return { success: false, message: data.message }
        }
      } catch (error) {
        console.error('Update error:', error)
        return { success: false, message: '网络错误，请稍后重试' }
      }
    },

    async changePassword(oldPassword, newPassword) {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/changePassword`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            userId: this.user.id,
            oldPassword,
            newPassword
          })
        })
        const data = await response.json()
        return { success: data.success, message: data.message }
      } catch (error) {
        console.error('Change password error:', error)
        return { success: false, message: '网络错误，请稍后重试' }
      }
    },

    loadUser() {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        this.user = JSON.parse(userStr)
        this.isAuthenticated = true
      }
    },

    checkAuth() {
      this.loadUser()
    }
  }
})
