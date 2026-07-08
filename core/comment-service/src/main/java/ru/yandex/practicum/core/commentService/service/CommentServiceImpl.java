package ru.yandex.practicum.core.commentService.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final CommentMapper commentMapper;
    private final ReactionMapper reactionMapper;
    private final UserClient userClient;
    private final EventClient eventClient;

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
    @Transactional
    public CommentDto create(CommentRequestDto commentRequestDto, Long commentatorId, Long eventId) {
        log.info("Создание комментария: commentatorId={}, eventId={}", commentatorId, eventId);

        Comment commentCreate = Comment.builder()
                .commentatorId(commentatorId)
                .eventId(eventId)
                .created(LocalDateTime.now())
                .text(commentRequestDto.getText())
                .build();

        Comment commentSaved = commentRepository.save(commentCreate);
        log.info("Комментарий успешно создан с id: {}", commentSaved.getId());

        return commentMapper.toCommentDto(commentSaved);
    }

    @Override
    @Transactional
    public CommentDto update(CommentRequestDto commentRequestDto, Long eventId, Long commentatorId, Long commentId) {
        log.info("Обновление комментария: commentId={}, eventId={}, commentatorId={}", commentId, eventId, commentatorId);

        Comment commentFindById = commentRepository.findById(commentId).orElseThrow(() -> {
            log.warn("Комментарий с id={} не найден для обновления", commentId);
            return new NotFoundException("комментарий с id = " + commentId + " не найден");
        });

        if (!commentFindById.getCommentatorId().equals(commentatorId)) {
            throw new ConflictException("редактировать комментарий может только его автор");
        }

        commentFindById.setText(commentRequestDto.getText());
        Comment commentUpdate = commentRepository.save(commentFindById);
        log.info("Комментарий с id={} успешно обновлён", commentUpdate.getId());

        return commentMapper.toCommentDto(commentUpdate);
    }

    @Override
    @Transactional
    public void delete(Long eventId, Long commentId, Long userId) {
        log.info("Удаление комментария пользователем: eventId={}, commentId={}, userId={}", eventId, commentId, userId);

        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> {
            log.warn("Комментарий с id={} не найден при удалении", commentId);
            return new NotFoundException("комментария с id = " + commentId + " не существует");
        });

        if (!eventId.equals(comment.getEventId())) {
            throw new ConflictException("Комментарий с id = " + commentId + " не относится к событию с id = " + eventId);
        }

        EventFullDto event = getEventById(eventId);

        boolean isAuthor = comment.getCommentatorId().equals(userId);
        boolean isEventInitiator = event.getInitiator().getId().equals(userId);

        if (!isAuthor && !isEventInitiator) {
            throw new ConflictException("комментарий может удалять только его автор или инициатор события");
        }

        commentRepository.deleteById(commentId);
        log.info("Комментарий с id={} успешно удалён пользователем id={}", commentId, userId);
    }

    @Override
    @Transactional
    public void deleteByAdmin(Long commentId) {
        log.info("Удаление комментария администратором: commentId={}", commentId);

        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("комментария с id = " + commentId + " не существует");
        }

        commentRepository.deleteById(commentId);
        log.info("Комментарий с id={} успешно удалён администратором", commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getCommentsByEvent(Long eventId, String sortOrder, int from, int size) {
        log.info("Запрос комментариев события: eventId={}, sortOrder={}, from={}, size={}", eventId, sortOrder, from, size);

        getEventById(eventId);

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
    @Transactional
    public ReactionResponseDto addVote(Long evaluatorId, Long commentId, String voteType) {
        log.info("Добавление реакции: evaluatorId={}, commentId={}, voteType={}", evaluatorId, commentId, voteType);

        UserDto evaluator = getUserById(evaluatorId);

        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> {
            log.warn("Комментарий с id={} не найден при добавлении реакции", commentId);
            return new NotFoundException("Комментарий с id = " + commentId + " не найден");
        });

        Optional<Reaction> reactionOpt = reactionRepository.existByUserAndComment(evaluatorId, commentId);

        Reaction reaction;
        if (reactionOpt.isPresent()) {
            reaction = reactionOpt.get();
            if (!reaction.getVoteType().equals(voteType)) {
                reaction.setVoteType(voteType);
                reaction = reactionRepository.save(reaction);
                log.info("Реакция обновлена: id={}, voteType={}", reaction.getId(), voteType);
            }
        } else {
            reaction = Reaction.builder()
                    .voteType(voteType)
                    .evaluatorId(evaluatorId)
                    .commentId(commentId)
                    .build();
            reaction = reactionRepository.save(reaction);
            log.info("Реакция создана: id={}, voteType={}", reaction.getId(), voteType);
        }

        CommentResponseDto commentResponseDto = commentMapper.toCommentResponseDto(comment);
        return reactionMapper.toReactionResponseDto(reaction, evaluator, commentResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentStatsResponse getReactionStatsByComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("комментария с id = " + commentId + " не существует");
        }

        List<Object[]> results = reactionRepository.getLikesAndDislikesCount(commentId);

        long likes = 0;
        long dislikes = 0;

        for (Object[] row : results) {
            String voteType = (String) row[0];
            Long count = (Long) row[1];

            if (voteType.equals(CommentsSortType.LIKE.name())) {
                likes = count;
            } else if (voteType.equals(CommentsSortType.DISLIKE.name())) {
                dislikes = count;
            }
        }

        return new CommentStatsResponse(commentId, likes, dislikes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsBy(CommentsSortType sort, String direction, Integer from, Integer size) {
        List<Long> commentIds;
        if (direction.equalsIgnoreCase(DirectionSortType.ASC.name())) {
            commentIds = reactionRepository.findCommentIdsByVoteTypeAsc(String.valueOf(sort));
        } else {
            commentIds = reactionRepository.findCommentIdsByVoteTypeDesc(String.valueOf(sort));
        }

        int start = Math.min(from, commentIds.size());
        int end = Math.min(from + size, commentIds.size());
        if (start >= end) {
            return Collections.emptyList();
        }
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