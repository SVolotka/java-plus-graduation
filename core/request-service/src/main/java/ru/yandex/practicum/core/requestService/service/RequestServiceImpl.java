package ru.yandex.practicum.core.requestService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.common.eventService.event.dto.EventFullDto;
import ru.yandex.practicum.common.eventService.event.enums.State;
import ru.yandex.practicum.common.exception.ConflictException;
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
        EventFullDto event = eventClient.getEventById(eventId);
        if (event == null) {
            throw new NotFoundException(String.format("Событие с id = %d не найдено", eventId));
        }
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(String.format("Событие с id = %d не принадлежит пользователю %d", eventId, userId));
        }
        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);
        return requestMapper.toDtoList(requests);
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
        EventFullDto event = eventClient.getEventById(eventId);
        if (event == null) {
            throw new NotFoundException(String.format("Событие с id = %d не найдено", eventId));
        }
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
        if (event.getParticipantLimit() != 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников для данного события");
        }
        RequestStatus status = (!event.getRequestModeration() || event.getParticipantLimit() == 0)
                ? RequestStatus.CONFIRMED : RequestStatus.PENDING;

        ParticipationRequest request = transactionTemplate.execute(statusTx ->
                saveNewRequest(userId, eventId, status));
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
        EventFullDto event = eventClient.getEventById(eventId);
        if (event == null) {
            throw new NotFoundException(String.format("Событие с id = %d не найдено", eventId));
        }
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(String.format("Событие с id = %d не принадлежит пользователю %d", eventId, userId));
        }
        if (!State.PUBLISHED.name().equals(event.getState())) {
            throw new ConflictException("Нельзя изменять статус заявок для неопубликованного события");
        }

        return transactionTemplate.execute(status -> {
            Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
            Integer participantLimit = event.getParticipantLimit();
            if (confirmedRequests >= participantLimit) {
                throw new ConflictException("Достигнут лимит одобренных заявок");
            }
            List<ParticipationRequest> requests = requestRepository.findAllByIdInAndEventId(updateRequest.getRequestIds(), eventId);
            for (ParticipationRequest req : requests) {
                if (!req.getStatus().equals(RequestStatus.PENDING)) {
                    throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
                }
            }
            List<ParticipationRequest> confirmedList = new ArrayList<>();
            List<ParticipationRequest> rejectedList = new ArrayList<>();
            if (participantLimit == 0 || !event.getRequestModeration()) {
                return EventRequestStatusUpdateResult.builder()
                        .confirmedRequests(List.of())
                        .rejectedRequests(List.of())
                        .build();
            }
            if ("CONFIRMED".equals(updateRequest.getStatus())) {
                for (ParticipationRequest req : requests) {
                    if (confirmedRequests < participantLimit) {
                        req.setStatus(RequestStatus.CONFIRMED);
                        confirmedList.add(req);
                        confirmedRequests++;
                    } else {
                        req.setStatus(RequestStatus.REJECTED);
                        rejectedList.add(req);
                    }
                }
                if (confirmedRequests >= participantLimit) {
                    requestRepository.rejectAllPendingRequestsByEventId(eventId);
                }
            } else if ("REJECTED".equals(updateRequest.getStatus())) {
                for (ParticipationRequest req : requests) {
                    req.setStatus(RequestStatus.REJECTED);
                    rejectedList.add(req);
                }
            }
            requestRepository.saveAll(confirmedList);
            requestRepository.saveAll(rejectedList);
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(requestMapper.toDtoList(confirmedList))
                    .rejectedRequests(requestMapper.toDtoList(rejectedList))
                    .build();
        });
    }
}