package ru.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.stats.collector.ActionTypeProto;
import ru.yandex.practicum.grpc.stats.collector.UserActionProto;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.DateTimeException;
import java.time.Instant;

@Component
public class UserActionMapper {

    public UserActionAvro map(UserActionProto source) {
        Instant timestamp;
        try {
            timestamp = Instant.ofEpochSecond(source.getTimestamp().getSeconds(), source.getTimestamp().getNanos());
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid action timestamp", exception);
        }

        return UserActionAvro.newBuilder()
                .setUserId(source.getUserId())
                .setEventId(source.getEventId())
                .setActionType(mapActionType(source.getActionType()))
                .setTimestamp(timestamp)
                .build();
    }

    private ActionTypeAvro mapActionType(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown action type");
        };
    }
}