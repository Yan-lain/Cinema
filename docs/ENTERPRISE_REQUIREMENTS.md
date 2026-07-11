# 企业级影院管理系统整改需求文档

## 文档信息

| 属性 | 值 |
|------|-----|
| 文档版本 | v1.0 |
| 创建日期 | 2026-07-11 |
| 适用项目 | 影院管理系统（Spring Boot + Vue3） |
| 文档状态 | 待评审 |

---

## 一、现有整改思路的详细阐述与技术规范

### 1.1 前端权限认证系统

#### 1.1.1 当前问题分析

**问题1：Token伪造，无真实认证机制**

当前前端token仅为字符串拼接：
```javascript
// frontend/src/stores/auth.js 第32行
this.token = `user_${data.data.id}`
```
后端无任何认证拦截器，所有API完全开放访问。

**问题2：登录返回完整用户对象含密码**

```java
// AuthController.java 第228行
result.put("data", user);  // 返回了包含明文密码的完整用户对象
```

**问题3：前端无路由守卫**

```javascript
// router/index.js 无任何路由守卫逻辑
```

#### 1.1.2 JWT认证实现方案

##### 技术选型

| 组件 | 技术 | 版本 |
|------|-----|------|
| JWT库 | jjwt-api + jjwt-impl + jjwt-jackson | 0.12.x |
| Spring Security | Spring Boot Starter Security | 3.2.x |
| Token存储 | HttpOnly Cookie + 请求头双重模式 | - |

##### Token生成流程

```
用户登录 → 验证用户名密码 → 生成Access Token(30分钟) + Refresh Token(7天)
        → Access Token存入响应头 Authorization: Bearer xxx
        → Refresh Token存入HttpOnly Cookie
```

##### Token结构设计

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
{
  "sub": "user",
  "userId": 1,
  "username": "admin",
  "role": "ADMIN",
  "exp": 1718092800,
  "iat": 1718091000
}
```

##### Token验证流程

```
请求到达 → 过滤器提取Token → 验证签名和过期时间 → 解析用户信息 → 设置SecurityContext → 继续请求
```

##### Token刷新机制

```
Access Token过期 → 返回401错误 → 前端携带Refresh Token请求刷新接口
        → 验证Refresh Token → 生成新的Access Token → 返回给前端
