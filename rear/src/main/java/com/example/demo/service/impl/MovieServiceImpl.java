package com.example.demo.service.impl;

import com.example.demo.common.PageResponse;
import com.example.demo.constant.ErrorCode;
import com.example.demo.constant.RedisKey;
import com.example.demo.dto.response.MovieResponse;
import com.example.demo.entity.Movie;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.MovieMapper;
import com.example.demo.service.MovieService;
import com.example.demo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 电影服务实现类
 * 
 * 【架构说明】
 * 实现电影相关的业务逻辑，包括电影列表查询、详情查询、搜索、增删改等
 * 
 * 【核心功能】
 * 1. 获取所有电影（getAllMovies）：带Redis缓存
 * 2. 获取电影详情（getMovieById）：带Redis缓存
 * 3. 搜索电影（searchMovies）：根据关键词搜索
 * 4. 获取正在上映电影（getShowingMovies）：带Redis缓存
 * 5. 添加电影（addMovie）：创建新电影并清除缓存
 * 6. 更新电影（updateMovie）：修改电影信息并清除缓存
 * 7. 删除电影（deleteMovie）：删除电影并清除缓存
 * 
 * 【缓存策略】
 * - 电影列表缓存30分钟
 * - 电影详情缓存5分钟
 * - 正在上映电影缓存10分钟
 * - 更新/删除电影时清除相关缓存
 * 
 * 【安全风险】
 * 1. 无权限校验：添加/更新/删除电影不需要管理员权限
 * 2. 缓存数据无版本管理：可能出现缓存一致性问题
 * 
 * 【改进建议】
 * 1. 添加管理员权限校验
 * 2. 考虑使用缓存版本号或消息队列保证缓存一致性
 */
@Service
public class MovieServiceImpl implements MovieService {

    /** 电影数据访问层 */
    @Autowired
    private MovieMapper movieMapper;

    /** Redis服务 */
    @Autowired
    private RedisService redisService;

    

    /**
     * 获取所有电影
     * 
     * @return 电影列表响应
     */
    @Override
    public List<MovieResponse> getAllMovies() {
        // 先从Redis缓存中获取
        //redis不对电影数据进行缓存 这是一次测试
        Object cached = redisService.get(RedisKey.MOVIE_LIST);
        if (cached != null) {
            return (List<MovieResponse>) cached;
        }

        List<Movie> movies = movieMapper.findAll(); 
        //对movie进行转换为MovieResponse对象，将电影实体转换为响应对象
        //.stream() 用于将集合转换为流，方便进行数据处理
        //.map(MovieResponse::fromEntity) 用于将电影实体转换为响应对象 
        // 里面的::fromEntity是MovieResponse类的方法引用 
        // 为什么是：：？ 因为MovieResponse类的方法引用是静态方法引用
        //.collect(Collectors.toList()) 用于将流转换为列表
        //Collectors.toList() 用于将流转换为列表，返回一个新列表 是java.util.stream.Collectors类的方法引用
        List<MovieResponse> responses = movies.stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());

                //redis 缓存电影列表 缓存30分钟 根据RedisKey.MOVIE_LIST响应对象
        redisService.set(RedisKey.MOVIE_LIST, responses, 30, TimeUnit.MINUTES);
        return responses;
    }

    /**
     * 根据ID获取电影详情
     * 
     * @param id 电影ID
     * @return 电影详情响应
     */
    @Override
    public MovieResponse getMovieById(Long id) {
        String cacheKey = RedisKey.movieDetail(id);
        
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            return (MovieResponse) cached;
        }

        Movie movie = movieMapper.findById(id);
        if (movie == null) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "电影不存在");
        }

        MovieResponse response = MovieResponse.fromEntity(movie);
        redisService.set(cacheKey, response, 5, TimeUnit.MINUTES);
        return response;
    }

    /**
     * 根据关键词搜索电影
     * 
     * @param keyword 搜索关键词（电影标题）
     * @return 匹配的电影列表
     */
    @Override
    public List<MovieResponse> searchMovies(String keyword) {
        List<Movie> movies = movieMapper.findByTitle(keyword);
        return movies.stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取正在上映的电影
     * 
     * @return 正在上映的电影列表
     */
    @Override
    public List<MovieResponse> getShowingMovies() {
        // 先从Redis缓存中获取
        Object cached = redisService.get(RedisKey.MOVIE_SHOWING);
        if (cached != null) {
            return (List<MovieResponse>) cached;
        }

        List<Movie> movies = movieMapper.findShowingMoviesWithValidSchedule();
        List<MovieResponse> responses = movies.stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());

        redisService.set(RedisKey.MOVIE_SHOWING, responses, 10, TimeUnit.MINUTES);
        return responses;
    }

    /**
     * 添加电影
     * 
     * @param movie 电影实体
     * @return 添加后的电影响应
     */
    @Override
    public MovieResponse addMovie(Movie movie) {
        // 设置电影状态为"正在上映"
        movie.setStatus("showing");
        movieMapper.insert(movie);

        // 清除相关缓存，确保数据一致性
        clearMovieCache();
        return MovieResponse.fromEntity(movie);
    }

    /**
     * 更新电影信息
     * 
     * @param movie 更新后的电影实体
     * @return 更新后的电影响应
     */
    @Override
    public MovieResponse updateMovie(Movie movie) {
        // 检查电影是否存在
        Movie existing = movieMapper.findById(movie.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "电影不存在");
        }

        // 更新电影信息
        movieMapper.update(movie);

        clearMovieCache();
        redisService.delete(RedisKey.movieDetail(movie.getId()));
        return MovieResponse.fromEntity(movie);
    }

    /**
     * 删除电影
     * 
     * @param id 电影ID
     */
    @Override
    public void deleteMovie(Long id) {
        // 检查电影是否存在
        Movie movie = movieMapper.findById(id);
        if (movie == null) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "电影不存在");
        }

        // 删除电影
        movieMapper.deleteById(id);

        clearMovieCache();
        redisService.delete(RedisKey.movieDetail(id));
    }

    /**
     * 清除电影相关缓存（私有方法）
     */
    private void clearMovieCache() {
        redisService.delete(RedisKey.MOVIE_LIST);
        redisService.delete(RedisKey.MOVIE_SHOWING);
    }

    /**
     * 分页获取电影列表
     * 
     * 【设计说明】
     * 支持按状态筛选，分页查询避免一次性加载所有数据
     * 
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param status 状态筛选（可选）
     * @return 分页响应
     */
    @Override
    public PageResponse<MovieResponse> getMoviesWithPagination(int page, int size, String status) {
        int offset = page * size;
        List<Movie> movies;
        long total;

        if (status != null && !status.isEmpty()) {
            movies = movieMapper.findByStatusWithPagination(status, offset, size);
            total = movieMapper.countByStatus(status);
        } else {
            movies = movieMapper.findAllWithPagination(offset, size);
            total = movieMapper.countAll();
        }

        List<MovieResponse> responses = movies.stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(responses, total, page, size);
    }

    /**
     * 分页搜索电影
     * 
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 分页响应
     */
    @Override
    public PageResponse<MovieResponse> searchMoviesWithPagination(String keyword, int page, int size) {
        int offset = page * size;
        List<Movie> movies = movieMapper.findByTitleWithPagination(keyword, offset, size);
        long total = movieMapper.countByTitle(keyword);

        List<MovieResponse> responses = movies.stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(responses, total, page, size);
    }
}