package com.example.demo.controller;

import com.example.demo.entity.BrowseHistory;
import com.example.demo.mapper.BrowseHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/browse")
public class BrowseHistoryController {

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @PostMapping("/add")
    public Map<String, Object> addBrowseHistory(@RequestBody Map<String, Long> params) {
        Map<String, Object> result = new HashMap<>();
        Long userId = params.get("userId");
        Long movieId = params.get("movieId");

        if (userId == null || movieId == null) {
            result.put("success", false);
            result.put("message", "用户ID和电影ID不能为空");
            return result;
        }

        try {
            browseHistoryMapper.deleteByUserAndMovie(userId, movieId);
            
            BrowseHistory history = new BrowseHistory(userId, movieId);
            browseHistoryMapper.insert(history);
            
            result.put("success", true);
            result.put("message", "浏览记录添加成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "添加浏览记录失败: " + e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> getBrowseHistory(@RequestParam Long userId, @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Map<String, Object>> historyList = browseHistoryMapper.findWithUserAndMovie(userId, limit);
            
            int totalCount = browseHistoryMapper.countByUserId(userId);
            
            result.put("success", true);
            result.put("total", totalCount);
            result.put("data", historyList);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取浏览记录失败: " + e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/count")
    public Map<String, Object> getBrowseCount(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            int count = browseHistoryMapper.countByUserId(userId);
            result.put("success", true);
            result.put("count", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取浏览记录数量失败: " + e.getMessage());
        }
        
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteBrowseHistory(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            int affected = browseHistoryMapper.deleteById(id);
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

    @DeleteMapping("/clear")
    public Map<String, Object> clearBrowseHistory(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            browseHistoryMapper.deleteByUserId(userId);
            result.put("success", true);
            result.put("message", "浏览记录清空成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "清空浏览记录失败: " + e.getMessage());
        }
        
        return result;
    }
}