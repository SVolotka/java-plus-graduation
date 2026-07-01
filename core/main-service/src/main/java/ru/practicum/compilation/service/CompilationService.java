package ru.practicum.compilation.service;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto createNewCompilation(NewCompilationDto newCompilationDto);

    void deleteCompilation(long compId);

    CompilationDto patchCompilation(long compId,UpdateCompilationRequest updateCompilationRequest);

    CompilationDto getCompilationById(long compId);

    List<CompilationDto> getCompilations(Boolean pinned, int from, int size);
}