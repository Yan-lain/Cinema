package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
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
import com.example.demo.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 *
 * 【架构说明】
 * 仅负责接收 HTTP 请求、调用 AdminService 处理业务、封装 ApiResponse 返回。
 * 业务逻辑全部下沉到 AdminServiceImpl，异常由 GlobalExceptionHandler 统一处理。
 *
 * 【职责边界】
 * - 本类：路由映射、参数提取、响应包装
 * - AdminService：业务校验、数据组装、缓存维护
 * - GlobalExceptionHandler：异常捕获与统一错误响应
 *
 * 【安全说明】
 * 管理员接口访问权限由 SecurityConfig 的 hasRole("ADMIN") 控制，
 * 登录接口 /api/admin/login 在 SecurityConfig 中配置为 permitAll。
 */
@Tag(name = "管理员后台", description = "管理员专用接口：影院、电影、场次、订单、用户、公告等全模块管理")
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")//配置了全局的CORS，这里可以省略
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ==================== 管理员登录 ====================

    /**
     * 管理员登录接口
     *
     * @param request 登录请求（用户名 + 密码）
     * @return 登录成功返回用户信息和 Token
     * @throws IllegalArgumentException 如果用户名或密码错误
     * @throws Exception 如果其他业务异常
     * @return ApiResponse<UserResponse>
     * @see LoginRequest
     * @see UserResponse
     * @see ApiResponse
     */
    @Operation(summary = "管理员登录", description = "管理员用户名密码登录，返回 JWT Token")
    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        UserResponse response = adminService.login(request);
         return ApiResponse.success("管理员登录成功", response);
    }

    // ==================== 影院管理 ====================

    /**
     * 查询所有有效影院
     */
    @Operation(summary = "查询所有影院（管理员）")
    @GetMapping("/cinemas")
    public ApiResponse<List<Cinema>> getAllCinemas() {
        return ApiResponse.success(adminService.getAllCinemas());
    }

    /**
     * 根据 ID 查询影院
     */
    @Operation(summary = "根据 ID 查询影院")
    @GetMapping("/cinemas/{id}")
    public ApiResponse<Cinema> getCinemaById(@PathVariable Long id) {
        return ApiResponse.success(adminService.getCinemaById(id));
    }

    /**
     * 新增影院
     */
    @Operation(summary = "新增影院")
    @PostMapping("/cinemas")
    public ApiResponse<Cinema> addCinema(@RequestBody Cinema cinema) {
        return ApiResponse.success("影院添加成功", adminService.addCinema(cinema));
    }

    /**
     * 更新影院信息
     */
    @Operation(summary = "更新影院信息")
    @PutMapping("/cinemas/{id}")
    public ApiResponse<Void> updateCinema(@PathVariable Long id, @RequestBody Cinema cinema) {
        adminService.updateCinema(id, cinema);
        return ApiResponse.success("影院更新成功", null);
    }

    /**
     * 删除影院
     */
    @Operation(summary = "删除影院")
    @DeleteMapping("/cinemas/{id}")
    public ApiResponse<Void> deleteCinema(@PathVariable Long id) {
        adminService.deleteCinema(id);
        return ApiResponse.success("影院删除成功", null);
    }

    // ==================== 放映厅管理 ==================
    /**
     * 查询所有放映厅
     */
    @Operation(summary = "查询所有放映厅")
    @GetMapping("/halls")
    public ApiResponse<List<Hall>> getAllHalls() {
        return ApiResponse.success(adminService.getAllHalls());
    }

    //====================================待定

    // ==================== 电影管理 ====================

    /**
     * 查询所有电影
     */
    @Operation(summary = "查询所有电影（管理员）")
    @GetMapping("/movies")
    public ApiResponse<List<MovieResponse>> getAllMovies() {
        // 调用 AdminService 获取所有电影并转换为 MovieResponse 列表
        List<MovieResponse> responses = adminService.getAllMovies();
        return ApiResponse.success(responses);
    }

    /**
     * 根据 ID 查询电影
     */
    @Operation(summary = "根据 ID 查询电影")
    @GetMapping("/movies/{id}")
    public ApiResponse<MovieResponse> getMovieById(@PathVariable Long id) {
        return ApiResponse.success(adminService.getMovieById(id));
    }

    /**
     * 新增电影
     */
    @Operation(summary = "新增电影")
    @PostMapping("/movies")
    public ApiResponse<MovieResponse> addMovie(@RequestBody Movie movie) {
        return ApiResponse.success("电影添加成功", adminService.addMovie(movie));
    }

    /**
     * 更新电影信息
     */
    @Operation(summary = "更新电影信息")
    @PutMapping("/movies/{id}")
    public ApiResponse<MovieResponse> updateMovie(@PathVariable Long id, @RequestBody Movie movie) {
        return ApiResponse.success("电影更新成功", adminService.updateMovie(id, movie));
    }

    /**
     * 删除电影
     */
    @Operation(summary = "删除电影")
    @DeleteMapping("/movies/{id}")
    public ApiResponse<Void> deleteMovie(@PathVariable Long id) {
        adminService.deleteMovie(id);
        return ApiResponse.success("电影删除成功", null);
    }

    /**
     * 更新电影状态
     */
    @Operation(summary = "更新电影状态")
    @PutMapping("/movies/{id}/status")
    public ApiResponse<Void> updateMovieStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        adminService.updateMovieStatus(id, params.get("status"));
        return ApiResponse.success("电影状态更新成功", null);
    }

    // ==================== 场次管理 ====================

    /**
     * 查询场次列表（支持多条件筛选）
     */
    @Operation(summary = "查询场次列表（支持筛选）")
    @GetMapping("/schedules")
    public ApiResponse<List<Map<String, Object>>> getAllSchedules(
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.getAllSchedules(cinemaId, status, keyword));
    }

    /**
     * 根据 ID 查询场次详情
     */
    @Operation(summary = "根据 ID 查询场次详情")
    @GetMapping("/schedules/{id}")
    public ApiResponse<Map<String, Object>> getScheduleById(@PathVariable Long id) {
        return ApiResponse.success(adminService.getScheduleById(id));
    }

    /**
     * 新增场次
     */
    @Operation(summary = "新增场次")
    @PostMapping("/schedules")
    public ApiResponse<Schedule> addSchedule(@RequestBody Schedule schedule) {
        return ApiResponse.success("场次添加成功", adminService.addSchedule(schedule));
    }

    /**
     * 更新场次信息
     */
    @Operation(summary = "更新场次信息")
    @PutMapping("/schedules/{id}")
    public ApiResponse<Schedule> updateSchedule(@PathVariable Long id, @RequestBody Schedule schedule) {
        return ApiResponse.success("场次更新成功", adminService.updateSchedule(id, schedule));
    }

    /**
     * 删除场次
     */
    @Operation(summary = "删除场次")
    @DeleteMapping("/schedules/{id}")
    public ApiResponse<Void> deleteSchedule(@PathVariable Long id) {
        adminService.deleteSchedule(id);
        return ApiResponse.success("场次删除成功", null);
    }

    /**
     * 查询指定影厅在某日的已占用场次时段
     */
    @Operation(summary = "查询影厅某日已占用时段")
    @GetMapping("/schedule/occupied")
    public ApiResponse<List<Map<String, Object>>> getOccupiedSlots(
            @RequestParam Long hallId,
            @RequestParam String date) {
        LocalDate showDate = LocalDate.parse(date);
        return ApiResponse.success(adminService.getOccupiedSlots(hallId, showDate));
    }

    /**
     * 检查场次时间冲突
     *
     * 【参数处理】
     * 前端可能以不同类型（Integer/Long/String）传递 hallId 和 duration，
     * 这里统一转换为强类型后再委托 Service 处理。
     */
    @Operation(summary = "检查场次时间冲突")
    @PostMapping("/schedule/conflict-check")
    public ApiResponse<Map<String, Object>> checkConflict(@RequestBody Map<String, Object> params) {
        Long hallId = parseLong(params.get("hallId"));
        String date = (String) params.get("date");
        String startTime = (String) params.get("startTime");
        Integer duration = params.get("duration") instanceof Integer
                ? (Integer) params.get("duration")
                : Integer.parseInt(String.valueOf(params.get("duration")));

        if (hallId == null || date == null || startTime == null || duration == null) {
            return ApiResponse.error(400, "参数不全");
        }

        LocalDate showDate = LocalDate.parse(date);
        Map<String, Object> result = adminService.checkConflict(hallId, showDate, startTime, duration);

        if ((Boolean) result.get("conflict")) {
            return ApiResponse.success("该影厅此时段已有场次，请重选", result);
        } else {
            return ApiResponse.success("无冲突", result);
        }
    }

    // ==================== 公告管理 ====================

    /**
     * 获取最新公告（公开接口，无需登录）
     */
    @Operation(summary = "获取最新公告（公开）")
    @GetMapping("/announcements/latest")
    public ApiResponse<Announcement> getLatestAnnouncement() {
        return ApiResponse.success(adminService.getLatestAnnouncement());
    }

    /**
     * 查询所有公告
     */
    @Operation(summary = "查询所有公告")
    @GetMapping("/announcements")
    public ApiResponse<List<Announcement>> getAllAnnouncements() {
        return ApiResponse.success(adminService.getAllAnnouncements());
    }

    /**
     * 根据 ID 查询公告
     */
    @Operation(summary = "根据 ID 查询公告")
    @GetMapping("/announcements/{id}")
    public ApiResponse<Announcement> getAnnouncementById(@PathVariable Long id) {
        return ApiResponse.success(adminService.getAnnouncementById(id));
    }

    /**
     * 新增公告
     */
    @Operation(summary = "新增公告")
    @PostMapping("/announcements")
    public ApiResponse<Announcement> addAnnouncement(@RequestBody Announcement announcement) {
        return ApiResponse.success("公告发布成功", adminService.addAnnouncement(announcement));
    }

    /**
     * 更新公告
     */
    @Operation(summary = "更新公告")
    @PutMapping("/announcements/{id}")
    public ApiResponse<Announcement> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        return ApiResponse.success("公告更新成功", adminService.updateAnnouncement(id, announcement));
    }

    /**
     * 删除公告
     */
    @Operation(summary = "删除公告")
    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        adminService.deleteAnnouncement(id);
        return ApiResponse.success("公告删除成功", null);
    }

    /**
     * 更新公告状态
     */
    @Operation(summary = "更新公告状态")
    @PutMapping("/announcements/{id}/status")
    public ApiResponse<Void> updateAnnouncementStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        adminService.updateAnnouncementStatus(id, params.get("status"));
        return ApiResponse.success("公告状态更新成功", null);
    }

    // ==================== 用户管理 ====================

    /**
     * 查询用户列表（支持按角色筛选）
     */
    @Operation(summary = "查询用户列表（支持角色筛选）")
    @GetMapping("/users")
    public ApiResponse<List<User>> getAllUsers(@RequestParam(required = false) String role) {
        return ApiResponse.success(adminService.getAllUsers(role));
    }

    /**
     * 根据 ID 查询用户（密码已脱敏）
     */
    @Operation(summary = "根据 ID 查询用户")
    @GetMapping("/users/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        return ApiResponse.success(adminService.getUserById(id));
    }

    /**
     * 新增用户
     */
    @Operation(summary = "新增用户")
    @PostMapping("/users")
    public ApiResponse<User> addUser(@RequestBody User user) {
        return ApiResponse.success("用户添加成功", adminService.addUser(user));
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息")
    @PutMapping("/users/{id}")
    public ApiResponse<Void> updateUser(@PathVariable Long id, @RequestBody User user) {
        adminService.updateUser(id, user);
        return ApiResponse.success("用户更新成功", null);
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户")
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ApiResponse.success("用户删除成功", null);
    }

    /**
     * 更新用户状态
     */
    @Operation(summary = "更新用户状态")
    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        adminService.updateUserStatus(id, params.get("status"));
        return ApiResponse.success("用户状态更新成功", null);
    }

    /**
     * 重置用户密码
     */
    @Operation(summary = "重置用户密码")
    @PutMapping("/users/{id}/password")
    public ApiResponse<Void> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, String> params) {
        adminService.resetUserPassword(id, params.get("password"));
        return ApiResponse.success("密码重置成功", null);
    }

    // ==================== 订单管理 ====================

    /**
     * 查询订单列表（支持按订单号搜索或按状态筛选）
     */
    @Operation(summary = "查询订单列表（支持搜索筛选）")
    @GetMapping("/orders")
    public ApiResponse<List<Order>> getAllOrders(
        // 分页参数
        //@RequestParam的作用 required = false 表示可选参数，不填时默认值为 null，不报错        
            @RequestParam(required = false) String q,
        // 状态筛选参数
            @RequestParam(required = false) String status) {
        return ApiResponse.success(adminService.getAllOrders(q, status));
    }

    /**
     * 根据 ID 查询订单
     */
    @Operation(summary = "根据 ID 查询订单")
    @GetMapping("/orders/{id}")
    public ApiResponse<Order> getOrderById(@PathVariable Long id) {
        return ApiResponse.success(adminService.getOrderById(id));
    }

    /**
     * 删除订单
     */
    @Operation(summary = "删除订单")
    @DeleteMapping("/orders/{id}")
    public ApiResponse<Void> deleteOrder(@PathVariable Long id) {
        adminService.deleteOrder(id);
        return ApiResponse.success("订单删除成功", null);
    }

    /**
     * 订单退款
     */
    @Operation(summary = "订单退款")
    @PutMapping("/orders/{id}/refund")
    public ApiResponse<Void> refundOrder(@PathVariable Long id) {
        adminService.refundOrder(id);
        return ApiResponse.success("退款成功", null);
    }

    /**
     * 订单取消
     */
    @Operation(summary = "订单取消")
    @PutMapping("/orders/{id}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable Long id) {
        //adminService.cancelOrder(id);
        return ApiResponse.success("订单取消成功", null);
    }

    /**
     * 订单确认
     */
    @Operation(summary = "订单确认")
    @PutMapping("/orders/{id}/confirm")
    public ApiResponse<Void> confirmOrder(@PathVariable Long id) {
        //adminService.confirmOrder(id);
        return ApiResponse.success("订单确认成功", null);
    }

    /**
     * 座位管理
     */
    @Operation(summary = "查询所有座位")
    @GetMapping("/seats")
    public ApiResponse<List<Seat>> getAllSeats() {
        return ApiResponse.success(adminService.getAllSeats());
    }

    // ==================== 私有工具方法 ====================

    /**
     * 将 Object 安全转换为 Long
     * 前端传参类型不确定，统一在此转换
     */
    private Long parseLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof String) return Long.parseLong((String) obj);
        if (obj instanceof Number) return ((Number) obj).longValue();
        return null;
    }
}
