package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.service.BrowseHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/browse")
public class BrowseHistoryController {

    @Autowired
    private BrowseHistoryService browseHistoryService;

    @PostMapping("/add")
    public ApiResponse<Void> addBrowseHistory(@RequestBody Map<String, Long> params) {
        Long userId = params.get("userId");
        Long movieId = params.get("movieId");

        if (userId == null || movieId == null) {
            return ApiResponse.error(400, "用户ID和电影ID不能为空");
        }

        browseHistoryService.addBrowseHistory(userId, movieId);
        return ApiResponse.success("浏览记录添加成功", null);
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getBrowseHistory(@RequestParam Long userId, @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = browseHistoryService.getBrowseHistory(userId, limit);
        return ApiResponse.success(result);
    }

    @GetMapping("/count")
    public ApiResponse<Integer> getBrowseCount(@RequestParam Long userId) {
        int count = browseHistoryService.getBrowseCount(userId);
        return ApiResponse.success(count);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteBrowseHistory(@PathVariable Long id) {
        try {
            browseHistoryService.deleteById(id);
            return ApiResponse.success("删除成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    public ApiResponse<Void> clearBrowseHistory(@RequestParam Long userId) {
        browseHistoryService.clearBrowseHistory(userId);
        return ApiResponse.success("浏览记录清空成功", null);
    }
}
