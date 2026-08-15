/**
 * 路由配置入口文件
 *
 * 负责：
 * 1. 集成用户端和管理端的模块化路由
 * 2. 配置全局导航守卫，实现权限控制
 * 3. 创建并导出路由实例
 */
import { createRouter, createWebHistory } from 'vue-router'
import { TOKEN_KEYS, isTokenExpired } from '../utils/auth'

// 导入模块化路由配置
import userRoutes from './user'   // 用户端路由配置
import adminRoutes from './admin' // 管理端路由配置

/**
 * 创建路由实例
 * 使用 HTML5 History 模式（去除 URL 中的 #）
 * 通过展开运算符合并用户端和管理端路由
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...userRoutes,   // 用户端路由（首页、电影、影院、订单等）
    ...adminRoutes   // 管理端路由（登录、后台首页）
  ]
})

/**
 * 全局导航守卫（前置守卫）
 * 在路由跳转前执行权限检查
 *
 * 【增强点】除了检查 Token 是否存在，还检查 accessToken 是否已过期：
 * - 过期但有 refreshToken → 由 request.js 在下次请求时自动刷新，这里先放行（避免阻塞路由）
 * - 过期且无 refreshToken → 清除登录态并跳转登录页
 *
 * @param {Route} to - 目标路由对象
 * @param {Route} from - 当前导航正要离开的路由
 * @param {Function} next - 调用该方法 resolve 这个钩子
 */
router.beforeEach((to, from, next) => {
  // 获取本地存储的用户认证信息
  const userToken = localStorage.getItem(TOKEN_KEYS.USER_TOKEN)
  const adminToken = localStorage.getItem(TOKEN_KEYS.ADMIN_TOKEN)
  const userRefresh = localStorage.getItem(TOKEN_KEYS.USER_REFRESH)
  const adminRefresh = localStorage.getItem(TOKEN_KEYS.ADMIN_REFRESH)

  // 获取用户信息（普通用户）
  const user = localStorage.getItem(TOKEN_KEYS.USER) ? JSON.parse(localStorage.getItem(TOKEN_KEYS.USER)) : null
  // 获取管理员信息
  const admin = localStorage.getItem(TOKEN_KEYS.ADMIN) ? JSON.parse(localStorage.getItem(TOKEN_KEYS.ADMIN)) : null

  // 管理员登录页不需要认证，直接放行
  if (to.path === '/admin/login') {
    next()
    return
  }

  // 检查管理端路由的角色权限
  if (to.meta.role === 'admin') {
    // 管理端页面需要管理员Token或管理员身份
    if (!adminToken || admin?.role !== 'admin') {
      // 管理员未登录或角色错误，跳转到管理员登录页
      next('/admin/login')
      return
    }
    // accessToken 已过期且无 refreshToken → 登录态失效，跳登录页
    // （有 refreshToken 时放行，由 request.js 在发请求时自动刷新）
    if (isTokenExpired(adminToken) && !adminRefresh) {
      localStorage.removeItem(TOKEN_KEYS.ADMIN_TOKEN)
      localStorage.removeItem(TOKEN_KEYS.ADMIN)
      next('/admin/login')
      return
    }
    next()
    return
  }

  // 检查用户端路由是否需要登录认证
  if (to.meta.requiresAuth) {
    if (!userToken) {
      next('/')
      return
    }
    // accessToken 已过期且无 refreshToken → 登录态失效，回首页
    if (isTokenExpired(userToken) && !userRefresh) {
      localStorage.removeItem(TOKEN_KEYS.USER_TOKEN)
      localStorage.removeItem(TOKEN_KEYS.USER)
      next('/')
      return
    }
  }

  // 权限检查通过，允许路由跳转
  next()
})

export default router
