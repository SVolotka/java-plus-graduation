package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.Interaction;
import ru.practicum.analyzer.repository.InteractionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserActionsConsumer {
    private final InteractionRepository interactionRepository;

    @KafkaListener(topics = "stats.user-actions.v1", containerFactory = "userActionsKafkaListenerContainerFactory")
    public void consume(UserActionAvro action) {
        float rating = switch (action.getActionType()) {
            case VIEW -> 0.3f;
            case REGISTER -> 0.6f;
            case LIKE -> 1.0f;
        };
        double weight = switch (action.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };

        Optional<Interaction> existing = interactionRepository.findByUserIdAndEventId(
                action.getUserId(), action.getEventId());

        if (existing.isPresent()) {
            Interaction interaction = existing.get();
            if (weight > interaction.getWeight()) {
                interaction.setRating(rating);
                interaction.setWeight(weight);
                interaction.setTimestamp(action.getTimestamp());
                interactionRepository.save(interaction);
            }
        } else {
            Interaction interaction = Interaction.builder()
                    .userId(action.getUserId())
                    .eventId(action.getEventId())
                    .rating(rating)
                    .weight(weight)
                    .timestamp(action.getTimestamp())
                    .build();
            interactionRepository.save(interaction);
        }
    }
}