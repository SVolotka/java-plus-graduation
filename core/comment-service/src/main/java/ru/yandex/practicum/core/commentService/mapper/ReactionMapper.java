package ru.yandex.practicum.core.commentService.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.common.comment.dto.CommentResponseDto;
import ru.yandex.practicum.common.comment.dto.ReactionResponseDto;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.common.userService.dto.UserShortDto;
import ru.yandex.practicum.core.commentService.entity.Reaction;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ReactionMapper {

    private final CommentMapper commentMapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReactionResponseDto toReactionResponseDto(Reaction reaction, UserDto userDto, CommentResponseDto commentResponseDto) {
        UserShortDto userShortDto = new UserShortDto(userDto.getId(), userDto.getName());
        return ReactionResponseDto.builder()
                .id(reaction.getId())
                .voteType(reaction.getVoteType())
                .evaluator(userShortDto)
                .commentResponseDto(commentResponseDto)
                .created(formatter.format(reaction.getCreated()))
                .updated(reaction.getUpdated() != null ? formatter.format(reaction.getUpdated()) : null)
                .build();
    }
}