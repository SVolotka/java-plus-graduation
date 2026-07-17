package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.PatchEventDto;
import ru.practicum.event.service.EventService;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;
import ru.yandex.practicum.common.eventService.event.dto.EventShortDto;
import ru.yandex.practicum.common.feignClient.RequestClient;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateResult;
import ru.yandex.practicum.common.requestService.dto.ParticipationRequestDto;
import ru.yandex.practicum.common.exception.ValidationException;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController {

    private final EventService eventService;
    private final RequestClient requestClient;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> findEventsBy(
            @PathVariable("userId") @Min(1) Long id,
            @RequestParam(value = "from", defaultValue = "0") Integer from,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return ResponseEntity.ok(eventService.findEventsBy(id, from, size));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> findEventById(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId) {
        return ResponseEntity.ok(eventService.findEventByIdAndUser(userId, eventId));
    }

    @PostMapping
    public ResponseEntity<EventFullDto> saveNewEvent(
            @PathVariable("userId") @Min(1) Long id,
            @Valid @RequestBody NewEventDto newEventDto) {
        EventFullDto eventFullDto = eventService.saveNewEvent(id, newEventDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventFullDto);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> patchEventByInitiator(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @Valid @RequestBody PatchEventDto patchEventDto) {
        return ResponseEntity.ok(eventService.patchEventByUser(userId, eventId, patchEventDto));
    }

    @GetMapping("/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> findRequestsForEventsByUser(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId) {
        return ResponseEntity.ok(requestClient.getEventParticipants(userId, eventId));
    }

    @PatchMapping("/{eventId}/requests")
    public ResponseEntity<EventRequestStatusUpdateResult> patchStatusOfRequest(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @RequestBody(required = false) @Valid EventRequestStatusUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("Тело запроса не может быть пустым");
        }
        return ResponseEntity.ok(requestClient.changeRequestStatus(userId, eventId, request));
    }
}