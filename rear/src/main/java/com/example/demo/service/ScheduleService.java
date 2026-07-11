package com.example.demo.service;

import com.example.demo.entity.Cinema;
import com.example.demo.entity.Hall;
import com.example.demo.entity.Movie;
import com.example.demo.entity.Schedule;
import com.example.demo.mapper.CinemaMapper;
import com.example.demo.mapper.HallMapper;
import com.example.demo.mapper.MovieMapper;
import com.example.demo.mapper.ScheduleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleService {

    @Autowired
    private CinemaMapper cinemaMapper;

    @Autowired
    private HallMapper hallMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    // 场间间隔时间（分钟）
    private static final int INTERVAL_MINUTES = 20;

    // 厅之间错开时间（分钟）
    private static final int HALL_OFFSET_MINUTES = 15;

    /**
     * 为指定影院的所有厅自动排片
     *
     * @param cinemaId 影院ID
     * @param movieId  电影ID
     * @param price    票价
     * @param date     排片日期（如：2026-05-15）
     * @return 排片结果
     */
    @Transactional
    public Map<String, Object> autoSchedule(Long cinemaId, Long movieId, BigDecimal price, String date) {
        try {
            // 获取影院信息
            Cinema cinema = cinemaMapper.findById(cinemaId);
            if (cinema == null) {
                return Map.of("success", false, "message", "影院不存在");
            }

            // 获取电影信息
            Movie movie = movieMapper.findById(movieId);
            if (movie == null) {
                return Map.of("success", false, "message", "电影不存在");
            }

            // 获取影院的所有放映厅
            List<Hall> halls = hallMapper.findByCinemaId(cinemaId);
            if (halls.isEmpty()) {
                return Map.of("success", false, "message", "该影院没有可用的放映厅");
            }

            // 解析营业时间
            String businessHours = cinema.getBusinessHours();
            if (businessHours == null || businessHours.trim().isEmpty()) {
                return Map.of("success", false, "message", "影院未设置营业时间");
            }

            // 解析日期
            LocalDate scheduleDate;
            try {
                scheduleDate = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                return Map.of("success", false, "message", "日期格式错误，请使用 yyyy-MM-dd 格式");
            }

            // 解析营业时间
            String[] hours = businessHours.split("-");
            if (hours.length != 2) {
                return Map.of("success", false, "message", "营业时间格式错误，应为 09:00-22:00");
            }

            LocalTime openTime = LocalTime.parse(hours[0].trim());
            LocalTime closeTime = LocalTime.parse(hours[1].trim());

            // 计算排片周期（影片时长 + 场间间隔，向上取整到小时）
            int movieDuration = movie.getDuration(); // 分钟
            int cycleMinutes = movieDuration + INTERVAL_MINUTES;
            // 向上取整到30分钟的倍数，确保排片周期是30分钟的倍数
            int cycleHours = (int) Math.ceil(cycleMinutes / 30.0)*30;

            // 计算每个厅的排片
            int createdCount = 0;

            for (int i = 0; i < halls.size(); i++) {
                Hall hall = halls.get(i);
                
                // 计算该厅的起始时间（错开15分钟）
                LocalTime hallStartTime = openTime.plusMinutes(i * HALL_OFFSET_MINUTES);
                
                // 如果起始时间超过营业时间，跳过
                if (hallStartTime.isAfter(closeTime)) {
                    continue;
                }

                // 生成该厅的所有排片时间
                LocalDateTime currentTime = LocalDateTime.of(scheduleDate, hallStartTime);
                
                while (true) {
                    // 计算 电影结束时间=当前时间+影片时长+场间间隔时间
                    LocalDateTime endTime = currentTime.plusMinutes(movieDuration+INTERVAL_MINUTES);
                    
                    // 检查最后一场是否在闭店前结束
                    LocalTime endTimeOfDay = endTime.toLocalTime();
                    if (endTimeOfDay.isAfter(closeTime)) {
                        break;
                    }

                    // 检查是否已存在相同场次
                    List<Schedule> existing = scheduleMapper.findByHallIdAndDurationTime(hall.getId(), currentTime, endTime);
                    if (existing.isEmpty()) {
                        // 创建排片记录
                        Schedule schedule = new Schedule();
                        schedule.setCinemaId(cinemaId);
                        schedule.setHallId(hall.getId());
                        schedule.setMovieId(movieId);
                        // 排片开始时间=当前时间
                        schedule.setShowTime(currentTime);
                        // 排片结束时间=当前时间+影片时长+场间间隔时间
                        schedule.setEndTime(endTime);                        
                        schedule.setPrice(price);
                        schedule.setStatus("available");
                        
                        scheduleMapper.insert(schedule);
                        createdCount++;
                    }

                    // 下一场排片时间=当前时间+排片周期时间

                    currentTime = currentTime.plusHours(cycleHours);
                }
            }

            return Map.of(
                    "success", true,
                    "message", String.format("成功为影院「%s」创建 %d 场排片", cinema.getName(), createdCount),
                    "count", createdCount
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "排片失败：" + e.getMessage());
        }
    }

    /**
     * 为所有影院自动排片（指定电影和日期）
     */
    @Transactional
    public Map<String, Object> autoScheduleAllCinemas(Long movieId, BigDecimal price, String date) {
        List<Cinema> cinemas = cinemaMapper.findAll();
        int totalCreated = 0;
        List<String> messages = new ArrayList<>();

        for (Cinema cinema : cinemas) {
            if ("active".equals(cinema.getStatus())) {
                Map<String, Object> result = autoSchedule(cinema.getId(), movieId, price, date);
                if ((Boolean) result.get("success")) {
                    totalCreated += (Integer) result.get("count");
                    messages.add((String) result.get("message"));
                }
            }
        }

        return Map.of(
                "success", true,
                "message", String.format("共为 %d 家影院创建 %d 场排片", cinemas.size(), totalCreated),
                "details", messages
        );
    }

    /**
     * 获取指定影院的固定排片档时间（用于排片时选择）
     * 
     * @param cinemaId 影院ID
     * @param duration 影片时长（分钟），用于计算排片周期
     * @param date     排片日期（如：2026-05-15），为空则返回时间格式，否则返回完整日期时间格式
     * @return 各厅的排片档时间列表
     */
    public Map<String, Object> getScheduleSlots(Long cinemaId, Integer duration, String date) {
        try {
            Cinema cinema = cinemaMapper.findById(cinemaId);
            if (cinema == null) {
                return Map.of("success", false, "message", "影院不存在");
            }

            String businessHours = cinema.getBusinessHours();
            if (businessHours == null || businessHours.trim().isEmpty()) {
                return Map.of("success", false, "message", "影院未设置营业时间");
            }

            // 解析营业时间
            String[] hours = businessHours.split("-");
            if (hours.length != 2) {
                return Map.of("success", false, "message", "营业时间格式错误，应为 09:00-22:00");
            }

            LocalTime openTime = LocalTime.parse(hours[0].trim());
            LocalTime closeTime = LocalTime.parse(hours[1].trim());

            // 计算排片周期（影片时长 + 20分钟场间间隔，向上取整到小时）
            int cycleMinutes = duration + INTERVAL_MINUTES;
            int cycleHours = (int) Math.ceil(cycleMinutes / 30.0)*30;

            // 获取影院的所有放映厅
            List<Hall> halls = hallMapper.findByCinemaId(cinemaId);
            if (halls.isEmpty()) {
                return Map.of("success", false, "message", "该影院没有可用的放映厅");
            }

            // 解析日期，如果没有提供则使用今天
            LocalDate showDate = (date != null && !date.trim().isEmpty()) 
                    ? LocalDate.parse(date) 
                    : LocalDate.now();
            
            // 根据是否有日期决定返回格式
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            
            List<Map<String, Object>> hallSlots = new ArrayList<>();

            for (int i = 0; i < halls.size(); i++) {
                Hall hall = halls.get(i);
                
                // 计算该厅的起始时间（错开15分钟）
                LocalTime hallStartTime = openTime.plusMinutes(i * HALL_OFFSET_MINUTES);
                
                if (hallStartTime.isAfter(closeTime)) {
                    continue;
                }

                List<String> slots = new ArrayList<>();
                LocalTime currentTime = hallStartTime;

                while (true) {
                    // 计算电影结束时间
                    LocalTime endTime = currentTime.plusMinutes(duration);
                    
                    // 检查最后一场是否在闭店前结束
                    if (endTime.isAfter(closeTime)) {
                        break;
                    }

                    // 根据是否有日期决定返回格式
                    if (date != null && !date.trim().isEmpty()) {
                        LocalDateTime slotDateTime = LocalDateTime.of(showDate, currentTime);
                        slots.add(slotDateTime.format(dateTimeFormatter));
                    } else {
                        slots.add(currentTime.format(timeFormatter));
                    }
                    
                    currentTime = currentTime.plusHours(cycleHours);
                }

                hallSlots.add(Map.of(
                        "hallId", hall.getId(),
                        "hallNumber", hall.getHallNumber(),
                        "startTime", hallStartTime.format(timeFormatter),
                        "slots", slots
                ));
            }

            return Map.of(
                    "success", true,
                    "cinemaId", cinemaId,
                    "cinemaName", cinema.getName(),
                    "businessHours", businessHours,
                    "movieDuration", duration,
                    "cycleHours", cycleHours,
                    "intervalMinutes", INTERVAL_MINUTES,
                    "hallSlots", hallSlots
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "获取排片档失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有影院的排片档时间（批量）
     */
    public Map<String, Object> getAllScheduleSlots(Integer duration, String date) {
        List<Cinema> cinemas = cinemaMapper.findAllActive();
        List<Map<String, Object>> allSlots = new ArrayList<>();

        for (Cinema cinema : cinemas) {
            Map<String, Object> result = getScheduleSlots(cinema.getId(), duration, date);
            if ((Boolean) result.get("success")) {
                allSlots.add(result);
            }
        }

        return Map.of(
                "success", true,
                "count", allSlots.size(),
                "data", allSlots
        );
    }

    /**
     * 计算指定影院指定日期的所有排片时间（预览）
     */
    public Map<String, Object> previewSchedule(Long cinemaId, Long movieId, String date) {
        try {
            Cinema cinema = cinemaMapper.findById(cinemaId);
            if (cinema == null) {
                return Map.of("success", false, "message", "影院不存在");
            }

            Movie movie = movieMapper.findById(movieId);
            if (movie == null) {
                return Map.of("success", false, "message", "电影不存在");
            }

            List<Hall> halls = hallMapper.findByCinemaId(cinemaId);
            if (halls.isEmpty()) {
                return Map.of("success", false, "message", "该影院没有可用的放映厅");
            }

            String businessHours = cinema.getBusinessHours();
            if (businessHours == null || businessHours.trim().isEmpty()) {
                return Map.of("success", false, "message", "影院未设置营业时间");
            }

            LocalDate scheduleDate = LocalDate.parse(date);
            String[] hours = businessHours.split("-");
            LocalTime openTime = LocalTime.parse(hours[0].trim());
            LocalTime closeTime = LocalTime.parse(hours[1].trim());

            int movieDuration = movie.getDuration();
            int cycleMinutes = movieDuration + INTERVAL_MINUTES;
            int cycleHours = (int) Math.ceil(cycleMinutes / 30.0)*30;

            List<Map<String, Object>> schedulePreview = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            for (int i = 0; i < halls.size(); i++) {
                Hall hall = halls.get(i);
                LocalTime hallStartTime = openTime.plusMinutes(i * HALL_OFFSET_MINUTES);

                if (hallStartTime.isAfter(closeTime)) {
                    continue;
                }

                LocalDateTime currentTime = LocalDateTime.of(scheduleDate, hallStartTime);

                while (true) {
                    LocalDateTime endTime = currentTime.plusMinutes(movieDuration);
                    LocalTime endTimeOfDay = endTime.toLocalTime();

                    if (endTimeOfDay.isAfter(closeTime)) {
                        break;
                    }

                    schedulePreview.add(Map.of(
                            "hallNumber", hall.getHallNumber(),
                            "startTime", currentTime.format(formatter),
                            "endTime", endTime.format(formatter),
                            "duration", movieDuration
                    ));

                    currentTime = currentTime.plusHours(cycleHours);
                }
            }

            return Map.of(
                    "success", true,
                    "cinema", cinema.getName(),
                    "businessHours", businessHours,
                    "movie", movie.getTitle(),
                    "duration", movieDuration,
                    "cycleHours", cycleHours,
                    "scheduleCount", schedulePreview.size(),
                    "scheduleList", schedulePreview
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "预览失败：" + e.getMessage());
        }
    }
}
