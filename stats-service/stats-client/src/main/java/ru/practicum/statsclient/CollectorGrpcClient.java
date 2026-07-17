package ru.practicum.statsclient;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.stats.collector.ActionTypeProto;
import ru.yandex.practicum.grpc.stats.collector.UserActionControllerGrpc;
import ru.yandex.practicum.grpc.stats.collector.UserActionProto;

import java.time.Instant;

@Component
public class CollectorGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void sendUserAction(long userId, long eventId, ActionTypeProto actionType) {
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(Instant.now().getEpochSecond())
                .setNanos(Instant.now().getNano())
                .build();
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(timestamp)
                .build();
        collectorStub.collectUserAction(request);
    }
}