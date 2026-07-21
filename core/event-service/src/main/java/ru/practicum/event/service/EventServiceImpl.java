package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
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
import ru.practicum.statsclient.ActionType;
import ru.practicum.statsclient.AnalyzerGrpcClient;
import ru.practicum.statsclient.CollectorGrpcClient;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;
import ru.yandex.practicum.common.eventService.event.dto.EventShortDto;
import ru.yandex.practicum.common.exception.ConflictException;
import ru.yandex.practicum.common.exception.NotFoundException;
import ru.yandex.practicum.common.exception.ValidationException;
import ru.yandex.practicum.common.feignClient.RequestClient;
import ru.yandex.practicum.common.feignClient.UserClient;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.common.userService.dto.UserShortDto;
import ru.yandex.practicum.grpc.stats.dashboard.RecommendedEventProto;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final TransactionTemplate transactionTemplate;
    private final AnalyzerGrpcClient analyzerClient;
    private final CollectorGrpcClient collectorClient;
    private final Clock clock;

    @Override
    public List<EventShortDto> findEventsBy(PublicEventParam param) {
        validateRange(param);

        EventSpecification specification = PublicEventSpecification.builder()
                .text(param.getText())
                .categories(param.getCategories())
                .paid(param.getPaid())
                .onlyAvailable(param.getOnlyAvailable())
                .rangeStart(param.getRangeStart())
                .rangeEnd(param.getRangeEnd())
                .build();

        int pageNumber = param.getFrom() / param.getSize();
        Pageable pageable;
        if (SortType.EVENT_DATE.name().equalsIgnoreCase(param.getSort())) {
            pageable = PageRequest.of(pageNumber, param.getSize(), Sort.by(Sort.Direction.DESC, "eventDate"));
        } else {
            pageable = PageRequest.of(pageNumber, param.getSize());
        }

        Page<Event> eventPage = eventRepository.findAll(specification.toSpecification(), pageable);
        List<Event> events = eventPage.getContent();

        Map<Long, Double> ratingsForEvents = getRatings(events);
        Map<Long, Long> requestsForEvents = getRequestsForEvents(events);
        Map<Long, UserShortDto> usersMap = getUsersMap(events);

        List<EventShortDto> dtos = eventMapper.toListShortDtoWithRatingsAndRequests(
                events, ratingsForEvents, requestsForEvents, usersMap);

        if (SortType.VIEWS.name().equalsIgnoreCase(param.getSort())) {
            dtos.sort(Comparator.comparing(EventShortDto::getRating, Comparator.nullsLast(Double::compareTo)).reversed());
        }

        return dtos;
    }

    @Override
    public EventFullDto findEventById(Long id) {
        Event event = eventRepository.findPublishedEventById(id)
                .orElseThrow(() -> new NotFoundException("События с id = " + id + " не существует."));

        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventFullDto dto = eventMapper.toFullDto(event, initiator);
        dto.setRating(getRatingForEvent(id));
        dto.setConfirmedRequests(
                requestClient.getConfirmedRequestsForEvents(List.of(id)).getOrDefault(id, 0L)
        );
        return dto;
    }

    @Override
    public EventShortDto getEventShortDto(Long eventId, Double rating) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventShortDto dto = eventMapper.toShortDto(event, initiator);
        dto.setRating(rating);
        dto.setConfirmedRequests(0L);
        return dto;
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

        int pageNumber = param.getFrom() / param.getSize();
        Pageable pageable = PageRequest.of(pageNumber, param.getSize());

        List<Event> events = eventRepository.findAll(specification.toSpecification(), pageable).getContent();

        Map<Long, Double> ratingsForEvents = getRatings(events);
        Map<Long, Long> requestsForEvents = getRequestsForEvents(events);
        Map<Long, UserShortDto> usersMap = getUsersMap(events);

        return eventMapper.toListFullDtoWithRatingsAndRequests(events, ratingsForEvents, requestsForEvents, usersMap);
    }

    @Override
    public EventFullDto patchEvent(Long id, PatchEventDto patchEventDto) {
        if (patchEventDto.getStateAction() != null) {
            switch (patchEventDto.getStateAction()) {
                case "PUBLISH_EVENT", "REJECT_EVENT" -> {}
                default -> throw new ValidationException("Недопустимое действие: " + patchEventDto.getStateAction());
            }
        }

        Event updatedEvent = transactionTemplate.execute(status -> {
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Событие с id = " + id + " отсутствует."));

            if (patchEventDto.getStateAction() != null) {
                switch (patchEventDto.getStateAction()) {
                    case "PUBLISH_EVENT" -> {
                        if (event.getState() != State.PENDING) {
                            throw new ConflictException(
                                    "Событие можно публиковать только в состоянии ожидания.");
                        }
                        event.setState(State.PUBLISHED);
                        event.setPublishedOn(LocalDateTime.now());
                        if (event.getEventDate().isBefore(event.getPublishedOn().plusHours(1))) {
                            throw new ConflictException(
                                    "Дата начала должна быть не ранее чем за час от публикации.");
                        }
                    }
                    case "REJECT_EVENT" -> {
                        if (event.getState() != State.PENDING) {
                            throw new ConflictException(
                                    "Событие можно отклонить, только если оно еще не опубликовано.");
                        }
                        event.setState(State.CANCELED);
                    }
                }
            }

            patchFieldValidation(event, patchEventDto);
            return eventRepository.save(event);
        });

        UserShortDto initiator = getUserShortDto(updatedEvent.getInitiatorId());
        EventFullDto dto = eventMapper.toFullDto(updatedEvent, initiator);
        dto.setRating(getRatingForEvent(id));
        dto.setConfirmedRequests(
                requestClient.getConfirmedRequestsForEvents(List.of(id)).getOrDefault(id, 0L)
        );
        return dto;
    }

    @Override
    public List<EventShortDto> findEventsBy(Long userId, Integer from, Integer size) {
        checkUserExists(userId);

        int pageNumber = from / size;
        Pageable pageable = PageRequest.of(pageNumber, size);

        Page<Event> eventPage = eventRepository.findAllByInitiatorId(userId, pageable);
        List<Event> events = eventPage.getContent();

        Map<Long, Double> ratings = getRatings(events);
        Map<Long, Long> requests = getRequestsForEvents(events);
        Map<Long, UserShortDto> users = getUsersMap(events);

        return eventMapper.toListShortDtoWithRatingsAndRequests(events, ratings, requests, users);
    }

    @Override
    public EventFullDto findEventByIdAndUser(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventFullDto dto = eventMapper.toFullDto(event, initiator);
        if (event.getPublishedOn() != null) {
            dto.setRating(getRatingForEvent(eventId));
        }
        return dto;
    }

    @Override
    public EventFullDto findEventByIdInternal(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventFullDto dto = eventMapper.toFullDto(event, initiator);
        dto.setRating(0.0);
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
            dto.setRating(getRatingForEvent(eventId));
        }
        return dto;
    }

    @Override
    public boolean existsById(Long id) {
        return eventRepository.existsById(id);
    }

    @Override
    public void likeEvent(long eventId, long userId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        if (!analyzerClient.hasUserInteraction(userId, eventId)) {
            throw new ValidationException("Пользователь может лайкать только посещённые мероприятия.");
        }

        collectorClient.sendUserAction(userId, eventId, ActionType.LIKE, clock.instant());
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

    private Map<Long, Double> getRatings(List<Event> events) {
        if (events == null || events.isEmpty()) return Collections.emptyMap();
        List<Long> ids = events.stream().map(Event::getId).toList();
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return analyzerClient.getInteractionsCount(ids)
                            .collect(Collectors.toMap(
                                    RecommendedEventProto::getEventId,
                                    RecommendedEventProto::getScore,
                                    (a, b) -> a
                            ));
                } catch (Exception e) {
                    log.warn("Analyzer getInteractionsCount failed", e);
                    throw new RuntimeException(e);
                }
            }).get(4, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинги из analyzer за отведённое время: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Double getRatingForEvent(Long eventId) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return analyzerClient.getInteractionsCount(List.of(eventId))
                            .findFirst()
                            .map(RecommendedEventProto::getScore)
                            .orElse(0.0);
                } catch (Exception e) {
                    log.warn("Analyzer getInteractionsCount for event {} failed", eventId, e);
                    throw new RuntimeException(e);
                }
            }).get(4, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинг для события {} из analyzer: {}", eventId, e.getMessage());
            return 0.0;
        }
    }

    private Map<Long, Long> getRequestsForEvents(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();
        List<Long> ids = events.stream().map(Event::getId).toList();
        return requestClient.getConfirmedRequestsForEvents(ids);
    }

    private void validateRange(PublicEventParam param) {
        if (param.getRangeStart() != null && !param.getRangeStart().isBlank() &&
                param.getRangeEnd() != null && !param.getRangeEnd().isBlank()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime start = LocalDateTime.parse(param.getRangeStart(), formatter);
            LocalDateTime end = LocalDateTime.parse(param.getRangeEnd(), formatter);
            if (start.isAfter(end)) {
                throw new ValidationException("rangeStart не может быть позже rangeEnd");
            }
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