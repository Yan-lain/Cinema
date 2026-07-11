# Spring Boot 项目架构优化方案

## 文档信息

| 属性 | 值 |
|------|-----|
| 文档版本 | v1.0 |
| 创建日期 | 2026-07-11 |
| 适用项目 | 影院管理系统（Spring Boot） |
| 文档状态 | 待评审 |

---

## 一、当前项目结构分析

### 1.1 当前目录结构

```
demo/src/main/java/com/example/demo/
├── config/                    # ✅ 配置类（2个文件）
│   ├── CorsConfig.java
│   └── RedisConfig.java
├── controller/                # ✅ 控制器（11个文件）
│   ├── AdminController.java
│   ├── AuthController.java
│   └── ...
├── entity/                    # ✅ 实体类（12个文件）
│   ├── User.java
│   ├── Movie.java
│   └── ...
├── mapper/                    # ✅ 数据访问层（12个文件）
│   ├── UserMapper.java
│   ├── MovieMapper.java
│   └── ...
├── service/                   # ⚠️ 服务层（不完整，3个文件）
│   ├── EmailService.java
│   ├── RedisService.java
│   └── ScheduleService.java
├── task/                      # ⚠️ 定时任务（1个文件）
│   └── ScheduleTask.java
└── DemoApplication.java       # ✅ 启动类
```

### 1.2 缺失的关键模块

| 缺失模块 | 重要性 | 说明 |
|---------|-------|------|
| **constant** | 高 | 常量定义（状态码、错误码、配置常量） |
| **interceptor** | 高 | 请求拦截器（认证、日志、限流） |
| **util** | 中 | 工具类（日期、加密、字符串处理） |
| **service/impl** | 高 | 业务逻辑实现层 |
| **dto** | 高 | 数据传输对象（请求/响应） |
| **exception** | 高 | 异常处理（全局异常、业务异常） |

---

## 二、目标项目结构（视频标准）

```
demo/src/main/java/com/example/demo/
├── config/                    # 配置类
│   ├── CorsConfig.java
│   ├── RedisConfig.java
│   └── WebMvcConfig.java      # 新增：注册拦截器
├── constant/                  # 新增：常量类
│   ├── StatusCode.java        # HTTP状态码
│   ├── ErrorCode.java         # 业务错误码
│   └── Constants.java         # 通用常量
├── controller/                # 控制器
│   ├── AdminController.java
│   ├── AuthController.java
│   └── ...
├── dto/                       # 新增：数据传输对象
│   ├── request/               # 请求DTO
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   └── ...
│   └── response/              # 响应DTO
│       ├── ApiResponse.java   # 统一响应格式
│       ├── UserResponse.java
│       └── ...
├── entity/                    # 实体类
│   ├── User.java
│   ├── Movie.java
│   └── ...
├── exception/                 # 新增：异常处理
│   ├── BusinessException.java # 业务异常
│   └── GlobalExceptionHandler.java # 全局异常处理
├── interceptor/               # 新增：拦截器
│   ├── AuthInterceptor.java   # 认证拦截器
│   ├── LogInterceptor.java    # 日志拦截器
│   └── RateLimitInterceptor.java # 限流拦截器
├── mapper/                    # 数据访问层
│   ├── UserMapper.java
│   ├── MovieMapper.java
│   └── ...
├── service/                   # 服务接口
│   ├── UserService.java
│   ├── MovieService.java
│   ├── OrderService.java
│   └── ...
├── service/impl/              # 新增：服务实现
│   ├── UserServiceImpl.java
│   ├── MovieServiceImpl.java
│   ├── OrderServiceImpl.java
│   └── ...
├── task/                      # 定时任务
│   └── ScheduleTask.java
├── util/                      # 新增：工具类
│   ├── JwtUtil.java           # JWT工具
│   ├── PasswordUtil.java      # 密码工具
│   ├── DateUtil.java          # 日期工具
│   └── StringUtil.java        # 字符串工具
└── DemoApplication.java       # 启动类
```

