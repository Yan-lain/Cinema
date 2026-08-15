package com.example.demo.service.impl;

import com.example.demo.entity.Favorite;
import com.example.demo.mapper.FavoriteMapper;
import com.example.demo.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Override
    public void addFavorite(Long userId, Long movieId) {
        Favorite existing = favoriteMapper.findByUserAndMovie(userId, movieId);
        if (existing != null) {
            throw new RuntimeException("已收藏过该电影");
        }
        Favorite favorite = new Favorite(userId, movieId);
        favoriteMapper.insert(favorite);
    }

    @Override
    public void removeFavorite(Long userId, Long movieId) {
        int affected = favoriteMapper.deleteByUserAndMovie(userId, movieId);
        if (affected == 0) {
            throw new RuntimeException("收藏记录不存在");
        }
    }

    @Override
    public void deleteById(Long id) {
        int affected = favoriteMapper.deleteById(id);
        if (affected == 0) {
            throw new RuntimeException("记录不存在");
        }
    }

    @Override
    public Map<String, Object> getFavoriteList(Long userId, Integer limit) {
        List<Map<String, Object>> list = favoriteMapper.findWithUserAndMovie(userId, limit);
        int totalCount = favoriteMapper.countByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("total", totalCount);
        result.put("data", list);
        return result;
    }

    @Override
    public int getFavoriteCount(Long userId) {
        return favoriteMapper.countByUserId(userId);
    }

    @Override
    public Map<String, Object> checkFavorite(Long userId, Long movieId) {
        Favorite favorite = favoriteMapper.findByUserAndMovie(userId, movieId);
        Map<String, Object> result = new HashMap<>();
        result.put("isFavorite", favorite != null);
        if (favorite != null) {
            result.put("favoriteId", favorite.getId());
        }
        return result;
    }
}
