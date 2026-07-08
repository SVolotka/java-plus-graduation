package ru.practicum.event.specification;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.event.entity.Event;

public interface EventSpecification {
    Specification<Event> toSpecification();
}