```

##### 过期处理机制

| Token类型 | 过期时间 | 处理方式 |
|----------|---------|---------|
| Access Token | 30分钟 | 返回401，前端自动刷新 |
| Refresh Token | 7天 | 返回401，强制用户重新登录 |

#### 1.1.3 前端路由守卫实现

```javascript
// router/index.js
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // 公共页面
  const publicPages = ['/', '/movies', '/cinemas', '/admin/login']
  
  if (!publicPages.includes(to.path)) {
    if (!authStore.isAuthenticated) {
      next('/')
      return
    }
    
    // 管理员页面权限校验
    if (to.path.startsWith('/admin') && authStore.user?.role !== 'ADMIN') {
      next('/')
      return
    }
  }
  
  next()
})
```

---

### 1.2 密码安全策略

#### 1.2.1 当前问题分析

**问题1：密码明文存储**

```java
// AuthController.java 第187行
user.setPassword(password);  // 明文存储！
```

**问题2：密码明文比对**

```java
// AuthController.java 第219行
} else if (!password.equals(user.getPassword())) {  // 明文比对！
```

**问题3：无密码强度校验**

注册时未校验密码长度、复杂度等。

#### 1.2.2 Bcrypt加密实现标准

##### 加密算法选择

| 算法 | 安全性 | 推荐度 | 说明 |
|------|-------|-------|------|
| MD5 | 低 | ❌ | 已被破解，禁止使用 |
| SHA-256 | 中 | ❌ | 无盐值时易受彩虹表攻击 |
| Bcrypt | 高 | ✅ | 自动加盐，可配置计算复杂度 |

##### Bcrypt配置规范

```java
// BCryptPasswordEncoder配置
@Bean
public PasswordEncoder passwordEncoder() {
    // strength: 10-12，数值越大安全性越高但性能越低
    return new BCryptPasswordEncoder(10);
}
```

##### 加密流程

```
用户注册 → 密码强度校验 → BCryptPasswordEncoder.encode(password) 
        → 存储加密后的密码(含盐值)
```

##### 密码比对流程

```
用户登录 → BCryptPasswordEncoder.matches(rawPassword, encodedPassword)
        → 返回boolean结果
```

##### 密码强度要求

| 规则 | 要求 |
|------|-----|
| 最小长度 | 8位 |
| 最大长度 | 128位 |
| 字符类型 | 至少包含大小写字母、数字、特殊字符各1个 |
| 禁止内容 | 用户名、连续相同字符超过3个 |

---

## 二、补充的项目整改建议

### 2.1 系统架构优化建议

#### 2.1.1 分层架构优化

当前问题：Controller层承担了过多业务逻辑，缺少Service层和DTO层。

**优化方案：**

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│            Controller (参数校验、响应封装)                      │
│            DTO (请求/响应数据传输对象)                          │
├─────────────────────────────────────────────────────────────┤
│                      Business Layer                          │
│            Service (业务逻辑处理)                             │
│            ServiceImpl (业务逻辑实现)                          │
│            BO (业务对象)                                       │
├─────────────────────────────────────────────────────────────┤
│                      Data Access Layer                        │
│            Mapper (数据库操作接口)                             │
│            Entity (数据库实体映射)                             │
├─────────────────────────────────────────────────────────────┤
│                      Infrastructure                           │
│            Config (配置类)                                     │
│            Filter/Interceptor (过滤器/拦截器)                   │
│            ExceptionHandler (全局异常处理)                      │
│            Util (工具类)                                       │
└─────────────────────────────────────────────────────────────┘
```

#### 2.1.2 模块化拆分

```
demo/
├── controller/           # REST API控制层
├── service/              # 业务逻辑层
├── mapper/               # 数据访问层
├── entity/               # 数据库实体
├── dto/                  # 数据传输对象（新增）
│   ├── request/          # 请求DTO
│   └── response/         # 响应DTO
├── config/               # 配置类
├── security/             # 安全相关（新增）
│   ├── filter/           # 认证过滤器
│   ├── handler/          # 认证处理器
│   └── jwt/              # JWT工具类
├── exception/            # 异常处理（新增）
│   ├── BusinessException # 业务异常
│   └── GlobalExceptionHandler # 全局异常处理
└── util/                 # 工具类（新增）
```

---

### 2.2 数据安全与加密策略

#### 2.2.1 敏感数据保护

| 数据类型 | 保护措施 |
|---------|---------|
| 用户密码 | Bcrypt加密存储 |
| 邮箱/手机号 | 脱敏显示（123****5678） |
| 订单号 | 加密传输 |
| 支付信息 | HTTPS传输 |

#### 2.2.2 数据库安全

```sql
-- 用户表密码字段变更
ALTER TABLE user MODIFY COLUMN password VARCHAR(255) NOT NULL;

-- 创建索引
CREATE INDEX idx_user_username ON user(username);
CREATE INDEX idx_user_email ON user(email);

-- 限制查询字段（避免SELECT *）
SELECT id, username, nickname, email, role, status, created_at FROM user;
```

#### 2.2.3 HTTPS配置

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: password
    key-store-type: PKCS12
    key-alias: tomcat
```

---

### 2.3 接口设计与API管理规范

#### 2.3.1 统一响应格式

```java
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    
    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> success(String message, T data) { ... }
    public static <T> ApiResponse<T> error(Integer code, String message) { ... }
    public static <T> ApiResponse<T> error(String message) { ... }
}
```

#### 2.3.2 错误码规范

| 错误码范围 | 含义 |
|-----------|-----|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权/Token过期 |
| 403 | 禁止访问/权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001-1099 | 用户相关错误 |
| 2001-2099 | 电影相关错误 |
| 3001-3099 | 订单相关错误 |
| 4001-4099 | 管理员相关错误 |

#### 2.3.3 API版本控制

```java
// 版本1（当前）
@RequestMapping("/api/v1/auth")

