package ru.practicum.comment.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.dto.CommentResponseDto;
import ru.practicum.comment.service.CommentService;


@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/events/{eventId}/comments")
@Validated
public class CommentPublicController {
    private final CommentService commentService;

    @GetMapping
    ResponseEntity<Page<CommentResponseDto>> getCommentsByEvent(
            @NotNull @Positive @PathVariable Long eventId,
            @RequestParam(required = false, defaultValue = "asc") String sort,
            @RequestParam(required = false, defaultValue = "0") int from,
            @RequestParam(required = false, defaultValue = "10") int size) {
        Page<CommentResponseDto> comments = commentService.getCommentsByEvent(eventId, sort, from, size);
        return ResponseEntity.ok(comments);
    }
}