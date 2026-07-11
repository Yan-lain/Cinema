package com.example.demo.task;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderSeat;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.OrderSeatMapper;
import com.example.demo.mapper.ScheduleMapper;
import com.example.demo.mapper.ScheduleSeatMapper;
import com.example.demo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ScheduleTask {

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderSeatMapper orderSeatMapper;

    @Autowired
    private ScheduleSeatMapper scheduleSeatMapper;

    @Autowired
    private RedisService redisService;

    private static final String MOVIE_SHOWING_KEY = "movie:showing";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 定时检查过期场次并更新状态
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void checkAndUpdateExpiredSchedules() {
        try {
            // 先统计有多少过期场次
            int expiredCount = scheduleMapper.countExpiredSchedules();

            if (expiredCount > 0) {
                // 更新过期场次状态为 expired
                int updatedCount = scheduleMapper.updateExpiredSchedules();

                String logMessage = String.format("[%s] 检查到 %d 个过期场次，已更新 %d 个",
                        LocalDateTime.now().format(FORMATTER), expiredCount, updatedCount);
                System.out.println(logMessage);

                // 清除相关缓存，下次请求会重新获取数据
                redisService.delete(MOVIE_SHOWING_KEY);
            }

        } catch (Exception e) {
            String errorMessage = String.format("[%s] 定时任务执行失败: %s",
                    LocalDateTime.now().format(FORMATTER), e.getMessage());
            System.err.println(errorMessage);
            e.printStackTrace();
        }
    }

    /**
     * 每天凌晨2点执行一次，进行更全面的清理
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyScheduleCleanup() {
        try {
            int expiredCount = scheduleMapper.countExpiredSchedules();

            if (expiredCount > 0) {
                int updatedCount = scheduleMapper.updateExpiredSchedules();

                String logMessage = String.format("[%s] 每日清理任务 - 检查到 %d 个过期场次，已更新 %d 个",
                        LocalDateTime.now().format(FORMATTER), expiredCount, updatedCount);
                System.out.println(logMessage);
            }

            // 清除所有相关缓存
            redisService.delete(MOVIE_SHOWING_KEY);

        } catch (Exception e) {
            String errorMessage = String.format("[%s] 每日清理任务执行失败: %s",
                    LocalDateTime.now().format(FORMATTER), e.getMessage());
            System.err.println(errorMessage);
            e.printStackTrace();
        }
    }

    /**
     * 定时检查超时未支付的订单并取消
     * 每1分钟执行一次，检查超过15分钟未支付的订单
     */
    @Scheduled(fixedRate = 60000) // 1分钟 = 60000毫秒
    public void checkAndCancelTimeoutOrders() {
        try {
            // 统计超时未支付的订单数量
            int timeoutCount = orderMapper.countTimeoutUnpaidOrders();

            if (timeoutCount > 0) {
                // 获取所有超时未支付的订单
                List<Order> timeoutOrders = orderMapper.findTimeoutUnpaidOrders();

                // 逐个处理订单，释放座位资源
                for (Order order : timeoutOrders) {
                    // 获取订单关联的座位
                    List<OrderSeat> orderSeats = orderSeatMapper.findByOrderId(order.getId());

                    // 释放每个座位（更新schedule_seat表中的状态）
                    for (OrderSeat orderSeat : orderSeats) {
                        scheduleSeatMapper.releaseSeat(order.getScheduleId(), orderSeat.getSeatId());
                    }
                }

                // 批量更新订单状态为已取消
                int cancelledCount = orderMapper.cancelTimeoutOrders();

                String logMessage = String.format("[%s] 检查到 %d 个超时未支付订单，已取消 %d 个，释放对应座位资源",
                        LocalDateTime.now().format(FORMATTER), timeoutCount, cancelledCount);
                System.out.println(logMessage);
            }

        } catch (Exception e) {
            String errorMessage = String.format("[%s] 超时订单检查任务执行失败: %s",
                    LocalDateTime.now().format(FORMATTER), e.getMessage());
            System.err.println(errorMessage);
            e.printStackTrace();
        }
    }

    /**
     * 定时清理过期场次的座位记录
     * 每天凌晨3点执行一次，删除过期超过3天的场次座位记录
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredScheduleSeats() {
        try {
            // 获取所有过期超过3天的场次ID
            List<Long> expiredScheduleIds = scheduleMapper.findExpiredScheduleIdsForCleanup();

            if (!expiredScheduleIds.isEmpty()) {
                // 删除过期场次的座位记录
                int deletedSeatCount = 0;
                for (Long scheduleId : expiredScheduleIds) {
                    deletedSeatCount += scheduleSeatMapper.deleteByScheduleId(scheduleId);
                }

                // 删除过期场次记录（可选）
                int deletedScheduleCount = scheduleMapper.deleteExpiredSchedules();

                String logMessage = String.format("[%s] 清理过期场次座位 - 删除 %d 条座位记录，%d 场过期场次",
                        LocalDateTime.now().format(FORMATTER), deletedSeatCount, deletedScheduleCount);
                System.out.println(logMessage);
            }

        } catch (Exception e) {
            String errorMessage = String.format("[%s] 清理过期场次座位任务执行失败: %s",
                    LocalDateTime.now().format(FORMATTER), e.getMessage());
            System.err.println(errorMessage);
            e.printStackTrace();
        }
    }

    /**
     * 定时清理已取消的订单记录
     * 每小时执行一次，删除已取消超过24小时的订单及其关联记录
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void cleanupCancelledOrders() {
        try {
            // 统计已取消超过24小时的订单数量
            int cancelledCount = orderMapper.countCancelledOrdersForCleanup();

            if (cancelledCount > 0) {
                // 获取所有已取消超过24小时的订单
                List<Order> cancelledOrders = orderMapper.findCancelledOrdersForCleanup();

                // 先删除关联的订单座位记录
                for (Order order : cancelledOrders) {
                    orderSeatMapper.deleteByOrderId(order.getId());
                }

                // 批量删除已取消订单
                int deletedCount = orderMapper.deleteCancelledOrders();

                String logMessage = String.format("[%s] 清理已取消订单 - 删除 %d 个已取消超过24小时的订单及其关联记录",
                        LocalDateTime.now().format(FORMATTER), deletedCount);
                System.out.println(logMessage);
            }

        } catch (Exception e) {
            String errorMessage = String.format("[%s] 已取消订单清理任务执行失败: %s",
                    LocalDateTime.now().format(FORMATTER), e.getMessage());
            System.err.println(errorMessage);
            e.printStackTrace();
        }
    }
}