---

## 三、各模块详细设计

### 3.1 constant（常量模块）

#### 3.1.1 StatusCode.java

**作用**：定义HTTP状态码常量

```java
public class StatusCode {
    public static final int SUCCESS = 200;
    public static final int CREATED = 201;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int INTERNAL_SERVER_ERROR = 500;
}
```

**效果**：替代魔法数字，提高代码可读性

#### 3.1.2 ErrorCode.java

**作用**：定义业务错误码

```java
public class ErrorCode {
    // 用户相关
    public static final int USER_NOT_FOUND = 1001;
    public static final int USER_EXISTS = 1002;
    public static final int WRONG_PASSWORD = 1003;
    
    // 电影相关
    public static final int MOVIE_NOT_FOUND = 2001;
    public static final int MOVIE_EXISTS = 2002;
    
    // 订单相关
    public static final int ORDER_NOT_FOUND = 3001;
    public static final int ORDER_STATUS_ERROR = 3002;
}
```

**效果**：统一错误码管理，前端可根据错误码做不同处理

#### 3.1.3 Constants.java

**作用**：通用常量

```java
public class Constants {
    // JWT相关
    public static final String JWT_SECRET = "your-256-bit-secret";
    public static final long JWT_EXPIRE_TIME = 30 * 60 * 1000L; // 30分钟
    
    // 密码相关
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;
    
    // Redis缓存键前缀
    public static final String CACHE_MOVIE_PREFIX = "movie:";
    public static final String CACHE_CINEMA_PREFIX = "cinema:";
    
    // 正则表达式
    public static final String REGEX_EMAIL = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    public static final String REGEX_PHONE = "^1[3-9]\\d{9}$";
}
```

**效果**：集中管理配置常量，便于维护和修改

---

### 3.2 interceptor（拦截器模块）

#### 3.2.1 AuthInterceptor.java

**作用**：认证拦截器，验证用户登录状态

```java
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        
        // 白名单路径（无需登录）
        String[] whiteList = {"/api/auth/login", "/api/auth/register", "/api/movies"};
        String requestUri = request.getRequestURI();
        
        for (String path : whiteList) {
            if (requestUri.startsWith(path)) {
                return true;
            }
        }
        
        // 验证Token
        if (token == null || !JwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write(JSON.toJSONString(ApiResponse.error(401, "未授权")));
            return false;
        }
        
        // 将用户信息放入请求上下文
        Long userId = JwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        return true;
    }
}
```

**效果**：统一认证，未登录用户无法访问需要权限的接口

#### 3.2.2 LogInterceptor.java

**作用**：日志拦截器，记录请求日志

```java
public class LogInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LogInterceptor.class);
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        
        log.info("请求开始 - {} {} - {}", 
            request.getMethod(), 
            request.getRequestURI(),
            request.getRemoteAddr());
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("请求结束 - {} {} - {}ms - {}", 
            request.getMethod(), 
            request.getRequestURI(),
            duration,
            response.getStatus());
    }
}
```

**效果**：自动记录所有请求的日志，便于问题排查

#### 3.2.3 RateLimitInterceptor.java

**作用**：限流拦截器，防止接口被恶意调用

```java
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Map<String, Long> requestCount = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = request.getRemoteAddr();
        String key = clientIp + ":" + request.getRequestURI();
        Long count = requestCount.getOrDefault(key, 0L);
        
        if (count >= MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429);
            response.getWriter().write(JSON.toJSONString(ApiResponse.error(429, "请求过于频繁，请稍后重试")));
            return false;
        }
        
        requestCount.put(key, count + 1);
        // 1分钟后清除计数
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                requestCount.remove(key);
            }
        }, 60000);
        
        return true;
    }
}
```

**效果**：防止接口被恶意攻击，保护服务器资源

---

### 3.3 service + service/impl（服务层重构）

#### 3.3.1 Service接口

**作用**：定义业务方法契约

