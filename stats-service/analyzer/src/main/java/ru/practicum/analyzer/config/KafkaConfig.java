package ru.practicum.analyzer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.kafka.deserializer.ConfluentEventSimilarityAvroDeserializer;
import ru.yandex.practicum.kafka.deserializer.UserActionAvroDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<Long, UserActionAvro> userActionConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-user-actions");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, UserActionAvro> userActionsKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<Long, UserActionAvro>();
        factory.setConsumerFactory(userActionConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<Long, EventSimilarityAvro> similarityConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ConfluentEventSimilarityAvroDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-similarities");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, EventSimilarityAvro> similaritiesKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<Long, EventSimilarityAvro>();
        factory.setConsumerFactory(similarityConsumerFactory());
        return factory;
    }
}