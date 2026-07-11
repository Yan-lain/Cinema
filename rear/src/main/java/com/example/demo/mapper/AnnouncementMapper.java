package com.example.demo.mapper;

import com.example.demo.entity.Announcement;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AnnouncementMapper {

    @Select("SELECT * FROM announcement WHERE id = #{id}")
    Announcement findById(@Param("id") Long id);

    @Select("SELECT * FROM announcement ORDER BY published_at DESC")
    List<Announcement> findAll();

    @Select("SELECT * FROM announcement WHERE status = #{status} ORDER BY published_at DESC")
    List<Announcement> findByStatus(@Param("status") String status);

    @Insert("INSERT INTO announcement(title, content, status, published_at) " +
            "VALUES(#{title}, #{content}, #{status}, #{publishedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Announcement announcement);

    @Update("UPDATE announcement SET title=#{title}, content=#{content}, status=#{status} WHERE id=#{id}")
    int update(Announcement announcement);

    @Delete("DELETE FROM announcement WHERE id=#{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE announcement SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
