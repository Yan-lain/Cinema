import axios from 'axios'
import API_BASE_URL from '../api'
import {
  TOKEN_KEYS,
  isTokenExpiringSoon,
  pickTokenByPath
} from './auth'


/**
 * 创建axios实例
 * @description: 配置axios实例，添加请求拦截器和响应拦截器
 * @param {*} config - axios配置对象
 * @returns - axios实例
 * @example
 * import request from './request'
 * request({
 *   url: '/api/login',
 *   method: 'post',
 *   data: {
 *     username: 'admin',
 *     password: '123456'
 *   }
 * }).then(res => {
 *   console.log(res)
 * })
 * @throws {Error} - 请求失败时抛出错误
 */
const service = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ============ 无感刷新状态（模块级单例） ============
/**
 * 【并发刷新控制】
 * 多个请求同时命中"临期"时，只允许第一个请求触发 /auth/refresh，
 * 其余请求挂起等待，刷新完成后统一用新 Token 发出。
 * - isRefreshing: 是否正在刷新中
 * - pendingQueue: 等待刷新完成的请求队列（每项是一个 resolve/reject 回调）
 */
let isRefreshing = false
let pendingQueue = []

/**
 * 将一个待发请求推入等待队列
 * @returns {Promise<string>} 刷新完成后 resolve 新 Token；失败则 reject
 */
function pushPendingRequest() {
  return new Promise((resolve, reject) => {
    pendingQueue.push({ resolve, reject })
  })
}

/**
 * 刷新成功后，唤醒所有挂起请求；失败则全部拒绝
 */
function flushPendingQueue(newToken, error) {
  pendingQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve(newToken)
    }
  })
  pendingQueue = []
}

/**
 * 调用 /auth/refresh 刷新 Token，并更新 localStorage
 *
 * 【执行流程】
 * 1. 读取当前路径对应的 refreshToken
 * 2. 调用 /api/auth/refresh 接口（该接口在白名单中，无需 accessToken）
 * 3. 成功后更新 localStorage 中的 accessToken 和 refreshToken
 * 4. 返回新的 accessToken 供请求拦截器写入 Authorization 头
 *
 * @param {{ isAdmin: boolean, tokenKey: string, refreshKey: string }} tokenInfo 由 pickTokenByPath 返回
 * @returns {Promise<string>} 新的 accessToken
 * @throws {Error} 没有 refreshToken 或刷新失败时抛出
 */
async function doRefresh(tokenInfo) {
  const refreshToken = localStorage.getItem(tokenInfo.refreshKey)
  if (!refreshToken) {
    throw new Error('无 refreshToken，无法刷新')
  }

  // 注意：这里直接用 axios 发原始请求，避免再走 service 拦截器造成循环
  const resp = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
  const body = resp.data
  if (!body || body.code !== 200 || !body.data || !body.data.token) {
    throw new Error(body?.message || '刷新令牌失败')
  }

  const { token: newAccessToken, refreshToken: newRefreshToken } = body.data

  // 更新 localStorage 中的 Token（refreshToken 旋转，新值替换旧值）
  localStorage.setItem(tokenInfo.tokenKey, newAccessToken)
  if (newRefreshToken) {
    localStorage.setItem(tokenInfo.refreshKey, newRefreshToken)
  }

  // 同步更新 user/admin 信息里的 token 字段（部分页面会从 user 对象读 token）
  const infoKey = tokenInfo.isAdmin ? TOKEN_KEYS.ADMIN : TOKEN_KEYS.USER
  const infoStr = localStorage.getItem(infoKey)
  if (infoStr) {
    try {
      const info = JSON.parse(infoStr)
      info.token = newAccessToken
      if (newRefreshToken) info.refreshToken = newRefreshToken
      localStorage.setItem(infoKey, JSON.stringify(info))
    } catch (_) {
      // 解析失败忽略，不影响主流程
    }
  }

  return newAccessToken
}

