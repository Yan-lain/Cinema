package com.example.demo.service;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.Announcement;
import com.example.demo.entity.Cinema;
import com.example.demo.entity.Movie;
import com.example.demo.entity.Order;
import com.example.demo.entity.Hall;
import com.example.demo.entity.Schedule;
import com.example.demo.entity.User;
import com.example.demo.entity.Seat;
import com.example.demo.dto.response.MovieResponse;



import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 管理员服务接口
 *
 * 【架构说明】
 * 定义管理员后台所有业务操作的契约，由 AdminServiceImpl 实现。
 * 接口方法返回业务数据，业务失败时抛出 BusinessException，
 * 由 GlobalExceptionHandler 统一转换为 ApiResponse。
 *
 * 【职责划分】
 * 1. 管理员登录认证
 * 2. 电影管理（CRUD + 状态切换 + 缓存维护）
 * 3. 影院管理（CRUD）
 * 4. 场次管理（委托 ScheduleService）
 * 5. 公告管理（CRUD + 状态切换）
 * 6. 用户管理（CRUD + 状态切换 + 密码重置）
 * 7. 订单管理（查询 + 删除 + 退款）
 */
public interface AdminService {

    // ============ 管理员登录 ============

    /**
     * 管理员登录
     *
     * 【执行流程】
     * 1. 根据用户名查询用户
     * 2. 校验密码（兼容 BCrypt 加密）
     * 3. 校验用户角色是否为管理员
     * 4. 校验用户状态是否正常
     * 5. 生成 JWT Token 并封装返回
     *
     * @param loginRequest 登录请求（用户名 + 密码）
     * @return 登录成功后的用户信息（含 Token）
     */
    UserResponse login(LoginRequest loginRequest);

    // ============ 影院管理 ============

    /**
     * 查询所有有效影院
     *
     * @return 影院列表
     */
    List<Cinema> getAllCinemas();

    /**
     * 根据 ID 查询影院
     *
     * @param id 影院 ID
     * @return 影院信息（不存在时抛出 BusinessException）
     */
    Cinema getCinemaById(Long id);

    /**
     * 新增影院
     *
     * @param cinema 影院信息
     * @return 新增后的影院（含生成的 ID）
     */
    Cinema addCinema(Cinema cinema);

    /**
     * 更新影院信息
     *
     * @param id     影院 ID
     * @param cinema 影院信息
     */
    void updateCinema(Long id, Cinema cinema);

    /**
     * 删除影院
     *
     * @param id 影院 ID
     */
    void deleteCinema(Long id);

    /**
     * 查询所有放映厅
     *
     * @return 放映厅列表
     */
    List<Hall> getAllHalls();

    
    // ============ 电影管理 ============

    /**
     * 查询所有电影
     *
     * @return 电影列表
     */
    List<MovieResponse> getAllMovies();

    /**
     * 根据 ID 查询电影
     *
     * @param id 电影 ID
     * @return 电影信息（不存在时抛出 BusinessException）
     */
    MovieResponse getMovieById(Long id);

    /**
     * 新增电影并清除相关缓存
     *
     * @param movie 电影信息
     * @return 新增后的电影（含生成的 ID）
     */
    MovieResponse addMovie(Movie movie);

    /**
     * 更新电影信息并清除相关缓存
     *
     * @param id    电影 ID
     * @param movie 电影信息
     * @return 更新后的电影
     */
    MovieResponse updateMovie(Long id, Movie movie);

    /**
     * 删除电影并清除相关缓存
     *
     * @param id 电影 ID
     */
    void deleteMovie(Long id);

    /**
     * 更新电影状态并清除相关缓存
     *
     * @param id     电影 ID
     * @param status 新状态
     */
    void updateMovieStatus(Long id, String status);

    // ============ 场次管理 ============

    /**
     * 查询场次列表（支持按影院、状态、关键字筛选）
     *
     * @param cinemaId 影院 ID（可选）
     * @param status   状态（可选）
     * @param keyword  搜索关键字（可选）
     * @return 场次列表
     */
    List<Map<String, Object>> getAllSchedules(Long cinemaId, String status, String keyword);

    /**
     * 根据 ID 查询场次详情
     *
     * @param id 场次 ID
     * @return 场次详情（不存在时抛出 BusinessException）
     */
    Map<String, Object> getScheduleById(Long id);

