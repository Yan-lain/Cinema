//const API_BASE_URL = 'http://localhost:8080/api';
const API_BASE_URL = '/api';// 基础URL
const API_BASE_URL_ADMIN = '/api/admin';// 管理端基础URL
export { API_BASE_URL_ADMIN };
export default API_BASE_URL;
/**
 * 解释一下这里是干什么的?
 * 这里是定义了一个基础URL，用于所有API请求的前缀。
 * 例如，一个GET请求到 /api/movie/123 会被发送到 http://localhost:8080/api/movie/123
 * 这样可以避免在每个API请求中都写完整的URL，提高了代码的可维护性和可读性。
 * 这一行的代码有什么用？
 * 它的作用是定义了一个基础URL，用于所有API请求的前缀。
 * 这样可以避免在每个API请求中都写完整的URL，提高了代码的可维护性和可读性。
 * 导出方式：
 * 1. 命名导出：用于在其他文件中单独导入 API_BASE_URL_ADMIN
 * 2. 默认导出：用于在其他文件中直接导入 API_BASE_URL
 * 例如：
 * import API_BASE_URL from '@/api'
 * import API_BASE_URL_ADMIN from '@/api'
 * 什么时候需要花括号？
 * 1. 当导出多个变量时，需要使用花括号
 * 2. 当导出的是一个对象或数组时，需要使用花括号
 * 3. 当导出的是一个函数时，需要使用花括号
 * 4. 当导出的是一个类时，需要使用花括号
 * 5. 当导出的是一个枚举时，需要使用花括号
 * 现在是多少个？
 * 2个
 * 这两个不一样啊
 * 谁需要花括号？
 * 命名导出需要花括号，因为导出的是多个变量
 * 默认导出不需要花括号，因为导出的是一个变量
 * */
