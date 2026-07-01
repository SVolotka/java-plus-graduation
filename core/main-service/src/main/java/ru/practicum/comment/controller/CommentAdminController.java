package ru.practicum.comment.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.service.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "admin/comments/{commentId}")
@Validated
public class CommentAdminController {
    private final CommentService commentService;

    @DeleteMapping
    public ResponseEntity<Void> delete(@NotNull @Positive @PathVariable("commentId") Long commentId) {
        commentService.deleteByAdmin(commentId);
        return ResponseEntity.noContent().build();
    }
}