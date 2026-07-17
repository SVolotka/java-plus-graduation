package ru.yandex.practicum.kafka.serializer;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

public class ConfluentAvroSerializer implements Serializer<SpecificRecordBase> {

    private final GeneralAvroSerializer avroSerializer = new GeneralAvroSerializer();

    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        byte[] avroBytes = avroSerializer.serialize(topic, data);
        if (avroBytes == null) {
            return null;
        }
        byte[] confluentBytes = new byte[5 + avroBytes.length];
        confluentBytes[0] = 0x00;
        confluentBytes[1] = 0;
        confluentBytes[2] = 0;
        confluentBytes[3] = 0;
        confluentBytes[4] = 0;
        System.arraycopy(avroBytes, 0, confluentBytes, 5, avroBytes.length);
        return confluentBytes;
    }
}