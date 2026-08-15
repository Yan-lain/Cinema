package com.example.demo.constant;

/**
 * Redis缓存Key常量类
 * 
 * 【设计说明】
 * 统一管理所有Redis缓存键，避免硬编码和拼写错误
 * 使用静态工厂方法生成带参数的Key，提高代码可读性和维护性
 * 
 * 【命名规范】
 * - 使用冒号":"作为层级分隔符
 * - 格式：{模块}:{类型}:{id}
 * - 例如：movie:detail:1, schedule:movie:100
 * 
 * 【缓存策略说明】
 * - MOVIE_LIST: 电影列表，30分钟过期
 * - MOVIE_SHOWING: 正在上映电影，10分钟过期
 * - MOVIE_DETAIL: 电影详情，5分钟过期
 * - SCHEDULE_DETAIL: 场次详情，5分钟过期
 * - SCHEDULE_MOVIE: 电影场次列表，10分钟过期
 * - SCHEDULE_CINEMA: 影院场次列表，10分钟过期
 * - SCHEDULE_HALL: 影厅场次列表，5分钟过期
 * - SCHEDULE_OCCUPIED: 影厅已占用时间段，2小时过期
 * - LOCK_PREFIX: 分布式锁前缀
 */
public final class RedisKey {

    private RedisKey() {
    }

    // ============ 电影模块 ============

    public static final String MOVIE_LIST = "movie:list";
    
    public static final String MOVIE_SHOWING = "movie:showing";
    
    public static String movieDetail(Long id) {
        return "movie:detail:" + id;
    }

    // ============ 场次模块 ============

    public static String scheduleDetail(Long id) {
        return "schedule:detail:" + id;
    }

    public static String scheduleMovie(Long movieId) {
        return "schedule:movie:" + movieId;
    }

    public static String scheduleCinema(Long cinemaId) {
        return "schedule:cinema:" + cinemaId;
    }

    public static String scheduleHall(Long hallId) {
        return "schedule:hall:" + hallId;
    }

    public static String scheduleOccupied(Long hallId, String date) {
        return "schedule:occupied:" + hallId + ":" + date;
    }

    // ============ 用户模块 ============

    public static String userDetail(Long userId) {
        return "user:detail:" + userId;
    }

    public static String userSession(String token) {
        return "user:session:" + token;
    }

    /**
     * 生成 RefreshToken 的 Redis Key
     * 【设计说明】以 refreshToken 字符串作为索引，value 存储用户身份信息，
     * 用于 /api/auth/refresh 接口校验 refreshToken 合法性并签发新的 accessToken。
     */
    public static String refreshToken(String token) {
        return "refresh:token:" + token;
    }

    // ============ 验证码模块 ============

    //手机号码验证码暂未实现
    public static String smsCode(String phone) {
        return "sms:code:" + phone;
    }
    
    //邮箱验证码
    public static String emailCode(String email) {
        return "email:code:" + email;
    }

    // ============ 订单模块 ============

    public static String orderDetail(Long orderId) {
        return "order:detail:" + orderId;
    }

    public static String orderUser(Long userId) {
        return "order:user:" + userId;
    }

    // ============ 分布式锁 ============

    public static String lock(String key) {
        return "lock:" + key;
    }

    public static final String LOCK_SEAT_PREFIX = "lock:seat:";
    
    public static String lockSeat(Long scheduleId) {
        return "lock:seat:" + scheduleId;
    }

    // ============ 布隆过滤器 ============
    // 【设计说明】
    // 用于快速判断元素是否存在，避免重复操作
    // 【缓存策略说明】
    // - 电影ID布隆过滤器：30分钟过期
    // - 用户ID布隆过滤器：30分钟过期
    //我不了解布隆过滤器
    //如何实现布隆过滤器
    //1. 定义布隆过滤器的大小（例如，1000000个元素）
    //2. 定义布隆过滤器的哈希函数数量（例如，16个）
    //3. 每个元素插入到布隆过滤器时，使用多个哈希函数计算元素的哈希值
    //4. 每个哈希值对应一个位，将该位设置为1
    //5. 每个元素查询时，使用多个哈希函数计算元素的哈希值
    //6. 每个哈希值对应一个位，判断该位是否为1
    //7. 如果所有位都为1，则认为元素存在（概率为1/24）
    //8. 如果至少有一个位为0，则认为元素不存在（概率为1/24）

    public static final String BLOOM_MOVIE = "bloom:movie";
    
    public static final String BLOOM_USER = "bloom:user";

    // ============ 统计模块 ============

    public static final String STATS_VISIT = "stats:visit";
    
    public static final String STATS_ORDER = "stats:order";

    // ============ 热门数据 ============

    public static final String HOT_MOVIES = "hot:movies";
    
    public static final String HOT_CINEMAS = "hot:cinemas";
}
