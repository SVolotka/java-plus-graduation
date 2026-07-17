package ru.yandex.practicum.kafka.deserializer;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class ConfluentAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final BaseAvroDeserializer<T> baseDeserializer;

    public ConfluentAvroDeserializer(Schema schema) {
        this.baseDeserializer = new BaseAvroDeserializer<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        if (data.length < 5) {
            throw new SerializationException("Data too short for Confluent wire format");
        }
        byte[] avroBytes = new byte[data.length - 5];
        System.arraycopy(data, 5, avroBytes, 0, avroBytes.length);
        return baseDeserializer.deserialize(topic, avroBytes);
    }
}