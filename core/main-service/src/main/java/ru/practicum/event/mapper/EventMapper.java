package ru.practicum.event.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.category.entity.Category;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.event.dto.*;
import ru.practicum.event.entity.Event;
import ru.practicum.event.enums.State;
import ru.practicum.user.entity.User;
import ru.practicum.user.mapper.UserMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EventFullDto toFullDto(Event event) {
        EventFullDto eventFullDto = EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toCategoryDto(event.getCategory()))
                .description(event.getDescription())
                .initiator(userMapper.toUserShortDto(event.getInitiator()))
                .location(event.getLocation())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(String.valueOf(event.getState()))
                .title(event.getTitle())
                .build();

        if (event.getCreatedOn() != null) {
            eventFullDto.setCreatedOn(formatter.format(event.getCreatedOn()));
        }

        if (event.getEventDate() != null) {
            eventFullDto.setEventDate(formatter.format(event.getEventDate()));
        }

        if (event.getPublishedOn() != null) {
            eventFullDto.setPublishedOn(formatter.format(event.getPublishedOn()));
        }

        return eventFullDto;
    }

    public List<EventShortDto> toListShortDtoWithViewsAndRequests(
            List<Event> events, Map<Long, Long> viewsForEvents, Map<Long, Long> requests) {
        return events.stream()
                .map(e -> {
                    EventShortDto eventShortDto = toShortDto(e);
                    eventShortDto.setViews(viewsForEvents.getOrDefault(e.getId(), 0L));
                    eventShortDto.setConfirmedRequests(requests.getOrDefault(e.getId(), 0L));
                    return eventShortDto;
                })
                .toList();
    }

    public List<EventFullDto> toListFullDtoWithViewsAndRequests(
            List<Event> events, Map<Long, Long> viewsForEvents, Map<Long, Long> requests) {
        return events.stream()
                .map(e -> {
                    EventFullDto eventFullDto = toFullDto(e);
                    eventFullDto.setViews(viewsForEvents.getOrDefault(e.getId(), 0L));
                    eventFullDto.setConfirmedRequests(requests.getOrDefault(e.getId(), 0L));
                    return eventFullDto;
                })
                .toList();
    }

    public EventShortDto toShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toCategoryDto(event.getCategory()))
                .eventDate(formatter.format(event.getEventDate()))
                .initiator(userMapper.toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .build();
    }

    public Event toEntity(NewEventDto newEventDto, User user, Category category) {
        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .initiator(user)
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
}
