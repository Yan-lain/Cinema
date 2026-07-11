package com.example.demo.controller;

import com.example.demo.entity.Seat;
import com.example.demo.entity.Schedule;
import com.example.demo.mapper.SeatMapper;
import com.example.demo.mapper.ScheduleMapper;
import com.example.demo.mapper.ScheduleSeatMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seats")
@CrossOrigin(origins = "http://localhost:5173")
public class SeatController {

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private ScheduleSeatMapper scheduleSeatMapper;

    @GetMapping
    public Map<String, Object> getAllSeats() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Seat> seats = seatMapper.findAll();
            result.put("success", true);
            result.put("data", seats);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取座位列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getSeatById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Seat seat = seatMapper.findById(id);
            if (seat != null) {
                result.put("success", true);
                result.put("data", seat);
            } else {
                result.put("success", false);
                result.put("message", "座位不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取座位失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/hall/{hallId}")
    public Map<String, Object> getSeatsByHallId(@PathVariable Long hallId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Seat> seats = seatMapper.findByHallId(hallId);
            result.put("success", true);
            result.put("data", seats);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取座位列表失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/batch")
    public Map<String, Object> batchAddSeats(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long hallId = ((Number) request.get("hallId")).longValue();
            int rows = (Integer) request.get("rows");
            int cols = (Integer) request.get("cols");
            int startRow = request.get("startRow") != null ? (Integer) request.get("startRow") : 1;
            int startCol = request.get("startCol") != null ? (Integer) request.get("startCol") : 1;

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    Seat seat = new Seat();
                    seat.setHallId(hallId);
                    seat.setRowNum(startRow + i);
                    seat.setColNum(startCol + j);
                    seat.setSeatNumber((startRow + i) + "-" + (startCol + j));
                    seatMapper.insert(seat);
                }
            }

            result.put("success", true);
            result.put("message", "成功创建 " + (rows * cols) + " 个座位");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "批量创建座位失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/schedule/{scheduleId}")
    public Map<String, Object> getSeatsByScheduleId(@PathVariable Long scheduleId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Schedule schedule = scheduleMapper.findById(scheduleId);
            if (schedule == null) {
                result.put("success", false);
                result.put("message", "场次不存在");
                return result;
            }

            List<Seat> seats = seatMapper.findByHallId(schedule.getHallId());

            // 获取该场次已售出和已锁定的座位ID列表
            List<Long> unavailableSeatIds = scheduleSeatMapper.findUnavailableSeatIdsByScheduleId(scheduleId);

            List<Map<String, Object>> seatList = new java.util.ArrayList<>();
            for (Seat seat : seats) {
                Map<String, Object> seatMap = new HashMap<>();
                seatMap.put("id", seat.getId());
                seatMap.put("hallId", seat.getHallId());
                seatMap.put("row", seat.getRowNum());
                seatMap.put("col", seat.getColNum());
                seatMap.put("seatNumber", seat.getSeatNumber());

                // 根据schedule_seat表判断状态，而不是seat表
                if (unavailableSeatIds.contains(seat.getId())) {
                    seatMap.put("status", "sold");
                } else {
                    seatMap.put("status", "available");
                }

                seatList.add(seatMap);
            }

            result.put("success", true);
            result.put("data", seatList);
            result.put("hallId", schedule.getHallId());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取座位列表失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/batch")
    public Map<String, Object> batchUpdateStatus(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Long> seatIds = (List<Long>) request.get("seatIds");

            for (Long seatId : seatIds) {
                Seat seat = seatMapper.findById(seatId);
                if (seat != null) {
                    seatMapper.update(seat);
                }
            }

            result.put("success", true);
            result.put("message", "成功更新 " + seatIds.size() + " 个座位状态");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "批量更新座位状态失败: " + e.getMessage());
        }
        return result;
    }
}