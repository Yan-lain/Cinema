package com.example.demo.service;

import com.example.demo.entity.OrderSeat;

import java.util.List;

public interface OrderSeatService {
    List<OrderSeat> getAllOrderSeats();
    List<OrderSeat> getOrderSeatsByOrderId(Long orderId);
}
