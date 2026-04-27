package com.hailie.mini_event_system.consumer.buffer;

import com.hailie.mini_event_system.domain.type.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CountBufferFlusher {

    private final CountBuffer countBuffer;
    private final MongoTemplate mongoTemplate;

    @Scheduled(fixedDelayString = "${app.buffer.flush-interval-ms:10000}")
    public void flushAll() {
        Map<String, Long> snapshot = countBuffer.drain();
        if (snapshot.isEmpty()) {
            return;
        }
        log.debug("Scheduled flush. entries={}", snapshot.size());
        snapshot.forEach(this::flushToMongo);
    }

    public void flush(String key, long delta) {
        flushToMongo(key, delta);
    }

    private void flushToMongo(String key, long delta) {
        String[] parts = key.split(":", 2);
        InteractionType type = InteractionType.valueOf(parts[0]);
        String gameId = parts[1];
        String field = resolveField(type);

        mongoTemplate.upsert(
                Query.query(Criteria.where("game_id").is(gameId)),
                new Update().inc(field, delta),
                "action_counts"
        );
        log.debug("Flushed to MongoDB. gameId={}, type={}, delta={}", gameId, type, delta);
    }

    private String resolveField(InteractionType type) {
        return switch (type) {
            case LIKE -> "like_count";
            case DISLIKE -> "dislike_count";
            case PLAY -> "play_count";
            case DOWNLOAD -> "download_count";
        };
    }
}
