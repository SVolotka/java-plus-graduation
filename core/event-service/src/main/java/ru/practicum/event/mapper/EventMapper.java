package ru.practicum.event.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.category.entity.Category;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.event.dto.*;
import ru.practicum.event.entity.Event;
import ru.practicum.event.enums.State;
import ru.practicum.event.entity.Location;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;
import ru.yandex.practicum.common.eventService.event.dto.EventShortDto;
import ru.yandex.practicum.common.eventService.event.model.LocationDto;
import ru.yandex.practicum.common.userService.dto.UserShortDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final CategoryMapper categoryMapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EventFullDto toFullDto(Event event, UserShortDto initiator) {
        EventFullDto dto = EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toCategoryDto(event.getCategory()))
                .description(event.getDescription())
                .initiator(initiator)
                .location(toLocationDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(String.valueOf(event.getState()))
                .title(event.getTitle())
                .build();

        if (event.getCreatedOn() != null) {
            dto.setCreatedOn(formatter.format(event.getCreatedOn()));
        }
        if (event.getEventDate() != null) {
            dto.setEventDate(formatter.format(event.getEventDate()));
        }
        if (event.getPublishedOn() != null) {
            dto.setPublishedOn(formatter.format(event.getPublishedOn()));
        }
        return dto;
    }

    public EventShortDto toShortDto(Event event, UserShortDto initiator) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toCategoryDto(event.getCategory()))
                .eventDate(formatter.format(event.getEventDate()))
                .initiator(initiator)
                .paid(event.getPaid())
                .title(event.getTitle())
                .build();
    }

    public List<EventShortDto> toListShortDtoWithRatingsAndRequests(
            List<Event> events, Map<Long, Double> ratingsForEvents,
            Map<Long, Long> requests, Map<Long, UserShortDto> users) {
        return events.stream()
                .map(e -> {
                    EventShortDto dto = toShortDto(e, users.get(e.getInitiatorId()));
                    dto.setRating(ratingsForEvents.getOrDefault(e.getId(), 0.0));
                    dto.setConfirmedRequests(requests.getOrDefault(e.getId(), 0L));
                    return dto;
                })
                .toList();
    }

    public List<EventFullDto> toListFullDtoWithRatingsAndRequests(
            List<Event> events, Map<Long, Double> ratingsForEvents,
            Map<Long, Long> requests, Map<Long, UserShortDto> users) {
        return events.stream()
                .map(e -> {
                    EventFullDto dto = toFullDto(e, users.get(e.getInitiatorId()));
                    dto.setRating(ratingsForEvents.getOrDefault(e.getId(), 0.0));
                    dto.setConfirmedRequests(requests.getOrDefault(e.getId(), 0L));
                    return dto;
                })
                .toList();
    }

    public Event toEntity(NewEventDto newEventDto, Long userId, Category category) {
        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .initiatorId(userId)
                .location(newEventDto.getLocation())
                .description(newEventDto.getDescription())
                .createdOn(LocalDateTime.now())
                .eventDate(LocalDateTime.parse(newEventDto.getEventDate(), formatter))
                .state(State.PENDING)
                .title(newEventDto.getTitle())
                .build();

        if (newEventDto.getPaid() != null) {
            event.setPaid(newEventDto.getPaid());
        } else {
            event.setPaid(false);
        }
        if (newEventDto.getParticipantLimit() != null) {
            event.setParticipantLimit(newEventDto.getParticipantLimit());
        } else {
            event.setParticipantLimit(0);
        }
        if (newEventDto.getRequestModeration() != null) {
            event.setRequestModeration(newEventDto.getRequestModeration());
        } else {
            event.setRequestModeration(true);
        }
        return event;
    }

    private LocationDto toLocationDto(Location location) {
        if (location == null) return null;
        return new LocationDto(location.getLat(), location.getLon());
    }
}