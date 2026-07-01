package ru.practicum.event.annotations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeStartValidator implements ConstraintValidator<DateTimeStart, String> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private long value;

    @Override
    public void initialize(DateTimeStart constraintAnnotation) {
        this.value = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            return LocalDateTime.parse(s, formatter).minusHours(value).isAfter(LocalDateTime.now());
    }
}
