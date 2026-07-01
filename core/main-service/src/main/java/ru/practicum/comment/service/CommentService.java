package ru.practicum.comment.service;

import org.springframework.data.domain.Page;
import ru.practicum.comment.dto.*;
import ru.practicum.comment.enums.CommentsSortType;

import java.util.List;

public interface CommentService {

    CommentDto create(CommentRequestDto commentRequestDto, Long commentatorId, Long eventId);

    CommentDto update(CommentRequestDto commentRequestDto, Long eventId, Long commentatorId, Long commentId);

    void delete(Long eventId, Long commentId, Long commentatorId);

    void deleteByAdmin(Long commentId);

    Page<CommentResponseDto> getCommentsByEvent(Long eventId, String  sort, int from, int size);

    ReactionResponseDto addVote(Long evaluatorId, Long commentId, String voteType);

    CommentStatsResponse getReactionStatsByComment(Long commentId);

    List<CommentResponseDto> getCommentsBy(CommentsSortType sort, String direction, Integer from, Integer size);
}