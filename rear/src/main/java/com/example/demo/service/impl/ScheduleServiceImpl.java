package com.example.demo.service.impl;

import com.example.demo.constant.RedisKey;
import com.example.demo.entity.Hall;
import com.example.demo.entity.Movie;
import com.example.demo.entity.Schedule;
import com.example.demo.mapper.HallMapper;
import com.example.demo.mapper.MovieMapper;
import com.example.demo.mapper.ScheduleMapper;
import com.example.demo.service.RedisService;
import com.example.demo.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 场次服务实现类
 * 
 * 【核心功能】
 * 1. 场次数据的CRUD操作
 * 2. Redis缓存管理（读缓存优先、写后删除）
 * 3. 场次冲突检测
 * 4. 影厅占用时间查询
 * 
 * 【缓存策略】
 * - 场次详情缓存：5分钟过期
 * - 电影场次列表缓存：10分钟过期
 * - 影院场次列表缓存：10分钟过期
 * - 影厅当日场次缓存：5分钟过期
 * - 影厅已占用时间段缓存：2小时过期
 * 
 * 【缓存Key设计】
 * - schedule:detail:{id}       - 场次详情
 * - schedule:movie:{movieId}   - 某电影的所有场次
 * - schedule:cinema:{cinemaId} - 某影院的所有场次
 * - schedule:hall:{hallId}     - 某影厅的所有场次
 * - schedule:occupied:{hallId}:{date} - 影厅当日已占用时间段
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private HallMapper hallMapper;

    @Autowired
    private RedisService redisService;

    
    /**
     * 获取场次详情
     * 
     * @param id 场次ID
     * @return 场次详情响应
     */
    @Override
    public Map<String, Object> getScheduleById(Long id) {
        String cacheKey = RedisKey.scheduleDetail(id);
        
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            return (Map<String, Object>) cached;
        }

        Schedule schedule = scheduleMapper.findById(id);
        if (schedule == null) {
            redisService.cacheNullValue(cacheKey);
            return null;
        }

        Map<String, Object> scheduleMap = buildScheduleMap(schedule);
        
        redisService.set(cacheKey, scheduleMap, 5, TimeUnit.MINUTES);
        return scheduleMap;
    }

    /**
     * 获取电影场次列表
     * 
     * @param movieId 电影ID
     * @return 电影场次列表响应
     */
    @Override
    public List<Map<String, Object>> getSchedulesByMovieId(Long movieId) {
        String cacheKey = RedisKey.scheduleMovie(movieId);
        
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            return (List<Map<String, Object>>) cached;
        }

        List<Schedule> schedules = scheduleMapper.findByMovieId(movieId);
        List<Map<String, Object>> result = schedules.stream()
                .filter(s -> "available".equals(s.getStatus()) && s.getShowTime().isAfter(LocalDateTime.now()))
                .map(this::buildScheduleMap)
                .collect(Collectors.toList());

        redisService.set(cacheKey, result, 10, TimeUnit.MINUTES);
        return result;
    }

    /**
     * 获取影院场次列表
     * 
     * @param cinemaId 影院ID
     * @return 影院场次列表响应
     */
    @Override
    public List<Map<String, Object>> getSchedulesByCinemaId(Long cinemaId) {
        String cacheKey = RedisKey.scheduleCinema(cinemaId);
        
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            return (List<Map<String, Object>>) cached;
        }

        List<Schedule> schedules = scheduleMapper.findByCinemaId(cinemaId);
        List<Map<String, Object>> result = schedules.stream()
                .filter(s -> "available".equals(s.getStatus()) && s.getShowTime().isAfter(LocalDateTime.now()))
                .map(this::buildScheduleMap)
                .collect(Collectors.toList());

        redisService.set(cacheKey, result, 10, TimeUnit.MINUTES);
        return result;
    }

    /**
     * 获取影厅当日场次
     * 
     * @param hallId 影厅ID
     * @return 影厅当日场次响应
     */
    @Override
    public List<Map<String, Object>> getSchedulesByHallId(Long hallId) {
        String cacheKey = RedisKey.scheduleHall(hallId);
        
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            return (List<Map<String, Object>>) cached;
        }

        List<Schedule> schedules = scheduleMapper.findByHallId(hallId);
        List<Map<String, Object>> result = schedules.stream()
                .filter(s -> "available".equals(s.getStatus()) && s.getShowTime().isAfter(LocalDateTime.now()))
                .map(this::buildScheduleMap)
                .collect(Collectors.toList());

        redisService.set(cacheKey, result, 5, TimeUnit.MINUTES);
        return result;
    }

    /**
     * 获取所有场次
     * 
     * @param cinemaId 影院ID
     * @param status 状态
     * @param keyword 搜索关键词
     * @return 所有场次响应
     */
    @Override
    public List<Map<String, Object>> getAllSchedules(Long cinemaId, String status, String keyword) {
        List<Schedule> schedules;
        if (cinemaId != null && status != null && !status.isEmpty()) {
            schedules = scheduleMapper.findByCinemaIdAndStatus(cinemaId, status);
        } else if (cinemaId != null) {
            schedules = scheduleMapper.findByCinemaId(cinemaId);
        } else if (status != null && !status.isEmpty()) {
            schedules = scheduleMapper.findByStatus(status);
        } else {
            schedules = scheduleMapper.findAll();
        }

        List<Map<String, Object>> scheduleList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Schedule schedule : schedules) {
            if ("available".equals(schedule.getStatus()) && schedule.getShowTime().isBefore(now)) {
                continue;
            }

            if (keyword != null && !keyword.isEmpty()) {
                Movie movie = movieMapper.findById(schedule.getMovieId());
                String movieTitle = movie != null ? movie.getTitle() : "";
                if (!movieTitle.toLowerCase().contains(keyword.toLowerCase())) {
                    continue;
                }
            }

            scheduleList.add(buildScheduleMap(schedule));
        }

        return scheduleList;
    }

    /**
     * 添加场次
     * 
     * @param schedule 场次实体
     * @return 添加后的场次实体
     */
    @Override
    public Schedule addSchedule(Schedule schedule) {
        if (schedule.getStatus() == null) {
            schedule.setStatus("available");
        }

        Movie movie = movieMapper.findById(schedule.getMovieId());
        if (movie != null) {
            int duration = movie.getDuration();
            int cycleMinutes = (int) Math.ceil((duration + 20) / 30.0) * 30;
            LocalDateTime endTime = schedule.getShowTime().plusMinutes(cycleMinutes);
            schedule.setEndTime(endTime);
        }

        scheduleMapper.insert(schedule);

        // 【缓存清除】添加场次后清除相关缓存，保证数据一致性
        clearScheduleCache(schedule.getId());

        redisService.delete(RedisKey.MOVIE_SHOWING);

        return schedule;
    }

    /**
     * 更新场次
     * 
     * @param id 场次ID
     * @param schedule 场次实体
     * @return 更新后的场次实体
     */
    @Override
    public Schedule updateSchedule(Long id, Schedule schedule) {
        Schedule existing = scheduleMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("场次不存在");
        }

        Movie movie = movieMapper.findById(schedule.getMovieId());
        if (movie != null) {
            int duration = movie.getDuration();
            int cycleMinutes = (int) Math.ceil((duration + 20) / 30.0) * 30;
            LocalDateTime endTime = schedule.getShowTime().plusMinutes(cycleMinutes);
            schedule.setEndTime(endTime);
        }

        schedule.setId(id);
        scheduleMapper.update(schedule);

        // 【缓存清除】更新场次后清除相关缓存
        clearScheduleCache(id);

        redisService.delete(RedisKey.MOVIE_SHOWING);

        return schedule;
    }

    /**
     * 删除场次
     * 
     * @param id 场次ID
     */
    @Override
    public void deleteSchedule(Long id) {
        Schedule schedule = scheduleMapper.findById(id);
        if (schedule == null) {
            throw new RuntimeException("场次不存在");
        }

        scheduleMapper.deleteById(id);

        // 【缓存清除】删除场次后清除相关缓存
        clearScheduleCache(id);

        redisService.delete(RedisKey.MOVIE_SHOWING);
    }

    /**
     * 获取影厅当日已占用时间段
     * 
     * @param hallId 影厅ID
     * @param date 日期
     * @return 影厅当日已占用时间段响应
     */
    @Override
    public List<Map<String, Object>> getOccupiedSlots(Long hallId, LocalDate date) {
        String cacheKey = RedisKey.scheduleOccupied(hallId, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            return (List<Map<String, Object>>) cached;
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Schedule> schedules = scheduleMapper.findByHallIdAndShowTimeBetween(hallId, startOfDay, endOfDay);

        List<Map<String, Object>> occupiedList = new ArrayList<>();
        for (Schedule schedule : schedules) {
            LocalDateTime showTime = schedule.getShowTime();
            LocalDateTime endTime = schedule.getEndTime();

            if (endTime == null) {
                Movie movie = movieMapper.findById(schedule.getMovieId());
                int duration = movie != null ? movie.getDuration() : 120;
                endTime = showTime.plusMinutes(duration + 20);
            }

            Map<String, Object> slot = new HashMap<>();
            slot.put("scheduleId", schedule.getId());
            slot.put("startTime", showTime.toLocalTime().toString());
            slot.put("endTime", endTime.toLocalTime().toString());

            occupiedList.add(slot);
        }

        occupiedList.sort((a, b) -> {
            String timeA = (String) a.get("startTime");
            String timeB = (String) b.get("startTime");
            return timeA.compareTo(timeB);
        });

        // 【写缓存】已占用时间段缓存2小时
        redisService.set(cacheKey, occupiedList, 2, TimeUnit.HOURS);
        return occupiedList;
    }

    /**
     * 检查场次冲突
     * 
     * @param hallId 影厅ID
     * @param date 日期
     * @param startTime 开始时间
     * @param duration 时长
     * @return 冲突检查结果
     */
    @Override
    public Map<String, Object> checkConflict(Long hallId, LocalDate date, String startTime, Integer duration) {
        LocalTime startTimeObj = LocalTime.parse(startTime);
        LocalDateTime showStartTime = LocalDateTime.of(date, startTimeObj);
        LocalDateTime showEndTime = showStartTime.plusMinutes(duration + 20);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<Schedule> schedules = scheduleMapper.findByHallIdAndShowTimeBetween(hallId, startOfDay, endOfDay);

        Map<String, Object> result = new HashMap<>();

        for (Schedule schedule : schedules) {
            LocalDateTime existingStartTime = schedule.getShowTime();
            LocalDateTime existingEndTime = schedule.getEndTime();

            if (existingEndTime == null) {
                Movie movie = movieMapper.findById(schedule.getMovieId());
                int existingDuration = movie != null ? movie.getDuration() : 120;
                existingEndTime = existingStartTime.plusMinutes(existingDuration + 20);
            }

            if (!(showEndTime.isBefore(existingStartTime) || showStartTime.isAfter(existingEndTime))) {
                result.put("conflict", true);
                return result;
            }
        }

        result.put("conflict", false);
        return result;
    }

    /**
     * 清除场次缓存
     * 
     * @param scheduleId 场次ID
     */
    @Override
    public void clearScheduleCache(Long scheduleId) {
        Schedule schedule = scheduleMapper.findById(scheduleId);
        if (schedule == null) {
            return;
        }

        redisService.delete(RedisKey.scheduleDetail(scheduleId));

        redisService.delete(RedisKey.scheduleMovie(schedule.getMovieId()));

        redisService.delete(RedisKey.scheduleHall(schedule.getHallId()));

        Hall hall = hallMapper.findById(schedule.getHallId());
        if (hall != null) {
            redisService.delete(RedisKey.scheduleCinema(hall.getCinemaId()));

            LocalDate date = schedule.getShowTime().toLocalDate();
            String occupiedKey = RedisKey.scheduleOccupied(hall.getId(), date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            redisService.delete(occupiedKey);
        }
    }

    /**
     * 构建场次响应数据（私有方法）
     * 将Schedule实体转换为包含电影和影厅信息的Map
     * 
     * @param schedule 场次实体
     * @return 包含场次、电影、影厅信息的Map
     */
    private Map<String, Object> buildScheduleMap(Schedule schedule) {
        Map<String, Object> scheduleMap = new HashMap<>();
        scheduleMap.put("id", schedule.getId());
        scheduleMap.put("movieId", schedule.getMovieId());
        scheduleMap.put("hallId", schedule.getHallId());
        scheduleMap.put("showTime", schedule.getShowTime().toString());
        scheduleMap.put("endTime", schedule.getEndTime() != null ? schedule.getEndTime().toString() : null);
        scheduleMap.put("price", schedule.getPrice());

        // 【动态状态判断】根据放映时间判断是否过期
        String displayStatus = schedule.getStatus();
        if ("available".equals(schedule.getStatus()) && schedule.getShowTime().isBefore(LocalDateTime.now())) {
            displayStatus = "expired";
        }
        scheduleMap.put("status", displayStatus);

        // 通过放映厅获取影院信息
        Hall hall = hallMapper.findById(schedule.getHallId());
        if (hall != null) {
            scheduleMap.put("hallNumber", hall.getHallNumber());
            scheduleMap.put("rows", hall.getRows());
            scheduleMap.put("cols", hall.getCols());
            scheduleMap.put("cinemaId", hall.getCinemaId());
        }

        // 添加电影信息
        Movie movie = movieMapper.findById(schedule.getMovieId());
        if (movie != null) {
            scheduleMap.put("movieTitle", movie.getTitle());
            scheduleMap.put("moviePoster", movie.getPoster());
        }

        return scheduleMap;
    }
}
