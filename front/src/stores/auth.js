/**
 * 认证状态管理模块
 * 
 * 【架构说明】
 * 本模块基于Pinia（Vue的状态管理库）实现，负责管理用户的登录、注册、登出等认证相关功能
 * 
 * 【核心职责】
 * 1. 用户身份认证：登录验证、注册新用户
 * 2. 会话状态管理：维护当前登录状态、用户信息、Token
 * 3. 本地持久化：将认证信息存储在localStorage中，实现页面刷新后状态不丢失
 * 
 * 【技术要点】
 * - 使用JWT Token进行无状态认证
 * - 通过localStorage持久化登录状态
 * - 区分普通用户和管理员角色，分别存储不同的Token和用户信息
 */

import { defineStore } from 'pinia'
import request from '../utils/request'
import { TOKEN_KEYS } from '../utils/auth'

/**
 * 定义认证Store
 * 
 * defineStore是Pinia的核心方法，用于创建一个全局状态管理单元
 * 参数'auth'是Store的唯一标识符
 */
export const useAuthStore = defineStore('auth', {
  
  /**
   * 状态定义（state）
   * 
   * 【作用】定义Store中存储的响应式数据
   * 这些数据会被Vue组件自动追踪，变化时触发UI更新
   */
  state: () => ({
    // 当前登录用户的详细信息对象（包含id、username、role等）
    user: null,
    
    // JWT令牌，用于后端API请求的身份验证
    // 从localStorage初始化，实现页面刷新后自动恢复登录状态
    token: localStorage.getItem('user_token') || null,
    
    // 认证状态标识，true表示已登录
    // 使用!!双重否定运算符将字符串转为布尔值
    isAuthenticated: !!localStorage.getItem('user_token')
  }),

  /**
   * 计算属性（getters）
   * 
   * 【作用】基于state派生出的只读属性
   * 类似Vue的computed属性，具有缓存特性，依赖变化时才重新计算
   */
  getters: {
    // 获取当前登录用户信息
    // 返回state.user的引用，方便组件访问
    getUser: (state) => state.user,
    
    // 获取登录状态（getter方式）
    // 与state.isAuthenticated功能相同，提供更规范的访问方式
    isLoggedIn: (state) => state.isAuthenticated
  },

  /**
   * 操作方法（actions）
   * 
   * 【作用】定义修改state的异步或同步操作
   * 所有状态变更必须通过actions进行，这是Pinia的推荐实践
   */
  actions: {
    
    /**
     * 用户登录
     * 
     * 【执行流程】
     * 1. 向后端发送登录请求（POST /auth/login）
     * 2. 后端验证用户名和密码，返回用户信息和JWT Token
     * 3. 根据用户角色（admin/user）分别处理：
     *    - 管理员：存储admin_token和admin信息
     *    - 普通用户：存储user_token和user信息，并更新状态
     * 4. 返回登录结果，供UI层进行后续处理（如页面跳转）
     * 
     * 【异常处理】
     * - 请求失败时捕获错误，返回统一格式的错误信息
     * - Axios拦截器已处理401、403等HTTP错误码，此处处理业务层错误
     * 
     * @param {string} username - 用户名
     * @param {string} password - 密码
     * @returns {Object} 登录结果 { success: boolean, message: string, isAdmin: boolean }
     */
    async login(username, password) {
      try {
        // 发送登录请求到后端接口
        // request.post基于Axios封装，自动添加Authorization头和错误处理
        const response = await request.post('/auth/login', { username, password })
        
        // 判断用户角色：admin表示管理员，其他为普通用户
        if (response.role === 'admin') {
          // 管理员登录处理
          // 将管理员 accessToken 和 refreshToken 存入 localStorage，用于后续请求鉴权与无感刷新
          localStorage.setItem(TOKEN_KEYS.ADMIN_TOKEN, response.token)
          if (response.refreshToken) {
            localStorage.setItem(TOKEN_KEYS.ADMIN_REFRESH, response.refreshToken)
          }
          // 将管理员完整信息序列化为JSON存储
          // 便于页面刷新后恢复管理员状态
          localStorage.setItem(TOKEN_KEYS.ADMIN, JSON.stringify(response))
          // 返回管理员登录成功标识
          return { success: true, message: '管理员登录成功', isAdmin: true }
        } else {
          // 普通用户登录处理
          // 更新Pinia响应式状态，Vue组件会自动响应状态变化
          this.user = response                    // 存储用户信息到state
          this.token = response.token             // 存储Token到state
          this.isAuthenticated = true             // 设置已登录状态

          // 同步数据到localStorage实现持久化
          // 同时保存 accessToken 和 refreshToken，供请求拦截器无感刷新使用
          localStorage.setItem(TOKEN_KEYS.USER_TOKEN, response.token)
          if (response.refreshToken) {
            localStorage.setItem(TOKEN_KEYS.USER_REFRESH, response.refreshToken)
          }
          localStorage.setItem(TOKEN_KEYS.USER, JSON.stringify(response))

          // 返回普通用户登录成功标识
          return { success: true, message: '登录成功', isAdmin: false }
        }
      } catch (error) {
        // 异常处理：记录错误日志并返回错误信息
        // 错误信息来源：Axios拦截器的错误消息或网络异常
        console.error('Login error:', error)
        return { success: false, message: error.message || '网络错误，请稍后重试' }
      }
    },

    /**
     * 用户注册
     * 
     * 【执行流程】
     * 1. 向后端发送注册请求（POST /auth/register）
     * 2. 后端验证邮箱格式、密码强度、验证码、用户名唯一性
     * 3. 创建用户并自动登录，返回用户信息和Token
     * 4. 将注册返回的用户信息和Token存储到state和localStorage
     * 
     * @param {Object} userData - 注册数据 { username, email, password, code }
     * @returns {Object} 注册结果 { success: boolean, message: string }
     */
    async register(userData) {
      try {
        // 发送注册请求，后端会自动创建用户并登录
        const response = await request.post('/auth/register', userData)
        
        // 注册成功后自动登录，更新状态
        this.user = response                      // 存储新用户信息
        this.token = response.token               // 存储Token
        this.isAuthenticated = true               // 标记为已登录

        // 持久化到localStorage（同时保存 refreshToken）
        localStorage.setItem(TOKEN_KEYS.USER_TOKEN, response.token)
        if (response.refreshToken) {
          localStorage.setItem(TOKEN_KEYS.USER_REFRESH, response.refreshToken)
        }
        localStorage.setItem(TOKEN_KEYS.USER, JSON.stringify(response))

        return { success: true, message: '注册成功' }
      } catch (error) {
        console.error('Register error:', error)
        return { success: false, message: error.message || '网络错误，请稍后重试' }
      }
    },

    /**
     * 用户登出
     * 
     * 【执行流程】
     * 1. 清除Pinia中的响应式状态（user、token、isAuthenticated）
     * 2. 清除localStorage中的所有认证信息
     * 3. 支持同时清除用户和管理员的登录信息
     * 
     * 【注意】
     * - 此方法为同步操作，确保立即生效
     * - 如需调用后端注销接口（服务端销毁Token），可扩展为异步方法
     */
    logout() {
      // 清除Pinia响应式状态
      this.user = null
      this.token = null
      this.isAuthenticated = false

      // 清除localStorage中的认证信息
      // 同时移除用户和管理员的 Token（accessToken + refreshToken），确保完全登出
      localStorage.removeItem(TOKEN_KEYS.USER_TOKEN)
      localStorage.removeItem(TOKEN_KEYS.ADMIN_TOKEN)
      localStorage.removeItem(TOKEN_KEYS.USER_REFRESH)
      localStorage.removeItem(TOKEN_KEYS.ADMIN_REFRESH)
      localStorage.removeItem(TOKEN_KEYS.USER)
      localStorage.removeItem(TOKEN_KEYS.ADMIN)
    },

    /**
     * 更新用户信息
     * 
     * 【执行流程】
     * 1. 向后端发送更新请求（POST /auth/update）
     * 2. 后端根据userId更新用户的昵称、手机号、邮箱、头像等信息
     * 3. 更新成功后同步更新本地state和localStorage
     * 
     * @param {Object} userData - 更新数据 { id, nickname, phone, email, avatar }
     * @returns {Object} 更新结果 { success: boolean, message: string }
     */
    async updateUser(userData) {
      try {
        // 发送更新请求到后端
        const response = await request.post('/auth/update', userData)
        
        // 更新本地状态，保持数据同步
        this.user = response
        localStorage.setItem('user', JSON.stringify(response))
        
        return { success: true, message: '更新成功' }
      } catch (error) {
        console.error('Update error:', error)
        return { success: false, message: error.message || '网络错误，请稍后重试' }
      }
    },

    /**
     * 修改密码
     * 
     * 【执行流程】
     * 1. 从当前state获取userId（需先登录）
     * 2. 向后端发送修改密码请求（POST /auth/changePassword）
     * 3. 后端验证原密码，强度校验后更新为新密码
     * 4. 修改成功后返回成功，失败则返回错误
     * 
     * @param {string} oldPassword - 原密码
     * @param {string} newPassword - 新密码
     * @returns {Object} 修改结果 { success: boolean, message: string }
     */
    async changePassword(oldPassword, newPassword) {
      try {
        // 发送修改密码请求，携带userId用于后端定位用户
        await request.post('/auth/changePassword', {
          userId: this.user.id,     // 当前登录用户的ID
          oldPassword,              // 原密码（用于验证身份）
          newPassword               // 新密码（需符合密码强度要求）
        })
        return { success: true, message: '密码修改成功' }
      } catch (error) {
        console.error('Change password error:', error)
        return { success: false, message: error.message || '网络错误，请稍后重试' }
      }
    },

    /**
     * 从本地存储加载用户信息
     * 
     * 【执行流程】
     * 1. 从localStorage获取JSON格式的用户信息字符串
     * 2. 解析为JavaScript对象
     * 3. 更新Pinia状态，恢复登录状态
     * 
     * 【使用场景】
     * - 页面刷新后恢复登录状态
     * - 应用启动时检查本地存储的登录信息
     */
    loadUser() {
      // 从localStorage读取用户信息
      const userStr = localStorage.getItem('user')
      if (userStr) {
        // 解析JSON字符串为JavaScript对象
        this.user = JSON.parse(userStr)
        this.isAuthenticated = true
      }
    },

    /**
     * 检查认证状态
     * 
     * 【执行流程】
     * 1. 调用loadUser()从本地存储恢复状态
     * 2. 这是一个便捷方法，便于组件在需要时快速验证登录状态
     */
    checkAuth() {
      this.loadUser()
    }
  }
})
