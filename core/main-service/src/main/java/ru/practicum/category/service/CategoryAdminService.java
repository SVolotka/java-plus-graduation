package ru.practicum.category.service;

import ru.practicum.category.dto.CategoryRequestDto;
import ru.yandex.practicum.common.eventService.category.dto.CategoryDto;

public interface CategoryAdminService {

    CategoryDto create(CategoryRequestDto categoryRequestDto);

    CategoryDto update(Long id, CategoryRequestDto categoryRequestDto);

    void delete(Long id);
}
