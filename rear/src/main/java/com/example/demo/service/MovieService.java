package com.example.demo.service;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.response.MovieResponse;
import com.example.demo.entity.Movie;

import java.util.List;

public interface MovieService {
    List<MovieResponse> getAllMovies();

    MovieResponse getMovieById(Long id);

    List<MovieResponse> searchMovies(String keyword);

    List<MovieResponse> getShowingMovies();

    MovieResponse addMovie(Movie movie);

    MovieResponse updateMovie(Movie movie);

    void deleteMovie(Long id);

    PageResponse<MovieResponse> getMoviesWithPagination(int page, int size, String status);

    PageResponse<MovieResponse> searchMoviesWithPagination(String keyword, int page, int size);
}