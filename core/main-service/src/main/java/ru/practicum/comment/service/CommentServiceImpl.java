package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.*;
import ru.practicum.comment.entity.Comment;
import ru.practicum.comment.entity.Reaction;
import ru.practicum.comment.enums.CommentsSortType;
import ru.practicum.comment.enums.DirectionSortType;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.mapper.ReactionMapper;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.comment.repository.ReactionRepository;
import ru.practicum.event.entity.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.entity.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;
    private final ReactionRepository reactionRepository;
    private final ReactionMapper reactionMapper;

    @Override
    @Transactional
    public CommentDto create(CommentRequestDto commentRequestDto, Long commentatorId, Long eventId) {
        log.info("Создание комментария: commentatorId={}, eventId={}",
                commentatorId, eventId);

        User commentator = userRepository.findById(commentatorId).orElseThrow(() -> {
            log.warn("Пользователь с id={} не найден при создании комментария", commentatorId);
            return new NotFoundException("Пользователь с id = " + commentatorId + " не найден");
        });

        Event event = eventRepository.findById(eventId).orElseThrow(() -> {
            log.warn("Событие с id={} не найдено при создании комментария", eventId);
            return new NotFoundException("мероприятия с id = " + eventId + " не существует");
        });

        Comment commentCreate = Comment.builder()
                .commentator(commentator)
                .event(event)
                .created(LocalDateTime.now())
                .text(commentRequestDto.getText())
                .build();

        Comment commentSaved = commentRepository.save(commentCreate);
        log.info("Комментарий успешно создан с id: {}", commentSaved.getId());

        return commentMapper.toCommentDto(commentSaved);
    }

    @Override
    @Transactional
    public CommentDto update(
            CommentRequestDto commentRequestDto,
            Long eventId,
            Long commentatorId,
            Long commentId) {
        log.info("Обновление комментария: commentId={}, eventId={}, commentatorId={}",
                commentId, eventId, commentatorId);

        Comment commentFindById = commentRepository.findById(commentId).orElseThrow(() -> {
            log.warn("Комментарий с id={} не найден для обновления", commentId);
            return new NotFoundException("комментарий с id = " + commentId + " не найден");
        });

        if (!commentFindById.getCommentator().getId().equals(commentatorId)) {
            log.warn("Попытка редактирования чужого комментария: commentId={}, commentatorId={}, автор={}",
                    commentId, commentatorId, commentFindById.getCommentator().getId());
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
        log.info("Удаление комментария пользователем: eventId={}, commentId={}, userId={}",
                eventId, commentId, userId);

        Event event = eventRepository.findById(eventId).orElseThrow(() -> {
            log.warn("Событие с id={} не найдено при удалении комментария", eventId);
            return new NotFoundException("мероприятия с id = " + eventId + " не существует");
        });

        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> {
            log.warn("Комментарий с id={} не найден при удалении", commentId);
            return new NotFoundException("комментария с id = " + commentId + " не существует");
        });

        if (!eventId.equals(comment.getEvent().getId())) {
            log.warn("Комментарий id={} не относится к событию id={}", commentId, eventId);
            throw new ConflictException("Комментарий с id = " + commentId + " не относится к событию с id = " + eventId);
        }

        if (!comment.getCommentator().getId().equals(userId) && !event.getInitiator().getId().equals(userId)) {
            log.warn("Пользователь id={} не является автором и не инициатор события для комментария id={}",
                    userId, commentId);
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
            log.warn("Комментарий с id={} не найден при удалении администратором", commentId);
            throw new NotFoundException("комментария с id = " + commentId + " не существует");
        }

        commentRepository.deleteById(commentId);
        log.info("Комментарий с id={} успешно удалён администратором", commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getCommentsByEvent(Long eventId, String sortOrder, int from, int size) {
        log.info("Запрос комментариев события: eventId={}, sortOrder={}, from={}, size={}",
                eventId, sortOrder, from, size);

        if (!eventRepository.existsById(eventId)) {
            log.warn("Событие с id={} не найдено при запросе комментариев", eventId);
            throw new NotFoundException("мероприятия с id = " + eventId + " не существует");
        }

        Sort sort = Sort.by("created");
        if ("desc".equalsIgnoreCase(sortOrder)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }

        Pageable pageable = PageRequest.of(from, size, sort);
        Page<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);
        log.debug("Найдено {} комментариев для события id={}", comments.getNumberOfElements(), eventId);
        return comments.map(commentMapper::toCommentResponseDto);
    }

    @Override
    @Transactional
    public ReactionResponseDto addVote(Long evaluatorId, Long commentId, String voteType) {
        log.info("Добавление реакции: evaluatorId={}, commentId={}, voteType={}",
                evaluatorId, commentId, voteType);

        User evaluator = userRepository.findById(evaluatorId).orElseThrow(() -> {
            log.warn("Пользователь с id={} не найден при добавлении реакции", evaluatorId);
            return new NotFoundException("Пользователь с id = " + evaluatorId + " не найден");
        });

        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> {
            log.warn("Комментарий с id={} не найден при добавлении реакции", commentId);
            return new NotFoundException("Комментарий с id = " + commentId + " не найден");
        });

        Optional<Reaction> reactionOpt = reactionRepository.existByUserAndComment(evaluatorId, commentId);

        if (reactionOpt.isPresent()) {
            Reaction reaction = reactionOpt.get();
            if (!reaction.getVoteType().equals(voteType)) {
                reaction.setVoteType(voteType);
                Reaction savedReaction = reactionRepository.save(reaction);
                log.info("Реакция обновлена: id={}, voteType={}", savedReaction.getId(), voteType);
                return reactionMapper.toReactionResponseDto(savedReaction);
            }
            return reactionMapper.toReactionResponseDto(reaction);
        }

        Reaction reactionForSave = Reaction.builder()
                .voteType(voteType)
                .evaluator(evaluator)
                .comment(comment)
                .build();

        Reaction createdReaction = reactionRepository.save(reactionForSave);
        log.info("Реакция создана: id={}, voteType={}", createdReaction.getId(), voteType);
        return reactionMapper.toReactionResponseDto(createdReaction);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentStatsResponse getReactionStatsByComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            log.warn("Комментарий с id={} не найден при получении статистики", commentId);
            throw new NotFoundException("комментария с id = " + commentId + " не существует");
        }

        List<Object[]> results = reactionRepository.getLikesAndDislikesCount(commentId);

        long likes = 0;
        long dislikes = 0;

        for (Object[] row : results) {
            String voteType = (String) row[0];
            Long count = (Long) row[1];

            if (voteType.equals(String.valueOf(CommentsSortType.LIKE))) {
                likes = count;
            }

            if (voteType.equals(String.valueOf(CommentsSortType.DISLIKE))) {
                dislikes = count;
            }
        }

        return new CommentStatsResponse(commentId, likes, dislikes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsBy(CommentsSortType sort, String direction, Integer from, Integer size) {

        Pageable pageable = PageRequest.of(from, size);

        List<Comment> comments;

        if (direction.equals(String.valueOf(DirectionSortType.ASC))) {
            comments = commentRepository.getCommentsByAsc(String.valueOf(sort), pageable).getContent();
        } else {
            comments = commentRepository.getCommentsByDesc(String.valueOf(sort), pageable).getContent();
        }

        return comments.stream()
                .map(commentMapper::toCommentResponseDto)
                .toList();
    }
}