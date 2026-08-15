package com.example.demo.service.impl;

import com.example.demo.entity.OrderSeat;
import com.example.demo.mapper.OrderSeatMapper;
import com.example.demo.service.OrderSeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderSeatServiceImpl implements OrderSeatService {

    @Autowired
    private OrderSeatMapper orderSeatMapper;

    @Override
    public List<OrderSeat> getAllOrderSeats() {
        // 从数据库中查询所有订单座位
        return orderSeatMapper.findAll();
    }

    @Override
    public List<OrderSeat> getOrderSeatsByOrderId(Long orderId) {
        // 从数据库中查询指定订单的所有座位
        //生成订单时，会根据订单ID查询订单座位，用于显示订单详情时展示座位信息
        return orderSeatMapper.findByOrderId(orderId);
    }
}
