package com.hailie.mini_event_system.publisher.kafka;

import com.hailie.mini_event_system.common.dto.InteractionEventMessage;
import com.hailie.mini_event_system.common.kafka.KafkaTopics;
import com.hailie.mini_event_system.domain.type.InteractionType;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventProducer {

    private final KafkaTemplate<String, InteractionEventMessage> kafkaTemplate;

    public void send(InteractionEventMessage message) {
        String topic = resolveTopic(message.interactionType());
        String key = message.interactionType().name() + ":" + message.gameId();
        kafkaTemplate.send(topic, key, message);
    }

    private String resolveTopic(InteractionType interactionType) {
        return switch (interactionType) {
            case LIKE, DISLIKE -> KafkaTopics.GAME_LIKE;
            case PLAY -> KafkaTopics.GAME_PLAY;
            case DOWNLOAD -> KafkaTopics.GAME_DOWNLOAD;
        };
    }
}
