package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.entity.OrderSeat;
import com.example.demo.service.OrderSeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-seats")
public class OrderSeatController {

    @Autowired
    private OrderSeatService orderSeatService;

    // 获取所有订单座位
    @GetMapping
    public ApiResponse<List<OrderSeat>> getAllOrderSeats() {
        List<OrderSeat> seats = orderSeatService.getAllOrderSeats();
        return ApiResponse.success(seats);
    }

    // 根据订单ID获取订单座位
    @GetMapping("/order/{orderId}")
    public ApiResponse<List<OrderSeat>> getOrderSeatsByOrderId(@PathVariable Long orderId) {
        List<OrderSeat> seats = orderSeatService.getOrderSeatsByOrderId(orderId);
        return ApiResponse.success(seats);
    }
}
