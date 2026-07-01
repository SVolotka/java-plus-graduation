package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.entityparam.AdminEventParam;
import ru.practicum.event.entityparam.PublicEventParam;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.PatchEventDto;

import java.util.List;

public interface EventService {

    List<EventShortDto> findEventsBy(PublicEventParam param, HttpServletRequest httpServletRequest);

    List<EventFullDto> findEventsBy(AdminEventParam param);

    List<EventShortDto> findEventsBy(Long id, Integer from, Integer size);

    EventFullDto findEventById(Long id, HttpServletRequest httpServletRequest);

    EventFullDto patchEvent(Long id, PatchEventDto patchEventDto);

    EventFullDto patchEventByUser(Long userId, Long eventId, PatchEventDto patchEventDto);

    EventFullDto findEventByIdAndUser(Long userId, Long eventId);

    EventFullDto saveNewEvent(Long userId, NewEventDto newEventDto);
}
