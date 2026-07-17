package ru.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.collector.mapper.UserActionMapper;
import ru.yandex.practicum.grpc.stats.collector.UserActionControllerGrpc;
import ru.yandex.practicum.grpc.stats.collector.UserActionProto;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.concurrent.ExecutionException;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionControllerImpl extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionMapper mapper;
    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    @Value("${collector.kafka.user-actions-topic}")
    private String userActionsTopic;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            UserActionAvro action = mapper.map(request);
            // .get() гарантирует, что мы дождемся подтверждения от Kafka
            kafkaTemplate.send(userActionsTopic, action.getUserId(), action).get();
            log.info("✅ Успешно отправлено действие type={} в топик {}", action.getActionType(), userActionsTopic);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.error("❌ Ошибка валидации: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Отправка в Kafka прервана", e);
            responseObserver.onError(Status.UNAVAILABLE.withDescription("Отправка прервана").asRuntimeException());
        } catch (ExecutionException e) {
            // Здесь мы увидим истинную причину, если Kafka отвергла сообщение
            log.error("❌ Ошибка при отправке в Kafka: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Ошибка Kafka: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())).asRuntimeException());
        } catch (Exception e) {
            log.error("❌ Неожиданная ошибка: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Неожиданная ошибка").asRuntimeException());
        }
    }
}