service.interceptors.request.use(
  async config => {
    const userToken = localStorage.getItem('user_token')
    const adminToken = localStorage.getItem('admin_token')

    /**
     * 根据请求路径选择正确的Token
     *
     * 【规则说明】
     * 1. 管理员接口（/api/admin/开头）必须使用admin_token
     * 2. 普通接口优先使用user_token，如无则使用admin_token
     *
     * 【原因】
     * 管理员Token和普通用户Token是独立的，携带错误的Token会导致：
     * - 权限不足（403 Forbidden）
     * - 数据不一致（管理员看到普通用户数据）
     */
    const tokenInfo = pickTokenByPath(config.url || '')
    let token = null

    if (tokenInfo.isAdmin) {
      token = adminToken
    } else {
      token = userToken || adminToken
    }

    // 【无感刷新】若选中 Token 且即将过期，先刷新再发请求
    // /auth/refresh 自身不携带 Token（白名单接口），跳过刷新逻辑避免循环
    const isRefreshCall = (config.url || '').startsWith('/auth/refresh')
    if (token && !isRefreshCall && isTokenExpiringSoon(token)) {
      if (!isRefreshing) {
        // 当前路径对应的 refreshToken（管理员请求用 admin_refresh_token，否则优先 user_refresh_token）
        const refreshInfo = tokenInfo.isAdmin
          ? tokenInfo
          : { ...tokenInfo, refreshKey: localStorage.getItem(TOKEN_KEYS.USER_REFRESH) ? TOKEN_KEYS.USER_REFRESH : TOKEN_KEYS.ADMIN_REFRESH }
        isRefreshing = true
        try {
          const newToken = await doRefresh(refreshInfo)
          token = newToken
          // 唤醒所有挂起请求
          flushPendingQueue(newToken, null)
        } catch (err) {
          flushPendingQueue(null, err)
          // 刷新失败：清空登录态并跳登录，不再继续本次请求
          clearAuthAndRedirect()
          return Promise.reject(err)
        } finally {
          isRefreshing = false
        }
      } else {
        // 已有刷新在进行中，挂起等待新 Token
        token = await pushPendingRequest()
      }
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    // 处理请求拦截器错误
    console.error('Request interceptor error:', error)
    return Promise.reject(error)
  }
)

/**
 * 清空本地认证信息并跳转到登录页（保留回跳路径）
 * 【说明】被 401 处理或刷新失败时调用
 */
function clearAuthAndRedirect() {
  localStorage.removeItem(TOKEN_KEYS.USER_TOKEN)
  localStorage.removeItem(TOKEN_KEYS.ADMIN_TOKEN)
  localStorage.removeItem(TOKEN_KEYS.USER_REFRESH)
  localStorage.removeItem(TOKEN_KEYS.ADMIN_REFRESH)
  localStorage.removeItem(TOKEN_KEYS.USER)
  localStorage.removeItem(TOKEN_KEYS.ADMIN)
  // 记住当前路径，登录后可回跳；首页则不记录
  const currentPath = window.location.pathname + window.location.search
  if (currentPath && currentPath !== '/' && !currentPath.startsWith('/login')) {
    sessionStorage.setItem('redirect_after_login', currentPath)
  }
  // 跳转到首页（用户端登录入口），管理员登录失败由管理员路由守卫处理
  window.location.href = '/'
}

// 响应拦截器：解包 ApiResponse，返回 data 字段内容（电影列表）
// 【说明】前端直接使用 data 字段内容，无需再解包
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else {
      console.error('Response error:', res.message)
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  async error => {
    // 处理响应拦截器错误
    console.error('Response interceptor error:', error)
    const originalRequest = error.config

    // 【401 重放】 accessToken 已过期（或被服务端拒绝）时，尝试用 refreshToken 换新 Token 后重放原请求
    // _retried 标志防止无限重试
    if (error.response && error.response.status === 401 && originalRequest && !originalRequest._retried) {
      originalRequest._retried = true

      // 选出当前请求应使用的 refreshToken
      const tokenInfo = pickTokenByPath(originalRequest.url || '')
      const refreshToken = localStorage.getItem(
        tokenInfo.isAdmin ? TOKEN_KEYS.ADMIN_REFRESH : (localStorage.getItem(TOKEN_KEYS.USER_REFRESH) ? TOKEN_KEYS.USER_REFRESH : TOKEN_KEYS.ADMIN_REFRESH)
      )

      if (refreshToken) {
        try {
          // 复用并发刷新机制：若已有刷新在进行，挂起等待；否则自己发起一次
          let newToken
          if (isRefreshing) {
            newToken = await pushPendingRequest()
          } else {
            isRefreshing = true
            try {
              newToken = await doRefresh(tokenInfo.isAdmin ? tokenInfo : { ...tokenInfo, refreshKey: localStorage.getItem(TOKEN_KEYS.USER_REFRESH) ? TOKEN_KEYS.USER_REFRESH : TOKEN_KEYS.ADMIN_REFRESH })
              flushPendingQueue(newToken, null)
            } catch (e) {
              flushPendingQueue(null, e)
              throw e
            } finally {
              isRefreshing = false
            }
          }
          // 用新 Token 重放原请求
          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return service(originalRequest)
        } catch (retryErr) {
          // 刷新失败，走标准登出流程
          clearAuthAndRedirect()
          return Promise.reject(new Error('登录已过期，请重新登录'))
        }
      } else {
        // 没有 refreshToken，直接登出
        clearAuthAndRedirect()
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
    }

    let errorMessage = '网络错误，请稍后重试'

    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        errorMessage = '登录已过期，请重新登录'
        // 401 但已重试过仍失败的情况，由上面的分支处理；这里兜底
        clearAuthAndRedirect()
      } else if (status === 403) {
        errorMessage = '权限不足，无法访问'
      } else if (status === 404) {
        errorMessage = '请求的资源不存在'
      } else if (status >= 500) {
        errorMessage = '服务器内部错误'
      } else {
        errorMessage = error.response.data?.message || '请求失败'
      }
    } else if (error.request) {
      errorMessage = '请求超时或服务器无响应'
    }

    return Promise.reject(new Error(errorMessage))
  }
)

export default service