// 版本2（未来扩展）
@RequestMapping("/api/v2/auth")
```

---

### 2.4 错误处理与日志系统

#### 2.4.1 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AuthenticationException.class)
    public ApiResponse<Void> handleAuthException(AuthenticationException e) {
        return ApiResponse.error(401, e.getMessage());
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e) {
        return ApiResponse.error(403, "权限不足");
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
        // 记录日志
        log.error("系统异常", e);
        return ApiResponse.error(500, "系统繁忙，请稍后重试");
    }
}
```

#### 2.4.2 日志规范

```xml
<!-- logback配置 -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

---

### 2.5 性能优化方案

#### 2.5.1 数据库优化

| 优化项 | 方案 |
|-------|-----|
| 索引优化 | 为查询频繁的字段创建索引 |
| 分页查询 | 使用LIMIT替代全表扫描 |
| 批量操作 | 使用MyBatis Batch操作 |
| 查询缓存 | 使用Redis缓存热点数据 |

#### 2.5.2 缓存策略

```java
// Redis缓存配置
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // 序列化配置
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);
        
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
```

#### 2.5.3 连接池优化

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

---

### 2.6 安全防护措施

#### 2.6.1 CSRF防护

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())  // JWT模式下可禁用
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
```

#### 2.6.2 请求限流

```java
// 使用Bucket4j实现限流
@Component
public class RateLimitFilter implements Filter {
    private final Bucket bucket;
    
    public RateLimitFilter() {
        this.bucket = Bucket.builder()
            .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
            .build();
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).setStatus(429);
        }
    }
}
```

#### 2.6.3 SQL注入防护

| 防护方式 | 说明 |
|---------|-----|
| 使用MyBatis参数化查询 | `#{param}`自动转义 |
| 禁止拼接SQL | 禁止使用`${param}`进行动态拼接 |
| 输入校验 | 使用`@Valid`注解校验参数 |

---

### 2.7 代码质量与规范

#### 2.7.1 代码规范

| 规范项 | 要求 |
|-------|-----|
| 命名规范 | 类名大驼峰，方法名小驼峰，常量全大写下划线分隔 |
| 注释规范 | 所有public方法必须有Javadoc注释 |
| 异常处理 | 禁止catch后不处理，必须记录日志或向上抛出 |
| 依赖注入 | 使用`@Autowired`或构造器注入，禁止字段注入 |

#### 2.7.2 静态代码检查

```xml
<!-- pom.xml 添加SonarQube插件 -->
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.9.1.2184</version>
</plugin>
```

#### 2.7.3 单元测试

```xml
<!-- pom.xml 添加测试依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

### 2.8 部署与运维策略

#### 2.8.1 Docker容器化

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2.8.2 Docker Compose编排

```yaml
# docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: cinema
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
  
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/cinema
      SPRING_REDIS_HOST: redis
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started

volumes:
  mysql-data:
  redis-data:
