package com.example.demo.service;


import java.util.Map;

public interface FavoriteService {
    void addFavorite(Long userId, Long movieId);
    void removeFavorite(Long userId, Long movieId);
    void deleteById(Long id);
    Map<String, Object> getFavoriteList(Long userId, Integer limit);
    int getFavoriteCount(Long userId);
    Map<String, Object> checkFavorite(Long userId, Long movieId);
}
