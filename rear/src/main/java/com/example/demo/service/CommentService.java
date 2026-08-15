package com.example.demo.service;

import com.example.demo.entity.Comment;

import java.util.List;
import java.util.Map;

public interface CommentService {
    List<Map<String, Object>> getCommentsByMovieId(Long movieId);
    Comment addComment(Comment comment);
    List<Map<String, Object>> getCommentsByUserId(Long userId);
    void deleteComment(Long commentId, Long userId);
}
