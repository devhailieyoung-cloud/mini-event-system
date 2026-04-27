package com.hailie.mini_event_system.domain.repository;

import com.hailie.mini_event_system.domain.document.GameDislike;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GameDislikeRepository extends MongoRepository<GameDislike, String> {

    Optional<GameDislike> findByMemberNoAndGameId(long memberNo, String gameId);

    void deleteByMemberNoAndGameId(long memberNo, String gameId);
}
