package com.example.demo.service.impl;

import com.example.demo.constant.Constants;
import com.example.demo.constant.ErrorCode;
import com.example.demo.dto.request.CreateOrderRequest;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.*;
import com.example.demo.service.OrderService;
import com.example.demo.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * 
 * 【架构说明】
 * 实现订单相关的业务逻辑，包括创建订单、支付、取消、退票等
 * 是影院管理系统的核心业务之一，涉及座位锁、事务管理等复杂逻辑
 * 
 * 【核心功能】
 * 1. 创建订单（createOrder）：包含分布式锁、座位验证、订单创建
 * 2. 获取订单详情（getOrderById）：根据ID查询订单
 * 3. 获取用户订单列表（getOrdersByUserId）：查询用户的所有订单
 * 4. 支付订单（payOrder）：支付订单并锁定座位
 * 5. 取消订单（cancelOrder）：取消未支付订单并释放座位
 * 6. 退票（refundOrder）：已支付订单退票，释放座位
 * 7. 超时订单自动取消（cancelTimeoutOrders）：@Scheduled 定时扫描
 * 
 * 【座位锁机制】
 * 使用Redis分布式锁防止座位超卖：
 * 1. 创建订单前，先为选中的每个座位获取锁
 * 2. 订单创建成功后释放锁
 * 3. 超时未支付订单由定时任务自动取消并释放座位
 * 
 * 【线程模型】
 * - 请求处理：Spring Boot 内置线程池（默认200线程）
 * - 超时扫描：@Scheduled 定时任务，固定频率60秒
 * - 异步任务：AsyncConfig 配置的专用线程池（core=5, max=20）
 * 
 * 【改进历史】
 * - 原 Thread.sleep() 方案已替换为 @Scheduled 定时扫描
 * - 分布式锁释放已使用 Lua 脚本保证原子性
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    /** 订单数据访问层 */
    @Autowired
    private OrderMapper orderMapper;

    /** 订单座位关系数据访问层 */
    @Autowired
    private OrderSeatMapper orderSeatMapper;

    /** 座位数据访问层 */
    @Autowired
    private SeatMapper seatMapper;

    /** 场次数据访问层 */
    @Autowired
    private ScheduleMapper scheduleMapper;

    /** 场次座位关系数据访问层 */
    @Autowired
    private ScheduleSeatMapper scheduleSeatMapper;

    /** Redis服务 */
    @Autowired
    private RedisService redisService;

    /** 分布式锁超时时间 */
    private static final long LOCK_TIMEOUT = 30;
    private static final TimeUnit LOCK_TIME_UNIT = TimeUnit.SECONDS;
    
    /** 座位锁Key前缀 */
    private static final String SEAT_LOCK_PREFIX = "seat:lock:";

    /**
     * 创建订单
     * 
     * 【业务流程】
     * 1. 验证场次是否存在
     * 2. 为每个选中的座位获取分布式锁
     * 3. 验证座位是否存在且可用
     * 4. 生成订单号并创建订单
     * 5. 锁定场次座位并关联订单座位
     * 6. 释放分布式锁
     * 7. 超时订单由 @Scheduled 定时任务统一扫描处理
     * 
     * @param request 创建订单请求（包含scheduleId、userId、seats、totalPrice）
     * @return 订单响应
     * //只读事务，确保订单创建过程中不修改数据
     * @Transactional(readOnly = true)
     * //事务隔离级别：READ_COMMITTED，确保订单创建过程中其他事务可以读取到已提交的数据
     * @Transactional(isolation = Isolation.READ_COMMITTED)
     * // 事务隔离级别，解决并发超卖、脏读问题
     * @Transactional(rollbackFor = Exception.class,isolation = Isolation.READ_COMMITTED)
     * // 事务传播行为：REQUIRED，确保订单创建过程中其他事务可以正常执行
     * @Transactional(propagation = Propagation.REQUIRED)
     * // 事务超时时间：30秒
     * @Transactional(timeout = 30)
     * // 事务回滚策略：任何异常都回滚 有全局异常捕获，所以这里需要手动抛出异常
     * @Transactional(rollbackFor = Exception.class)
     */
    @Override
    @Transactional(rollbackFor = Exception.class,isolation = Isolation.READ_COMMITTED)
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 生成唯一的锁值，用于释放锁时验证
        String lockValue = UUID.randomUUID().toString();
        // 记录已获取的锁Key，用于异常时释放
        List<String> lockedSeatKeys = new java.util.ArrayList<>();

        try {
            // 验证场次是否存在
            Schedule schedule = scheduleMapper.findById(request.getScheduleId());
            if (schedule == null) {
                throw new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND, "场次不存在");
            }

            Long hallId = schedule.getHallId();

            // 步骤1：为每个座位获取分布式锁
            for (CreateOrderRequest.SeatRequest seat : request.getSeats()) {
                String seatLockKey = SEAT_LOCK_PREFIX + request.getScheduleId() + ":" + hallId + ":" + seat.getRow() + ":" + seat.getCol();
                if (!redisService.tryLock(seatLockKey, lockValue, LOCK_TIMEOUT, LOCK_TIME_UNIT)) {
                    // 获取锁失败，释放已获取的锁并抛出异常
                    releaseSeatLocks(lockedSeatKeys, lockValue);
                    throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE, "座位正在被其他用户操作，请稍后重试");
                }
                lockedSeatKeys.add(seatLockKey);
            }

            // 步骤2：验证座位是否存在且可用
            for (CreateOrderRequest.SeatRequest seat : request.getSeats()) {
                Long seatId = findSeatIdByRowCol(hallId, seat.getRow(), seat.getCol());
                if (seatId == null) {
                    releaseSeatLocks(lockedSeatKeys, lockValue);
                    throw new BusinessException(ErrorCode.SEAT_NOT_FOUND, "座位不存在");
                }

                // 查询该场次中已被占用的座位
                List<Long> unavailableSeatIds = scheduleSeatMapper.findUnavailableSeatIdsByScheduleId(request.getScheduleId());
                if (unavailableSeatIds.contains(seatId)) {
                    releaseSeatLocks(lockedSeatKeys, lockValue);
                    throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE, "座位已被其他用户选择，请重新选择");
                }
            }

            // 步骤3：生成订单号并创建订单
            String orderNumber = generateOrderNumber();

            Order order = new Order();
            order.setOrderNumber(orderNumber);
            order.setUserId(request.getUserId());
            order.setScheduleId(request.getScheduleId());
            order.setTotalPrice(request.getTotalPrice());
            order.setStatus(Constants.ORDER_STATUS_PENDING);
            order.setPayStatus(Constants.PAY_STATUS_UNPAID);
            order.setRefundStatus(Constants.REFUND_STATUS_NONE);
            order.setCreatedAt(LocalDateTime.now());

            orderMapper.insert(order);

            // 步骤4：锁定场次座位并关联订单座位
            for (CreateOrderRequest.SeatRequest seat : request.getSeats()) {
                Long seatId = findSeatIdByRowCol(hallId, seat.getRow(), seat.getCol());

                // 创建场次座位锁定记录
                ScheduleSeat scheduleSeat = new ScheduleSeat();
                scheduleSeat.setScheduleId(request.getScheduleId());
                scheduleSeat.setSeatId(seatId);
                scheduleSeat.setStatus(Constants.SEAT_STATUS_LOCKED);
                scheduleSeat.setLockTime(LocalDateTime.now());
                scheduleSeat.setLockUserId(request.getUserId());
                scheduleSeatMapper.insert(scheduleSeat);

                // 创建订单座位关联记录（包含场次ID和价格快照）
                OrderSeat orderSeat = new OrderSeat();
                orderSeat.setOrderId(order.getId());
                orderSeat.setSeatId(seatId);
                orderSeat.setScheduleId(request.getScheduleId());
                orderSeat.setPrice(schedule.getPrice());
                orderSeatMapper.insert(orderSeat);
            }

            // 步骤5：释放分布式锁
            releaseSeatLocks(lockedSeatKeys, lockValue);

            // 订单超时处理由 @Scheduled 定时任务统一扫描，无需在此启动

            return OrderResponse.fromEntity(order);

        } catch (BusinessException e) {
            // 业务异常，释放已获取的锁
            releaseSeatLocks(lockedSeatKeys, lockValue);
            throw e;
        } catch (Exception e) {
            // 系统异常，释放已获取的锁
            releaseSeatLocks(lockedSeatKeys, lockValue);
            throw new BusinessException("创建订单失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取订单详情
     * 
     * @param id 订单ID
     * @return 订单响应
     */
    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        return OrderResponse.fromEntity(order);
    }

    /**
     * 获取用户的订单列表
     * 
     * @param userId 用户ID
     * @return 订单响应列表
     */
    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<Order> orders = orderMapper.findByUserId(userId);
        return orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 支付订单
     * 
     * @param id 订单ID
     * @return 支付后的订单响应
     */
    @Override
    // 事务隔离级别，解决并发超卖、脏读问题 怎么解决的 
    @Transactional(rollbackFor = Exception.class,isolation = Isolation.READ_COMMITTED)
    public OrderResponse payOrder(Long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // 验证订单状态是否允许支付
        if (!Constants.ORDER_STATUS_PENDING.equals(order.getStatus()) || !Constants.PAY_STATUS_UNPAID.equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
        }

        // 更新订单状态为已完成、已支付
        order.setStatus(Constants.ORDER_STATUS_COMPLETED);
        order.setPayStatus(Constants.PAY_STATUS_PAID);
        order.setPaidAt(LocalDateTime.now());
        orderMapper.update(order);

        // 将锁定的座位标记为已售出
        List<OrderSeat> orderSeats = orderSeatMapper.findByOrderId(id);
        for (OrderSeat os : orderSeats) {
            scheduleSeatMapper.markAsSold(order.getScheduleId(), os.getSeatId());
        }

        return OrderResponse.fromEntity(order);
    }

    /**
     * 取消订单
     * 
     * @param id 订单ID
     * @return 取消后的订单响应
     */
    @Override
    // 事务隔离级别，解决并发超卖、脏读问题 怎么解决的 
    @Transactional(rollbackFor = Exception.class,isolation = Isolation.READ_COMMITTED)
    public OrderResponse cancelOrder(Long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // 已支付的订单需要申请退票，不能直接取消
        if (Constants.ORDER_STATUS_COMPLETED.equals(order.getStatus()) && Constants.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "已支付的订单需要申请退票");
        }

        // 订单已取消，不能重复取消
        if (Constants.ORDER_STATUS_CANCELLED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单已取消");
        }

        // 更新订单状态为已取消
        order.setStatus(Constants.ORDER_STATUS_CANCELLED);
        orderMapper.update(order);

        // 释放锁定的座位
        List<OrderSeat> orderSeats = orderSeatMapper.findByOrderId(id);
        for (OrderSeat os : orderSeats) {
            scheduleSeatMapper.releaseSeat(order.getScheduleId(), os.getSeatId());
        }

        return OrderResponse.fromEntity(order);
    }

    /**
     * 退票（已支付订单）
     * 
     * @param id 订单ID
     * @return 退票后的订单响应
     */
    @Override
    // 事务隔离级别，解决并发超卖、脏读问题 怎么解决的 
    @Transactional(rollbackFor = Exception.class,isolation = Isolation.READ_COMMITTED)
    public OrderResponse refundOrder(Long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // 验证订单状态是否允许退票
        if (!Constants.ORDER_STATUS_COMPLETED.equals(order.getStatus()) || !Constants.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_REFUND_ERROR, "订单状态不允许退票");
        }

        // 订单已申请退票或已退票，不能重复申请
        if (!Constants.REFUND_STATUS_NONE.equals(order.getRefundStatus())) {
            throw new BusinessException(ErrorCode.ORDER_REFUND_ERROR, "订单已申请退票或已退票");
        }

        // 验证场次信息是否存在
        Schedule schedule = scheduleMapper.findById(order.getScheduleId());
        if (schedule == null) {
            throw new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND, "场次信息不存在");
        }

        // 验证电影是否已开场
        LocalDateTime showTime = schedule.getShowTime();
        LocalDateTime now = LocalDateTime.now();

        if (showTime.isBefore(now)) {
            throw new BusinessException(ErrorCode.ORDER_REFUND_ERROR, "场次已过期，无法退票");
        }

        // 验证距离开场是否超过半小时
        long minutesUntilShow = java.time.Duration.between(now, showTime).toMinutes();
        if (minutesUntilShow <= Constants.REFUND_MINUTES_BEFORE_SHOW) {
            throw new BusinessException(ErrorCode.ORDER_REFUND_ERROR, "距离电影开场不足半小时，无法退票");
        }

        // 更新订单退票状态
        order.setRefundStatus(Constants.REFUND_STATUS_REFUNDED);
        order.setRefundedAt(LocalDateTime.now());
        orderMapper.update(order);

        // 释放已售出的座位
        List<OrderSeat> orderSeats = orderSeatMapper.findByOrderId(id);
        for (OrderSeat os : orderSeats) {
            scheduleSeatMapper.releaseSeat(order.getScheduleId(), os.getSeatId());
        }

        return OrderResponse.fromEntity(order);
    }

    /**
     * 释放座位锁（私有方法）
     * 
     * @param lockKeys 已获取的锁Key列表
     * @param lockValue 锁值（用于验证锁的持有者）
     */
    private void releaseSeatLocks(List<String> lockKeys, String lockValue) {
        for (String key : lockKeys) {
            redisService.unlock(key, lockValue);
        }
    }

    /**
     * 生成订单号（私有方法）
     * 
     * 订单号格式：ORD + 时间戳（yyyyMMddHHmmss）+ 4位随机数
     * 
     * @return 订单号
     */
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD" + timestamp + random;
    }

    /**
     * 根据行号和列号查找座位ID（私有方法）
     * 
     * @param hallId 放映厅ID
     * @param row 行号
     * @param col 列号
     * @return 座位ID，如果不存在返回null
     */
    private Long findSeatIdByRowCol(Long hallId, int row, int col) {
        List<Seat> seats = seatMapper.findByHallId(hallId);
        return seats.stream()
                .filter(s -> s.getRowNum() == row && s.getColNum() == col)
                .map(Seat::getId)
                .findFirst()
                .orElse(null);
    }

    /**
     * 定时扫描超时未支付订单（每分钟执行一次）
     *
     * 【设计说明】
     * 替代原有的 Thread.sleep() 方案。Spring 定时任务由框架管理线程池，
     * 不会阻塞业务线程，也不会因应用重启丢失定时任务。
     *
     * 【执行流程】
     * 1. 查询所有超过超时时间且仍为待支付状态的订单
     * 2. 逐个释放已锁定的座位
     * 3. 将订单状态更新为已取消
     *
     * 【容错处理】
     * - 单个订单处理失败不影响其他订单
     * - 异常通过日志记录，便于排查
     */
    @Scheduled(fixedRate = 60000)
    public void cancelTimeoutOrders() {
        logger.debug("开始扫描超时未支付订单...");

        List<Order> timeoutOrders = orderMapper.findTimeoutUnpaidOrders();
        if (timeoutOrders.isEmpty()) {
            return;
        }

        logger.info("发现 {} 个超时未支付订单，开始处理", timeoutOrders.size());

        int cancelledCount = 0;
        for (Order order : timeoutOrders) {
            try {
                // 释放已锁定的座位
                List<OrderSeat> orderSeats = orderSeatMapper.findByOrderId(order.getId());
                for (OrderSeat os : orderSeats) {
                    scheduleSeatMapper.releaseSeat(order.getScheduleId(), os.getSeatId());
                }

                // 更新订单状态为已取消
                order.setStatus(Constants.ORDER_STATUS_CANCELLED);
                orderMapper.update(order);

                cancelledCount++;
                logger.info("订单 {} 超时已自动取消，释放 {} 个座位",
                        order.getOrderNumber(), orderSeats.size());

            } catch (Exception e) {
                logger.error("处理超时订单 {} 失败: {}", order.getOrderNumber(), e.getMessage(), e);
            }
        }

        if (cancelledCount > 0) {
            logger.info("超时订单处理完成，共取消 {} 个订单", cancelledCount);
        }
    }
}