package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping("/add")
    public ApiResponse<Void> addFavorite(@RequestBody Map<String, Long> params) {
        Long userId = params.get("userId");
        Long movieId = params.get("movieId");

        if (userId == null || movieId == null) {
            return ApiResponse.error(400, "用户ID和电影ID不能为空");
        }

        try {
            favoriteService.addFavorite(userId, movieId);
            return ApiResponse.success("收藏成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public ApiResponse<Void> removeFavorite(@RequestBody Map<String, Long> params) {
        Long userId = params.get("userId");
        Long movieId = params.get("movieId");

        if (userId == null || movieId == null) {
            return ApiResponse.error(400, "用户ID和电影ID不能为空");
        }

        try {
            favoriteService.removeFavorite(userId, movieId);
            return ApiResponse.success("取消收藏成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteById(@PathVariable Long id) {
        try {
            favoriteService.deleteById(id);
            return ApiResponse.success("删除成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getFavoriteList(@RequestParam Long userId, @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = favoriteService.getFavoriteList(userId, limit);
        return ApiResponse.success(result);
    }

    @GetMapping("/count")
    public ApiResponse<Integer> getFavoriteCount(@RequestParam Long userId) {
        int count = favoriteService.getFavoriteCount(userId);
        return ApiResponse.success(count);
    }

    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> checkFavorite(@RequestParam Long userId, @RequestParam Long movieId) {
        Map<String, Object> result = favoriteService.checkFavorite(userId, movieId);
        return ApiResponse.success(result);
    }
}
