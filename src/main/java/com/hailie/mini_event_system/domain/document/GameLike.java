package com.hailie.mini_event_system.domain.document;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Builder
@Document(collection = "game_likes")
@CompoundIndexes({
        @CompoundIndex(def = "{'member_no': 1, 'game_id': 1}", unique = true)
})
public class GameLike {

    @Id
    private String id;

    @Field("member_no")
    private long memberNo;

    @Indexed
    @Field("game_id")
    private String gameId;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
