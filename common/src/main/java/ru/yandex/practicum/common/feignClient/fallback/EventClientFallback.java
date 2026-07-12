package ru.yandex.practicum.common.feignClient.fallback;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;
import ru.yandex.practicum.common.feignClient.EventClient;

@Component
public class EventClientFallback implements EventClient {
    @Override
    public EventFullDto getEventById(Long id) {
        return null;
    }

    @Override
    public boolean existsById(Long id) {
        return false;
    }
}
