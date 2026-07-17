package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionKafkaListener {

    private final SimilarityState similarityState;
    private final KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate;

    @Value("${aggregator.kafka.events-similarity-topic}")
    private String eventsSimilarityTopic;

    @KafkaListener(
            topics = "${aggregator.kafka.user-actions-topic}",
            groupId = "${aggregator.kafka.consumer-group}"
    )
    public void consume(UserActionAvro action, Acknowledgment acknowledgment) throws Exception {
        similarityState.process(action, (similarities, ignore) -> {
            try {
                CompletableFuture<?>[] publications = similarities.stream()
                        .map(similarity -> {
                            long key = similarity.getEventA();
                            return kafkaTemplate.send(eventsSimilarityTopic, key, similarity);
                        })
                        .toArray(CompletableFuture[]::new);
                CompletableFuture.allOf(publications).get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to publish similarities", e);
            }
        });
        acknowledgment.acknowledge();
        log.debug("Processed user action type={} for event={}", action.getActionType(), action.getEventId());
    }
}