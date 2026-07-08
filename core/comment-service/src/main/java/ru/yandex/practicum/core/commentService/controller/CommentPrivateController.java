package ru.yandex.practicum.core.commentService.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.common.comment.dto.CommentDto;
import ru.yandex.practicum.core.commentService.dto.CommentRequestDto;
import ru.yandex.practicum.core.commentService.service.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/events/{eventId}/users/{commentatorId}/comment")
@Validated
public class CommentPrivateController {
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDto> create(
            @Valid @RequestBody CommentRequestDto commentRequestDto,
            @NotNull @Positive @PathVariable("eventId") Long eventId,
            @NotNull @Positive @PathVariable("commentatorId") Long commentatorId) {
        CommentDto commentDto = commentService.create(commentRequestDto, commentatorId, eventId);
        return ResponseEntity.ok(commentDto);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> update(
            @Valid @RequestBody CommentRequestDto commentRequestDto,
            @NotNull @Positive @PathVariable("eventId") Long eventId,
            @NotNull @Positive @PathVariable("commentatorId") Long commentatorId,
            @NotNull @Positive @PathVariable("commentId") Long commentId) {
        CommentDto commentDto = commentService.update(commentRequestDto, eventId, commentatorId, commentId);
        return ResponseEntity.ok(commentDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @NotNull @Positive @PathVariable("eventId") Long eventId,
            @NotNull @Positive @PathVariable("commentatorId") Long commentatorId,
            @NotNull @Positive @PathVariable("commentId") Long commentId) {
        commentService.delete(eventId, commentId, commentatorId);
    }
}