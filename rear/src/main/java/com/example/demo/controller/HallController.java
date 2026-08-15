package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.entity.Hall;
import com.example.demo.service.HallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/halls")
public class HallController {

    @Autowired
    private HallService hallService;

    @GetMapping
    public ApiResponse<List<Hall>> getAllHalls(@RequestParam(required = false) Long cinemaId) {
        List<Hall> halls = hallService.getAllHalls(cinemaId);
        return ApiResponse.success(halls);
    }

    @GetMapping("/{id}")
    public ApiResponse<Hall> getHallById(@PathVariable Long id) {
        Hall hall = hallService.getHallById(id);
        if (hall == null) {
            return ApiResponse.error(404, "放映厅不存在");
        }
        return ApiResponse.success(hall);
    }

    @PostMapping
    public ApiResponse<Hall> addHall(@RequestBody Hall hall) {
        Hall savedHall = hallService.addHall(hall);
        return ApiResponse.success("放映厅添加成功", savedHall);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateHall(@PathVariable Long id, @RequestBody Hall hall) {
        Hall existing = hallService.getHallById(id);
        if (existing == null) {
            return ApiResponse.error(404, "放映厅不存在");
        }
        hallService.updateHall(id, hall);
        return ApiResponse.success("放映厅更新成功", null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateHallStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            hallService.updateHallStatus(id, request.get("status"));
            return ApiResponse.success("状态更新成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteHall(@PathVariable Long id) {
        try {
            hallService.deleteHall(id);
            return ApiResponse.success("放映厅删除成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }
}