```

#### 2.8.3 健康检查与监控

```java
// Actuator配置
@Configuration
public class ActuatorConfig {
    @Bean
    public HealthIndicator customHealthIndicator() {
        return () -> {
            // 自定义健康检查逻辑
            return Health.up().withDetail("service", "cinema").build();
        };
    }
}
```

---

## 三、企业级项目应达到的标准要求

### 3.1 功能完整性标准

| 标准项 | 要求 | 当前状态 |
|-------|-----|---------|
| 用户认证 | 支持注册、登录、退出、密码重置 | ❌ 缺少JWT |
| 权限管理 | 支持用户/管理员角色分离 | ❌ 无权限控制 |
| 电影管理 | 支持电影增删改查、排片管理 | ✅ 基本完成 |
| 订单管理 | 支持购票、退票、订单查询 | ✅ 基本完成 |
| 数据持久化 | 所有业务数据正确存储 | ✅ |

### 3.2 性能指标要求

| 指标 | 要求 |
|------|-----|
| API响应时间 | P95 < 200ms |
| 并发处理能力 | 支持1000+并发用户 |
| 数据库查询时间 | 单表查询 < 50ms |
| 页面加载时间 | 首屏 < 2s |

### 3.3 安全合规标准

| 标准 | 要求 |
|------|-----|
| 密码安全 | Bcrypt加密，禁止明文存储 |
| 数据传输 | HTTPS加密传输 |
| 认证机制 | JWT无状态认证 |
| 权限控制 | RBAC角色权限管理 |
| 输入验证 | 参数校验，防SQL注入/XSS |

### 3.4 可扩展性要求

| 要求 | 说明 |
|------|-----|
| 模块化设计 | 各模块低耦合，便于独立开发 |
| API版本化 | 支持API版本升级，兼容旧版本 |
| 配置外部化 | 支持环境变量、配置中心 |
| 插件化架构 | 支持功能模块热插拔 |

### 3.5 可维护性规范

| 规范 | 要求 |
|------|-----|
| 代码注释 | 关键类和方法有详细注释 |
| 日志规范 | 统一日志格式，分级记录 |
| 异常处理 | 全局异常处理，统一错误响应 |
| 测试覆盖率 | 核心业务代码测试覆盖率 > 80% |

### 3.6 兼容性要求

| 兼容项 | 要求 |
|-------|-----|
| 浏览器兼容 | Chrome、Firefox、Safari最新3个版本 |
| 移动端适配 | 响应式设计，支持手机/平板 |
| 数据库兼容 | MySQL 8.0+ |
| JDK版本 | Java 17 LTS |

### 3.7 用户体验标准

| 标准 | 要求 |
|------|-----|
| 页面响应 | 操作反馈 < 500ms |
| 错误提示 | 友好的错误提示，指导用户操作 |
| 加载状态 | 数据加载时显示Loading状态 |
| 空状态处理 | 无数据时显示友好提示 |

### 3.8 文档完整性要求

| 文档 | 要求 |
|------|-----|
| 接口文档 | Swagger/OpenAPI自动生成 |
| 部署文档 | Docker部署、环境配置说明 |
| 操作手册 | 用户操作指南 |
| 开发文档 | 架构设计、技术选型说明 |

---

## 四、整改优先级与实施路线建议

### 4.1 优先级划分

#### P0 - 安全致命（立即修复）

| 序号 | 整改项 | 风险等级 | 预计工时 |
|-----|-------|---------|---------|
| 1 | 密码加密存储（Bcrypt） | 高危 | 4h |
| 2 | 密码明文比对修复 | 高危 | 2h |
| 3 | 后端认证拦截器 | 高危 | 8h |
| 4 | JWT生成与验证 | 高危 | 8h |
| 5 | 登录返回不含密码 | 中危 | 2h |

#### P1 - 核心功能（一周内完成）

| 序号 | 整改项 | 风险等级 | 预计工时 |
|-----|-------|---------|---------|
| 6 | 前端路由守卫 | 中危 | 4h |
| 7 | Token刷新机制 | 中危 | 6h |
| 8 | 统一响应格式 | 低危 | 4h |
| 9 | 全局异常处理 | 低危 | 6h |
| 10 | 密码强度校验 | 中危 | 4h |

#### P2 - 架构优化（两周内完成）

| 序号 | 整改项 | 风险等级 | 预计工时 |
|-----|-------|---------|---------|
| 11 | 分层架构重构 | 低危 | 16h |
| 12 | DTO层设计与实现 | 低危 | 12h |
| 13 | Redis缓存优化 | 低危 | 8h |
| 14 | 连接池配置优化 | 低危 | 4h |

#### P3 - 规范提升（持续改进）

| 序号 | 整改项 | 风险等级 | 预计工时 |
|-----|-------|---------|---------|
| 15 | 代码规范检查 | 低危 | 持续 |
| 16 | 单元测试编写 | 低危 | 持续 |
| 17 | Docker容器化 | 低危 | 8h |
| 18 | API文档生成 | 低危 | 4h |
| 19 | 日志系统完善 | 低危 | 6h |

### 4.2 实施路线图

```
第一阶段（1-2天）：安全修复
├── 完成P0所有任务
├── 验证：密码加密、认证拦截、JWT正常工作
└── 验收标准：Postman测试所有认证接口

