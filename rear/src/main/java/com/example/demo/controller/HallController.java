package com.example.demo.controller;

import com.example.demo.entity.Hall;
import com.example.demo.mapper.HallMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/halls")
@CrossOrigin(origins = "http://localhost:5173")
public class HallController {

    @Autowired
    private HallMapper hallMapper;

    @GetMapping
    public Map<String, Object> getAllHalls(@RequestParam(required = false) Long cinemaId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Hall> halls;
            if (cinemaId != null) {
                halls = hallMapper.findByCinemaId(cinemaId);
            } else {
                halls = hallMapper.findAll();
            }
            result.put("success", true);
            result.put("data", halls);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取放映厅列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getHallById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Hall hall = hallMapper.findById(id);
            if (hall != null) {
                result.put("success", true);
                result.put("data", hall);
            } else {
                result.put("success", false);
                result.put("message", "放映厅不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取放映厅信息失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping
    public Map<String, Object> addHall(@RequestBody Hall hall) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            if (hall.getStatus() == null) {
                hall.setStatus("active");
            }
            hallMapper.insert(hall);
            result.put("success", true);
            result.put("message", "放映厅添加成功");
            result.put("data", hall);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "添加放映厅失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateHall(@PathVariable Long id, @RequestBody Hall hall) {
        Map<String, Object> result = new HashMap<>();
        try {
            Hall existing = hallMapper.findById(id);
            if (existing == null) {
                result.put("success", false);
                result.put("message", "放映厅不存在");
                return result;
            }
            
            hall.setId(id);
            hallMapper.update(hall);
            result.put("success", true);
            result.put("message", "放映厅更新成功");
            result.put("data", hall);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新放映厅失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateHallStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Hall hall = hallMapper.findById(id);
            if (hall == null) {
                result.put("success", false);
                result.put("message", "放映厅不存在");
                return result;
            }
            
            String status = request.get("status");
            hall.setStatus(status);
            hallMapper.update(hall);
            result.put("success", true);
            result.put("message", "状态更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新状态失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteHall(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Hall hall = hallMapper.findById(id);
            if (hall == null) {
                result.put("success", false);
                result.put("message", "放映厅不存在");
                return result;
            }
            
            hallMapper.deleteById(id);
            result.put("success", true);
            result.put("message", "放映厅删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "删除放映厅失败: " + e.getMessage());
        }
        return result;
    }
}