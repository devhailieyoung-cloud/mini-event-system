# Kafka

## Topic 별 설명

### game-like

| 항목                | 설명                                                                                                                                 |
|-------------------|------------------------------------------------------------------------------------------------------------------------------------|
| **목적**            | MongoDB Update                                                                                                                     |
| **발행 상황**         | POST 좋아요, 싫어요 API 들어왔을 때                                                                                                           |
| **applicationId** | `game-like-streams`                                                                                                                |
| **key**           | `interaction-type:gameId`<br>- 같은 키는 항상 같은 파티션으로 라우팅 (하나의 컨슈머만 처리)<br>- groupId 동일하면 여러 consumer app 띄워도 동일하게 적용<br>- state store 공유 위해 gameId를 키로 사용 |
| **value**         | `{gameId, userId, status, timestamp}`                                                                                              |
| **compact 설정**    | 특정 key값으로 마지막 최신 값만 필요하므로 compact 설정                                                                                               |
| **Consumer 작업**   | MongoDB Upsert<br>CountData(game_ACTION_COUNT)는 특정 조건(수량, 시간) 만족 시 MongoDB flush                                                   |

### game-{play/downloads}

| 항목                | 설명                                                                               |
|-------------------|----------------------------------------------------------------------------------|
| **목적**            | MongoDB Update                                                                   |
| **발행 상황**         | POST 플레이/다운로드 API 들어왔을 때                                                         |
| **applicationId** | `game-{play/downloads}-streams`                                                  |
| **key**           | `interaction-type:gameId`                                                        |
| **value**         | `{gameId, userId, timestamp}`                                                    |
| **Consumer 작업**   | MongoDB Upsert<br>CountData(game_ACTION_COUNT)는 특정 조건(수량, 시간) 만족 시 MongoDB flush |

### game-{like/play/downloads}-error-redis

| 항목                | 설명                                                                                                                                                                             |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **목적**            | Redis에 CountData 저장/갱신 실패했을 때, 관련 대응을 위한 이벤트<br>MongoDB에 데이터 Upsert되기 전에 Redis를 CountData 임시 저장소로 사용하므로, Redis 데이터 갱신 실패 시 관련 대응 작업 필요                                         |
| **발행 상황**         | Redis에 CountData 갱신하는데 실패했을 때 발행<br>각 POST 좋아요/싫어요/플레이/다운로드 모든 상황에서 발생 가능                                                                                                      |
| **applicationId** | 각 해당하는 interactionType(like/play/downloads) 정상 이벤트와 동일<br>State-Store(buffer) 공유 위함                                                                                            |
| **key**           | 각 해당하는 interactionType(like/play/downloads) 정상 이벤트와 동일                                                                                                                         |
| **value**         | 각 해당하는 interactionType(like/play/downloads) 정상 이벤트와 동일                                                                                                                         |
| **Consumer 작업**   | MongoDB에 데이터 즉시 업데이트 및 관련 Redis CountData evict 처리<br>- Redis CountData Evict 하여도 해당 작업을 통해 MongoDB 데이터가 최신화되어 있는 상태이므로 문제 없음<br>- 단, Redis Evict 실패 시 SIP Alert 전송하여 수동 처리 필요 |

**비고**

- Redis CountData를 evict 하더라도 MongoDB 데이터가 최신화되어 있다면 복구 가능
- Redis ev 실패하면 Alert 전송 후 수동 대응 등 고려

### Kafka Consumer - 특정 조건 만족 시 MongoDB Update 방식

Kafka Streams가 관리하는 **KeyValueStore 형태의 버퍼**를 이용해 CountData를 모은 뒤, 조건 만족 시 MongoDB에 flush

```
Kafka Topic (like-events)
   ↓
Kafka Streams (transform)
   ↓
KeyValueStore (gameId → count)
   ↓
조건 만족 (100개 도달 or 10초/Redis TTL 경과)
   ↓
MongoDB flush (incrementLikeCount 등)
```

> 내부적으로 Buffer(ConcurrentHashMap) 구현, 스케줄러 이용하여 MongoDB flush