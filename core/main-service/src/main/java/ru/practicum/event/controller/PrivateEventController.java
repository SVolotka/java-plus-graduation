package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.PatchEventDto;
import ru.practicum.event.service.EventService;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController {

    private final EventService eventService;
    private final RequestService requestService;

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
            @Valid @RequestBody @NotNull NewEventDto newEventDto) {
        EventFullDto eventFullDto = eventService.saveNewEvent(id, newEventDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventFullDto);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> patchEventByInitiator(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @Valid @RequestBody @NotNull PatchEventDto patchEventDto) {
        return ResponseEntity.ok(eventService.patchEventByUser(userId, eventId, patchEventDto));
    }

    @GetMapping("/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> findRequestsForEventsByUser(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId) {
        return ResponseEntity.ok(requestService.getEventParticipants(userId, eventId));
    }

    @PatchMapping("/{eventId}/requests")
    public ResponseEntity<EventRequestStatusUpdateResult> patchStatusOfRequest(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @RequestBody @NotNull EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        return ResponseEntity.ok(requestService.changeRequestStatus(userId, eventId, eventRequestStatusUpdateRequest));
    }
}
