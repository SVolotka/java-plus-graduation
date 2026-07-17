package ru.yandex.practicum.kafka.deserializer;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class ConfluentEventSimilarityAvroDeserializer extends ConfluentAvroDeserializer<EventSimilarityAvro> {
    public ConfluentEventSimilarityAvroDeserializer() {
        super(EventSimilarityAvro.getClassSchema());
    }
}