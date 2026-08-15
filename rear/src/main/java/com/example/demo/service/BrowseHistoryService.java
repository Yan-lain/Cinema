package com.example.demo.service;

import java.util.Map;

public interface BrowseHistoryService {
    void addBrowseHistory(Long userId, Long movieId);
    Map<String, Object> getBrowseHistory(Long userId, Integer limit);
    int getBrowseCount(Long userId);
    void deleteById(Long id);
    void clearBrowseHistory(Long userId);
}
