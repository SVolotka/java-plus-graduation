package ru.practicum.compilation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.entity.Compilation;
import ru.practicum.event.mapper.EventMapper;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface CompilationMapper {

    @Mapping(target = "events", ignore = true)
    @Mapping(target = "id", ignore = true)
    Compilation toEntity(NewCompilationDto dto);

    CompilationDto toDto(Compilation compilation);
}