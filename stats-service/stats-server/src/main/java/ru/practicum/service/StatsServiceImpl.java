package ru.practicum.service;

import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHitDto;
import ru.practicum.ViewStatsDto;
import ru.practicum.error.exception.ValidationException;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final StatsMapper statsMapper;

    @Override
    @Transactional
    public void addHit(EndpointHitDto endpointHitDto) {
        EndpointHit stat = statsMapper.toStat(endpointHitDto);
        statsRepository.save(stat);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        validateDates(start, end);

        List<Tuple> tuples = fetchStats(start, end, uris, unique);

        return tuples.stream()
                .filter(Objects::nonNull)
                .map(this::convertTupleToDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ValidationException("Даты начала и окончания не могут быть null");
        }
        if (start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть после даты окончания");
        }
    }

    private List<Tuple> fetchStats(LocalDateTime start, LocalDateTime end,
                                   List<String> uris, Boolean unique) {
        boolean isUnique = Boolean.TRUE.equals(unique);
        boolean hasUris = uris != null && !uris.isEmpty();

        if (isUnique) {
            if (hasUris) {
                return statsRepository.findUniqueStatsByUris(start, end, uris);
            } else {
                return statsRepository.findUniqueStats(start, end);
            }
        } else {
            if (hasUris) {
                return statsRepository.findStatsByUris(start, end, uris);
            } else {
                return statsRepository.findStats(start, end);
            }
        }
    }

    private ViewStatsDto convertTupleToDto(Tuple tuple) {
        try {
            String app = tuple.get("app", String.class);
            String uri = tuple.get("uri", String.class);
            Long hits = tuple.get("hits", Long.class);

            if (app == null || uri == null || hits == null) {
                return null;
            }
            return new ViewStatsDto(app, uri, hits);
        } catch (Exception e) {
            System.err.println("Ошибка преобразования Tuple: " + e.getMessage());
            return null;
        }
    }
}