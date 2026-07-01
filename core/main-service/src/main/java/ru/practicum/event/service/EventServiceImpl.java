package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHitDto;
import ru.practicum.StatsClient;
import ru.practicum.ViewStatsDto;
import ru.practicum.category.entity.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.PatchEventDto;
import ru.practicum.event.entity.Event;
import ru.practicum.event.entity.Location;
import ru.practicum.event.entityparam.AdminEventParam;
import ru.practicum.event.entityparam.PublicEventParam;
import ru.practicum.event.enums.SortType;
import ru.practicum.event.enums.State;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.specification.AdminEventSpecification;
import ru.practicum.event.specification.EventSpecification;
import ru.practicum.event.specification.PublicEventSpecification;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.entity.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    @Transactional(readOnly = true)
    @Override
    public List<EventShortDto> findEventsBy(PublicEventParam param, HttpServletRequest httpServletRequest) {
        saveHit(httpServletRequest);

        EventSpecification specification = PublicEventSpecification.builder()
                .text(param.getText())
                .categories(param.getCategories())
                .paid(param.getPaid())
                .onlyAvailable(param.getOnlyAvailable())
                .rangeStart(param.getRangeStart())
                .rangeEnd(param.getRangeEnd())
                .build();

        Pageable pageable = PageRequest.of(param.getFrom(), param.getSize());

        if (param.getSort() != null && param.getSort().isBlank()) {
            if (String.valueOf(SortType.EVENT_DATE).equals(param.getSort())) {
                Sort sort = Sort.by(Sort.Direction.DESC, param.getSort());
                pageable = PageRequest.of(param.getFrom(), param.getSize(), sort);
                Page<Event> events = eventRepository.findAll(specification.toSpecification(), pageable);
                return events.stream()
                        .map(eventMapper::toShortDto)
                        .toList();
            }
        }

        List<Event> events = eventRepository.findAll(specification.toSpecification(), pageable).getContent();

        Map<Long, Long> viewsForEvents = getViews(events);
        Map<Long, Long> requestsForEvents = getRequestsForEvents(events);
        System.out.println(requestsForEvents.toString());

        List<EventShortDto> eventsDto = eventMapper.toListShortDtoWithViewsAndRequests(events, viewsForEvents, requestsForEvents);

        if (param.getSort() != null && !param.getSort().isBlank()) {
            if (String.valueOf(SortType.VIEWS).equals(param.getSort())) {
                eventsDto.sort(Comparator.comparing(EventShortDto::getViews).reversed());
            }
        }

        return eventsDto;
    }

    @Transactional(readOnly = true)
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

        return eventMapper.toListFullDtoWithViewsAndRequests(events, viewsForEvents, requestsForEvents);
    }

    @Transactional(readOnly = true)
    @Override
    public EventFullDto findEventById(Long id, HttpServletRequest httpServletRequest) {
        log.info("Получение пользователя по id.");
        Event event = eventRepository.findPublishedEventById(id)
                .orElseThrow(() -> new NotFoundException(String.format("События с id = %d не существует.", id)));
        log.info("Информация о событии получена.");
        saveHit(httpServletRequest);

        EventFullDto eventFullDto = eventMapper.toFullDto(event);

        log.info("Получаем количество просмотров.");
        eventFullDto.setViews(getStats(event));
        eventFullDto.setConfirmedRequests(requestRepository.countConfirmedRequestsByEventId(id));
        return eventFullDto;
    }

    @Transactional
    @Override
    public EventFullDto patchEvent(Long id, PatchEventDto patchEventDto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id = %d отсутствует.", id)));

        if (!event.getState().equals(State.PENDING)) {
            throw new ConflictException("Событие можно публиковать, только если оно в состоянии ожидания публикации.");
        }

        if (patchEventDto.getStateAction() != null) {
            switch (patchEventDto.getStateAction()) {
                case "PUBLISH_EVENT" -> {
                    event.setState(State.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    eventRepository.save(event);
                }

                case "REJECT_EVENT" -> {
                    event.setState(State.CANCELED);
                    eventRepository.save(event);
                }
            }
        }

        patchFieldValidation(event, patchEventDto);
        EventFullDto eventFullDto = eventMapper.toFullDto(event);

        if (event.getPublishedOn() != null) {
            eventFullDto.setViews(getStats(event));

            if (event.getEventDate().plusHours(1).isBefore(event.getPublishedOn())) {
                throw new ConflictException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации. " +
                        "Дата события: " + event.getEventDate() + ", дата публикации: " + event.getPublishedOn());
            }
        }

        eventFullDto.setConfirmedRequests(requestRepository.countConfirmedRequestsByEventId(id));

        return eventFullDto;
    }

    @Transactional
    @Override
    public EventFullDto patchEventByUser(Long userId, Long eventId, PatchEventDto patchEventDto) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("Пользователя с id = %d не существует.", userId));
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id = %d отсутствует.", eventId)));

        if (!(event.getState().equals(State.PENDING) || event.getState().equals(State.CANCELED))) {
            throw new ConflictException("События со статусом PUBLISHED не могут быть изменены.");
        }

        if (patchEventDto.getStateAction() != null) {
            switch (patchEventDto.getStateAction()) {
                case "CANCEL_REVIEW" -> {
                    event.setState(State.CANCELED);
                    eventRepository.save(event);
                }

                case "SEND_TO_REVIEW" -> {
                    event.setState(State.PENDING);
                    eventRepository.save(event);
                }
            }
        }

        EventFullDto eventFullDto = eventMapper.toFullDto(event);

        if (event.getPublishedOn() != null) {
            eventFullDto.setViews(getStats(event));
        }

        return eventFullDto;
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventShortDto> findEventsBy(Long userId, Integer from, Integer size) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("Пользователя с id = %d не существует.", userId));
        }

        Pageable pageable = PageRequest.of(from, size);
        List<Event> events = eventRepository.findAll(pageable).getContent();

        Map<Long, Long> viewsForEvents = getViews(events);
        Map<Long, Long> requestsForEvents = getRequestsForEvents(events);

        return eventMapper.toListShortDtoWithViewsAndRequests(events, viewsForEvents, requestsForEvents);
    }

    @Transactional(readOnly = true)
    @Override
    public EventFullDto findEventByIdAndUser(Long userId, Long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("Пользователя с id = %d не существует.", userId));
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id = %d отсутствует.", eventId)));

        EventFullDto eventFullDto = eventMapper.toFullDto(event);

        if (event.getPublishedOn() != null) {
            eventFullDto.setViews(getStats(event));
        }

        return eventFullDto;
    }

    @Transactional
    @Override
    public EventFullDto saveNewEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователя с id = %d не существует.", userId)));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException(String.format("Категории с id = %d не существует.", newEventDto.getCategory())));

        Event event = eventMapper.toEntity(newEventDto, user, category);
        Event createdEvent = eventRepository.save(event);

        return eventMapper.toFullDto(createdEvent);
    }

    private Map<Long, Long> getRequestsForEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> eventsIds = events.stream().map(Event::getId).toList();

        List<Object[]> results = requestRepository.countConfirmedRequestsForEvents(eventsIds);

        return results.stream()
                .collect(Collectors.toMap(
                        o -> (Long) o[0],
                        o -> (Long) o[1]
                ));
    }

    private void saveHit(HttpServletRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            statsClient.saveHit(new EndpointHitDto(
                    null,
                    "main-service",
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    LocalDateTime.now().format(formatter)
            ));
        } catch (Exception e) {
            log.warn("Произошла ошибка при сохранении статистики.");
            throw new RuntimeException(e);
        }
    }

    private Long getStats(Event event) {
        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    event.getPublishedOn(),
                    LocalDateTime.now(),
                    List.of("/events/" + event.getId()),
                    true);

            return stats.isEmpty() ? 0L : stats.getFirst().getHits();
        } catch (Exception e) {
            log.warn("Произошла ошибка при получении статистики.");
            throw new RuntimeException(e);
        }
    }

    private Map<Long, Long> getViews(List<Event> events) {
        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .toList();

        List<ViewStatsDto> stats = statsClient.getStats(
                LocalDateTime.now(),
                LocalDateTime.now(),
                uris,
                true);

        Pattern pattern = Pattern.compile("/events/(\\d+)");
        return stats.stream().collect(Collectors.toMap(s ->
               Long.parseLong(String.valueOf(pattern.matcher(s.getUri()).find())), ViewStatsDto::getHits));
    }

    private void patchFieldValidation(Event event, PatchEventDto patchEventDto) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if (patchEventDto.getCategory() != null) {
            Category category = categoryRepository.findById(patchEventDto.getCategory())
                    .orElseThrow(() -> new NotFoundException(String.format("Категории с id = %d не существует.", patchEventDto.getCategory())));
            event.setCategory(category);
        }

        if (patchEventDto.getLocation() != null) {
            Location location = event.getLocation();
            location.setLat(patchEventDto.getLocation().getLat());
            location.setLon(patchEventDto.getLocation().getLon());
            event.setLocation(location);
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
