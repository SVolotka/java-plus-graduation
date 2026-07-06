package ru.practicum.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.comment.dto.CommentRequestDto;
import ru.practicum.comment.entity.Comment;
import ru.yandex.practicum.common.comment.dto.CommentDto;
import ru.yandex.practicum.common.comment.dto.CommentResponseDto;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "commentatorId", ignore = true)
    @Mapping(target = "event", ignore = true)
    Comment toComment(CommentRequestDto commentRequestDto);

    @Mapping(source = "commentatorId", target = "commentatorId")
    CommentResponseDto toCommentResponseDto(Comment comment);

    @Mapping(source = "commentatorId", target = "commentatorId")
    CommentDto toCommentDto(Comment comment);
}