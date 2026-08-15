package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.entity.Cinema;
import com.example.demo.service.CinemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cinemas")
public class CinemaController {

    @Autowired
    private CinemaService cinemaService;

    @GetMapping
    public ApiResponse<List<Cinema>> getAllCinemas() {
        List<Cinema> cinemas = cinemaService.getAllCinemas();
        return ApiResponse.success(cinemas);
    }

    @GetMapping("/city/{city}")
    public ApiResponse<List<Cinema>> getCinemasByCity(@PathVariable String city) {
        List<Cinema> cinemas = cinemaService.getCinemasByCity(city);
        return ApiResponse.success(cinemas);
    }

    @GetMapping("/search")
    public ApiResponse<List<Cinema>> searchCinemas(@RequestParam(required = false) String name) {
        List<Cinema> cinemas = cinemaService.searchCinemas(name);
        return ApiResponse.success(cinemas);
    }

    @GetMapping("/{id}")
    public ApiResponse<Cinema> getCinemaById(@PathVariable Long id) {
        Cinema cinema = cinemaService.getCinemaById(id);
        if (cinema == null) {
            return ApiResponse.error(404, "影院不存在");
        }
        return ApiResponse.success(cinema);
    }

    @PostMapping
    public ApiResponse<Cinema> addCinema(@RequestBody Cinema cinema) {
        if (cinema.getStatus() == null) {
            cinema.setStatus("active");
        }
        Cinema savedCinema = cinemaService.addCinema(cinema);
        return ApiResponse.success("影院添加成功", savedCinema);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateCinema(@PathVariable Long id, @RequestBody Cinema cinema) {
        Cinema existing = cinemaService.getCinemaById(id);
        if (existing == null) {
            return ApiResponse.error(404, "影院不存在");
        }
        cinema.setId(id);
        cinemaService.updateCinema(cinema);
        return ApiResponse.success("影院更新成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCinema(@PathVariable Long id) {
        Cinema cinema = cinemaService.getCinemaById(id);
        if (cinema == null) {
            return ApiResponse.error(404, "影院不存在");
        }
        cinemaService.deleteCinema(id);
        return ApiResponse.success("影院删除成功", null);
    }
}
