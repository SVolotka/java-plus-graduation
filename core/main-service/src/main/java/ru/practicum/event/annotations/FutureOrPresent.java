package ru.practicum.event.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureOrPresentValidator.class)
public @interface FutureOrPresent {
    String message() default "Невозможно установить дату и время на уже наступившую.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
