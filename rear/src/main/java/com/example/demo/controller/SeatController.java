package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.entity.Seat;
import com.example.demo.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 座位控制器
 * 【技术说明】用于处理与座位相关的HTTP请求
 * 【功能说明】提供获取所有座位、根据ID获取座位、根据厅ID获取座位、批量添加座位、根据排片ID获取座位、批量更新座位状态等功能
 * 【依赖说明】依赖SeatService，用于调用业务逻辑
 * 【接口说明】提供GET、POST、PUT、DELETE等HTTP方法，用于处理与座位相关的请求
 * 【返回值说明】返回JSON格式的响应体，包含状态码、消息、数据等
 * 【参数说明】根据请求方法不同，参数不同，具体请参考接口文档
 * 【异常说明】处理可能的异常情况，如座位不存在、排片不存在等
 * 【接口文档】请参考接口文档，包含所有接口的详细说明
 * 【接口示例】请参考接口文档，包含所有接口的示例请求和响应
 * */
@RestController
@RequestMapping("/api/seats")
public class SeatController {

    @Autowired
    private SeatService seatService;

    @GetMapping
    public ApiResponse<List<Seat>> getAllSeats() {
        List<Seat> seats = seatService.getAllSeats();
        return ApiResponse.success(seats);
    }

    @GetMapping("/{id}")
    public ApiResponse<Seat> getSeatById(@PathVariable Long id) {
        Seat seat = seatService.getSeatById(id);
        if (seat == null) {
            return ApiResponse.error(404, "座位不存在");
        }
        return ApiResponse.success(seat);
    }


    @GetMapping("/hall/{hallId}")
    public ApiResponse<List<Seat>> getSeatsByHallId(@PathVariable Long hallId) {
        List<Seat> seats = seatService.getSeatsByHallId(hallId);
        return ApiResponse.success(seats);
    }

    /**
     * 批量添加座位
     * 【技术说明】用于批量添加座位到指定厅
     * 【功能说明】根据请求体中的参数，批量创建指定数量的座位
     * 【依赖说明】依赖SeatService，用于调用业务逻辑
     * 【接口说明】提供POST方法，用于处理批量添加座位请求
     * 【返回值说明】返回JSON格式的响应体，包含状态码、消息、数据等
     * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
     * 【异常说明】处理可能的异常情况，如厅不存在、参数错误等
     * */
    @PostMapping("/batch")
    public ApiResponse<Void> batchAddSeats(@RequestBody Map<String, Object> request) {
        Long hallId = ((Number) request.get("hallId")).longValue();
        int rows = (Integer) request.get("rows");
        int cols = (Integer) request.get("cols");
        int startRow = request.get("startRow") != null ? (Integer) request.get("startRow") : 1;
        int startCol = request.get("startCol") != null ? (Integer) request.get("startCol") : 1;

        seatService.batchAddSeats(hallId, rows, cols, startRow, startCol);
        return ApiResponse.success("成功创建 " + (rows * cols) + " 个座位", null);
    }

    /**
     * 根据排片ID获取座位
     * 【技术说明】用于根据排片ID获取所有座位
     * 【功能说明】根据排片ID查询所有座位
     * 【依赖说明】依赖SeatService，用于调用业务逻辑
     * 【接口说明】提供GET方法，用于处理根据排片ID获取座位请求
     * 【返回值说明】返回JSON格式的响应体，包含状态码、消息、数据等
     * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
     * 【异常说明】处理可能的异常情况，如排片不存在等
     * */
    @GetMapping("/schedule/{scheduleId}")
    public ApiResponse<List<Map<String, Object>>> getSeatsByScheduleId(@PathVariable Long scheduleId) {
        try {
            List<Map<String, Object>> seats = seatService.getSeatsByScheduleId(scheduleId);
            return ApiResponse.success(seats);
        } catch (RuntimeException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    /**
     * 批量更新座位状态
     * 【技术说明】用于批量更新座位状态
     * 【功能说明】根据请求体中的参数，批量更新指定座位的状态
     * 【依赖说明】依赖SeatService，用于调用业务逻辑
     * 【接口说明】提供PUT方法，用于处理批量更新座位状态请求
     * 【返回值说明】返回JSON格式的响应体，包含状态码、消息、数据等
     * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
     * 【异常说明】处理可能的异常情况，如座位不存在等
     * */
    @PutMapping("/batch")
    public ApiResponse<Void> batchUpdateStatus(@RequestBody Map<String, Object> request) {
        List<Long> seatIds = (List<Long>) request.get("seatIds");
        seatService.batchUpdateStatus(seatIds);
        return ApiResponse.success("成功更新 " + seatIds.size() + " 个座位状态", null);
    }
}
