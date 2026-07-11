package com.example.demo.controller;

import com.example.demo.entity.Favorite;
import com.example.demo.mapper.FavoriteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @PostMapping("/add")
    public Map<String, Object> addFavorite(@RequestBody Map<String, Long> params) {
        Map<String, Object> result = new HashMap<>();
        Long userId = params.get("userId");
        Long movieId = params.get("movieId");

        if (userId == null || movieId == null) {
            result.put("success", false);
            result.put("message", "用户ID和电影ID不能为空");
            return result;
        }

        try {
            Favorite existing = favoriteMapper.findByUserAndMovie(userId, movieId);
            if (existing != null) {
                result.put("success", false);
                result.put("message", "已收藏过该电影");
                return result;
            }

            Favorite favorite = new Favorite(userId, movieId);
            favoriteMapper.insert(favorite);

            result.put("success", true);
            result.put("message", "收藏成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "收藏失败: " + e.getMessage());
        }

        return result;
    }

    @DeleteMapping("/remove")
    public Map<String, Object> removeFavorite(@RequestBody Map<String, Long> params) {
        Map<String, Object> result = new HashMap<>();
        Long userId = params.get("userId");
        Long movieId = params.get("movieId");

        if (userId == null || movieId == null) {
            result.put("success", false);
            result.put("message", "用户ID和电影ID不能为空");
            return result;
        }

        try {
            int affected = favoriteMapper.deleteByUserAndMovie(userId, movieId);
            if (affected > 0) {
                result.put("success", true);
                result.put("message", "取消收藏成功");
            } else {
                result.put("success", false);
                result.put("message", "收藏记录不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "取消收藏失败: " + e.getMessage());
        }

        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            int affected = favoriteMapper.deleteById(id);
            if (affected > 0) {
                result.put("success", true);
                result.put("message", "删除成功");
            } else {
                result.put("success", false);
                result.put("message", "记录不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> getFavoriteList(@RequestParam Long userId, @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> list = favoriteMapper.findWithUserAndMovie(userId, limit);
            int totalCount = favoriteMapper.countByUserId(userId);

            result.put("success", true);
            result.put("total", totalCount);
            result.put("data", list);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取收藏列表失败: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/count")
    public Map<String, Object> getFavoriteCount(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            int count = favoriteMapper.countByUserId(userId);
            result.put("success", true);
            result.put("count", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取收藏数量失败: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/check")
    public Map<String, Object> checkFavorite(@RequestParam Long userId, @RequestParam Long movieId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Favorite favorite = favoriteMapper.findByUserAndMovie(userId, movieId);
            result.put("success", true);
            result.put("isFavorite", favorite != null);
            if (favorite != null) {
                result.put("favoriteId", favorite.getId());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查收藏状态失败: " + e.getMessage());
        }

        return result;
    }
}