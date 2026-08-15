package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.entity.Comment;
import com.example.demo.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/movie/{movieId}")
    public ApiResponse<List<Map<String, Object>>> getCommentsByMovieId(@PathVariable Long movieId) {
        List<Map<String, Object>> comments = commentService.getCommentsByMovieId(movieId);
        return ApiResponse.success(comments);
    }

    @PostMapping
    public ApiResponse<Comment> addComment(@RequestBody Comment comment) {
        Comment savedComment = commentService.addComment(comment);
        return ApiResponse.success("评论添加成功", savedComment);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Map<String, Object>>> getCommentsByUserId(@PathVariable Long userId) {
        List<Map<String, Object>> comments = commentService.getCommentsByUserId(userId);
        return ApiResponse.success(comments);
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId, @RequestParam Long userId) {
        try {
            commentService.deleteComment(commentId, userId);
            return ApiResponse.success("删除成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }
}