```java
public interface UserService {
    UserResponse login(LoginRequest request);
    UserResponse register(RegisterRequest request);
    UserResponse getUserById(Long id);
    void updatePassword(Long userId, String newPassword);
}
```

#### 3.3.2 Service实现

**作用**：实现具体业务逻辑

```java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public UserResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_PASSWORD, "密码错误");
        }
        
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .token(token)
            .build();
    }
}
```

**效果**：
- Controller层只负责参数校验和响应封装
- 业务逻辑集中在Service层，便于测试和复用
- 接口与实现分离，便于扩展

---

### 3.4 dto（数据传输对象）

#### 3.4.1 请求DTO

**作用**：封装请求参数

```java
@Data
@Validated
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 128, message = "密码长度必须在8-128位之间")
    private String password;
}
```

#### 3.4.2 响应DTO

**作用**：封装响应数据

```java
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private String token;
    // 注意：不包含password字段
}
```

#### 3.4.3 ApiResponse（统一响应格式）

**作用**：统一所有接口的响应格式

```java
@Data
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}
```

**效果**：
- 前端处理响应更统一，无需判断不同格式
- 隐藏敏感字段（如password）
- 参数校验更规范

---

### 3.5 exception（异常处理）

#### 3.5.1 BusinessException（业务异常）

**作用**：自定义业务异常

```java
public class BusinessException extends RuntimeException {
    private Integer code;
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public Integer getCode() {
        return code;
    }
}
```

#### 3.5.2 GlobalExceptionHandler（全局异常处理）

**作用**：统一处理所有异常

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ApiResponse.error(400, message);
    }
    
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(500, "系统繁忙，请稍后重试");
    }
}
```

**效果**：
- 统一异常响应格式
- 隐藏技术细节，不暴露堆栈信息给前端
- 业务异常统一处理，代码更整洁

---

### 3.6 util（工具类）

#### 3.6.1 JwtUtil.java

**作用**：JWT生成、验证工具

```java
public class JwtUtil {
    private static final String SECRET = Constants.JWT_SECRET;
    
