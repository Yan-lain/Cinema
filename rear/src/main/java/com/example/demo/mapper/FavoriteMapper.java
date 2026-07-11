package com.example.demo.mapper;

import com.example.demo.entity.Favorite;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface FavoriteMapper {

    @Insert("INSERT INTO favorite (user_id, movie_id) VALUES (#{userId}, #{movieId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Favorite favorite);

    @Delete("DELETE FROM favorite WHERE user_id = #{userId} AND movie_id = #{movieId}")
    int deleteByUserAndMovie(@Param("userId") Long userId, @Param("movieId") Long movieId);

    @Delete("DELETE FROM favorite WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    @Select("SELECT * FROM favorite WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Favorite> findByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT f.id, f.user_id, f.movie_id, f.created_at, " +
            "m.title, m.poster, m.rating, m.genre, m.duration, m.description, " +
            "u.username, u.nickname " +
            "FROM favorite f " +
            "LEFT JOIN movie m ON f.movie_id = m.id " +
            "LEFT JOIN `user` u ON f.user_id = u.id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.created_at DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> findWithUserAndMovie(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT * FROM favorite WHERE user_id = #{userId} AND movie_id = #{movieId}")
    Favorite findByUserAndMovie(@Param("userId") Long userId, @Param("movieId") Long movieId);
}