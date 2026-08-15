package com.example.demo.service.impl;

import com.example.demo.constant.Constants;
import com.example.demo.constant.ErrorCode;
import com.example.demo.constant.RedisKey;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.dto.response.MovieResponse;
import com.example.demo.entity.Announcement;
import com.example.demo.entity.Cinema;
import com.example.demo.entity.Movie;
import com.example.demo.entity.Order;
import com.example.demo.entity.Schedule;
import com.example.demo.entity.User;
import com.example.demo.entity.Hall;
import com.example.demo.entity.Seat;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.AnnouncementMapper;
import com.example.demo.mapper.CinemaMapper;
import com.example.demo.mapper.MovieMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.mapper.HallMapper;
import com.example.demo.mapper.SeatMapper;
import com.example.demo.service.AdminService;
import com.example.demo.service.RedisService;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.service.ScheduleService;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理员服务实现类
 *
 * 【架构说明】
 * 实现 AdminService 接口，承载管理员后台全部业务逻辑。
 * 业务失败时抛出 BusinessException，由 GlobalExceptionHandler 统一转换为 ApiResponse。
 *
 * 【分层职责】
 * - 本类：业务校验、数据组装、缓存维护、事务边界
 * - Controller：仅负责 HTTP 接收与响应包装
 * - Mapper：仅负责数据库读写
 *
 * 【依赖说明】
 * 直接依赖各 Mapper 进行 CRUD，依赖 ScheduleService 处理场次相关业务，
 * 依赖 RedisService 维护电影列表/详情缓存一致性。
 */
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private CinemaMapper cinemaMapper;
    
    @Autowired
    private HallMapper hallMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private SeatMapper seatMapper;

    /** RefreshToken 服务（用于签发长寿命刷新令牌，实现无感续期） */
    @Autowired
    private RefreshTokenService refreshTokenService;

    // ==================== 管理员登录 ====================

    /**
     * 管理员登录
     *
     * 【执行流程】
     * 1. 按用户名查询用户，不存在则抛 WRONG_PASSWORD（避免暴露用户是否存在）
     * 2. 使用 BCrypt 校验密码
     * 3. 校验角色是否为管理员（兼容大小写）
     * 4. 校验账号是否被禁用
     * 5. 生成 JWT Token 并封装 UserResponse 返回
     */
    @Override
    public UserResponse login(LoginRequest loginRequest) {
        // 根据用户名查询用户
        User user = userMapper.findByUsername(loginRequest.getUsername());
        if (user == null) {
            System.out.println("管理员登录失败：用户不存在，username=" + loginRequest.getUsername());
            throw new BusinessException(ErrorCode.WRONG_PASSWORD);
        }

        // 校验密码（BCrypt 加密比对）
        if (!PasswordUtil.matches(loginRequest.getPassword(), user.getPassword())) {
            System.out.println("管理员登录失败：密码错误，username=" + loginRequest.getUsername());
            throw new BusinessException(ErrorCode.WRONG_PASSWORD);
        }

        // 校验用户角色是否为管理员（兼容大小写）
        if (user.getRole() == null || !Constants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            System.out.println("管理员登录失败：非管理员账号，username=" + loginRequest.getUsername() + ", role=" + user.getRole());
            throw new BusinessException(ErrorCode.USER_NO_PERMISSION, "非管理员账号，无法登录管理后台");
        }

        // 校验用户状态是否正常
        if (Constants.STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "账号已被禁用，请联系超级管理员");
        }

        // 同时签发短寿命 accessToken（JWT）和长寿命 refreshToken（存 Redis）
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = refreshTokenService.generate(user.getId(), user.getUsername(), user.getRole());
        System.out.println("管理员登录成功：username=" + loginRequest.getUsername());

        // 封装并返回用户信息（含 accessToken 和 refreshToken）
        return UserResponse.fromEntity(user, token, refreshToken);
    }

    // ==================== 影院管理 ====================

    /**
     * 查询所有有效影院
     */
    @Override
    public List<Cinema> getAllCinemas() {
        return cinemaMapper.findByActive();
    }

    /**
     * 根据 ID 查询影院
     */
    @Override
    public Cinema getCinemaById(Long id) {
        Cinema cinema = cinemaMapper.findById(id);
        if (cinema == null) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_FOUND);
        }
        return cinema;
    }

    /**
     * 新增影院（默认状态为 active）
     */
    @Override
    public Cinema addCinema(Cinema cinema) {
        if (cinema.getStatus() == null) {
            cinema.setStatus(Constants.STATUS_ACTIVE);
        }
        cinemaMapper.insert(cinema);
        return cinema;
    }

    /**
     * 更新影院信息（先校验存在性）
     */
    @Override
    public void updateCinema(Long id, Cinema cinema) {
        // 校验影院是否存在
        Cinema existing = cinemaMapper.findById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_FOUND);
        }
        cinema.setId(id);
        cinemaMapper.update(cinema);
    }

    /**
     * 删除影院（先校验存在性）
     */
    @Override
    public void deleteCinema(Long id) {
        Cinema cinema = cinemaMapper.findById(id);
        if (cinema == null) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_FOUND);
        }
        cinemaMapper.deleteById(id);
    }
    
    /**
     * 查询所有放映厅
     */
    @Override
    public List<Hall> getAllHalls() {
        return hallMapper.findAll();
    }

    // ==================== 电影管理 ====================

    /**
     * 查询所有电影
     */
    @Override
    public List<MovieResponse> getAllMovies() {
        return MovieResponse.fromEntities(movieMapper.findAll());
    }

    /**
     * 根据 ID 查询电影
     */
    @Override
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieMapper.findById(id);
        if (movie == null) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
        }
        return MovieResponse.fromEntity(movie);
    }

    /**
     * 新增电影并清除列表/热映缓存
     */
    @Override
    public MovieResponse addMovie(Movie movie) {
        if (movie.getStatus() == null) {
            movie.setStatus("showing");
        }
        movieMapper.insert(movie);
        // 清除电影相关缓存，保证数据一致性
        clearMovieCache(null);
        return MovieResponse.fromEntity(movie);
    }

    /**
     * 更新电影信息并清除相关缓存
     */
    @Override
    public MovieResponse updateMovie(Long id, Movie movie) {
        Movie existing = movieMapper.findById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
        }
        movie.setId(id);
        movieMapper.update(movie);
        // 清除电影相关缓存（包括详情缓存）
        clearMovieCache(id);
        return MovieResponse.fromEntity(movie);
    }

    /**
     * 删除电影并清除相关缓存
     */
    @Override
    public void deleteMovie(Long id) {
        Movie movie = movieMapper.findById(id);
        if (movie == null) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
        }
        movieMapper.deleteById(id);
        clearMovieCache(id);
    }

    /**
     * 更新电影状态并清除相关缓存
     */
    @Override
    public void updateMovieStatus(Long id, String status) {
        movieMapper.updateStatus(id, status);
        clearMovieCache(id);
    }

    /**
     * 清除电影相关缓存（列表 + 热映 + 指定电影详情）
     *
     * @param movieId 电影 ID（为 null 时只清除列表和热映缓存）
     */
    private void clearMovieCache(Long movieId) {
        redisService.delete(RedisKey.MOVIE_LIST);
        redisService.delete(RedisKey.MOVIE_SHOWING);
        if (movieId != null) {
            redisService.delete(RedisKey.movieDetail(movieId));
        }
    }

    // ==================== 场次管理 ====================

    /**
     * 查询场次列表（委托 ScheduleService，支持多条件筛选）
     */
    @Override
    public List<Map<String, Object>> getAllSchedules(Long cinemaId, String status, String keyword) {
        return scheduleService.getAllSchedules(cinemaId, status, keyword);
    }

    /**
     * 根据 ID 查询场次详情
     */
    @Override
    public Map<String, Object> getScheduleById(Long id) {
        Map<String, Object> schedule = scheduleService.getScheduleById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND);
        }
        return schedule;
    }

    /**
     * 新增场次
     */
    @Override
    public Schedule addSchedule(Schedule schedule) {
        return scheduleService.addSchedule(schedule);
    }

    /**
     * 更新场次信息（ScheduleService 内部会校验存在性，不存在时抛 RuntimeException）
     */
    @Override
    public Schedule updateSchedule(Long id, Schedule schedule) {
        return scheduleService.updateSchedule(id, schedule);
    }

    /**
     * 删除场次
     */
    @Override
    public void deleteSchedule(Long id) {
        scheduleService.deleteSchedule(id);
    }

    /**
     * 查询指定影厅在某日的已占用场次时段
     */
    @Override
    public List<Map<String, Object>> getOccupiedSlots(Long hallId, LocalDate date) {
        return scheduleService.getOccupiedSlots(hallId, date);
    }

    /**
     * 检查场次时间冲突
     */
    @Override
    public Map<String, Object> checkConflict(Long hallId, LocalDate date, String startTime, Integer duration) {
        return scheduleService.checkConflict(hallId, date, startTime, duration);
    }

    // ==================== 公告管理 ====================

    /**
     * 获取最新已发布公告
     */
    @Override
    public Announcement getLatestAnnouncement() {
        List<Announcement> announcements = announcementMapper.findByStatus("published");
        return announcements.isEmpty() ? null : announcements.get(0);
    }

    /**
     * 查询所有公告
     */
    @Override
    public List<Announcement> getAllAnnouncements() {
        return announcementMapper.findAll();
    }

    /**
     * 根据 ID 查询公告
     */
    @Override
    public Announcement getAnnouncementById(Long id) {
        Announcement announcement = announcementMapper.findById(id);
        if (announcement == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "公告不存在");
        }
        return announcement;
    }

    /**
     * 新增公告（默认状态为 published，发布时间为当前时间）
     */
    @Override
    public Announcement addAnnouncement(Announcement announcement) {
        if (announcement.getStatus() == null) {
            announcement.setStatus("published");
        }
        announcement.setPublishedAt(LocalDateTime.now());
        announcementMapper.insert(announcement);
        return announcement;
    }

    /**
     * 更新公告（先校验存在性）
     */
    @Override
    public Announcement updateAnnouncement(Long id, Announcement announcement) {
        Announcement existing = announcementMapper.findById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "公告不存在");
        }
        announcement.setId(id);
        announcementMapper.update(announcement);
        return announcement;
    }

    /**
     * 删除公告（先校验存在性）
     */
    @Override
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementMapper.findById(id);
        if (announcement == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "公告不存在");
        }
        announcementMapper.deleteById(id);
    }

    /**
     * 更新公告状态
     */
    @Override
    public void updateAnnouncementStatus(Long id, String status) {
        announcementMapper.updateStatus(id, status);
    }

    // ==================== 用户管理 ====================

    /**
     * 查询用户列表（支持按角色筛选）
     */
    @Override
    public List<User> getAllUsers(String role) {
        if (role != null && !role.isEmpty()) {
            return userMapper.findByRole(role);
        }
        return userMapper.findAll();
    }

    /**
     * 根据 ID 查询用户（密码字段脱敏）
     */
    @Override
    public User getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 密码脱敏，避免明文密码返回前端
        user.setPassword("******");
        return user;
    }

    /**
     * 新增用户（默认角色 user，默认状态 active，密码脱敏返回）
     */
    @Override
    public User addUser(User user) {
        if (user.getRole() == null) {
            user.setRole(Constants.ROLE_USER.toLowerCase());
        }
        if (user.getStatus() == null) {
            user.setStatus(Constants.STATUS_ACTIVE);
        }        
        // 密码脱敏
        user.setPassword("******");
        userMapper.insert(user);
        return user;
    }

    /**
     * 更新用户信息（保留原密码，不通过此接口修改密码）
     */
    @Override
    public void updateUser(Long id, User user) {
        User existing = userMapper.findById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setId(id);
        // 保留原密码，防止被覆盖为空或不安全值
        user.setPassword(existing.getPassword());
        userMapper.update(user);
    }

    /**
     * 删除用户（先校验存在性）
     */
    @Override
    public void deleteUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userMapper.deleteById(id);
    }

    /**
     * 更新用户状态
     */
    @Override
    public void updateUserStatus(Long id, String status) {
        userMapper.updateStatus(id, status);
    }

    /**
     * 重置用户密码（明文密码加密后存储）
     */
    @Override
    public void resetUserPassword(Long id, String password) {
        userMapper.updatePassword(id, PasswordUtil.encode(password));
    }

    // ==================== 订单管理 ====================

    /**
     * 查询订单列表（支持按订单号搜索或按状态筛选）
     */
    @Override
    public List<Order> getAllOrders(String q, String status) {
        //q: 订单号搜索参数
        //status: 状态筛选参数
        // 优先按状态筛选
        if (status != null && !status.isEmpty()) {
            return orderMapper.findByStatus(status);
        }
        // 其次按订单号搜索
        if (q != null && !q.isEmpty()) {
            Order order = orderMapper.findByOrderNumber(q);
            return order != null ? List.of(order) : List.of();
        }
        // 无筛选条件，返回全部
        return orderMapper.findAll();
    }

    /**
     * 根据 ID 查询订单
     */
    @Override
    public Order getOrderById(Long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 删除订单（先校验存在性）
     */
    @Override
    public void deleteOrder(Long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        orderMapper.deleteById(id);
    }

    /**
     * 订单退款（仅允许对已支付订单退款）
     *
     * 【退款逻辑】
     * 1. 校验订单存在性
     * 2. 校验订单状态为已支付
     * 3. 更新订单状态为已退款、支付状态为已退款、退款状态为已退款
     * 4. 记录退款时间
     */
    @Override
    public void refundOrder(Long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 仅允许对已支付订单进行退款
        if (!"paid".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_REFUND_ERROR, "只能对已付款订单进行退款");
        }

        // 更新订单相关状态
        order.setStatus("refunded");
        order.setPayStatus("refunded");
        order.setRefundStatus("refunded");
        order.setRefundedAt(LocalDateTime.now());
        orderMapper.update(order);
    }

    /**
     * 订单取消（仅允许对已支付订单取消）
     *
     * @param id 订单 ID
     */
    @Override
    public void cancelOrder(Long id) {
        // Order order = orderMapper.findById(id);
        // if (order == null) {
        //     throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        // }

        // // 仅允许对已支付订单进行取消
        // if (!"paid".equals(order.getStatus())) {
        //     throw new BusinessException(ErrorCode.ORDER_CANCEL_ERROR, "只能对已付款订单进行取消");
        // }

        // // 更新订单相关状态
        // order.setStatus("canceled");
        // order.setPayStatus("canceled");
        // order.setCancelStatus("canceled");
        // order.setCanceledAt(LocalDateTime.now());
        // orderMapper.update(order);
        // // 订单确认（仅允许对已取消订单确认）
        // if (!"canceled".equals(order.getStatus())) {
        //     throw new BusinessException(ErrorCode.ORDER_CONFIRM_ERROR, "只能对已取消订单进行确认");
        // }

        // // 更新订单相关状态
        // order.setStatus("confirmed");
        // order.setPayStatus("confirmed");
        // order.setConfirmStatus("confirmed");
        // order.setConfirmedAt(LocalDateTime.now());
        // orderMapper.update(order);
    }
    /** 
     * 订单确认（仅允许对已取消订单确认）
     */
    @Override
    public void confirmOrder(Long id) {
        // Order order = orderMapper.findById(id);
        // if (order == null) {
        //     throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        // }

        // // 仅允许对已取消订单进行确认
        // if (!"canceled".equals(order.getStatus())) {
        //     throw new BusinessException(ErrorCode.ORDER_CONFIRM_ERROR, "只能对已取消订单进行确认");
        // }

        // // 更新订单相关状态
        // order.setStatus("confirmed");
        // order.setPayStatus("confirmed");
        // order.setConfirmStatus("confirmed");
        // order.setConfirmedAt(LocalDateTime.now());
        // orderMapper.update(order);
    }

    /**
     * 查询所有座位
     *
     * @return 座位列表
     */
    @Override
    public List<Seat> getAllSeats() {
        return seatMapper.findAll();
    }
}
