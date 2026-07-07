package ru.practicum.comment.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.comment.entity.Reaction;
import ru.yandex.practicum.common.comment.dto.CommentResponseDto;
import ru.yandex.practicum.common.comment.dto.ReactionResponseDto;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.common.userService.dto.UserShortDto;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ReactionMapper {

    private final CommentMapper commentMapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReactionResponseDto toReactionResponseDto(Reaction reaction, UserDto userDto) {
        UserShortDto userShortDto = new UserShortDto(userDto.getId(), userDto.getName());
        CommentResponseDto commentResponseDto = commentMapper.toCommentResponseDto(reaction.getComment());

        ReactionResponseDto reactionResponseDto = ReactionResponseDto.builder()
                .id(reaction.getId())
                .voteType(reaction.getVoteType())
                .evaluator(userShortDto)
                .commentResponseDto(commentResponseDto)
                .created(formatter.format(reaction.getCreated()))
                .build();

        if (reaction.getUpdated() != null) {
            reactionResponseDto.setUpdated(formatter.format(reaction.getUpdated()));
        }

        return reactionResponseDto;
    }
}