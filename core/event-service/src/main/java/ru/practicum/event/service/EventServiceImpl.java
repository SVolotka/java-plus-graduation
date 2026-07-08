package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.EndpointHitDto;
import ru.practicum.StatsClient;
import ru.practicum.ViewStatsDto;
import ru.practicum.category.entity.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.PatchEventDto;
import ru.practicum.event.entity.Event;
import ru.practicum.event.entityparam.AdminEventParam;
import ru.practicum.event.entityparam.PublicEventParam;
import ru.practicum.event.enums.SortType;
import ru.practicum.event.enums.State;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.specification.AdminEventSpecification;
import ru.practicum.event.specification.EventSpecification;
import ru.practicum.event.specification.PublicEventSpecification;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;
import ru.yandex.practicum.common.eventService.event.dto.EventShortDto;
import ru.yandex.practicum.common.exception.ConflictException;
import ru.yandex.practicum.common.exception.NotFoundException;
import ru.yandex.practicum.common.exception.ValidationException;
import ru.yandex.practicum.common.feignClient.RequestClient;
import ru.yandex.practicum.common.feignClient.UserClient;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.common.userService.dto.UserShortDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    public List<EventShortDto> findEventsBy(PublicEventParam param, HttpServletRequest httpServletRequest) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (param.getRangeStart() != null && !param.getRangeStart().isBlank() &&
                param.getRangeEnd() != null && !param.getRangeEnd().isBlank()) {
            LocalDateTime start = LocalDateTime.parse(param.getRangeStart(), formatter);
            LocalDateTime end = LocalDateTime.parse(param.getRangeEnd(), formatter);
            if (start.isAfter(end)) {
                throw new ValidationException("rangeStart не может быть позже rangeEnd");
            }
        }

        saveHit(httpServletRequest);

        EventSpecification specification = PublicEventSpecification.builder()
                .text(param.getText())
                .categories(param.getCategories())
                .paid(param.getPaid())
                .onlyAvailable(param.getOnlyAvailable())
                .rangeStart(param.getRangeStart())
                .rangeEnd(param.getRangeEnd())
                .build();

        Pageable pageable;
        if (param.getSort() != null && !param.getSort().isBlank()
                && SortType.EVENT_DATE.name().equalsIgnoreCase(param.getSort())) {
            pageable = PageRequest.of(param.getFrom(), param.getSize(), Sort.by(Sort.Direction.DESC, "eventDate"));
        } else {
            pageable = PageRequest.of(param.getFrom(), param.getSize());
        }

        Page<Event> eventPage = eventRepository.findAll(specification.toSpecification(), pageable);
        List<Event> events = eventPage.getContent();

        Map<Long, Long> viewsForEvents = getViews(events);
        Map<Long, Long> requestsForEvents = getRequestsForEvents(events);
        Map<Long, UserShortDto> usersMap = getUsersMap(events);

        List<EventShortDto> eventsDto = eventMapper.toListShortDtoWithViewsAndRequests(
                events, viewsForEvents, requestsForEvents, usersMap);

        if (param.getSort() != null && !param.getSort().isBlank()
                && SortType.VIEWS.name().equalsIgnoreCase(param.getSort())) {
            eventsDto.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        return eventsDto;
    }

    @Override
    public List<EventFullDto> findEventsBy(AdminEventParam param) {
        EventSpecification specification = AdminEventSpecification.builder()
                .users(param.getUsers())
                .states(param.getStates())
                .categories(param.getCategories())
                .rangeStart(param.getRangeStart())
                .rangeEnd(param.getRangeEnd())
                .build();

        Pageable pageable = PageRequest.of(param.getFrom(), param.getSize());
        List<Event> events = eventRepository.findAll(specification.toSpecification(), pageable).getContent();

        Map<Long, Long> viewsForEvents = getViews(events);
        Map<Long, Long> requestsForEvents = getRequestsForEvents(events);
        Map<Long, UserShortDto> usersMap = getUsersMap(events);

        return eventMapper.toListFullDtoWithViewsAndRequests(events, viewsForEvents, requestsForEvents, usersMap);
    }

    @Override
    public EventFullDto findEventById(Long id, HttpServletRequest httpServletRequest) {
        Event event = eventRepository.findPublishedEventById(id)
                .orElseThrow(() -> new NotFoundException("События с id = " + id + " не существует."));
        saveHit(httpServletRequest);

        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventFullDto dto = eventMapper.toFullDto(event, initiator);
        dto.setViews(getStats(event));
        dto.setConfirmedRequests(requestClient.getConfirmedRequestsForEvents(List.of(id)).getOrDefault(id, 0L));
        return dto;
    }

    @Override
    public List<EventShortDto> findEventsBy(Long userId, Integer from, Integer size) {
        checkUserExists(userId);

        Pageable pageable = PageRequest.of(from, size);
        Page<Event> eventPage = eventRepository.findAllByInitiatorId(userId, pageable);
        List<Event> events = eventPage.getContent();

        Map<Long, Long> viewsForEvents = getViews(events);
        Map<Long, Long> requestsForEvents = getRequestsForEvents(events);
        Map<Long, UserShortDto> usersMap = getUsersMap(events);

        return eventMapper.toListShortDtoWithViewsAndRequests(events, viewsForEvents, requestsForEvents, usersMap);
    }

    @Override
    public EventFullDto findEventByIdAndUser(Long userId, Long eventId) {
        checkUserExists(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " отсутствует."));

        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventFullDto dto = eventMapper.toFullDto(event, initiator);
        if (event.getPublishedOn() != null) {
            dto.setViews(getStats(event));
        }
        return dto;
    }

    @Override
    public EventFullDto findEventByIdInternal(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + id + " не существует."));
        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventFullDto dto = eventMapper.toFullDto(event, initiator);
        dto.setViews(0L);
        dto.setConfirmedRequests(0L);
        return dto;
    }

    @Override
    public EventFullDto saveNewEvent(Long userId, NewEventDto newEventDto) {
        checkUserExists(userId);

        LocalDateTime eventDate = LocalDateTime.parse(newEventDto.getEventDate(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event createdEvent = transactionTemplate.execute(status -> {
            Category category = categoryRepository.findById(newEventDto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категории с id = " + newEventDto.getCategory() + " не существует."));
            Event event = eventMapper.toEntity(newEventDto, userId, category);
            return eventRepository.save(event);
        });

        UserShortDto initiator = getUserShortDto(userId);
        return eventMapper.toFullDto(createdEvent, initiator);
    }

    @Override
    public EventFullDto patchEvent(Long id, PatchEventDto patchEventDto) {
        // Предварительная проверка (stateAction) без транзакции
        if (patchEventDto.getStateAction() != null) {
            switch (patchEventDto.getStateAction()) {
                case "PUBLISH_EVENT", "REJECT_EVENT" -> {} // ok
                default -> throw new ValidationException("Недопустимое действие: " + patchEventDto.getStateAction());
            }
        }

        Event updatedEvent = transactionTemplate.execute(status -> {
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Событие с id = " + id + " отсутствует."));
            if (event.getState() != State.PENDING) {
                throw new ConflictException("Событие можно публиковать только в состоянии ожидания.");
            }

            if (patchEventDto.getStateAction() != null) {
                switch (patchEventDto.getStateAction()) {
                    case "PUBLISH_EVENT" -> {
                        event.setState(State.PUBLISHED);
                        event.setPublishedOn(LocalDateTime.now());
                    }
                    case "REJECT_EVENT" -> event.setState(State.CANCELED);
                }
            }

            patchFieldValidation(event, patchEventDto);
            return eventRepository.save(event);
        });

        UserShortDto initiator = getUserShortDto(updatedEvent.getInitiatorId());
        EventFullDto dto = eventMapper.toFullDto(updatedEvent, initiator);
        if (updatedEvent.getPublishedOn() != null) {
            dto.setViews(getStats(updatedEvent));
            if (updatedEvent.getEventDate().plusHours(1).isBefore(updatedEvent.getPublishedOn())) {
                throw new ConflictException("Дата начала должна быть не ранее чем за час от публикации.");
            }
        }
        dto.setConfirmedRequests(requestClient.getConfirmedRequestsForEvents(List.of(id)).getOrDefault(id, 0L));
        return dto;
    }

    @Override
    public EventFullDto patchEventByUser(Long userId, Long eventId, PatchEventDto patchEventDto) {
        checkUserExists(userId);

        Event updatedEvent = transactionTemplate.execute(status -> {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " отсутствует."));

            if (event.getState() != State.PENDING && event.getState() != State.CANCELED) {
                throw new ConflictException("События со статусом PUBLISHED нельзя изменять.");
            }

            if (patchEventDto.getStateAction() != null) {
                switch (patchEventDto.getStateAction()) {
                    case "CANCEL_REVIEW" -> event.setState(State.CANCELED);
                    case "SEND_TO_REVIEW" -> event.setState(State.PENDING);
                }
            }

            patchFieldValidation(event, patchEventDto);
            return eventRepository.save(event);
        });

        UserShortDto initiator = getUserShortDto(updatedEvent.getInitiatorId());
        EventFullDto dto = eventMapper.toFullDto(updatedEvent, initiator);
        if (updatedEvent.getPublishedOn() != null) {
            dto.setViews(getStats(updatedEvent));
        }
        return dto;
    }

    private void checkUserExists(Long userId) {
        UserDto user = userClient.getById(userId);
        if (user == null) {
            throw new NotFoundException("Пользователя с id = " + userId + " не существует.");
        }
    }

    private UserShortDto getUserShortDto(Long userId) {
        UserDto dto = userClient.getById(userId);
        if (dto == null) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        return new UserShortDto(dto.getId(), dto.getName());
    }

    private Map<Long, UserShortDto> getUsersMap(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = events.stream().map(Event::getInitiatorId).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<UserShortDto> users = userClient.getUsersByIds(ids);
            return users.stream().collect(Collectors.toMap(UserShortDto::getId, Function.identity()));
        } catch (Exception e) {
            log.warn("Failed to fetch users: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, Long> getViews(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> uris = events.stream().map(e -> "/events/" + e.getId()).toList();
        List<ViewStatsDto> stats = statsClient.getStats(
                LocalDateTime.now().minusYears(10),
                LocalDateTime.now(),
                uris,
                true);
        Pattern pattern = Pattern.compile("/events/(\\d+)");
        return stats.stream()
                .filter(s -> s.getUri() != null)
                .collect(Collectors.toMap(
                        s -> {
                            Matcher m = pattern.matcher(s.getUri());
                            if (m.find()) return Long.parseLong(m.group(1));
                            throw new RuntimeException("Не удалось извлечь id из URI: " + s.getUri());
                        },
                        ViewStatsDto::getHits,
                        (a, b) -> a
                ));
    }

    private Long getStats(Event event) {
        LocalDateTime start = event.getPublishedOn() != null ? event.getPublishedOn() : LocalDateTime.now().minusYears(10);
        List<ViewStatsDto> stats = statsClient.getStats(
                start,
                LocalDateTime.now(),
                List.of("/events/" + event.getId()),
                true);
        return stats.isEmpty() ? 0L : stats.getFirst().getHits();
    }

    private Map<Long, Long> getRequestsForEvents(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = events.stream().map(Event::getId).toList();
        return requestClient.getConfirmedRequestsForEvents(ids);
    }

    private void saveHit(HttpServletRequest request) {
        try {
            statsClient.saveHit(new EndpointHitDto(
                    null,
                    "event-service",
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));
        } catch (Exception e) {
            log.warn("Ошибка сохранения статистики: {}", e.getMessage());
        }
    }

    private void patchFieldValidation(Event event, PatchEventDto patchEventDto) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if (patchEventDto.getCategory() != null) {
            Category category = categoryRepository.findById(patchEventDto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категории с id = " + patchEventDto.getCategory() + " не существует."));
            event.setCategory(category);
        }
        if (patchEventDto.getLocation() != null) {
            event.getLocation().setLat(patchEventDto.getLocation().getLat());
            event.getLocation().setLon(patchEventDto.getLocation().getLon());
        }
        if (patchEventDto.getAnnotation() != null) {
            event.setAnnotation(patchEventDto.getAnnotation());
        }
        if (patchEventDto.getDescription() != null) {
            event.setDescription(patchEventDto.getDescription());
        }
        if (patchEventDto.getEventDate() != null) {
            event.setEventDate(LocalDateTime.parse(patchEventDto.getEventDate(), formatter));
        }
        if (patchEventDto.getPaid() != null) {
            event.setPaid(patchEventDto.getPaid());
        }
        if (patchEventDto.getParticipantLimit() != null) {
            event.setParticipantLimit(patchEventDto.getParticipantLimit());
        }
        if (patchEventDto.getRequestModeration() != null) {
            event.setRequestModeration(patchEventDto.getRequestModeration());
        }
        if (patchEventDto.getTitle() != null) {
            event.setTitle(patchEventDto.getTitle());
        }
    }
}