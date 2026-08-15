package com.example.demo.dto.response;

import com.example.demo.entity.Movie;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Schema(description = "电影信息响应")
public class MovieResponse {
    @Schema(description = "电影 ID")
    private Long id;
    @Schema(description = "电影标题")
    private String title;
    @Schema(description = "导演")
    private String director;
    @Schema(description = "主演")
    private String cast;
    @Schema(description = "类型（如：科幻、爱情、动作）")
    private String genre;
    @Schema(description = "海报 URL")
    private String poster;
    @Schema(description = "评分")
    private BigDecimal rating;
    @Schema(description = "状态（showing/upcoming/offline）")
    private String status;
    @Schema(description = "时长（分钟）")
    private Integer duration;
    @Schema(description = "剧情简介")
    private String description;
    @Schema(description = "上映时间")
    private LocalDateTime releaseDate;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 从实体类转换为响应对象
     *
     * @param movie 电影实体
     * @return 电影 DTO
     */
    public static MovieResponse fromEntity(Movie movie) {
        MovieResponse response = new MovieResponse();
        response.setId(movie.getId());
        response.setTitle(movie.getTitle());
        response.setDirector(movie.getDirector());
        response.setCast(movie.getCast());
        response.setGenre(movie.getGenre());
        response.setPoster(movie.getPoster());
        response.setRating(movie.getRating());
        response.setStatus(movie.getStatus());
        response.setDuration(movie.getDuration());
        response.setDescription(movie.getDescription());
        response.setReleaseDate(movie.getReleaseDate());
        response.setCreateTime(movie.getCreatedAt());
        response.setUpdateTime(movie.getUpdatedAt());
        return response;
    }
    
    /**
     * 从实体类列表转换为响应对象列表
     *
     * @param movies 电影实体列表
     * @return 电影 DTO列表
     */
    public static List<MovieResponse> fromEntities(List<Movie> movies) {
        return movies.stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }
}