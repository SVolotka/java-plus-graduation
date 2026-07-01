package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.EndpointHitDto;
import ru.practicum.model.EndpointHit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface StatsMapper {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", source = "timestamp", qualifiedByName = "stringToLocalDateTime")
    EndpointHit toStat(EndpointHitDto endpointHitDto);

    @Named("stringToLocalDateTime")
    default LocalDateTime stringToLocalDateTime(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.parse(timestamp, FORMATTER);
    }
}