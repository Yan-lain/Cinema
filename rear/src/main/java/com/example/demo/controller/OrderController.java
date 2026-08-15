package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.request.CreateOrderRequest;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订单管理", description = "用户下单、支付、取消、退票等接口")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "创建订单", description = "选座后创建订单，座位锁定 15 分钟，超时自动取消")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ApiResponse.success("订单创建成功", response);
    }

    @Operation(summary = "根据 ID 查询订单详情")
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id);
        return ApiResponse.success(order);
    }

    @Operation(summary = "查询用户的订单列表")
    @GetMapping("/user/{userId}")
    public ApiResponse<List<OrderResponse>> getOrdersByUserId(@PathVariable Long userId) {
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        return ApiResponse.success(orders);
    }

    @Operation(summary = "支付订单", description = "将待支付订单标记为已支付，座位状态变为已售出")
    @PutMapping("/{id}/pay")
    public ApiResponse<OrderResponse> payOrder(@PathVariable Long id, @RequestBody java.util.Map<String, Object> request) {
        OrderResponse response = orderService.payOrder(id);
        return ApiResponse.success("支付成功", response);
    }

    @Operation(summary = "取消订单", description = "取消未支付订单，释放已锁定的座位")
    @PutMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ApiResponse.success("订单已取消", response);
    }

    @Operation(summary = "退票", description = "对已支付订单申请退票，电影开场前 30 分钟内不可退票")
    @PutMapping("/{id}/refund")
    public ApiResponse<OrderResponse> refundOrder(@PathVariable Long id) {
        OrderResponse response = orderService.refundOrder(id);
        return ApiResponse.success("退票成功，款项将在3-5个工作日内原路返回", response);
    }
}