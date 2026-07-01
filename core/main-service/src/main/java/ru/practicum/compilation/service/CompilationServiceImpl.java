package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.entity.Compilation;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.entity.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    public CompilationDto createNewCompilation(NewCompilationDto newCompilationDto) {
        log.info("Создание новой подборки: title={}, pinned={}, events={}",
                newCompilationDto.getTitle(),
                newCompilationDto.getPinned(),
                newCompilationDto.getEvents());

        if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
            log.warn("Попытка создать подборку с уже существующим названием: {}", newCompilationDto.getTitle());
            throw new ConflictException("Подборка с названием '" + newCompilationDto.getTitle() + "' уже существует");
        }

        Compilation compilation = compilationMapper.toEntity(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            compilation.setEvents(getEventList(newCompilationDto.getEvents()));
        } else {
            compilation.setEvents(new ArrayList<>());
            log.debug("Подборка создаётся без событий");
        }

        if (compilation.getPinned() == null) {
            compilation.setPinned(false);
        }

        log.debug("Сохранение подборки в БД");
        Compilation saved = compilationRepository.save(compilation);
        log.info("Подборка успешно создана с id: {}", saved.getId());
        return compilationMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCompilation(long compId) {
        log.info("Запрос на удаление подборки с id: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            log.warn("Подборка с id={} не найдена, удаление невозможно", compId);
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }

        log.debug("Подборка с id={} найдена, выполняется удаление", compId);
        compilationRepository.deleteById(compId);
        log.info("Подборка с id={} успешно удалена", compId);
    }

    @Override
    @Transactional
    public CompilationDto patchCompilation(long compId, UpdateCompilationRequest updateCompilationRequest) {
        log.info("Запрос на обновление подборки по id: {}", compId);
        Compilation compilation = getCompilation(compId);

        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }

        if (updateCompilationRequest.getTitle() != null) {
            if (!updateCompilationRequest.getTitle().equals(compilation.getTitle())) {
                if (compilationRepository.existsByTitle(updateCompilationRequest.getTitle())) {
                    log.warn("Попытка изменить название на уже существующее: {}", updateCompilationRequest.getTitle());
                    throw new ConflictException("Подборка с названием '" + updateCompilationRequest.getTitle() + "' уже существует");
                }
                compilation.setTitle(updateCompilationRequest.getTitle());
            }
        }

        if (updateCompilationRequest.getEvents() != null) {
            compilation.setEvents(getEventList(updateCompilationRequest.getEvents()));
        }

        compilationRepository.save(compilation);
        return compilationMapper.toDto(compilation);
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(long compId) {
        log.info("Запрос на получение подборки по id: {}", compId);
        Compilation compilation = getCompilation(compId);
        log.debug("Подборка с id={} найдена: {}", compId, compilation);
        return compilationMapper.toDto(compilation);
    }


    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Запрос на получение подборок: pinned={}, from={}, size={}", pinned, from, size);

        List<Long> ids = compilationRepository.findCompilationIds(pinned, from, size);
        log.debug("Найдено ID подборок: {}", ids);

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Compilation> compilations = compilationRepository.findCompilationsWithEventsByIds(ids);
        log.debug("Загружено подборок с событиями: {}", compilations.size());

        return compilations.stream()
                .map(compilationMapper::toDto)
                .toList();
    }

    private Compilation getCompilation(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> {
                    log.warn("Подборка с id={} не найдена", compId);
                    return new NotFoundException("Подборка с id=" + compId + " не найдена");
                });
    }

    private List<Event> getEventList(List<Long> eventIds) {
        log.debug("Загрузка событий по списку id: {}", eventIds);
        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.size() != eventIds.size()) {
            Set<Long> foundIds = events.stream()
                    .map(Event::getId).collect(Collectors.toSet());
            Set<Long> missingIds = new HashSet<>(eventIds);
            missingIds.removeAll(foundIds);
            log.warn("Не найдены события с id: {}", missingIds);
            throw new NotFoundException("События с id " + missingIds + " не найдены");
        }
        log.debug("В подборку добавлено {} событий", events.size());
        return events;
    }
}