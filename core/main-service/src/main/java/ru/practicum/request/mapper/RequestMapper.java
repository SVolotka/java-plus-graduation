package ru.practicum.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.event.entity.Event;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.entity.ParticipationRequest;
import ru.practicum.request.enums.RequestStatus;
import ru.practicum.user.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RequestMapper {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ParticipationRequest toEntity(User requester, Event event, RequestStatus status) {
        return ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .requester(requester)
                .event(event)
                .status(status)
                .build();
    }

    public ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(formatter.format(request.getCreated()))
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().name())
                .build();
    }

    public List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        return requests.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}