    public static String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject("user")
            .setExpiration(new Date(System.currentTimeMillis() + Constants.JWT_EXPIRE_TIME))
            .signWith(SignatureAlgorithm.HS256, SECRET)
            .compact();
    }
    
    public static boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(SECRET)
            .parseClaimsJws(token)
            .getBody();
        return claims.get("userId", Long.class);
    }
}
```

#### 3.6.2 PasswordUtil.java

**作用**：密码加密工具

```java
public class PasswordUtil {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }
    
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
    
    public static boolean isValid(String password) {
        if (password == null || password.length() < Constants.PASSWORD_MIN_LENGTH) {
            return false;
        }
        // 检查是否包含大小写字母、数字、特殊字符
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    }
}
```

**效果**：工具方法复用，代码更简洁

---

## 四、重构前后对比

### 4.1 目录结构对比

| 重构前 | 重构后 | 变化 |
|-------|-------|------|
| config/ (2个文件) | config/ (3个文件) | + WebMvcConfig |
| controller/ (11个文件) | controller/ (11个文件) | 不变 |
| entity/ (12个文件) | entity/ (12个文件) | 不变 |
| mapper/ (12个文件) | mapper/ (12个文件) | 不变 |
| service/ (3个文件) | service/ (接口) + service/impl/ (实现) | 拆分 |
| task/ (1个文件) | task/ (1个文件) | 不变 |
| ❌ constant | ✅ constant/ (3个文件) | 新增 |
| ❌ interceptor | ✅ interceptor/ (3个文件) | 新增 |
| ❌ dto | ✅ dto/request/ + dto/response/ | 新增 |
| ❌ exception | ✅ exception/ (2个文件) | 新增 |
| ❌ util | ✅ util/ (4个文件) | 新增 |

### 4.2 代码职责对比

| 模块 | 重构前职责 | 重构后职责 |
|------|-----------|-----------|
| Controller | 参数校验+业务逻辑+响应封装 | 参数校验+调用Service+响应封装 |
| Service | 零散的工具方法 | 业务逻辑接口定义 |
| Service/Impl | ❌ | 业务逻辑实现 |
| Mapper | ✅ | ✅ 不变 |
| Entity | ✅ | ✅ 不变 |

---

## 五、重构带来的结果

### 5.1 正面效果

| 效果 | 说明 |
|------|-----|
| **代码更清晰** | 每个模块职责明确，新人能快速理解项目结构 |
| **可维护性提升** | 业务逻辑集中在Service层，修改一处不影响其他层 |
| **安全性增强** | 认证拦截器统一处理，Token验证、密码加密标准化 |
| **可测试性提升** | Service层可独立进行单元测试，无需启动Spring容器 |
| **扩展性增强** | 接口与实现分离，可轻松替换实现方式 |
| **错误处理统一** | 全局异常处理，前端收到统一格式的错误响应 |
| **代码复用** | 工具类和常量类避免重复代码 |

### 5.2 潜在风险

| 风险 | 应对措施 |
|------|---------|
| 重构工作量大 | 分阶段实施，先完成核心模块（认证、异常处理） |
| 可能引入新Bug | 每完成一个模块进行单元测试和集成测试 |
| 团队学习成本 | 提供详细的架构文档和代码示例 |
| 向后兼容性 | 确保API接口保持不变，只修改内部实现 |

---

## 六、实施路线建议

### 第一阶段：基础架构搭建（1-2天）

| 任务 | 内容 |
|------|-----|
| 1. 创建常量类 | StatusCode、ErrorCode、Constants |
| 2. 创建工具类 | JwtUtil、PasswordUtil |
| 3. 创建统一响应 | ApiResponse |
| 4. 创建异常处理 | BusinessException、GlobalExceptionHandler |

### 第二阶段：认证体系（2-3天）

| 任务 | 内容 |
|------|-----|
| 1. 创建拦截器 | AuthInterceptor、LogInterceptor |
| 2. 注册拦截器 | WebMvcConfig |
| 3. 创建DTO | LoginRequest、RegisterRequest、UserResponse |
| 4. 重构AuthController | 使用Service层和DTO |

### 第三阶段：业务模块重构（3-5天）

| 任务 | 内容 |
|------|-----|
| 1. 创建Service接口 | UserService、MovieService、OrderService等 |
| 2. 创建Service实现 | UserServiceImpl等 |
| 3. 创建业务DTO | 各模块的请求/响应DTO |
| 4. 重构Controller | 所有Controller改为调用Service |

### 第四阶段：测试与验证（2天）

| 任务 | 内容 |
|------|-----|
| 1. 单元测试 | 为Service层编写单元测试 |
| 2. 集成测试 | 测试端到端流程 |
| 3. 回归测试 | 确保原有功能不受影响 |
| 4. 安全审计 | 检查认证、密码等安全相关代码 |

---

## 七、验收标准

### 架构层面

| 验收项 | 标准 |
|-------|-----|
| 目录结构 | 包含所有标准模块（constant、interceptor、dto、exception、util） |
| 代码分层 | Controller→Service→Mapper三层清晰分离 |
| 依赖关系 | 无循环依赖，模块间依赖单向流动 |

### 功能层面

| 验收项 | 标准 |
|-------|-----|
| 用户认证 | JWT认证正常，未登录用户无法访问受保护接口 |
| 密码安全 | 密码使用Bcrypt加密存储，登录时正确比对 |
| 错误处理 | 所有异常返回统一格式的ApiResponse |
| 参数校验 | 请求参数校验失败时返回400状态码和详细错误信息 |

### 代码质量

| 验收项 | 标准 |
|-------|-----|
| 代码覆盖率 | 核心Service代码单元测试覆盖率>80% |
| 代码规范 | 遵循Java编码规范，有完整的Javadoc注释 |
| 无魔法数字 | 所有常量定义在constant模块中 |

---

**文档结束**