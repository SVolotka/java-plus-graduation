package ru.yandex.practicum.core.requestService.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.core.requestService.service.RequestService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/count")
    public Map<Long, Long> getConfirmedRequestsForEvents(@RequestParam("eventIds") List<Long> eventIds) {
        return requestService.getConfirmedRequestsForEvents(eventIds);
    }

    @GetMapping("/is-confirmed")
    public boolean isUserConfirmed(@RequestParam Long userId, @RequestParam Long eventId) {
        return requestService.isUserConfirmedForEvent(userId, eventId);
    }
}