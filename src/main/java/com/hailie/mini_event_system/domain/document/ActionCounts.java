package com.hailie.mini_event_system.domain.document;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Builder
@Document(collection = "action_counts")
public class ActionCounts {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("game_id")
    private String gameId;

    @Field("like_count")
    private long likeCount;

    @Field("dislike_count")
    private long dislikeCount;

    @Field("download_count")
    private long downloadCount;

    @Field("play_count")
    private long playCount;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
