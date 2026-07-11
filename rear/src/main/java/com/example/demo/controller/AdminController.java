package com.example.demo.controller;

import com.example.demo.entity.Announcement;
import com.example.demo.entity.Cinema;
import com.example.demo.entity.Hall;
import com.example.demo.entity.Movie;
import com.example.demo.entity.Order;
import com.example.demo.entity.Schedule;
import com.example.demo.entity.User;
import com.example.demo.mapper.AnnouncementMapper;
import com.example.demo.mapper.CinemaMapper;
import com.example.demo.mapper.HallMapper;
import com.example.demo.mapper.MovieMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.ScheduleMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private HallMapper hallMapper;

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private CinemaMapper cinemaMapper;

    @Autowired
    private UserMapper userMapper;


    @Autowired
    private OrderMapper orderMapper;

    // ============ 影院管理 ============

    @GetMapping("/cinemas")
    public Map<String, Object> getAllCinemas() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Cinema> cinemas = cinemaMapper.findAllActive();
            result.put("success", true);
            result.put("data", cinemas);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取影院列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/cinemas/{id}")
    public Map<String, Object> getCinemaById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Cinema cinema = cinemaMapper.findById(id);
            if (cinema != null) {
                result.put("success", true);
                result.put("data", cinema);
            } else {
                result.put("success", false);
                result.put("message", "影院不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取影院信息失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/cinemas")
    public Map<String, Object> addCinema(@RequestBody Cinema cinema) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (cinema.getStatus() == null) {
                cinema.setStatus("active");
            }
            cinemaMapper.insert(cinema);
            result.put("success", true);
            result.put("message", "影院添加成功");
            result.put("data", cinema);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "添加影院失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/cinemas/{id}")
    public Map<String, Object> updateCinema(@PathVariable Long id, @RequestBody Cinema cinema) {
        System.out.println("SDadadadasada");
        Map<String, Object> result = new HashMap<>();
        try {
            Cinema existing = cinemaMapper.findById(id);
            if (existing == null) {
                result.put("success", false);
                result.put("message", "影院不存在");
                return result;
            }
            
            cinema.setId(id);
            cinemaMapper.update(cinema);
            result.put("success", true);
            result.put("message", "影院更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新影院失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/cinemas/{id}")
    public Map<String, Object> deleteCinema(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Cinema cinema = cinemaMapper.findById(id);
            if (cinema == null) {
                result.put("success", false);
                result.put("message", "影院不存在");
                return result;
            }
            
            cinemaMapper.deleteById(id);
            result.put("success", true);
            result.put("message", "影院删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "删除影院失败: " + e.getMessage());
        }
        return result;
    }

    // ============ 电影管理 ============

    @GetMapping("/movies")
    public Map<String, Object> getAllMovies() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Movie> movies = movieMapper.findAll();
            result.put("success", true);
            result.put("data", movies);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取电影列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/movies/{id}")
    public Map<String, Object> getMovieById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Movie movie = movieMapper.findById(id);
            if (movie != null) {
                result.put("success", true);
                result.put("data", movie);
            } else {
                result.put("success", false);
                result.put("message", "电影不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取电影信息失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/movies")
    public Map<String, Object> addMovie(@RequestBody Movie movie) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (movie.getStatus() == null) {
                movie.setStatus("showing");
            }
            movieMapper.insert(movie);
            
            // 更新缓存：删除相关缓存，下次请求会重新从数据库获取
            redisService.delete("movie:list");
            redisService.delete("movie:showing");
            
            result.put("success", true);
            result.put("message", "电影添加成功");
            result.put("data", movie);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "添加电影失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/movies/{id}")
    public Map<String, Object> updateMovie(@PathVariable Long id, @RequestBody Movie movie) {

        
        Map<String, Object> result = new HashMap<>();
        try {
            Movie existing = movieMapper.findById(id);
            if (existing == null) {
                result.put("success", false);
                result.put("message", "电影不存在");
                return result;
            }
            
            movie.setId(id);
            movieMapper.update(movie);
            
            // 更新缓存
            redisService.delete("movie:list");
            redisService.delete("movie:showing");
            redisService.delete("movie:detail:" + id);
            
            result.put("success", true);
            result.put("message", "电影更新成功");
            result.put("data", movie);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新电影失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/movies/{id}")
    public Map<String, Object> deleteMovie(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Movie movie = movieMapper.findById(id);
            if (movie == null) {
                result.put("success", false);
                result.put("message", "电影不存在");
                return result;
            }
            
            movieMapper.deleteById(id);
            
            // 更新缓存
            redisService.delete("movie:list");
            redisService.delete("movie:showing");
            redisService.delete("movie:detail:" + id);
            
            result.put("success", true);
            result.put("message", "电影删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "删除电影失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/movies/{id}/status")
    public Map<String, Object> updateMovieStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String status = params.get("status");
            movieMapper.updateStatus(id, status);
            
            // 更新缓存
            redisService.delete("movie:list");
            redisService.delete("movie:showing");
            redisService.delete("movie:detail:" + id);
            
            result.put("success", true);
            result.put("message", "电影状态更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新电影状态失败: " + e.getMessage());
        }
        return result;
    }

    // ============ 场次管理 ============

    @GetMapping("/schedules")
    public Map<String, Object> getAllSchedules(
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        try {
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
            
            List<Map<String, Object>> scheduleList = new java.util.ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            for (Schedule schedule : schedules) {
                // 过滤过期场次（即使状态为available，如果放映时间已过也视为过期）
                if ("available".equals(schedule.getStatus()) && schedule.getShowTime().isBefore(now)) {
                    continue;
                }
                
                // 关键词过滤
                if (keyword != null && !keyword.isEmpty()) {
                    Movie movie = movieMapper.findById(schedule.getMovieId());
                    String movieTitle = movie != null ? movie.getTitle() : "";
                    if (!movieTitle.toLowerCase().contains(keyword.toLowerCase())) {
                        continue;
                    }
                }
                
                Map<String, Object> scheduleMap = new HashMap<>();
                scheduleMap.put("id", schedule.getId());
                scheduleMap.put("movieId", schedule.getMovieId());
                scheduleMap.put("hallId", schedule.getHallId());
                scheduleMap.put("cinemaId", schedule.getCinemaId());
                scheduleMap.put("showTime", schedule.getShowTime().toString());
                scheduleMap.put("endTime", schedule.getEndTime() != null ?schedule.getEndTime().toString() : null);
                scheduleMap.put("price", schedule.getPrice());
                // 根据放映时间动态判断状态（用于getScheduleById）
                String displayStatus = schedule.getStatus();
                if ("available".equals(schedule.getStatus()) && schedule.getShowTime().isBefore(LocalDateTime.now())) {
                    displayStatus = "expired";
                }
                scheduleMap.put("status", displayStatus);
                
                Hall hall = hallMapper.findById(schedule.getHallId());
                if (hall != null) {
                    scheduleMap.put("hallNumber", hall.getHallNumber());
                    scheduleMap.put("rows", hall.getRows());
                    scheduleMap.put("cols", hall.getCols());
                }
                
                // 添加电影信息
                Movie movie = movieMapper.findById(schedule.getMovieId());
                if (movie != null) {
                    scheduleMap.put("movieTitle", movie.getTitle());
                    scheduleMap.put("moviePoster", movie.getPoster());
                }
                
                scheduleList.add(scheduleMap);
            }
            
            result.put("success", true);
            result.put("data", scheduleList);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取场次列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/schedules/{id}")
    public Map<String, Object> getScheduleById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Schedule schedule = scheduleMapper.findById(id);
            if (schedule != null) {
                Map<String, Object> scheduleMap = new HashMap<>();
                scheduleMap.put("id", schedule.getId());
                scheduleMap.put("movieId", schedule.getMovieId());
                scheduleMap.put("hallId", schedule.getHallId());
                scheduleMap.put("cinemaId", schedule.getCinemaId());
                scheduleMap.put("showTime", schedule.getShowTime().toString());
                scheduleMap.put("endTime", schedule.getEndTime() != null ? schedule.getEndTime().toString() : null);
                scheduleMap.put("price", schedule.getPrice());
                // 根据放映时间动态判断状态（用于getScheduleById）
                String displayStatus = schedule.getStatus();
                if ("available".equals(schedule.getStatus()) && schedule.getShowTime().isBefore(LocalDateTime.now())) {
                    displayStatus = "expired";
                }
                scheduleMap.put("status", displayStatus);
                
                // 添加放映厅信息
                Hall hall = hallMapper.findById(schedule.getHallId());
                if (hall != null) {
                    scheduleMap.put("hallNumber", hall.getHallNumber());
                    scheduleMap.put("rows", hall.getRows());
                    scheduleMap.put("cols", hall.getCols());
                }
                
                // 添加电影信息
                Movie movie = movieMapper.findById(schedule.getMovieId());
                System.out.println("=== 电影查询 (单个场次) ===");
                System.out.println("场次ID: " + id);
                System.out.println("场次的movieId: " + schedule.getMovieId());
                System.out.println("查询到的电影: " + (movie != null ? movie.getTitle() : "null"));
                if (movie != null) {
                    scheduleMap.put("movieTitle", movie.getTitle());
                    scheduleMap.put("moviePoster", movie.getPoster());
                    System.out.println("设置电影标题: " + movie.getTitle());
                } else {
                    System.out.println("电影不存在，movieId: " + schedule.getMovieId());
                }
                
                result.put("success", true);
                result.put("data", scheduleMap);
            } else {
                result.put("success", false);
                result.put("message", "场次不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取场次信息失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/schedules")
    public Map<String, Object> addSchedule(@RequestBody Schedule schedule) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (schedule.getStatus() == null) {
                schedule.setStatus("available");
            }
            
            // 计算结束时间
            Movie movie = movieMapper.findById(schedule.getMovieId());
            if (movie != null) {
                int duration = movie.getDuration();
                // 排片周期 = 影片时长 + 20分钟缓冲，向上取整到半小时
                int cycleMinutes = (int) Math.ceil((duration + 20) / 30.0) * 30;
                LocalDateTime endTime = schedule.getShowTime().plusMinutes(cycleMinutes);
                schedule.setEndTime(endTime);
            }
            
            scheduleMapper.insert(schedule);
            
            // 清除电影列表缓存，确保首页实时更新
            redisService.delete("movie:showing");
            
            result.put("success", true);
            result.put("message", "场次添加成功");
            result.put("data", schedule);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "添加场次失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/schedules/{id}")
    public Map<String, Object> updateSchedule(@PathVariable Long id, @RequestBody Schedule schedule) {
        Map<String, Object> result = new HashMap<>();
        try {
            Schedule existing = scheduleMapper.findById(id);
            if (existing == null) {
                result.put("success", false);
                result.put("message", "场次不存在");
                return result;
            }
            
            // 重新计算结束时间
            Movie movie = movieMapper.findById(schedule.getMovieId());
            if (movie != null) {
                int duration = movie.getDuration();
                // 排片周期 = 影片时长 + 20分钟缓冲，向上取整到半小时
                int cycleMinutes = (int) Math.ceil((duration + 20) / 30.0) * 30;
                LocalDateTime endTime = schedule.getShowTime().plusMinutes(cycleMinutes);
                schedule.setEndTime(endTime);
            }
            
            schedule.setId(id);
            scheduleMapper.update(schedule);
            result.put("success", true);
            result.put("message", "场次更新成功");
            result.put("data", schedule);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新场次失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/schedules/{id}")
    public Map<String, Object> deleteSchedule(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Schedule schedule = scheduleMapper.findById(id);
            if (schedule == null) {
                result.put("success", false);
                result.put("message", "场次不存在");
                return result;
            }
            
            scheduleMapper.deleteById(id);
            result.put("success", true);
            result.put("message", "场次删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "删除场次失败: " + e.getMessage());
        }
        return result;
    }

    // 解析 Long 类型参数
    private Long parseLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof String) return Long.parseLong((String) obj);
        if (obj instanceof Number) return ((Number) obj).longValue();
        return null;
    }
    // 解析 Double 类型参数
    private Double parseDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Integer) return ((Integer) obj).doubleValue();
        if (obj instanceof String) return Double.parseDouble((String) obj);
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        return null;
    }

    // /**
    //  * 自动排片预览（不实际创建排片）
    //  * GET /api/admin/schedule/preview?cinemaId=1&movieId=1&date=2026-05-15
    //  */
    // @GetMapping("/schedule/preview")
    // public Map<String, Object> previewAutoSchedule(
    //         @RequestParam Long cinemaId,
    //         @RequestParam Long movieId,
    //         @RequestParam String date) {
    //     return scheduleService.previewSchedule(cinemaId, movieId, date);
    // }

    // /**
    //  * 为指定影院自动排片
    //  * POST /api/admin/schedule/auto
    //  * body: { cinemaId: 1, movieId: 1, price: 50.00, date: "2026-05-15" }
    //  */
    // @PostMapping("/schedule/auto")
    // public Map<String, Object> autoSchedule(@RequestBody Map<String, Object> params) {
    //     Long cinemaId = parseLong(params.get("cinemaId"));
    //     Long movieId = parseLong(params.get("movieId"));
    //     Double price = parseDouble(params.get("price"));
    //     String date = (String) params.get("date");

    //     if (cinemaId == null || movieId == null || price == null || date == null) {
    //         Map<String, Object> result = new HashMap<>();
    //         result.put("success", false);
    //         result.put("message", "参数不全");
    //         return result;
    //     }

    //     return scheduleService.autoSchedule(cinemaId, movieId, BigDecimal.valueOf(price), date);
    // }

    // /**
    //  * 为所有影院自动排片
    //  * POST /api/admin/schedule/auto-all
    //  * body: { movieId: 1, price: 50.00, date: "2026-05-15" }
    //  */
    // @PostMapping("/schedule/auto-all")
    // public Map<String, Object> autoScheduleAll(@RequestBody Map<String, Object> params) {
    //     Long movieId = parseLong(params.get("movieId"));
    //     Double price = parseDouble(params.get("price"));
    //     String date = (String) params.get("date");

    //     if (movieId == null || price == null || date == null) {
    //         Map<String, Object> result = new HashMap<>();
    //         result.put("success", false);
    //         result.put("message", "参数不全");
    //         return result;
    //     }

    //     return scheduleService.autoScheduleAllCinemas(movieId, BigDecimal.valueOf(price), date);
    // }

    // /**
    //  * 获取指定影院的固定排片档时间（用于排片时选择）
    //  * GET /api/admin/schedule/slots?cinemaId=1&duration=148&date=2026-05-15
    //  */
    // @GetMapping("/schedule/slots")
    // public Map<String, Object> getScheduleSlots(
    //         @RequestParam Long cinemaId,
    //         @RequestParam Integer duration,
    //         @RequestParam(required = false) String date) {
    //     return scheduleService.getScheduleSlots(cinemaId, duration, date);
    // }

    // /**
    //  * 获取所有影院的固定排片档时间（批量）
    //  * GET /api/admin/schedule/slots-all?duration=148&date=2026-05-15
    //  */
    // @GetMapping("/schedule/slots-all")
    // public Map<String, Object> getAllScheduleSlots(
    //         @RequestParam Integer duration,
    //         @RequestParam(required = false) String date) {
    //     return scheduleService.getAllScheduleSlots(duration, date);
    // }

    /**
     * 获取影厅当日已占用的时间段
     * GET /api/admin/schedule/occupied?hallId=1&date=2026-05-15
     */
    @GetMapping("/schedule/occupied")
    public Map<String, Object> getOccupiedSlots(
            @RequestParam Long hallId,
            @RequestParam String date) {
        Map<String, Object> result = new HashMap<>();
        try {
            java.time.LocalDate showDate = java.time.LocalDate.parse(date);
            java.time.LocalDateTime startOfDay = showDate.atStartOfDay();
            java.time.LocalDateTime endOfDay = showDate.atTime(23, 59, 59);
            
            List<Schedule> schedules = scheduleMapper.findByHallIdAndShowTimeBetween(hallId, startOfDay, endOfDay);
            
            // 获取已占用时间段
            List<Map<String, Object>> occupiedList = new java.util.ArrayList<>();
            for (Schedule schedule : schedules) {
                java.time.LocalDateTime showTime = schedule.getShowTime();
                // 使用数据库中存储的结束时间
                java.time.LocalDateTime endTime = schedule.getEndTime();
                
                // 如果数据库中没有存储结束时间，则使用计算方式作为备用
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
            
            // 按开始时间排序
            occupiedList.sort((a, b) -> {
                String timeA = (String) a.get("startTime");
                String timeB = (String) b.get("startTime");
                return timeA.compareTo(timeB);
            });
            
            result.put("success", true);
            result.put("data", occupiedList);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取已占用场次失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 检查排片冲突
     * POST /api/admin/schedule/conflict-check
     * body: { hallId: 1, date: "2026-05-15", startTime: "14:00", duration: 120 }
     */
    @PostMapping("/schedule/conflict-check")
    public Map<String, Object> checkConflict(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long hallId = parseLong(params.get("hallId"));
            String date = (String) params.get("date");
            String startTime = (String) params.get("startTime");
            Integer duration = params.get("duration") instanceof Integer ? 
                    (Integer) params.get("duration") : 
                    Integer.parseInt(String.valueOf(params.get("duration")));
            
            if (hallId == null || date == null || startTime == null || duration == null) {
                result.put("success", false);
                result.put("message", "参数不全");
                return result;
            }
            
            // 解析日期和时间
            java.time.LocalDate showDate = java.time.LocalDate.parse(date);
            java.time.LocalTime startTimeObj = java.time.LocalTime.parse(startTime);
            java.time.LocalDateTime showStartTime = java.time.LocalDateTime.of(showDate, startTimeObj);
            java.time.LocalDateTime showEndTime = showStartTime.plusMinutes(duration + 20);
            
            // 查询该影厅当天所有场次
            java.time.LocalDateTime startOfDay = showDate.atStartOfDay();
            java.time.LocalDateTime endOfDay = showDate.atTime(23, 59, 59);
            List<Schedule> schedules = scheduleMapper.findByHallIdAndShowTimeBetween(hallId, startOfDay, endOfDay);
            
            // 检查是否有冲突
            for (Schedule schedule : schedules) {
                java.time.LocalDateTime existingStartTime = schedule.getShowTime();
                // 使用数据库中存储的结束时间进行冲突检测
                java.time.LocalDateTime existingEndTime = schedule.getEndTime();
                
                // 如果数据库中没有存储结束时间，则使用计算方式作为备用
                if (existingEndTime == null) {
                    Movie movie = movieMapper.findById(schedule.getMovieId());
                    int existingDuration = movie != null ? movie.getDuration() : 120;
                    existingEndTime = existingStartTime.plusMinutes(existingDuration + 20);
                }
                
                // 检查时间段是否重叠
                if (!(showEndTime.isBefore(existingStartTime) || showStartTime.isAfter(existingEndTime))) {
                    result.put("success", true);
                    result.put("conflict", true);
                    result.put("message", "该影厅此时段已有场次，请重选");
                    return result;
                }
            }
            
            result.put("success", true);
            result.put("conflict", false);
            result.put("message", "无冲突");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "冲突检测失败: " + e.getMessage());
        }
        return result;
    }

    // ============ 公告管理 ============

    @GetMapping("/announcements/latest")
    public Map<String, Object> getLatestAnnouncement() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Announcement> announcements = announcementMapper.findByStatus("published");
            if (!announcements.isEmpty()) {
                result.put("success", true);
                result.put("data", announcements.get(0));
            } else {
                result.put("success", true);
                result.put("data", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取最新公告失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/announcements")
    public Map<String, Object> getAllAnnouncements() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Announcement> announcements = announcementMapper.findAll();
            result.put("success", true);
            result.put("data", announcements);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取公告列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/announcements/{id}")
    public Map<String, Object> getAnnouncementById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Announcement announcement = announcementMapper.findById(id);
            if (announcement != null) {
                result.put("success", true);
                result.put("data", announcement);
            } else {
                result.put("success", false);
                result.put("message", "公告不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取公告信息失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/announcements")
    public Map<String, Object> addAnnouncement(@RequestBody Announcement announcement) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (announcement.getStatus() == null) {
                announcement.setStatus("published");
            }
            announcement.setPublishedAt(LocalDateTime.now());
            announcementMapper.insert(announcement);
            result.put("success", true);
            result.put("message", "公告发布成功");
            result.put("data", announcement);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "发布公告失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/announcements/{id}")
    public Map<String, Object> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        Map<String, Object> result = new HashMap<>();
        try {
            Announcement existing = announcementMapper.findById(id);
            if (existing == null) {
                result.put("success", false);
                result.put("message", "公告不存在");
                return result;
            }
            
            announcement.setId(id);
            announcementMapper.update(announcement);
            result.put("success", true);
            result.put("message", "公告更新成功");
            result.put("data", announcement);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新公告失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/announcements/{id}")
    public Map<String, Object> deleteAnnouncement(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Announcement announcement = announcementMapper.findById(id);
            if (announcement == null) {
                result.put("success", false);
                result.put("message", "公告不存在");
                return result;
            }
            
            announcementMapper.deleteById(id);
            result.put("success", true);
            result.put("message", "公告删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "删除公告失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/announcements/{id}/status")
    public Map<String, Object> updateAnnouncementStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String status = params.get("status");
            announcementMapper.updateStatus(id, status);
            result.put("success", true);
            result.put("message", "公告状态更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新公告状态失败: " + e.getMessage());
        }
        return result;
    }

    // ============ 用户管理 ============

    @GetMapping("/users")
    public Map<String, Object> getAllUsers(@RequestParam(required = false) String role) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<User> users;
            if (role != null && !role.isEmpty()) {
                users = userMapper.findByRole(role);
            } else {
                users = userMapper.findAll();
            }
            result.put("success", true);
            result.put("data", users);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取用户列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> getUserById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userMapper.findById(id);
            if (user != null) {
                user.setPassword("******");
                result.put("success", true);
                result.put("data", user);
            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取用户信息失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/users")
    public Map<String, Object> addUser(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (user.getRole() == null) {
                user.setRole("user");
            }
            if (user.getStatus() == null) {
                user.setStatus("active");
            }
            userMapper.insert(user);
            user.setPassword("******");
            result.put("success", true);
            result.put("message", "用户添加成功");
            result.put("data", user);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "添加用户失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            User existing = userMapper.findById(id);
            if (existing == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            user.setId(id);
            user.setPassword(existing.getPassword());
            userMapper.update(user);
            result.put("success", true);
            result.put("message", "用户更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新用户失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userMapper.findById(id);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            userMapper.deleteById(id);
            result.put("success", true);
            result.put("message", "用户删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "删除用户失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/users/{id}/status")
    public Map<String, Object> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String status = params.get("status");
            userMapper.updateStatus(id, status);
            result.put("success", true);
            result.put("message", "用户状态更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新用户状态失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/users/{id}/password")
    public Map<String, Object> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String password = params.get("password");
            userMapper.updatePassword(id, password);
            result.put("success", true);
            result.put("message", "密码重置成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "密码重置失败: " + e.getMessage());
        }
        return result;
    }

    // ============ 订单管理 ============

    @GetMapping("/orders")
    public Map<String, Object> getAllOrders(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Order> orders;
            if (status != null && !status.isEmpty()) {
                orders = orderMapper.findByStatus(status);
            } else if (q != null && !q.isEmpty()) {
                Order order = orderMapper.findByOrderNumber(q);
                orders = order != null ? List.of(order) : List.of();
            } else {
                orders = orderMapper.findAll();
            }
            result.put("success", true);
            result.put("data", orders);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取订单列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/orders/{id}")
    public Map<String, Object> getOrderById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Order order = orderMapper.findById(id);
            if (order != null) {
                result.put("success", true);
                result.put("data", order);
            } else {
                result.put("success", false);
                result.put("message", "订单不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取订单信息失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/orders/{id}")
    public Map<String, Object> deleteOrder(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Order order = orderMapper.findById(id);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }
            
            orderMapper.deleteById(id);
            result.put("success", true);
            result.put("message", "订单删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "删除订单失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/orders/{id}/refund")
    public Map<String, Object> refundOrder(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Order order = orderMapper.findById(id);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }
            
            if (!"paid".equals(order.getStatus())) {
                result.put("success", false);
                result.put("message", "只能对已付款订单进行退款");
                return result;
            }
            
            order.setStatus("refunded");
            order.setPayStatus("refunded");
            order.setRefundStatus("refunded");
            order.setRefundedAt(LocalDateTime.now());
            orderMapper.update(order);
            result.put("success", true);
            result.put("message", "退款成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "退款失败: " + e.getMessage());
        }
        return result;
    }
}