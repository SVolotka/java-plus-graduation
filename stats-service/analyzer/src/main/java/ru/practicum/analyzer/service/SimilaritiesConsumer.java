package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.Similarity;
import ru.practicum.analyzer.repository.SimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SimilaritiesConsumer {
    private final SimilarityRepository similarityRepository;

    @Transactional
    @KafkaListener(topics = "stats.events-similarity.v1", containerFactory = "similaritiesKafkaListenerContainerFactory")
    public void consume(EventSimilarityAvro similarityAvro) {
        Long event1 = similarityAvro.getEventA();
        Long event2 = similarityAvro.getEventB();
        Float similarity = (float) similarityAvro.getScore();
        Instant timestamp = similarityAvro.getTimestamp();

        int updated = similarityRepository.updateSimilarity(event1, event2, similarity, timestamp);
        if (updated == 0) {
            similarityRepository.save(Similarity.builder()
                    .event1(event1)
                    .event2(event2)
                    .similarity(similarity)
                    .timestamp(timestamp)
                    .build());
        }
    }
}