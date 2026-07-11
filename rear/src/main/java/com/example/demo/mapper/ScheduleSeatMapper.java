package com.example.demo.mapper;

import com.example.demo.entity.ScheduleSeat;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScheduleSeatMapper {

    @Select("SELECT * FROM schedule_seat WHERE id = #{id}")
    ScheduleSeat findById(@Param("id") Long id);

    @Select("SELECT * FROM schedule_seat WHERE schedule_id = #{scheduleId}")
    List<ScheduleSeat> findByScheduleId(@Param("scheduleId") Long scheduleId);

    @Select("SELECT * FROM schedule_seat WHERE schedule_id = #{scheduleId} AND seat_id = #{seatId}")
    ScheduleSeat findByScheduleIdAndSeatId(@Param("scheduleId") Long scheduleId, @Param("seatId") Long seatId);

    @Select("SELECT seat_id FROM schedule_seat WHERE schedule_id = #{scheduleId} AND status = 'sold'")
    List<Long> findSoldSeatIdsByScheduleId(@Param("scheduleId") Long scheduleId);

    @Select("SELECT seat_id FROM schedule_seat WHERE schedule_id = #{scheduleId} AND (status = 'sold' OR status = 'locked')")
    List<Long> findUnavailableSeatIdsByScheduleId(@Param("scheduleId") Long scheduleId);

    @Insert("INSERT INTO schedule_seat (schedule_id, seat_id, status, lock_time, lock_user_id) " +
            "VALUES (#{scheduleId}, #{seatId}, #{status}, #{lockTime}, #{lockUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduleSeat scheduleSeat);

    @Update("UPDATE schedule_seat SET status = #{status}, lock_time = #{lockTime}, lock_user_id = #{lockUserId} " +
            "WHERE schedule_id = #{scheduleId} AND seat_id = #{seatId}")
    int updateStatus(ScheduleSeat scheduleSeat);

    @Delete("DELETE FROM schedule_seat WHERE schedule_id = #{scheduleId} AND seat_id = #{seatId}")
    int releaseSeat(@Param("scheduleId") Long scheduleId, @Param("seatId") Long seatId);

    @Update("UPDATE schedule_seat SET status = 'locked', lock_time = NOW(), lock_user_id = #{lockUserId} " +
            "WHERE schedule_id = #{scheduleId} AND seat_id = #{seatId} AND status = 'available'")
    int lockSeat(@Param("scheduleId") Long scheduleId, @Param("seatId") Long seatId, @Param("lockUserId") Long lockUserId);

    @Update("UPDATE schedule_seat SET status = 'sold' " +
            "WHERE schedule_id = #{scheduleId} AND seat_id = #{seatId}")
    int markAsSold(@Param("scheduleId") Long scheduleId, @Param("seatId") Long seatId);

    @Delete("DELETE FROM schedule_seat WHERE schedule_id = #{scheduleId}")
    int deleteByScheduleId(@Param("scheduleId") Long scheduleId);

    @Insert("INSERT IGNORE INTO schedule_seat (schedule_id, seat_id, status) " +
            "SELECT s.id, seat.id, 'available' FROM schedule s " +
            "CROSS JOIN seat ON s.hall_id = seat.hall_id " +
            "WHERE s.status = 'available'")
    int initScheduleSeatsForAllSchedules();

    @Insert("INSERT IGNORE INTO schedule_seat (schedule_id, seat_id, status) " +
            "SELECT #{scheduleId}, seat.id, 'available' FROM seat WHERE seat.hall_id = #{hallId}")
    int initScheduleSeatsForSchedule(@Param("scheduleId") Long scheduleId, @Param("hallId") Long hallId);
}