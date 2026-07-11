package com.example.demo.controller;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderSeat;
import com.example.demo.entity.Schedule;
import com.example.demo.entity.Seat;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.OrderSeatMapper;
import com.example.demo.mapper.SeatMapper;
import com.example.demo.mapper.ScheduleMapper;
import com.example.demo.mapper.ScheduleSeatMapper;
import com.example.demo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderSeatMapper orderSeatMapper;

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private ScheduleSeatMapper scheduleSeatMapper;

    @Autowired
    private RedisService redisService;

    // Redis锁的过期时间（30秒）
    private static final long LOCK_TIMEOUT = 30;
    private static final TimeUnit LOCK_TIME_UNIT = TimeUnit.SECONDS;

    // 座位锁定的Redis key前缀（现在需要包含scheduleId）
    private static final String SEAT_LOCK_PREFIX = "seat:lock:";

    @PostMapping
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        String lockValue = UUID.randomUUID().toString();
        List<String> lockedSeatKeys = new java.util.ArrayList<>();

        try {
            Long userId = ((Number) request.get("userId")).longValue();
            Long scheduleId = ((Number) request.get("scheduleId")).longValue();
            BigDecimal totalPrice = BigDecimal.valueOf(((Number) request.get("totalPrice")).doubleValue());
            List<Map<String, Object>> seats = (List<Map<String, Object>>) request.get("seats");

            // 验证场次是否存在
            Schedule schedule = scheduleMapper.findById(scheduleId);
            if (schedule == null) {
                result.put("success", false);
                result.put("message", "场次不存在");
                return result;
            }

            Long hallId = schedule.getHallId();

            // ========== 使用Redis分布式锁防止座位超卖 ==========
            // 尝试获取所有选中座位的锁（锁的粒度精确到场次+座位）
            for (Map<String, Object> seat : seats) {
                int row = ((Number) seat.get("row")).intValue();
                int col = ((Number) seat.get("col")).intValue();
                String seatLockKey = SEAT_LOCK_PREFIX + scheduleId + ":" + hallId + ":" + row + ":" + col;

                // 尝试获取锁
                if (!redisService.tryLock(seatLockKey, lockValue, LOCK_TIMEOUT, LOCK_TIME_UNIT)) {
                    // 释放已获取的锁
                    releaseSeatLocks(lockedSeatKeys, lockValue);
                    result.put("success", false);
                    result.put("message", "座位正在被其他用户操作，请稍后重试");
                    return result;
                }
                lockedSeatKeys.add(seatLockKey);
            }

            // 验证座位是否可用（从schedule_seat表查询）
            for (Map<String, Object> seat : seats) {
                int row = ((Number) seat.get("row")).intValue();
                int col = ((Number) seat.get("col")).intValue();

                // 查找座位ID
                Long seatId = findSeatIdByRowCol(hallId, row, col);
                if (seatId == null) {
                    releaseSeatLocks(lockedSeatKeys, lockValue);
                    result.put("success", false);
                    result.put("message", "座位不存在");
                    return result;
                }

                // 从schedule_seat表查询该场次的座位状态
                List<Long> unavailableSeatIds = scheduleSeatMapper.findUnavailableSeatIdsByScheduleId(scheduleId);
                if (unavailableSeatIds.contains(seatId)) {
                    releaseSeatLocks(lockedSeatKeys, lockValue);
                    result.put("success", false);
                    result.put("message", "座位已被其他用户选择，请重新选择");
                    return result;
                }
            }

            // 生成订单编号
            String orderNumber = generateOrderNumber();

            // 创建订单
            Order order = new Order();
            order.setOrderNumber(orderNumber);
            order.setUserId(userId);
            order.setScheduleId(scheduleId);
            order.setTotalPrice(totalPrice);
            order.setStatus("pending");
            order.setPayStatus("unpaid");
            order.setRefundStatus("none");
            order.setCreatedAt(LocalDateTime.now());

            // 插入订单
            orderMapper.insert(order);

            // 锁定座位并创建订单座位关联
            for (Map<String, Object> seat : seats) {
                int row = ((Number) seat.get("row")).intValue();
                int col = ((Number) seat.get("col")).intValue();
                Long seatId = findSeatIdByRowCol(hallId, row, col);

                // 插入新的场次座位记录（按需生成）
                com.example.demo.entity.ScheduleSeat scheduleSeat = new com.example.demo.entity.ScheduleSeat();
                scheduleSeat.setScheduleId(scheduleId);
                scheduleSeat.setSeatId(seatId);
                scheduleSeat.setStatus("locked");
                scheduleSeat.setLockTime(LocalDateTime.now());
                scheduleSeat.setLockUserId(userId);
                scheduleSeatMapper.insert(scheduleSeat);

                // 创建订单座位关联记录
                OrderSeat orderSeat = new OrderSeat();
                orderSeat.setOrderId(order.getId());
                orderSeat.setSeatId(seatId);
                orderSeatMapper.insert(orderSeat);
            }

            // 订单创建成功后，释放Redis锁
            releaseSeatLocks(lockedSeatKeys, lockValue);

            result.put("success", true);
            result.put("message", "订单创建成功");
            result.put("data", order);

            // 启动超时任务（15分钟后自动取消订单）
            scheduleOrderTimeout(order.getId(), scheduleId);

        } catch (Exception e) {
            // 发生异常时释放所有已获取的锁
            releaseSeatLocks(lockedSeatKeys, lockValue);
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "创建订单失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 释放座位锁
     */
    private void releaseSeatLocks(List<String> lockKeys, String lockValue) {
        for (String key : lockKeys) {
            redisService.unlock(key, lockValue);
        }
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOrderById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Order order = orderMapper.findById(id);
            if (order != null) {
                result.put("success", true);
                result.put("data", order);
            } else {
                result.put("success", false);
                result.put("message", "订单不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取订单失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/user/{userId}")
    public Map<String, Object> getOrdersByUserId(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Order> orders = orderMapper.findByUserId(userId);
            result.put("success", true);
            result.put("data", orders);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取订单列表失败: " + e.getMessage());
        }
        return result;
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD" + timestamp + random;
    }

    private Long findSeatIdByRowCol(Long hallId, int row, int col) {
        List<Seat> seats = seatMapper.findByHallId(hallId);
        return seats.stream()
                .filter(s -> s.getRowNum() == row && s.getColNum() == col)
                .map(Seat::getId)
                .findFirst()
                .orElse(null);
    }

    @PutMapping("/{id}/pay")
    public Map<String, Object> payOrder(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            Order order = orderMapper.findById(id);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }

            if (!"pending".equals(order.getStatus()) || !"unpaid".equals(order.getPayStatus())) {
                result.put("success", false);
                result.put("message", "订单状态不允许支付");
                return result;
            }

            // 更新订单状态
            order.setStatus("completed");
            order.setPayStatus("paid");
            order.setPaidAt(LocalDateTime.now());
            orderMapper.update(order);

            // 更新schedule_seat表中的座位状态为sold
            List<OrderSeat> orderSeats = orderSeatMapper.findByOrderId(id);
            for (OrderSeat os : orderSeats) {
                scheduleSeatMapper.markAsSold(order.getScheduleId(), os.getSeatId());
            }

            result.put("success", true);
            result.put("message", "支付成功");
            result.put("data", order);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "支付失败: " + e.getMessage());
        }

        return result;
    }

    @PutMapping("/{id}/cancel")
    public Map<String, Object> cancelOrder(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Order order = orderMapper.findById(id);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }

            if ("completed".equals(order.getStatus()) && "paid".equals(order.getPayStatus())) {
                result.put("success", false);
                result.put("message", "已支付的订单需要申请退票");
                return result;
            }

            if ("cancelled".equals(order.getStatus())) {
                result.put("success", false);
                result.put("message", "订单已取消");
                return result;
            }

            // 更新订单状态
            order.setStatus("cancelled");
            orderMapper.update(order);

            // 释放schedule_seat表中的座位
            List<OrderSeat> orderSeats = orderSeatMapper.findByOrderId(id);
            for (OrderSeat os : orderSeats) {
                scheduleSeatMapper.releaseSeat(order.getScheduleId(), os.getSeatId());
            }

            result.put("success", true);
            result.put("message", "订单已取消");

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "取消订单失败: " + e.getMessage());
        }

        return result;
    }

    @PutMapping("/{id}/refund")
    public Map<String, Object> refundOrder(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Order order = orderMapper.findById(id);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }

            if (!"completed".equals(order.getStatus()) || !"paid".equals(order.getPayStatus())) {
                result.put("success", false);
                result.put("message", "订单状态不允许退票");
                return result;
            }

            if (!"none".equals(order.getRefundStatus())) {
                result.put("success", false);
                result.put("message", "订单已申请退票或已退票");
                return result;
            }

            // 检查场次信息
            Schedule schedule = scheduleMapper.findById(order.getScheduleId());
            if (schedule == null) {
                result.put("success", false);
                result.put("message", "场次信息不存在");
                return result;
            }
            
            LocalDateTime showTime = schedule.getShowTime();
            LocalDateTime now = LocalDateTime.now();
            
            // 检查场次是否已过期（已过期则不能退票）
            if (showTime.isBefore(now)) {
                result.put("success", false);
                result.put("message", "场次已过期，无法退票");
                return result;
            }
            
            // 检查距离开场是否不足半小时（不足半小时则不能退票）
            long minutesUntilShow = java.time.Duration.between(now, showTime).toMinutes();
            if (minutesUntilShow <= 30) {
                result.put("success", false);
                result.put("message", "距离电影开场不足半小时，无法退票");
                return result;
            }

            // 更新订单状态
            order.setRefundStatus("refunded");
            order.setRefundedAt(LocalDateTime.now());
            orderMapper.update(order);

            // 释放schedule_seat表中的座位
            List<OrderSeat> orderSeats = orderSeatMapper.findByOrderId(id);
            for (OrderSeat os : orderSeats) {
                scheduleSeatMapper.releaseSeat(order.getScheduleId(), os.getSeatId());
            }

            result.put("success", true);
            result.put("message", "退票成功，款项将在3-5个工作日内原路返回");
            result.put("data", order);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "退票失败: " + e.getMessage());
        }

        return result;
    }

    private void scheduleOrderTimeout(Long orderId, Long scheduleId) {
        new Thread(() -> {
            try {
                Thread.sleep(15 * 60 * 1000); // 15分钟
                Order order = orderMapper.findById(orderId);
                if (order != null && "unpaid".equals(order.getPayStatus())) {
                    // 取消订单
                    order.setStatus("cancelled");
                    orderMapper.update(order);

                    // 释放schedule_seat表中的座位
                    List<OrderSeat> orderSeats = orderSeatMapper.findByOrderId(orderId);
                    for (OrderSeat os : orderSeats) {
                        scheduleSeatMapper.releaseSeat(scheduleId, os.getSeatId());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}