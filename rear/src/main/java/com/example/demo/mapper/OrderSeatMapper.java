package com.example.demo.mapper;

import com.example.demo.entity.OrderSeat;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrderSeatMapper {

    @Select("SELECT * FROM order_seat WHERE id = #{id}")
    OrderSeat findById(@Param("id") Long id);

    @Select("SELECT * FROM order_seat")
    List<OrderSeat> findAll();

    @Select("SELECT * FROM order_seat WHERE order_id = #{orderId}")
    List<OrderSeat> findByOrderId(@Param("orderId") Long orderId);

    @Insert("INSERT INTO order_seat(order_id, seat_id) VALUES(#{orderId}, #{seatId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderSeat orderSeat);

    @Delete("DELETE FROM order_seat WHERE order_id=#{orderId}")
    int deleteByOrderId(@Param("orderId") Long orderId);
}
