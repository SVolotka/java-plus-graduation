package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequestDto;
import ru.practicum.user.entity.User;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminServiceImpl implements UserAdminService {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 250;
    private static final int MIN_EMAIL_LENGTH = 6;
    private static final int MAX_EMAIL_LENGTH = 254;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto create(UserRequestDto userRequestDto) {
        log.info("СОЗДАНИЕ ПОЛЬЗОВАТЕЛЯ");
        log.info("проверка на существования такого email");
        validateUserFields(userRequestDto);

        if (userRepository.existsByEmail(userRequestDto.getEmail())) {
            throw new ConflictException("пользователь с таким Email уже существует");
        }

        User userCreate = userMapper.toUser(userRequestDto);

        log.info("сохранение пользователя в БД");
        userRepository.save(userCreate);

        log.info("возвращение созданного пользователя");
        return userMapper.toUserDto(userCreate);
    }

    @Override
    public List<UserDto> getAllUsers(List<Long> ids, Pageable pageable) {
        log.info("ВЫВОД ВСЕХ ПОЛЬЗОВАТЕЛЕЙ");
        if (ids != null && !ids.isEmpty()) {
            return userRepository.findByIdIn(ids, pageable)
                    .map(userMapper::toUserDto)
                    .getContent();
        } else {
            return userRepository.findAll(pageable)
                    .map(userMapper::toUserDto)
                    .getContent();
        }
    }

    @Override
    public UserDto getUserById(Long id) {
        log.info("вывод пользователя по id = {}", id);
        User userGetById = userRepository.findById(id).orElseThrow(
                () -> new NotFoundException("пользователь с id = " + id + " не существует")
        );
        return userMapper.toUserDto(userGetById);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("удаление пользователя с id = {}", id);
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("пользователя с id = " + id + " не существует");
        }
        userRepository.deleteById(id);
        log.info("пользователь с id = {} удален", id);
    }

    private void validateUserFields(UserRequestDto userRequestDto) {
        log.debug("Валидация полей пользователя: {}", userRequestDto);

        validateEmail(userRequestDto.getEmail());
        validateName(userRequestDto.getName());
    }

    private void validateEmail(String email) {
        if (email == null) {
            throw new ValidationException("email не может быть null");
        }

        if (email.isBlank()) {
            throw new ValidationException("email не может быть пустым");
        }

        if (email.length() < MIN_EMAIL_LENGTH) {
            throw new ValidationException(
                    String.format("email должен содержать минимум %d символов (текущая длина: %d)",
                            MIN_EMAIL_LENGTH, email.length())
            );
        }

        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException(
                    String.format("email не может быть длиннее %d символов (текущая длина: %d)",
                            MAX_EMAIL_LENGTH, email.length())
            );
        }
    }

    private void validateName(String name) {
        if (name == null) {
            throw new ValidationException("имя не может быть null");
        }

        if (name.isBlank()) {
            throw new ValidationException("имя не может быть пустым или состоять только из пробелов");
        }

        String trimmedName = name.trim();
        if (trimmedName.length() < MIN_NAME_LENGTH) {
            throw new ValidationException(
                    String.format("имя должно содержать минимум %d символа (текущая длина: %d)",
                            MIN_NAME_LENGTH, trimmedName.length())
            );
        }

        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(
                    String.format("имя не может быть длиннее %d символов (текущая длина: %d)",
                            MAX_NAME_LENGTH, trimmedName.length())
            );
        }
    }
}
