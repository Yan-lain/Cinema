package com.example.demo.mapper;

import com.example.demo.entity.BrowseHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface BrowseHistoryMapper {

    @Insert("INSERT INTO browse_history (user_id, movie_id) VALUES (#{userId}, #{movieId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BrowseHistory browseHistory);

    // 删除用户浏览历史记录    
    @Delete("DELETE FROM browse_history WHERE user_id = #{userId} AND movie_id = #{movieId}")
    int deleteByUserAndMovie(@Param("userId") Long userId, @Param("movieId") Long movieId);

    @Select("SELECT * FROM browse_history WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<BrowseHistory> findByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM browse_history WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    @Delete("DELETE FROM browse_history WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    // 删除用户浏览历史记录    
    @Delete("DELETE FROM browse_history WHERE id = #{id}")
    int deleteById(Long id);//根据id删除记录 int类型返回值，表示删除的行数

    // 查询用户浏览历史记录    
    @Select("SELECT bh.id, bh.user_id, bh.movie_id, bh.created_at, " +
            "m.title, m.poster, m.rating, m.genre, m.duration, " +
            "u.username, u.nickname, u.phone " +
            "FROM browse_history bh " +
            "LEFT JOIN movie m ON bh.movie_id = m.id " +
            "LEFT JOIN `user` u ON bh.user_id = u.id " +
            "WHERE bh.user_id = #{userId} " +
            "ORDER BY bh.created_at DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> findWithUserAndMovie(@Param("userId") Long userId, @Param("limit") Integer limit);
}