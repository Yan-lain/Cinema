package com.example.demo.mapper;

import com.example.demo.entity.Order;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("SELECT * FROM orders WHERE id = #{id}")
    Order findById(@Param("id") Long id);

    @Select("SELECT * FROM orders")
    List<Order> findAll();

    @Select("SELECT * FROM orders WHERE user_id = #{userId}")
    List<Order> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM orders WHERE order_number = #{orderNumber}")
    Order findByOrderNumber(@Param("orderNumber") String orderNumber);

    @Select("SELECT * FROM orders WHERE status = #{status}")
    List<Order> findByStatus(@Param("status") String status);

    @Insert("INSERT INTO orders(order_number, user_id, schedule_id, total_price, status, pay_status, refund_status) " +
            "VALUES(#{orderNumber}, #{userId}, #{scheduleId}, #{totalPrice}, #{status}, #{payStatus}, #{refundStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("UPDATE orders SET status=#{status}, pay_status=#{payStatus}, refund_status=#{refundStatus} WHERE id=#{id}")
    int update(Order order);

    @Delete("DELETE FROM orders WHERE id=#{id}")
    int deleteById(@Param("id") Long id);

    /**
     * 查询超时未支付的订单（创建时间超过15分钟且状态为待支付）
     */
    @Select("SELECT * FROM orders WHERE status = 'pending' AND pay_status = 'unpaid' AND created_at < DATE_SUB(NOW(), INTERVAL 15 MINUTE)")
    List<Order> findTimeoutUnpaidOrders();

    /**
     * 批量更新订单状态为已取消
     */
    @Update("UPDATE orders SET status = 'cancelled', pay_status = 'cancelled' WHERE status = 'pending' AND pay_status = 'unpaid' AND created_at < DATE_SUB(NOW(), INTERVAL 15 MINUTE)")
    int cancelTimeoutOrders();

    /**
     * 统计超时未支付的订单数量
     */
    @Select("SELECT COUNT(*) FROM orders WHERE status = 'pending' AND pay_status = 'unpaid' AND created_at < DATE_SUB(NOW(), INTERVAL 15 MINUTE)")
    int countTimeoutUnpaidOrders();

    /**
     * 统计已取消订单的数量（已取消超过24小时）
     */
    @Select("SELECT COUNT(*) FROM orders WHERE status = 'cancelled' AND created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR)")
    int countCancelledOrdersForCleanup();

    /**
     * 删除已取消超过24小时的订单
     */
    @Delete("DELETE FROM orders WHERE status = 'cancelled' AND created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR)")
    int deleteCancelledOrders();

    /**
     * 查询已取消超过24小时的订单（用于先删除关联的订单座位记录）
     */
    @Select("SELECT * FROM orders WHERE status = 'cancelled' AND created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR)")
    List<Order> findCancelledOrdersForCleanup();
}
