package com.example.demo.mapper;

import com.example.demo.entity.Movie;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MovieMapper {

    @Select("SELECT * FROM movie WHERE id = #{id}")
    Movie findById(@Param("id") Long id);

    @Select("SELECT * FROM movie")
    List<Movie> findAll();

    @Select("SELECT * FROM movie WHERE status = #{status} ORDER BY rating DESC, release_date DESC")
    List<Movie> findByStatusOrderByRatingAndDate(@Param("status") String status);

    @Select("SELECT DISTINCT m.* FROM movie m " +
            "JOIN schedule s ON m.id = s.movie_id " +
            "WHERE m.status = 'showing' AND s.status = 'available' AND s.show_time > NOW() " +
            "ORDER BY m.rating DESC")
    List<Movie> findShowingMoviesWithValidSchedule();

    @Select("SELECT * FROM movie WHERE status = #{status}")
    List<Movie> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM movie WHERE title LIKE CONCAT('%', #{title}, '%')")
    List<Movie> findByTitle(@Param("title") String title);

    @Insert("INSERT INTO movie(title, poster, description, genre, duration, rating, status, release_date, director, cast) " +
            "VALUES(#{title}, #{poster}, #{description}, #{genre}, #{duration}, #{rating}, #{status}, #{releaseDate}, #{director}, #{cast})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Movie movie);

    @Update("UPDATE movie SET title=#{title}, poster=#{poster}, description=#{description}, " +
            "genre=#{genre}, duration=#{duration}, rating=#{rating}, status=#{status}, " +
            "release_date=#{releaseDate}, director=#{director}, cast=#{cast} WHERE id=#{id}")
    int update(Movie movie);

    @Delete("DELETE FROM movie WHERE id=#{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE movie SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
