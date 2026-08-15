package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResponse;
import com.example.demo.dto.response.MovieResponse;
import com.example.demo.entity.Movie;
import com.example.demo.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "电影管理", description = "电影查询、搜索、分类筛选等公开接口")
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @Operation(summary = "查询所有电影", description = "返回电影列表，包含所有状态的电影")
    @GetMapping
    public ApiResponse<List<MovieResponse>> getAllMovies() {
        List<MovieResponse> movies = movieService.getAllMovies();
        return ApiResponse.success(movies);
    }

    @Operation(summary = "分页查询电影", description = "支持按状态筛选，分页返回电影列表")
    @GetMapping("/page")
    public ApiResponse<PageResponse<MovieResponse>> getMoviesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        PageResponse<MovieResponse> response = movieService.getMoviesWithPagination(page, size, status);
        return ApiResponse.success(response);
    }

    @Operation(summary = "根据 ID 查询电影详情")
    @GetMapping("/{id}")
    public ApiResponse<MovieResponse> getMovieById(@PathVariable Long id) {
        MovieResponse movie = movieService.getMovieById(id);
        return ApiResponse.success(movie);
    }

    @Operation(summary = "搜索电影", description = "根据关键词搜索电影（标题、导演、演员等）")
    @GetMapping("/search")
    public ApiResponse<List<MovieResponse>> searchMovies(@RequestParam String keyword) {
        List<MovieResponse> movies = movieService.searchMovies(keyword);
        return ApiResponse.success(movies);
    }

    @Operation(summary = "分页搜索电影")
    @GetMapping("/search/page")
    public ApiResponse<PageResponse<MovieResponse>> searchMoviesPage(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<MovieResponse> response = movieService.searchMoviesWithPagination(keyword, page, size);
        return ApiResponse.success(response);
    }

    @Operation(summary = "查询正在上映的电影")
    @GetMapping("/showing")
    public ApiResponse<List<MovieResponse>> getShowingMovies() {
        List<MovieResponse> movies = movieService.getShowingMovies();
        return ApiResponse.success(movies);
    }

    @Operation(summary = "新增电影（管理员）")
    @PostMapping
    public ApiResponse<MovieResponse> addMovie(@RequestBody Movie movie) {
        MovieResponse response = movieService.addMovie(movie);
        return ApiResponse.success("电影添加成功", response);
    }

    @Operation(summary = "更新电影信息（管理员）")
    @PutMapping("/{id}")
    public ApiResponse<MovieResponse> updateMovie(@RequestBody Movie movie) {
        MovieResponse response = movieService.updateMovie(movie);
        return ApiResponse.success("电影更新成功", response);
    }

    @Operation(summary = "删除电影（管理员）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ApiResponse.success("电影删除成功", null);
    }
}