package com.example.demo.mapper;

import com.example.demo.entity.Seat;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SeatMapper {

    @Select("SELECT * FROM seat WHERE id = #{id}")
    Seat findById(@Param("id") Long id);

    @Select("SELECT * FROM seat")
    List<Seat> findAll();

    @Select("SELECT * FROM seat WHERE hall_id = #{hallId}")
    List<Seat> findByHallId(@Param("hallId") Long hallId);

    @Insert("INSERT INTO seat(hall_id, row_num, col_num, seat_number) " +
            "VALUES(#{hallId}, #{rowNum}, #{colNum}, #{seatNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Seat seat);

    @Update("UPDATE seat SET hall_id=#{hallId}, row_num=#{rowNum}, col_num=#{colNum}, " +
            "seat_number=#{seatNumber} WHERE id=#{id}")
    int update(Seat seat);

    @Delete("DELETE FROM seat WHERE id=#{id}")
    int deleteById(@Param("id") Long id);
    
}