    /**
     * 新增场次
     *
     * @param schedule 场次信息
     * @return 新增后的场次
     */
    Schedule addSchedule(Schedule schedule);

    /**
     * 更新场次信息
     *
     * @param id       场次 ID
     * @param schedule 场次信息
     * @return 更新后的场次
     */
    Schedule updateSchedule(Long id, Schedule schedule);

    /**
     * 删除场次
     *
     * @param id 场次 ID
     */
    void deleteSchedule(Long id);

    /**
     * 查询指定影厅在某日的已占用场次时段
     *
     * @param hallId 影厅 ID
     * @param date   日期
     * @return 已占用时段列表
     */
    List<Map<String, Object>> getOccupiedSlots(Long hallId, LocalDate date);

    /**
     * 检查场次时间冲突
     *
     * @param hallId    影厅 ID
     * @param date      日期
     * @param startTime 开始时间
     * @param duration  时长（分钟）
     * @return 冲突检测结果（包含 conflict 标志和冲突详情）
     */
    Map<String, Object> checkConflict(Long hallId, LocalDate date, String startTime, Integer duration);

    // ============ 公告管理 ============

    /**
     * 获取最新已发布公告
     *
     * @return 最新公告（无公告时返回 null）
     */
    Announcement getLatestAnnouncement();

    /**
     * 查询所有公告
     *
     * @return 公告列表
     */
    List<Announcement> getAllAnnouncements();

    /**
     * 根据 ID 查询公告
     *
     * @param id 公告 ID
     * @return 公告信息（不存在时抛出 BusinessException）
     */
    Announcement getAnnouncementById(Long id);

    /**
     * 新增公告
     *
     * @param announcement 公告信息
     * @return 新增后的公告
     */
    Announcement addAnnouncement(Announcement announcement);

    /**
     * 更新公告
     *
     * @param id           公告 ID
     * @param announcement 公告信息
     * @return 更新后的公告
     */
    Announcement updateAnnouncement(Long id, Announcement announcement);

    /**
     * 删除公告
     *
     * @param id 公告 ID
     */
    void deleteAnnouncement(Long id);

    /**
     * 更新公告状态
     *
     * @param id     公告 ID
     * @param status 新状态
     */
    void updateAnnouncementStatus(Long id, String status);

    // ============ 用户管理 ============

    /**
     * 查询用户列表（支持按角色筛选）
     *
     * @param role 角色（可选）
     * @return 用户列表（密码字段已脱敏）
     */
    List<User> getAllUsers(String role);

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户信息（密码字段已脱敏，不存在时抛出 BusinessException）
     */
    User getUserById(Long id);

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 新增后的用户（密码字段已脱敏）
     */
    User addUser(User user);

    /**
     * 更新用户信息（保留原密码，不通过此接口修改密码）
     *
     * @param id   用户 ID
     * @param user 用户信息
     */
    void updateUser(Long id, User user);

    /**
     * 删除用户
     *
     * @param id 用户 ID
     */
    void deleteUser(Long id);

    /**
     * 更新用户状态
     *
     * @param id     用户 ID
     * @param status 新状态
     */
    void updateUserStatus(Long id, String status);

    /**
     * 重置用户密码
     *
     * @param id       用户 ID
     * @param password 新密码（明文，内部会加密存储）
     */
    void resetUserPassword(Long id, String password);

    // ============ 订单管理 ============

    /**
     * 查询订单列表（支持按订单号搜索或按状态筛选）
     *
     * @param q      订单号关键字（可选）
     * @param status 订单状态（可选）
     * @return 订单列表
     */
    List<Order> getAllOrders(String q, String status);

    /**
     * 根据 ID 查询订单
     *
     * @param id 订单 ID
     * @return 订单信息（不存在时抛出 BusinessException）
     */
    Order getOrderById(Long id);

    /**
     * 删除订单
     *
     * @param id 订单 ID
     */
    void deleteOrder(Long id);

    /**
     * 订单退款（仅允许对已支付订单退款）
     *
     * @param id 订单 ID
     */
    void refundOrder(Long id);

    /**
     * 订单取消（仅允许对已支付订单取消）
     *
     * @param id 订单 ID
     */
    void cancelOrder(Long id);
    

    /**
     * 订单确认（仅允许对已取消订单确认）
     *
     * @param id 订单 ID
     */
    void confirmOrder(Long id);

    /**
     * 查询所有座位
     *
     * @return 座位列表
     */
    List<Seat> getAllSeats();

}
