package ru.yandex.practicum.core.commentService.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.common.comment.dto.CommentDto;
import ru.yandex.practicum.common.comment.dto.CommentResponseDto;
import ru.yandex.practicum.core.commentService.dto.CommentRequestDto;
import ru.yandex.practicum.core.commentService.entity.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "commentatorId", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    Comment toComment(CommentRequestDto commentRequestDto);

    CommentResponseDto toCommentResponseDto(Comment comment);

    @Mapping(source = "eventId", target = "eventId")
    CommentDto toCommentDto(Comment comment);
}