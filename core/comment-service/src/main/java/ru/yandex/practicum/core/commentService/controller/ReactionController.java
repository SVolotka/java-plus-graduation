package ru.yandex.practicum.core.commentService.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.common.comment.dto.CommentResponseDto;
import ru.yandex.practicum.common.comment.dto.CommentStatsResponse;
import ru.yandex.practicum.common.comment.dto.ReactionResponseDto;
import ru.yandex.practicum.common.comment.enums.CommentsSortType;
import ru.yandex.practicum.core.commentService.service.CommentService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class ReactionController {

    private final CommentService commentService;

    @PostMapping("/{commentId}/users/{evaluatorId}/vote")
    public ResponseEntity<ReactionResponseDto> addVote(
            @NotNull @Positive @PathVariable Long commentId,
            @NotNull @Positive @PathVariable Long evaluatorId,
            @NotBlank @Pattern(regexp = "^(LIKE|DISLIKE)$") @RequestParam String voteType) {
        ReactionResponseDto reactionResponseDto = commentService.addVote(evaluatorId, commentId, voteType);
        return ResponseEntity.ok(reactionResponseDto);
    }

    @GetMapping("/{commentId}/stats")
    public ResponseEntity<CommentStatsResponse> getReactionStatsByComment(
            @NotNull @Positive @PathVariable Long commentId) {
        CommentStatsResponse commentStatsResponse = commentService.getReactionStatsByComment(commentId);
        return ResponseEntity.ok(commentStatsResponse);
    }

    @GetMapping("/rating")
    public ResponseEntity<List<CommentResponseDto>> getRatingBy(
            @RequestParam(required = false, defaultValue = "LIKE") CommentsSortType sort,
            @RequestParam(required = false, defaultValue = "ASC") String direction,
            @RequestParam(required = false, defaultValue = "0") Integer from,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        List<CommentResponseDto> responseDtoList = commentService.getCommentsBy(sort, direction, from, size);
        return ResponseEntity.ok(responseDtoList);
    }
}