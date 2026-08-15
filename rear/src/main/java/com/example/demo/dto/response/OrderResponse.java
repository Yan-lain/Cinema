package com.example.demo.dto.response;

import com.example.demo.entity.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单信息响应")
public class OrderResponse {
    @Schema(description = "订单 ID")
    private Long id;
    @Schema(description = "订单编号")
    private String orderNumber;
    @Schema(description = "用户 ID")
    private Long userId;
    @Schema(description = "场次 ID")
    private Long scheduleId;
    @Schema(description = "订单总金额")
    private BigDecimal totalPrice;
    @Schema(description = "订单状态（pending/completed/cancelled）")
    private String status;
    @Schema(description = "支付状态（unpaid/paid）")
    private String payStatus;
    @Schema(description = "退票状态（none/refunded）")
    private String refundStatus;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "支付时间")
    private LocalDateTime paidAt;
    @Schema(description = "退票时间")
    private LocalDateTime refundedAt;

    public static OrderResponse fromEntity(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setScheduleId(order.getScheduleId());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus());
        response.setPayStatus(order.getPayStatus());
        response.setRefundStatus(order.getRefundStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setPaidAt(order.getPaidAt());
        response.setRefundedAt(order.getRefundedAt());
        return response;
    }

    public static List<OrderResponse> fromEntities(List<Order> orders) {
        return orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }
}