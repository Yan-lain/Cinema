package com.example.demo.service.impl;

import com.example.demo.entity.BrowseHistory;
import com.example.demo.mapper.BrowseHistoryMapper;
import com.example.demo.service.BrowseHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    /**
     * 添加浏览历史
     * 【技术说明】用于添加用户浏览历史记录
     * 【功能说明】根据请求体中的参数，添加用户浏览历史记录
     * 【依赖说明】依赖BrowseHistoryMapper，用于数据库操作
     * 【接口说明】提供POST方法，用于处理添加浏览历史请求
     * 【返回值说明】返回JSON格式的响应体，包含状态码、消息等
     * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
     * 【异常说明】处理可能的异常情况，如用户不存在等
     * */
    @Override
    public void addBrowseHistory(Long userId, Long movieId) {
        browseHistoryMapper.deleteByUserAndMovie(userId, movieId);
        BrowseHistory history = new BrowseHistory(userId, movieId);
        browseHistoryMapper.insert(history);
    }

    /**
     * 获取浏览历史
     * 【技术说明】用于获取用户浏览历史记录
     * 【功能说明】根据请求体中的参数，获取用户浏览历史记录
     * 【依赖说明】依赖BrowseHistoryMapper，用于数据库操作
     * 【接口说明】提供GET方法，用于处理获取浏览历史请求
     * 【返回值说明】返回JSON格式的响应体，包含状态码、消息等
     * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
     * 【异常说明】处理可能的异常情况，如用户不存在等
     * */
    @Override
    public Map<String, Object> getBrowseHistory(Long userId, Integer limit) {
        List<Map<String, Object>> historyList = browseHistoryMapper.findWithUserAndMovie(userId, limit);
        int totalCount = browseHistoryMapper.countByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("total", totalCount);
        result.put("data", historyList);
        return result;
    }

    /**
     * 获取浏览历史记录数量
     * 【技术说明】用于获取用户浏览历史记录数量
     * 【功能说明】根据请求体中的参数，获取用户浏览历史记录数量
     * 【依赖说明】依赖BrowseHistoryMapper，用于数据库操作
     * 【接口说明】提供GET方法，用于处理获取浏览历史记录数量请求
     * 【返回值说明】返回JSON格式的响应体，包含状态码、消息等
     * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
     * 【异常说明】处理可能的异常情况，如用户不存在等
     * */
    @Override
    public int getBrowseCount(Long userId) {
        return browseHistoryMapper.countByUserId(userId);
    }

    /**
     * 删除浏览历史记录
     * 【技术说明】用于删除用户浏览历史记录
     * 【功能说明】根据请求体中的参数，删除用户浏览历史记录
     * 【依赖说明】依赖BrowseHistoryMapper，用于数据库操作
     * 【接口说明】提供DELETE方法，用于处理删除浏览历史记录请求
     * 【返回值说明】返回JSON格式的响应体，包含状态码、消息等
     * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
     * 【异常说明】处理可能的异常情况，如记录不存在等
     * */
    @Override
    public void deleteById(Long id) {
        // 这个deleteById是根据id删除记录，affected表示删除的行数
        int affected = browseHistoryMapper.deleteById(id);
        if (affected == 0) {
            throw new RuntimeException("记录不存在");
        }
    }

    /**
     * 清空浏览历史记录
     * 【技术说明】用于清空用户浏览历史记录
     * 【功能说明】根据请求体中的参数，清空用户浏览历史记录
     * 【依赖说明】依赖BrowseHistoryMapper，用于数据库操作
     * 【接口说明】提供DELETE方法，用于处理清空浏览历史记录请求
     * 【返回值说明】返回JSON格式的响应体，包含状态码、消息等
     * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
     * 【异常说明】处理可能的异常情况，如用户不存在等
     * */
    @Override
    public void clearBrowseHistory(Long userId) {
        browseHistoryMapper.deleteByUserId(userId);
    }
}
