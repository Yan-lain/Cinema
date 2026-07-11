package com.example.demo.mapper;

import com.example.demo.entity.Cinema;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CinemaMapper {

    @Select("SELECT * FROM cinemas WHERE id = #{id}")
    Cinema findById(@Param("id") Long id);

    @Select("SELECT * FROM cinemas")
    List<Cinema> findAll();

    @Select("SELECT * FROM cinemas WHERE status = 'active'")
    List<Cinema> findAllActive();

    @Select("SELECT * FROM cinemas WHERE city = #{city} AND status = 'active'")
    List<Cinema> findByCity(@Param("city") String city);

    @Select("SELECT * FROM cinemas WHERE name LIKE CONCAT('%', #{name}, '%') AND status = 'active'")
    List<Cinema> findByName(@Param("name") String name);

    @Insert("INSERT INTO cinemas(name, city, district, address, phone, business_hours, image, status) " +
            "VALUES(#{name}, #{city}, #{district}, #{address}, #{phone}, #{business_hours}, #{image}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Cinema cinema);

    @Update("UPDATE cinemas SET name=#{name}, city=#{city}, district=#{district}, address=#{address}, " +
            "phone=#{phone}, business_hours=#{businessHours}, image=#{image}, status=#{status} WHERE id=#{id}")
    int update(Cinema cinema);

    @Delete("DELETE FROM cinemas WHERE id=#{id}")
    int deleteById(@Param("id") Long id);
}