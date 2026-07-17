package ru.yandex.practicum.core.requestService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.statsclient.CollectorGrpcClient;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;
import ru.yandex.practicum.common.eventService.event.enums.State;
import ru.yandex.practicum.common.exception.ConflictException;
import ru.yandex.practicum.common.exception.InternalServerException;
import ru.yandex.practicum.common.exception.NotFoundException;
import ru.yandex.practicum.common.exception.ValidationException;
import ru.yandex.practicum.common.feignClient.EventClient;
import ru.yandex.practicum.common.feignClient.UserClient;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateResult;
import ru.yandex.practicum.common.requestService.dto.ParticipationRequestDto;
import ru.yandex.practicum.common.requestService.enums.RequestStatus;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.core.requestService.entity.ParticipationRequest;
import ru.yandex.practicum.core.requestService.mapper.RequestMapper;
import ru.yandex.practicum.core.requestService.repository.RequestRepository;
import ru.yandex.practicum.grpc.stats.collector.ActionTypeProto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final TransactionTemplate transactionTemplate;
    private final CollectorGrpcClient collectorClient;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        UserDto user = userClient.getById(userId);
        if (user == null) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден", userId));
        }
        List<ParticipationRequest> requests = requestRepository.findAllByRequesterId(userId);
        return requestMapper.toDtoList(requests);
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsForEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> results = requestRepository.countConfirmedRequestsForEvents(eventIds);
        return results.stream()
                .collect(Collectors.toMap(
                        o -> (Long) o[0],
                        o -> (Long) o[1]
                ));
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        UserDto user = userClient.getById(userId);
        if (user == null) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден", userId));
        }
        EventFullDto event = getEventOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(String.format("Событие с id = %d не принадлежит пользователю %d", eventId, userId));
        }
        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);
        return requestMapper.toDtoList(requests);
    }

    @Override
    public boolean isUserConfirmedForEvent(Long userId, Long eventId) {
        return requestRepository.existsByRequesterIdAndEventIdAndStatus(userId, eventId, RequestStatus.CONFIRMED);
    }

    @Override
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        if (eventId == null) {
            throw new ValidationException("Параметр eventId отсутствует.");
        }
        UserDto requester = userClient.getById(userId);
        if (requester == null) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден", userId));
        }
        EventFullDto event = getEventOrThrow(eventId);
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }
        if (!State.PUBLISHED.name().equals(event.getState())) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Нельзя добавить повторный запрос на участие в событии");
        }

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        int participantLimit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();

        if (participantLimit != 0 && confirmedRequests >= participantLimit) {
            throw new ConflictException("Достигнут лимит участников для данного события");
        }

        boolean requestModeration = Boolean.TRUE.equals(event.getRequestModeration());
        RequestStatus status = (!requestModeration || participantLimit == 0)
                ? RequestStatus.CONFIRMED : RequestStatus.PENDING;

        ParticipationRequest request = transactionTemplate.execute(statusTx ->
                saveNewRequest(userId, eventId, status));

        try {
            collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
        } catch (Exception e) {
            log.warn("Не удалось отправить метрику в collector: {}", e.getMessage());
        }

        return requestMapper.toDto(request);
    }

    private ParticipationRequest saveNewRequest(Long userId, Long eventId, RequestStatus status) {
        ParticipationRequest request = requestMapper.toEntity(userId, eventId, status);
        return requestRepository.save(request);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        UserDto user = userClient.getById(userId);
        if (user == null) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден", userId));
        }

        return transactionTemplate.execute(status -> {
            ParticipationRequest request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException(String.format("Запрос с id = %d не найден", requestId)));

            if (!request.getRequesterId().equals(userId)) {
                throw new NotFoundException(String.format("Запрос с id = %d не найден у пользователя %d", requestId, userId));
            }

            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Нельзя отменить заявку со статусом " + request.getStatus());
            }

            request.setStatus(RequestStatus.CANCELED);
            ParticipationRequest canceledRequest = requestRepository.save(request);
            return requestMapper.toDto(canceledRequest);
        });
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        UserDto user = userClient.getById(userId);
        if (user == null) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден", userId));
        }
        EventFullDto event = getEventOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(String.format("Событие с id = %d не принадлежит пользователю %d", eventId, userId));
        }
        if (!State.PUBLISHED.name().equals(event.getState())) {
            throw new ConflictException("Нельзя изменять статус заявок для неопубликованного события");
        }

        return transactionTemplate.execute(status -> {
            List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());

            if (requests.size() != updateRequest.getRequestIds().size()) {
                throw new NotFoundException("Одна или несколько заявок не найдены");
            }
            for (ParticipationRequest req : requests) {
                if (!req.getEventId().equals(eventId)) {
                    throw new NotFoundException("Одна или несколько заявок не принадлежат этому событию");
                }
                if (!req.getStatus().equals(RequestStatus.PENDING)) {
                    throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
                }
            }

            List<ParticipationRequest> confirmedList = new ArrayList<>();
            List<ParticipationRequest> rejectedList = new ArrayList<>();

            Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
            int participantLimit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();

            if ("CONFIRMED".equals(updateRequest.getStatus())) {
                for (ParticipationRequest req : requests) {
                    if (participantLimit != 0 && confirmedRequests >= participantLimit) {
                        throw new ConflictException("Достигнут лимит одобренных заявок");
                    }
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmedList.add(req);
                    confirmedRequests++;
                }
                requestRepository.saveAll(confirmedList);
                if (participantLimit != 0 && confirmedRequests >= participantLimit) {
                    requestRepository.rejectAllPendingRequestsByEventId(eventId);
                }
            } else if ("REJECTED".equals(updateRequest.getStatus())) {
                for (ParticipationRequest req : requests) {
                    req.setStatus(RequestStatus.REJECTED);
                    rejectedList.add(req);
                }
                requestRepository.saveAll(rejectedList);
            } else {
                throw new ValidationException("Недопустимый статус заявки: " + updateRequest.getStatus());
            }

            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(requestMapper.toDtoList(confirmedList))
                    .rejectedRequests(requestMapper.toDtoList(rejectedList))
                    .build();
        });
    }

    private EventFullDto getEventOrThrow(Long eventId) {
        EventFullDto event = eventClient.getEventById(eventId);
        if (event == null) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено");
        }
        if (event.getInitiator() == null) {
            log.error("Event id={} returned with null initiator", eventId);
            throw new InternalServerException("Некорректные данные события (инициатор не указан)");
        }
        return event;
    }
}