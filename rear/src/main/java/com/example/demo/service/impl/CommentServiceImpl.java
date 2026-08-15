package com.example.demo.service.impl;

import com.example.demo.entity.Comment;
import com.example.demo.entity.User;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 根据电影ID查询评论
     * 【技术说明】用于根据电影ID查询所有评论
     * 【功能说明】根据请求体中的参数，查询指定电影的所有评论
     * 【依赖说明】依赖CommentMapper，用于数据库操作
     * 【接口说明】提供GET方法，用于处理根据电影ID查询评论请求
     * 【返回值说明】返回JSON格式的响应体，包含状态码、消息、数据等
     * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
     * 【异常说明】处理可能的异常情况，如电影不存在等
     * */
    @Override
    public List<Map<String, Object>> getCommentsByMovieId(Long movieId) {
        List<Comment> comments = commentMapper.findByMovieId(movieId);
        List<Map<String, Object>> commentWithUsers = new ArrayList<>();

        for (Comment comment : comments) {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("id", comment.getId());
            commentData.put("userId", comment.getUserId());
            commentData.put("movieId", comment.getMovieId());
            commentData.put("rating", comment.getRating());
            commentData.put("content", comment.getContent());
            commentData.put("createdAt", comment.getCreatedAt());

            User user = userMapper.findById(comment.getUserId());
            if (user != null) {
                // 如果用户存在，使用用户昵称或用户名
                String displayName = user.getNickname() != null && !user.getNickname().isEmpty()
                        ? user.getNickname()
                        : user.getUsername();
                commentData.put("username", displayName);
            } else {
                // 如果用户不存在，使用默认的匿名用户名
                //用户账号已被删除：用户账号已被删除后，评论记录会保留在数据库中，但是用户信息会被删除
                commentData.put("username", "匿名用户");
            }
            commentWithUsers.add(commentData);
        }
        return commentWithUsers;
    }

    @Override
    public Comment addComment(Comment comment) {
        commentMapper.insert(comment);
        return comment;
    }

    @Override
    public List<Map<String, Object>> getCommentsByUserId(Long userId) {
        List<Comment> comments = commentMapper.findByUserId(userId);
        List<Map<String, Object>> commentWithMovies = new ArrayList<>();

        for (Comment comment : comments) {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("id", comment.getId());
            commentData.put("userId", comment.getUserId());
            commentData.put("movieId", comment.getMovieId());
            commentData.put("rating", comment.getRating());
            commentData.put("content", comment.getContent());
            commentData.put("createdAt", comment.getCreatedAt());
            commentWithMovies.add(commentData);
        }
        return commentWithMovies;
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);
    }
}
