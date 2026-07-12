package ru.practicum.category.service;

import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.common.eventService.category.dto.CategoryDto;

import java.util.List;

public interface CategoryPublicService {

    List<CategoryDto> getListCategories(Pageable pageable);

    CategoryDto getCategoryById(Long id);
}
