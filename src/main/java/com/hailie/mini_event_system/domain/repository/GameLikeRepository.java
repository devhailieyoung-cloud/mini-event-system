package com.hailie.mini_event_system.domain.repository;

import com.hailie.mini_event_system.domain.document.GameLike;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GameLikeRepository extends MongoRepository<GameLike, String> {

    Optional<GameLike> findByMemberNoAndGameId(long memberNo, String gameId);

    void deleteByMemberNoAndGameId(long memberNo, String gameId);
}
