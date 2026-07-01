package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.CategoryRequestDto;
import ru.practicum.category.entity.Category;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryAdminServiceImpl implements CategoryAdminService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventRepository eventRepository;

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MIN_NAME_LENGTH = 1;

    @Override
    @Transactional
    public CategoryDto create(CategoryRequestDto categoryRequestDto) {
        log.info("добавление новой категории");

        validateCategoryName(categoryRequestDto.getName());

        if (categoryRepository.existsByName(categoryRequestDto.getName())) {
            throw new ConflictException("категория с таким именем уже существует");
        }

        Category categoryCreate = categoryMapper.toCategory(categoryRequestDto);
        log.info("сохранение категории");
        categoryRepository.save(categoryCreate);

        return categoryMapper.toCategoryDto(categoryCreate);
    }

    @Override
    @Transactional
    public CategoryDto update(Long id, CategoryRequestDto categoryRequestDto) {
        log.info("обновление категории с id = {}", id);

        if (id == null) {
            throw new ValidationException("обновление category: id не может быть равен null");
        }

        validateCategoryName(categoryRequestDto.getName());

        Category categoryUpdate = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("категория с id = " + id + " не найдена"));

        if (!categoryUpdate.getName().equals(categoryRequestDto.getName()) &&
                categoryRepository.existsByName(categoryRequestDto.getName())) {
            throw new ConflictException("категория с именем " + categoryRequestDto.getName() + " уже существует");
        }

        categoryUpdate.setName(categoryRequestDto.getName());
        categoryRepository.save(categoryUpdate);

        return categoryMapper.toCategoryDto(categoryUpdate);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("удаление категории с id = {}", id);

        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("категория с id = " + id + " не найдена");
        }

        if (eventRepository.existsByCategoryId(id)) {
            throw new ConflictException("удаление не возможно пока существуют события с этой категорией");
        }

        categoryRepository.deleteById(id);
        log.info("категория с id = {} удалена", id);
    }

    private void validateCategoryName(String name) {
        if (name == null) {
            throw new ValidationException("имя категории не может быть null");
        }
        if (name.trim().isEmpty()) {
            throw new ValidationException("имя категории не может быть пустым");
        }

        String trimmedName = name.trim();
        if (trimmedName.length() < MIN_NAME_LENGTH) {
            throw new ValidationException(
                    String.format("имя категории должно содержать минимум %d символ(а) (текущая длина: %d)",
                            MIN_NAME_LENGTH, trimmedName.length())
            );
        }
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(
                    String.format("имя категории не может быть длиннее %d символов (текущая длина: %d)",
                            MAX_NAME_LENGTH, trimmedName.length())
            );
        }
    }
}