第二阶段（3-5天）：核心功能完善
├── 完成P1所有任务
├── 验证：路由守卫、Token刷新、统一响应
└── 验收标准：前后端联调通过

第三阶段（6-10天）：架构优化
├── 完成P2所有任务
├── 验证：分层架构、DTO、缓存
└── 验收标准：性能测试通过

第四阶段（持续）：规范提升
├── 完成P3所有任务
├── 验证：代码质量、测试覆盖率、部署
└── 验收标准：SonarQube扫描通过、CI/CD流水线建立
```

### 4.3 验收标准

#### 第一阶段验收

| 验收项 | 验收标准 |
|-------|---------|
| 密码存储 | 数据库中密码为Bcrypt加密值 |
| 密码比对 | 使用BCryptPasswordEncoder.matches() |
| 认证拦截 | 未登录用户无法访问需要认证的接口 |
| JWT验证 | Token过期或无效时返回401 |
| 登录响应 | 返回数据不包含password字段 |

#### 第二阶段验收

| 验收项 | 验收标准 |
|-------|---------|
| 路由守卫 | 未登录自动跳转首页，管理员页面仅管理员可访问 |
| Token刷新 | Access Token过期后自动获取新Token |
| 统一响应 | 所有接口返回统一格式的ApiResponse |
| 异常处理 | 所有异常返回标准错误码和错误信息 |
| 密码校验 | 弱密码无法注册 |

#### 第三阶段验收

| 验收项 | 验收标准 |
|-------|---------|
| 分层架构 | Controller→Service→Mapper三层分离 |
| DTO层 | 请求/响应使用DTO，不直接返回Entity |
| 缓存效果 | 热点数据查询性能提升50%以上 |
| 连接池 | HikariCP配置正确，无连接泄漏 |

#### 第四阶段验收

| 验收项 | 验收标准 |
|-------|---------|
| 代码质量 | SonarQube代码质量评分 > 80 |
| 测试覆盖率 | 核心代码测试覆盖率 > 80% |
| Docker部署 | 一键启动所有服务 |
| API文档 | Swagger UI可访问，接口文档完整 |
| 日志规范 | 日志分级清晰，便于问题排查 |

---

## 附录：当前问题代码清单

### 安全问题

| 文件 | 问题 | 位置 |
|------|-----|------|
| [AuthController.java](file:///d:/Al-code/wang/demo/src/main/java/com/example/demo/controller/AuthController.java) | 密码明文存储 | 第187行 |
| [AuthController.java](file:///d:/Al-code/wang/demo/src/main/java/com/example/demo/controller/AuthController.java) | 密码明文比对 | 第219行 |
| [AuthController.java](file:///d:/Al-code/wang/demo/src/main/java/com/example/demo/controller/AuthController.java) | 登录返回含密码 | 第228行 |
| [UserMapper.java](file:///d:/Al-code/wang/demo/src/main/java/com/example/demo/mapper/UserMapper.java) | updatePassword明文更新 | 第45行 |
| [CorsConfig.java](file:///d:/Al-code/wang/demo/src/main/java/com/example/demo/config/CorsConfig.java) | CORS全放开 | 第17-20行 |
| [auth.js](file:///d:/Al-code/wang/frontend/src/stores/auth.js) | Token伪造 | 第32行 |
| [router/index.js](file:///d:/Al-code/wang/frontend/src/router/index.js) | 无路由守卫 | 全局 |

### 架构问题

| 文件 | 问题 | 位置 |
|------|-----|------|
| [AuthController.java](file:///d:/Al-code/wang/demo/src/main/java/com/example/demo/controller/AuthController.java) | 业务逻辑过重 | 全局 |
| 项目根目录 | 缺少DTO层 | - |
| 项目根目录 | 缺少Service层 | - |
| 项目根目录 | 缺少全局异常处理 | - |

---

**文档结束**