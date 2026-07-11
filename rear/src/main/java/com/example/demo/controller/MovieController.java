package com.example.demo.controller;

import com.example.demo.entity.Movie;
import com.example.demo.mapper.MovieMapper;
import com.example.demo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private RedisService redisService;

    // 缓存键常量
    private static final String MOVIE_LIST_KEY = "movie:list";
    private static final String MOVIE_SHOWING_KEY = "movie:showing";
    private static final String MOVIE_DETAIL_KEY = "movie:detail:";

    @GetMapping
    public Map<String, Object> getAllMovies() {
        Map<String, Object> result = new HashMap<>();
        
        // 先从Redis获取缓存
        Object cached = redisService.get(MOVIE_LIST_KEY);
        if (cached != null) {
            result.put("success", true);
            result.put("data", cached);
            result.put("from", "redis");
            return result;
        }
        
        // 从数据库获取
        List<Movie> movies = movieMapper.findAll();
        result.put("success", true);
        result.put("data", movies);
        result.put("from", "database");
        
        // 存入Redis，缓存30分钟
        redisService.set(MOVIE_LIST_KEY, movies, 30, TimeUnit.MINUTES);
        
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getMovieById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        String cacheKey = MOVIE_DETAIL_KEY + id;
        
        // 先从Redis获取缓存
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            result.put("success", true);
            result.put("data", cached);
            result.put("from", "redis");
            return result;
        }
        
        // 从数据库获取
        Movie movie = movieMapper.findById(id);
        if (movie != null) {
            result.put("success", true);
            result.put("data", movie);
            result.put("from", "database");
            
            // 存入Redis，缓存5分钟
            redisService.set(cacheKey, movie, 5, TimeUnit.MINUTES);
        } else {
            result.put("success", false);
            result.put("message", "电影不存在");
        }
        
        return result;
    }

    @GetMapping("/search")
    public Map<String, Object> searchMovies(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();
        List<Movie> movies = movieMapper.findByTitle(keyword);
        result.put("success", true);
        result.put("data", movies);
        return result;
    }

    @GetMapping("/showing")
    public Map<String, Object> getShowingMovies() {
        Map<String, Object> result = new HashMap<>();
        
        // 先从Redis获取缓存
        Object cached = redisService.get(MOVIE_SHOWING_KEY);
        if (cached != null) {
            result.put("success", true);
            result.put("data", cached);
            result.put("from", "redis");
            return result;
        }
        
        // 从数据库获取有有效场次的电影（关联场次表查询）
        List<Movie> movies = movieMapper.findShowingMoviesWithValidSchedule();
        result.put("success", true);
        result.put("data", movies);
        result.put("from", "database");
        
        // 存入Redis，缓存10分钟（场次更新较频繁）
        redisService.set(MOVIE_SHOWING_KEY, movies, 10, TimeUnit.MINUTES);
        
        return result;
    }

    @PostMapping
    public Map<String, Object> addMovie(@RequestBody Movie movie) {
        Map<String, Object> result = new HashMap<>();
        movie.setStatus("showing");
        movieMapper.insert(movie);
        
        // 更新缓存：删除相关缓存，下次请求会重新从数据库获取
        redisService.delete(MOVIE_LIST_KEY);
        redisService.delete(MOVIE_SHOWING_KEY);
        
        result.put("success", true);
        result.put("message", "电影添加成功");
        result.put("data", movie);
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateMovie(@RequestBody Movie movie) {
        Map<String, Object> result = new HashMap<>();
        movieMapper.update(movie);
        
        // 更新缓存
        redisService.delete(MOVIE_LIST_KEY);
        redisService.delete(MOVIE_SHOWING_KEY);
        redisService.delete(MOVIE_DETAIL_KEY + movie.getId());
        
        result.put("success", true);
        result.put("message", "电影更新成功");
        result.put("data", movie);
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteMovie(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        movieMapper.deleteById(id);
        
        // 更新缓存
        redisService.delete(MOVIE_LIST_KEY);
        redisService.delete(MOVIE_SHOWING_KEY);
        redisService.delete(MOVIE_DETAIL_KEY + id);
        
        result.put("success", true);
        result.put("message", "电影删除成功");
        return result;
    }
}
