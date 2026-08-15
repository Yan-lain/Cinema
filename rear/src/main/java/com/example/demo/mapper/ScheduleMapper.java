package com.example.demo.mapper;

import com.example.demo.entity.Schedule;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ScheduleMapper {

    @Select("SELECT * FROM schedule WHERE id = #{id}")
    Schedule findById(@Param("id") Long id);

    @Select("SELECT * FROM schedule")
    List<Schedule> findAll();

    @Select("SELECT * FROM schedule WHERE movie_id = #{movieId}")
    List<Schedule> findByMovieId(@Param("movieId") Long movieId);

    @Select("SELECT * FROM schedule WHERE hall_id = #{hallId}")
    List<Schedule> findByHallId(@Param("hallId") Long hallId);

    /**
     * 通过影院ID查询场次（通过hall表关联）
     */
    @Select("SELECT s.* FROM schedule s JOIN hall h ON s.hall_id = h.id WHERE h.cinema_id = #{cinemaId}")
    List<Schedule> findByCinemaId(@Param("cinemaId") Long cinemaId);

    /**
     * 通过影院ID和电影ID查询场次（通过hall表关联）
     */
    @Select("SELECT s.* FROM schedule s JOIN hall h ON s.hall_id = h.id WHERE h.cinema_id = #{cinemaId} AND s.movie_id = #{movieId}")
    List<Schedule> findByCinemaIdAndMovieId(@Param("cinemaId") Long cinemaId, @Param("movieId") Long movieId);

    @Select("SELECT * FROM schedule WHERE status = #{status}")
    List<Schedule> findByStatus(@Param("status") String status);

    /**
     * 通过影院ID和状态查询场次（通过hall表关联）
     */
    @Select("SELECT s.* FROM schedule s JOIN hall h ON s.hall_id = h.id WHERE h.cinema_id = #{cinemaId} AND s.status = #{status}")
    List<Schedule> findByCinemaIdAndStatus(@Param("cinemaId") Long cinemaId, @Param("status") String status);

    @Select("SELECT * FROM schedule WHERE hall_id = #{hallId} AND show_time < #{endTime} And end_time < #{showTime}")
    List<Schedule> findByHallIdAndDurationTime(@Param("hallId") Long hallId, @Param("showTime") java.time.LocalDateTime showTime, @Param("endTime") java.time.LocalDateTime endTime);

    @Select("SELECT * FROM schedule WHERE hall_id = #{hallId} AND show_time BETWEEN #{start} AND #{end}")
    List<Schedule> findByHallIdAndShowTimeBetween(@Param("hallId") Long hallId, @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Select("SELECT * FROM schedule WHERE show_time BETWEEN #{start} AND #{end}")
    List<Schedule> findByShowTimeBetween(@Param("start") String start, @Param("end") String end);

    @Insert("INSERT INTO schedule(movie_id, hall_id, show_time, end_time, price, status) " +
            "VALUES(#{movieId}, #{hallId}, #{showTime}, #{endTime}, #{price}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Schedule schedule);

    @Update("UPDATE schedule SET movie_id=#{movieId}, hall_id=#{hallId}, " +
            "show_time=#{showTime}, end_time=#{endTime}, price=#{price}, status=#{status} WHERE id=#{id}")
    int update(Schedule schedule);

    @Delete("DELETE FROM schedule WHERE id=#{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE schedule SET status = 'expired' WHERE status = 'available' AND show_time < NOW()")
    int updateExpiredSchedules();

    @Select("SELECT COUNT(*) FROM schedule WHERE status = 'available' AND show_time < NOW()")
    int countExpiredSchedules();

    @Select("SELECT id FROM schedule WHERE status = 'expired' AND show_time < DATE_SUB(NOW(), INTERVAL 3 DAY)")
    List<Long> findExpiredScheduleIdsForCleanup();

    @Delete("DELETE FROM schedule WHERE status = 'expired' AND show_time < DATE_SUB(NOW(), INTERVAL 3 DAY)")
    int deleteExpiredSchedules();
}
