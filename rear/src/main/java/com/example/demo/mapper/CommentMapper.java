package com.example.demo.mapper;

import com.example.demo.entity.Comment;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CommentMapper {

    @Select("SELECT * FROM comment WHERE id = #{id}")
    Comment findById(@Param("id") Long id);

    @Select("SELECT * FROM comment WHERE movie_id = #{movieId}")
    List<Comment> findByMovieId(@Param("movieId") Long movieId);

    @Select("SELECT * FROM comment WHERE user_id = #{userId}")
    List<Comment> findByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO comment(user_id, movie_id, rating, content) " +
            "VALUES(#{userId}, #{movieId}, #{rating}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    @Update("UPDATE comment SET rating=#{rating}, content=#{content} WHERE id=#{id}")
    int update(Comment comment);

    @Delete("DELETE FROM comment WHERE id=#{id}")
    int deleteById(@Param("id") Long id);
}
