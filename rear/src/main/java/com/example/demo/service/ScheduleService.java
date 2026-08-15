package com.example.demo.service;

import com.example.demo.entity.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ScheduleService {

    Map<String, Object> getScheduleById(Long id);
    List<Map<String, Object>> getSchedulesByMovieId(Long movieId);
    List<Map<String, Object>> getSchedulesByCinemaId(Long cinemaId);
    List<Map<String, Object>> getSchedulesByHallId(Long hallId);
    List<Map<String, Object>> getAllSchedules(Long cinemaId, String status, String keyword);
    Schedule addSchedule(Schedule schedule);
    Schedule updateSchedule(Long id, Schedule schedule);
    void deleteSchedule(Long id);
    //解释一下下面这三个方法的作用
    //1. getOccupiedSlots：获取指定厅在指定日期的已占用时间槽
    //2. checkConflict：检查指定时间槽是否与已占用时间槽冲突
    //3. clearScheduleCache：清除指定场次的缓存
    //缓存的作用是提高查询效率，避免重复查询数据库
    List<Map<String, Object>> getOccupiedSlots(Long hallId, LocalDate date);
    Map<String, Object> checkConflict(Long hallId, LocalDate date, String startTime, Integer duration);
    void clearScheduleCache(Long scheduleId);
}
