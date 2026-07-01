package ru.practicum.event.specification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.practicum.event.entity.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AdminEventSpecification implements EventSpecification {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<Long> users;
    private final List<String> states;
    private final List<Long> categories;
    private final String rangeStart;
    private final String rangeEnd;

    public AdminEventSpecification(Builder builder) {
        this.users = builder.users;
        this.states = builder.states;
        this.categories = builder.categories;
        this.rangeStart = builder.rangeStart;
        this.rangeEnd = builder.rangeEnd;
    }

    @Override
    public Specification<Event> toSpecification() {
        Specification<Event> spec = Specification.where(null);

        if (users != null && !users.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> root.get("initiator").get("id").in(users));
        }

        if (states != null && !states.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> root.get("state").in(states));
        }

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> root.get("category").get("id").in(categories));
        }

        if ((rangeStart != null && !rangeStart.isBlank()) && (rangeEnd != null && !rangeEnd.isBlank())) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.between(root.get("createdOn"),
                            LocalDateTime.parse(rangeStart, formatter),
                            LocalDateTime.parse(rangeEnd, formatter)));
        }

        if ((rangeStart != null && !rangeStart.isBlank()) && (rangeEnd == null || rangeEnd.isBlank())) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.between(root.get("createdOn"),
                            LocalDateTime.parse(rangeStart, formatter),
                            LocalDateTime.now()));
        }

        if ((rangeStart == null || rangeStart.isBlank()) && (rangeEnd != null && !rangeEnd.isBlank())) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.between(root.get("createdOn"),
                            LocalDateTime.now(),
                            LocalDateTime.parse(rangeEnd, formatter)));
        }

        if ((rangeStart == null || rangeStart.isBlank()) && (rangeEnd == null || rangeEnd.isBlank())) {
            spec = spec.and(null);
        }

        return spec;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Component
    public static class Builder {
        private List<Long> users;
        private List<String> states;
        private List<Long> categories;
        private String rangeStart;
        private String rangeEnd;

        public Builder users(List<Long> users) {
            this.users = users;
            return this;
        }

        public Builder states(List<String> states) {
            this.states = states;
            return this;
        }

        public Builder categories(List<Long> categories) {
            this.categories = categories;
            return this;
        }

        public Builder rangeStart(String rangeStart) {
            this.rangeStart = rangeStart;
            return this;
        }

        public Builder rangeEnd(String rangeEnd) {
            this.rangeEnd = rangeEnd;
            return this;
        }

        public AdminEventSpecification build() {
            return new AdminEventSpecification(this);
        }
    }
}
