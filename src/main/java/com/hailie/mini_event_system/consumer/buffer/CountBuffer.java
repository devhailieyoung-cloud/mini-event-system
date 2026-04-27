package com.hailie.mini_event_system.consumer.buffer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CountBuffer {

    @Value("${app.buffer.flush-threshold:100}")
    private int flushThreshold;

    // key: {InteractionType}:{gameId}
    private final ConcurrentHashMap<String, Long> buffer = new ConcurrentHashMap<>();

    /**
     * 버퍼에 1 증가. 임계값 도달 시 해당 key의 누적량을 반환하고 비움.
     */
    public Optional<Long> accumulate(String key) {
        Long newCount = buffer.merge(key, 1L, Long::sum);
        if (newCount >= flushThreshold) {
            Long delta = buffer.remove(key);
            return Optional.ofNullable(delta);
        }
        return Optional.empty();
    }

    /**
     * 버퍼 전체를 스냅샷으로 반환하고 비움. 스케줄러 기반 flush 용도.
     */
    public Map<String, Long> drain() {
        Map<String, Long> snapshot = new HashMap<>(buffer);
        snapshot.forEach((key, value) -> buffer.remove(key, value));
        return snapshot;
    }
}
