package com.hailie.mini_event_system.common.kafka;

public final class KafkaTopics {

    public static final String GAME_LIKE = "game-like";
    public static final String GAME_PLAY = "game-play";
    public static final String GAME_DOWNLOAD = "game-download";

    public static final String GAME_LIKE_ERROR_REDIS = "game-like-error-redis";
    public static final String GAME_PLAY_ERROR_REDIS = "game-play-error-redis";
    public static final String GAME_DOWNLOAD_ERROR_REDIS = "game-download-error-redis";

    private KafkaTopics() {}
}
