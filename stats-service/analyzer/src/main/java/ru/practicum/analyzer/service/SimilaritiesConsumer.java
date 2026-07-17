package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.Similarity;
import ru.practicum.analyzer.repository.SimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Service
@RequiredArgsConstructor
public class SimilaritiesConsumer {
    private final SimilarityRepository similarityRepository;

    @KafkaListener(topics = "stats.events-similarity.v1", containerFactory = "similaritiesKafkaListenerContainerFactory")
    public void consume(EventSimilarityAvro similarityAvro) {
        Similarity similarity = Similarity.builder()
                .event1(similarityAvro.getEventA())
                .event2(similarityAvro.getEventB())
                .similarity((float) similarityAvro.getScore())
                .timestamp(similarityAvro.getTimestamp())
                .build();
        similarityRepository.save(similarity);
    }
}