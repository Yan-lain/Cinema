package com.example.demo.controller;

import com.example.demo.entity.Comment;
import com.example.demo.entity.User;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "http://localhost:5173")
public class CommentController {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/movie/{movieId}")
    public Map<String, Object> getCommentsByMovieId(@PathVariable Long movieId) {
        Map<String, Object> result = new HashMap<>();
        try {
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
                    // 优先使用昵称，如果昵称未设置则使用用户名
                    String displayName = user.getNickname() != null && !user.getNickname().isEmpty() 
                        ? user.getNickname() 
                        : user.getUsername();
                    commentData.put("username", displayName);
                } else {
                    commentData.put("username", "匿名用户");
                }
                commentWithUsers.add(commentData);
            }

            result.put("success", true);
            result.put("data", commentWithUsers);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取评论失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping
    public Map<String, Object> addComment(@RequestBody Comment comment) {
        Map<String, Object> result = new HashMap<>();
        try {
            commentMapper.insert(comment);

            result.put("success", true);
            result.put("message", "评论添加成功");
            result.put("data", comment);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "评论添加失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/user/{userId}")
    public Map<String, Object> getCommentsByUserId(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
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

            result.put("success", true);
            result.put("data", commentWithMovies);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取评论失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{commentId}")
    public Map<String, Object> deleteComment(@PathVariable Long commentId, @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }
            
            if (!comment.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "只能删除自己的评论");
                return result;
            }
            
            commentMapper.deleteById(commentId);
            
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }
        return result;
    }
}
