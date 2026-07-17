package ru.practicum.aggregator.model;

public record EventPair(long first, long second) {
    public static EventPair of(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return new EventPair(first, second);
    }

    public String kafkaKey() {
        return first + "-" + second;
    }
}