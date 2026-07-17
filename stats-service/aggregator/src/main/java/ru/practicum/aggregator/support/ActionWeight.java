package ru.practicum.aggregator.support;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;

public final class ActionWeight {
    private ActionWeight() {}

    public static double from(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}