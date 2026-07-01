package ru.practicum.event.specification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.practicum.event.entity.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PublicEventSpecification implements EventSpecification {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String text;
    private final List<Long> categories;
    private final Boolean paid;
    private final Boolean onlyAvailable;
    private final String rangeStart;
    private final String rangeEnd;

    @PersistenceContext
    private EntityManager entityManager;

    private PublicEventSpecification(Builder builder) {
        this.text = builder.text;
        this.categories = builder.categories;
        this.paid = builder.paid;
        this.onlyAvailable = builder.onlyAvailable;
        this.rangeStart = builder.rangeStart;
        this.rangeEnd = builder.rangeEnd;
    }

    @Override
    public Specification<Event> toSpecification() {
        Specification<Event> spec = Specification.where(null);

        if (text != null && !text.isBlank()) {
            String pattern = "%" + text.toLowerCase() + "%";

            Specification<Event> byAnnotation = ((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("annotation")), pattern));

            Specification<Event> byDescription = ((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern));

            spec = spec.and(byAnnotation.or(byDescription));
        }

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> root.get("category").get("id").in(categories));
        }

        if (paid != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("paid"), paid));
        }

        if (onlyAvailable != null) {
            if (onlyAvailable) {
                spec = spec.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.greaterThan(root.get("participantLimit"), root.get("confirmedRequests")));
            }
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

    @Slf4j
    @Component
    public static class Builder {
        private String text;
        private List<Long> categories;
        private Boolean paid;
        private Boolean onlyAvailable;
        private String rangeStart;
        private String rangeEnd;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder categories(List<Long> categories) {
            this.categories = categories;
            return this;
        }

        public Builder paid(Boolean paid) {
            this.paid = paid;
            return this;
        }

        public Builder onlyAvailable(Boolean onlyAvailable) {
            this.onlyAvailable = onlyAvailable;
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

        public PublicEventSpecification build() {
            return new PublicEventSpecification(this);
        }
    }
}
