package ru.practicum.event.service;

import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.entityparam.AdminEventParam;
import ru.practicum.event.entityparam.PublicEventParam;
import ru.practicum.event.dto.PatchEventDto;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;
import ru.yandex.practicum.common.eventService.event.dto.EventShortDto;

import java.util.List;

public interface EventService {

    List<EventShortDto> findEventsBy(PublicEventParam param);

    EventFullDto findEventById(Long id);

    List<EventFullDto> findEventsBy(AdminEventParam param);

    List<EventShortDto> findEventsBy(Long userId, Integer from, Integer size);

    EventFullDto findEventByIdAndUser(Long userId, Long eventId);

    EventFullDto findEventByIdInternal(Long id);

    EventFullDto saveNewEvent(Long userId, NewEventDto newEventDto);

    EventFullDto patchEvent(Long id, PatchEventDto patchEventDto);

    EventFullDto patchEventByUser(Long userId, Long eventId, PatchEventDto patchEventDto);

    boolean existsById(Long id);

    EventShortDto getEventShortDto(Long eventId, Double rating);
}
