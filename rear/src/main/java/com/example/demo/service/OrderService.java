package com.example.demo.service;

import com.example.demo.dto.request.CreateOrderRequest;
import com.example.demo.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(Long id);

    List<OrderResponse> getOrdersByUserId(Long userId);

    OrderResponse payOrder(Long id);

    OrderResponse cancelOrder(Long id);

    OrderResponse refundOrder(Long id);
}