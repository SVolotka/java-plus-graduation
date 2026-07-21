package ru.practicum.event.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.entityparam.PublicEventParam;
import ru.practicum.event.service.EventService;
import ru.practicum.statsclient.ActionType;
import ru.practicum.statsclient.AnalyzerGrpcClient;
import ru.practicum.statsclient.CollectorGrpcClient;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;
import ru.yandex.practicum.common.eventService.event.dto.EventShortDto;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@Slf4j
public class PublicEventController {

    private final EventService eventService;
    private final AnalyzerGrpcClient analyzerClient;
    private final CollectorGrpcClient collectorClient;
    private final Clock clock;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> findEventsBy(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "0") Integer from,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        PublicEventParam param = new PublicEventParam(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        return ResponseEntity.ok(eventService.findEventsBy(param));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> findEventById(
            @PathVariable @Min(1) Long id,
            @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId) {
        if (userId != null) {
            try {
                collectorClient.sendUserAction(userId, id, ActionType.VIEW, clock.instant());
            } catch (Exception e) {
                log.warn("Не удалось отправить метрику VIEW в collector: {}", e.getMessage());
            }
        }
        return ResponseEntity.ok(eventService.findEventById(id));
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") long userId,
            @RequestParam(defaultValue = "10") int maxResults) {
        return analyzerClient.getRecommendationsForUser(userId, maxResults)
                .map(proto -> eventService.getEventShortDto(proto.getEventId(), proto.getScore()))
                .collect(Collectors.toList());
    }

    @PutMapping("/{eventId}/like")
    public ResponseEntity<Void> likeEvent(
            @PathVariable long eventId,
            @RequestHeader("X-EWM-USER-ID") long userId) {
        eventService.likeEvent(eventId, userId);
        return ResponseEntity.ok().build();
    }
}