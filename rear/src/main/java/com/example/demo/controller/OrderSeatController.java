package com.example.demo.controller;

import com.example.demo.entity.OrderSeat;
import com.example.demo.mapper.OrderSeatMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order-seats")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderSeatController {

    @Autowired
    private OrderSeatMapper orderSeatMapper;

    @GetMapping
    public Map<String, Object> getAllOrderSeats() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<OrderSeat> seats = orderSeatMapper.findAll();
            result.put("success", true);
            result.put("data", seats);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取订单座位失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/order/{orderId}")
    public Map<String, Object> getOrderSeatsByOrderId(@PathVariable Long orderId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<OrderSeat> seats = orderSeatMapper.findByOrderId(orderId);
            result.put("success", true);
            result.put("data", seats);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取订单座位失败: " + e.getMessage());
        }
        return result;
    }
}
