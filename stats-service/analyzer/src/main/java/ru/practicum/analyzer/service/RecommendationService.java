package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.Interaction;
import ru.practicum.analyzer.model.Similarity;
import ru.practicum.analyzer.repository.InteractionRepository;
import ru.practicum.analyzer.repository.SimilarityRepository;
import ru.yandex.practicum.grpc.stats.dashboard.RecommendedEventProto;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    private static final int MAX_NEIGHBORS = 20;

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        List<Interaction> recentInteractions = interactionRepository.findByUserIdOrderByTimestampDesc(userId);
        if (recentInteractions.isEmpty()) return Collections.emptyList();

        List<Long> interactedEventIds = recentInteractions.stream()
                .map(Interaction::getEventId)
                .toList();

        Map<Long, Double> candidateMaxSimilarity = new HashMap<>();
        for (Interaction interaction : recentInteractions.stream().limit(MAX_NEIGHBORS).toList()) {
            List<Similarity> similarities = similarityRepository.findByEventId(interaction.getEventId());
            for (Similarity sim : similarities) {
                long candidate = sim.getEvent1().equals(interaction.getEventId()) ? sim.getEvent2() : sim.getEvent1();
                if (!interactedEventIds.contains(candidate)) {
                    candidateMaxSimilarity.merge(candidate, (double) sim.getSimilarity(), Math::max);
                }
            }
        }

        List<Long> topCandidates = candidateMaxSimilarity.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();

        return topCandidates.stream()
                .map(candidate -> {
                    double predictedRating = predictRating(userId, candidate);
                    return RecommendedEventProto.newBuilder()
                            .setEventId(candidate)
                            .setScore(predictedRating)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        List<Long> userInteractedEvents = interactionRepository.findEventIdsByUserId(userId);
        List<Similarity> similarities = similarityRepository.findByEventId(eventId);

        return similarities.stream()
                .map(sim -> {
                    long similarEvent = sim.getEvent1().equals(eventId) ? sim.getEvent2() : sim.getEvent1();
                    return new AbstractMap.SimpleEntry<>(similarEvent, (double) sim.getSimilarity());
                })
                .filter(entry -> !userInteractedEvents.contains(entry.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        return eventIds.stream()
                .map(eventId -> {
                    double sumWeight = interactionRepository.sumWeightByEventId(eventId);
                    return RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(sumWeight)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public boolean hasUserInteraction(long userId, long eventId) {
        return interactionRepository.findByUserIdAndEventId(userId, eventId).isPresent();
    }

    private double predictRating(long userId, long eventId) {
        List<Interaction> userInteractions = interactionRepository.findByUserIdOrderByTimestampDesc(userId);
        Map<Long, Double> interactedRatings = userInteractions.stream()
                .collect(Collectors.toMap(Interaction::getEventId, i -> (double) i.getRating()));

        List<Similarity> similarities = similarityRepository.findByEventId(eventId);
        List<Similarity> sortedSimilarities = similarities.stream()
                .sorted(Comparator.comparingDouble(Similarity::getSimilarity).reversed())
                .filter(s -> interactedRatings.containsKey(
                        s.getEvent1().equals(eventId) ? s.getEvent2() : s.getEvent1()))
                .limit(MAX_NEIGHBORS)
                .toList();

        double weightedSum = 0.0;
        double sumSimilarity = 0.0;
        for (Similarity sim : sortedSimilarities) {
            long otherEvent = sim.getEvent1().equals(eventId) ? sim.getEvent2() : sim.getEvent1();
            Double rating = interactedRatings.get(otherEvent);
            if (rating != null) {
                weightedSum += sim.getSimilarity() * rating;
                sumSimilarity += sim.getSimilarity();
            }
        }
        return sumSimilarity > 0 ? weightedSum / sumSimilarity : 0.0;
    }
}