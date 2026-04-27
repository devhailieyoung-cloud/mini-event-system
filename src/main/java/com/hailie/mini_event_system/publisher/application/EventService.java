package com.hailie.mini_event_system.publisher.application;

import com.hailie.mini_event_system.common.dto.InteractionEventMessage;
import com.hailie.mini_event_system.domain.type.InteractionType;
import com.hailie.mini_event_system.publisher.kafka.EventProducer;
import com.hailie.mini_event_system.publisher.web.dto.DownloadEventRequest;
import com.hailie.mini_event_system.publisher.web.dto.LikeEventRequest;
import com.hailie.mini_event_system.publisher.web.dto.PlayEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventProducer eventProducer;

    public void publishLikeEvent(LikeEventRequest request) {
        eventProducer.send(toMessage(request.gameId(), request.memberNo(), InteractionType.LIKE));
    }

    public void publishDislikeEvent(LikeEventRequest request) {
        eventProducer.send(toMessage(request.gameId(), request.memberNo(), InteractionType.DISLIKE));
    }

    public void publishPlayEvent(PlayEventRequest request) {
        eventProducer.send(toMessage(request.gameId(), request.memberNo(), InteractionType.PLAY));
    }

    public void publishDownloadEvent(DownloadEventRequest request) {
        eventProducer.send(toMessage(request.gameId(), request.memberNo(), InteractionType.DOWNLOAD));
    }

    private InteractionEventMessage toMessage(String gameId, long memberNo, InteractionType type) {
        return new InteractionEventMessage(gameId, memberNo, type, LocalDateTime.now());
    }
}
