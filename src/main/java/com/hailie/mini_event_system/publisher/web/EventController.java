package com.hailie.mini_event_system.publisher.web;

import com.hailie.mini_event_system.publisher.application.EventService;
import com.hailie.mini_event_system.publisher.web.dto.DownloadEventRequest;
import com.hailie.mini_event_system.publisher.web.dto.LikeEventRequest;
import com.hailie.mini_event_system.publisher.web.dto.PlayEventRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/like")
    public ResponseEntity<Void> like(@RequestBody @Valid LikeEventRequest request) {
        eventService.publishLikeEvent(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/dislike")
    public ResponseEntity<Void> dislike(@RequestBody @Valid LikeEventRequest request) {
        eventService.publishDislikeEvent(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/play")
    public ResponseEntity<Void> play(@RequestBody @Valid PlayEventRequest request) {
        eventService.publishPlayEvent(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/download")
    public ResponseEntity<Void> download(@RequestBody @Valid DownloadEventRequest request) {
        eventService.publishDownloadEvent(request);
        return ResponseEntity.accepted().build();
    }
}
