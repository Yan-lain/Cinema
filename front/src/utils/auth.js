/**
 * 认证 Token 工具函数模块
 *
 * 【模块职责】
 * 封装与 JWT / refreshToken 相关的纯函数，供 request.js 拦截器和路由守卫复用：
 * 1. 解析 JWT 载荷（payload）和过期时间（exp）
 * 2. 判断 accessToken 是否临期（剩余时间 < 阈值），用于"临期自动刷新"
 * 3. 统一管理 localStorage 中各 Token 键名，避免拼写漂移
 *
 * 【设计原则】
 * - 本模块只提供无副作用的工具函数，不发请求、不直接操作 axios
 * - 涉及网络刷新的逻辑放在 request.js 中，便于集中管理拦截器
 */

/** localStorage 键名常量（集中管理，防止多处硬编码拼写不一致） */
export const TOKEN_KEYS = {
  USER_TOKEN: 'user_token',           // 普通用户 accessToken
  ADMIN_TOKEN: 'admin_token',         // 管理员 accessToken
  USER_REFRESH: 'user_refresh_token', // 普通用户 refreshToken
  ADMIN_REFRESH: 'admin_refresh_token', // 管理员 refreshToken
  USER: 'user',                       // 普通用户信息
  ADMIN: 'admin'                      // 管理员信息
}

/** accessToken 临期阈值（毫秒）：剩余寿命 < 5 分钟时触发主动刷新 */
export const ACCESS_TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000

/**
 * 解析 JWT 的 payload 部分
 *
 * 【技术说明】JWT 由三段 Base64URL 字符串用 "." 拼接：header.payload.signature
 * payload 是 JSON 对象的 Base64URL 编码，包含 exp（过期时间戳，秒）、userId、role 等
 * 注意：payload 只是编码，未加密，因此不要在其中存放敏感信息
 *
 * @param {string} token JWT 字符串
 * @returns {Object|null} payload 对象；解析失败返回 null
 */
export function decodeToken(token) {
  if (!token || typeof token !== 'string') {
    return null
  }
  const parts = token.split('.')
  if (parts.length !== 3) {
    return null
  }
  try {
    // Base64URL → Base64：- 换 +，_ 换 /，缺省用 = 补齐
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const jsonStr = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonStr)
  } catch (e) {
    console.warn('[auth] 解析 JWT payload 失败:', e)
    return null
  }
}

/**
 * 获取 JWT 的过期时间戳（毫秒）
 *
 * @param {string} token JWT 字符串
 * @returns {number|null} 过期时间戳（毫秒）；无 exp 或解析失败返回 null
 */
export function getTokenExp(token) {
  const payload = decodeToken(token)
  if (!payload || typeof payload.exp !== 'number') {
    return null
  }
  // JWT 中 exp 以秒为单位，转换为毫秒
  return payload.exp * 1000
}

/**
 * 判断 Token 是否已过期
 *
 * @param {string} token JWT 字符串
 * @returns {boolean} true 表示已过期或无法解析；false 表示仍在有效期内
 */
export function isTokenExpired(token) {
  const exp = getTokenExp(token)
  if (exp === null) {
    // 无法解析过期时间的 token 视为无效（已过期），避免无限期使用
    return true
  }
  return Date.now() >= exp
}

/**
 * 判断 accessToken 是否"临期"——即剩余寿命小于阈值，应主动刷新
 *
 * 【使用场景】请求拦截器在每次发请求前调用：
 * - 临期且存在 refreshToken → 触发刷新，用新 Token 发请求
 * - 不临期 → 直接用原 Token
 *
 * @param {string} token JWT accessToken
 * @param {number} [thresholdMs] 临期阈值（毫秒），默认 5 分钟
 * @returns {boolean} true 表示临期需要刷新
 */
export function isTokenExpiringSoon(token, thresholdMs = ACCESS_TOKEN_REFRESH_THRESHOLD) {
  const exp = getTokenExp(token)
  if (exp === null) {
    // 无法解析过期时间，保守起见视为临期（触发一次刷新尝试）
    return true
  }
  return exp - Date.now() < thresholdMs
}

/**
 * 根据请求路径判断应使用哪个 Token 类型
 *
 * 【规则】与 request.js 请求拦截器保持一致：
 * - /admin/ 开头的请求使用管理员 Token
 * - 其他请求优先使用用户 Token，无则用管理员 Token
 *
 * @param {string} url 请求路径
 * @returns {{ tokenKey: string, refreshKey: string, isAdmin: boolean }} 对应的 Token 键名信息
 */
export function pickTokenByPath(url) {
  if (url && url.startsWith('/admin/')) {
    return {
      tokenKey: TOKEN_KEYS.ADMIN_TOKEN,
      refreshKey: TOKEN_KEYS.ADMIN_REFRESH,
      isAdmin: true
    }
  }
  return {
    tokenKey: TOKEN_KEYS.USER_TOKEN,
    refreshKey: TOKEN_KEYS.USER_REFRESH,
    isAdmin: false
  }
}
