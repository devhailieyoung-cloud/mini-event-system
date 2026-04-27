package com.hailie.mini_event_system.domain.repository;

import com.hailie.mini_event_system.domain.document.ActionCounts;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActionCountsRepository extends MongoRepository<ActionCounts, String> {

    Optional<ActionCounts> findByGameId(String gameId);
}
