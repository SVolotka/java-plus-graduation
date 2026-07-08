package ru.yandex.practicum.common.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/internal/events/{id}")
    EventFullDto getEventById(@PathVariable Long id);
}