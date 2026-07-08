package ru.yandex.practicum.core.commentService.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.common.comment.dto.CommentDto;
import ru.yandex.practicum.common.comment.dto.CommentResponseDto;
import ru.yandex.practicum.common.comment.dto.CommentStatsResponse;
import ru.yandex.practicum.common.comment.dto.ReactionResponseDto;
import ru.yandex.practicum.common.comment.enums.CommentsSortType;
import ru.yandex.practicum.common.comment.enums.DirectionSortType;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;
import ru.yandex.practicum.common.exception.ConflictException;
import ru.yandex.practicum.common.exception.NotFoundException;
import ru.yandex.practicum.common.feignClient.EventClient;
import ru.yandex.practicum.common.feignClient.UserClient;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.core.commentService.dto.CommentRequestDto;
import ru.yandex.practicum.core.commentService.entity.Comment;
import ru.yandex.practicum.core.commentService.entity.Reaction;
import ru.yandex.practicum.core.commentService.mapper.CommentMapper;
import ru.yandex.practicum.core.commentService.mapper.ReactionMapper;
import ru.yandex.practicum.core.commentService.repository.CommentRepository;
import ru.yandex.practicum.core.commentService.repository.ReactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final CommentMapper commentMapper;
    private final ReactionMapper reactionMapper;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final TransactionTemplate transactionTemplate;

    private UserDto getUserById(Long userId) {
        try {
            return userClient.getById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        } catch (FeignException e) {
            log.error("Ошибка при запросе пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException("Сервис пользователей недоступен", e);
        }
    }

    private EventFullDto getEventById(Long eventId) {
        try {
            return eventClient.getEventById(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено");
        } catch (FeignException e) {
            log.error("Ошибка при запросе события {}: {}", eventId, e.getMessage());
            throw new RuntimeException("Сервис событий недоступен", e);
        }
    }

    @Override
    public CommentDto create(CommentRequestDto commentRequestDto, Long commentatorId, Long eventId) {
        UserDto userDto = getUserById(commentatorId);
        EventFullDto event = getEventById(eventId);

        Comment comment = transactionTemplate.execute(status -> {
            Comment commentCreate = Comment.builder()
                    .commentatorId(commentatorId)
                    .eventId(eventId)
                    .created(LocalDateTime.now())
                    .text(commentRequestDto.getText())
                    .build();
            return commentRepository.save(commentCreate);
        });
        return commentMapper.toCommentDto(comment);
    }

    @Override
    public CommentDto update(CommentRequestDto commentRequestDto, Long eventId, Long commentatorId, Long commentId) {
        Comment commentFindById = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("комментарий с id = " + commentId + " не найден"));
        if (!commentFindById.getCommentatorId().equals(commentatorId)) {
            throw new ConflictException("редактировать комментарий может только его автор");
        }
        commentFindById.setText(commentRequestDto.getText());
        Comment updated = transactionTemplate.execute(status -> commentRepository.save(commentFindById));
        return commentMapper.toCommentDto(updated);
    }

    @Override
    public void delete(Long eventId, Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("комментария с id = " + commentId + " не существует"));
        if (!eventId.equals(comment.getEventId())) {
            throw new ConflictException("Комментарий не относится к событию");
        }
        EventFullDto event = getEventById(eventId);
        boolean isAuthor = comment.getCommentatorId().equals(userId);
        boolean isEventInitiator = event.getInitiator().getId().equals(userId);
        if (!isAuthor && !isEventInitiator) {
            throw new ConflictException("комментарий может удалять только его автор или инициатор события");
        }
        transactionTemplate.executeWithoutResult(status -> commentRepository.deleteById(commentId));
    }

    @Override
    public void deleteByAdmin(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("комментария с id = " + commentId + " не существует");
        }
        transactionTemplate.executeWithoutResult(status -> commentRepository.deleteById(commentId));
    }

    @Override
    public Page<CommentResponseDto> getCommentsByEvent(Long eventId, String sortOrder, int from, int size) {
        getEventById(eventId); // проверка существования события
        Sort sort = Sort.by("created");
        if ("desc".equalsIgnoreCase(sortOrder)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }
        Pageable pageable = PageRequest.of(from, size, sort);
        Page<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);
        return comments.map(commentMapper::toCommentResponseDto);
    }

    @Override
    public ReactionResponseDto addVote(Long evaluatorId, Long commentId, String voteType) {
        UserDto evaluator = getUserById(evaluatorId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id = " + commentId + " не найден"));
        Reaction reaction = transactionTemplate.execute(status -> {
            Optional<Reaction> reactionOpt = reactionRepository.existByUserAndComment(evaluatorId, commentId);
            if (reactionOpt.isPresent()) {
                Reaction r = reactionOpt.get();
                if (!r.getVoteType().equals(voteType)) {
                    r.setVoteType(voteType);
                    return reactionRepository.save(r);
                }
                return r;
            } else {
                Reaction newReaction = Reaction.builder()
                        .voteType(voteType)
                        .evaluatorId(evaluatorId)
                        .commentId(commentId)
                        .build();
                return reactionRepository.save(newReaction);
            }
        });
        CommentResponseDto commentResponseDto = commentMapper.toCommentResponseDto(comment);
        return reactionMapper.toReactionResponseDto(reaction, evaluator, commentResponseDto);
    }

    @Override
    public CommentStatsResponse getReactionStatsByComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("комментария с id = " + commentId + " не существует");
        }
        List<Object[]> results = reactionRepository.getLikesAndDislikesCount(commentId);
        long likes = 0, dislikes = 0;
        for (Object[] row : results) {
            String vt = (String) row[0];
            Long cnt = (Long) row[1];
            if (vt.equals(CommentsSortType.LIKE.name())) likes = cnt;
            else if (vt.equals(CommentsSortType.DISLIKE.name())) dislikes = cnt;
        }
        return new CommentStatsResponse(commentId, likes, dislikes);
    }

    @Override
    public List<CommentResponseDto> getCommentsBy(CommentsSortType sort, String direction, Integer from, Integer size) {
        List<Long> commentIds;
        if (direction.equalsIgnoreCase(DirectionSortType.ASC.name())) {
            commentIds = reactionRepository.findCommentIdsByVoteTypeAsc(String.valueOf(sort));
        } else {
            commentIds = reactionRepository.findCommentIdsByVoteTypeDesc(String.valueOf(sort));
        }
        int start = Math.min(from, commentIds.size());
        int end = Math.min(from + size, commentIds.size());
        if (start >= end) return List.of();
        List<Long> pageIds = commentIds.subList(start, end);
        List<Comment> comments = commentRepository.findAllById(pageIds);
        Map<Long, Comment> commentMap = comments.stream()
                .collect(Collectors.toMap(Comment::getId, Function.identity()));
        return pageIds.stream()
                .map(commentMap::get)
                .filter(Objects::nonNull)
                .map(commentMapper::toCommentResponseDto)
                .collect(Collectors.toList());
    }
}