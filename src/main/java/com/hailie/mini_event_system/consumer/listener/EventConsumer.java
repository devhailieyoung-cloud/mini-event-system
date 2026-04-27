package com.hailie.mini_event_system.consumer.listener;

import com.hailie.mini_event_system.common.dto.InteractionEventMessage;
import com.hailie.mini_event_system.common.kafka.KafkaTopics;
import com.hailie.mini_event_system.consumer.buffer.CountBuffer;
import com.hailie.mini_event_system.consumer.buffer.CountBufferFlusher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final CountBuffer countBuffer;
    private final CountBufferFlusher flusher;

    @KafkaListener(topics = KafkaTopics.GAME_LIKE)
    public void consumeLike(InteractionEventMessage message) {
        String key = message.interactionType().name() + ":" + message.gameId();
        countBuffer.accumulate(key).ifPresent(delta -> flusher.flush(key, delta));
    }

    @KafkaListener(topics = KafkaTopics.GAME_PLAY)
    public void consumePlay(InteractionEventMessage message) {
        String key = message.interactionType().name() + ":" + message.gameId();
        countBuffer.accumulate(key).ifPresent(delta -> flusher.flush(key, delta));
    }

    @KafkaListener(topics = KafkaTopics.GAME_DOWNLOAD)
    public void consumeDownload(InteractionEventMessage message) {
        String key = message.interactionType().name() + ":" + message.gameId();
        countBuffer.accumulate(key).ifPresent(delta -> flusher.flush(key, delta));
    }
}
