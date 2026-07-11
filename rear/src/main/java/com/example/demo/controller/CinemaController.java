package com.example.demo.controller;

import com.example.demo.entity.Cinema;
import com.example.demo.mapper.CinemaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cinemas")
@CrossOrigin(origins = "http://localhost:5173")
public class CinemaController {

    @Autowired
    private CinemaMapper cinemaMapper;

    @GetMapping
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

    @GetMapping("/city/{city}")
    public Map<String, Object> getCinemasByCity(@PathVariable String city) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Cinema> cinemas = cinemaMapper.findByCity(city);
            result.put("success", true);
            result.put("data", cinemas);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取影院列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/search")
    public Map<String, Object> searchCinemas(@RequestParam(required = false) String name) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Cinema> cinemas;
            if (name != null && !name.trim().isEmpty()) {
                cinemas = cinemaMapper.findByName(name.trim());
            } else {
                cinemas = cinemaMapper.findAllActive();
            }
            result.put("success", true);
            result.put("data", cinemas);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "搜索影院失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
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

    @PostMapping
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

    @PutMapping("/{id}")
    public Map<String, Object> updateCinema(@PathVariable Long id, @RequestBody Cinema cinema) {
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

    @DeleteMapping("/{id}")
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
}