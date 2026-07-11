package com.example.demo.mapper;

import com.example.demo.entity.Hall;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface HallMapper {

    @Select("SELECT * FROM hall WHERE id = #{id}")
    Hall findById(@Param("id") Long id);

    @Select("SELECT * FROM hall")
    List<Hall> findAll();

    @Select("SELECT * FROM hall WHERE cinema_id = #{cinemaId} AND status = 'active'")
    List<Hall> findByCinemaId(@Param("cinemaId") Long cinemaId);

    @Select("SELECT * FROM hall WHERE hall_number = #{hallNumber}")
    Hall findByHallNumber(@Param("hallNumber") String hallNumber);

    @Insert("INSERT INTO hall(cinema_id, hall_number, hall.rows, cols, status) " +
            "VALUES(#{cinemaId}, #{hallNumber}, #{rows}, #{cols}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Hall hall);

    @Update("UPDATE hall SET cinema_id=#{cinemaId}, hall_number=#{hallNumber}, hall.rows=#{rows}, cols=#{cols}, " +
            "status=#{status} WHERE id=#{id}")
    int update(Hall hall);

    @Delete("DELETE FROM hall WHERE id=#{id}")
    int deleteById(@Param("id") Long id);
}
