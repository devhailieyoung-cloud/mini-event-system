# 데이터 구조 설계 (MongoDB, Redis)

## MongoDB

#### ACTION_COUNTS

하나의 GAME 대한 ACTION COUNT 들

```json
{
  "_id": "ObjectId(\"...\")",
  "ref_game_id": "ObjectId(\"...\")",
  "like_count": 123,
  "dislike_count": 4,
  "download_count": 7890,
  "play_count": 456,
  "updated_at": "..."
}
```

- 한 게임에 대해 1개의 도큐먼트 생성

#### GAME_LIKES

유저가 LIKE 누른 GAME

```json
{
  "_id": "ObjectId(\"...\")",
  "member_no": 1234,
  "ref_game_id": "ObjectId(\"...\")",
  "updated_at": "..."
}
```

**인덱스:**

- `(memberNo, ref_game_id)` — 유일 제약 + 복합 조회 최적화 목적
    - 1명당 좋아요 수가 적고 결과 전체 캐싱되므로 단독 조회도 해당 인덱스로 커버 가능
- `(ref_game_id)` — 단일 게임에 좋아요 많은 구조 대비 → 역방향 조회 성능 위해 추가

**비즈니스 규칙:**

- 좋아요 해제 시 그냥 delete
- 좋아요 ↔ 싫어요 항상 하나만 존재해야하는 제약 조건은 Application Level에서 처리

#### GAME_DISLIKES

유저가 DISLIKE 누른 GAME

```json
{
  "_id": "ObjectId(\"...\")",
  "member_no": 1234,
  "ref_game_id": "ObjectId(\"...\")",
  "updated_at": "..."
}
```

**인덱스:**

- `(memberNo, ref_game_id)` — 유일 제약 + 복합 조회 최적화 목적
    - 1명당 싫어요 수가 적고 결과 전체 캐싱되므로 단독 조회도 해당 인덱스로 커버 가능
- `(ref_game_id)` — 단일 게임에 대한 싫어요 많은 구조 대비 → 역방향 조회 성능 위해 추가

**비즈니스 규칙:**

- 좋아요/싫어요 해제 시 그냥 delete
- 좋아요 ↔ 싫어요 항상 하나만 존재해야하는 제약 조건은 Application Level에서 처리

----

## Redis

| Key                                           | 설명                                                              | 구조                                                                                                                          |
|-----------------------------------------------|-----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| `{like/dislike/play/download}:count:{gameId}` | 단일 게임 카운트 관리 (타입별)                                              | value: 숫자<br>`INCR {like/dislike/play/download}:count:{gameId}`<br>TTL 지정 X                                                 |
| `like:status:{userId}`                        | 특정 유저가 어떤 게임에 어떤 상태를 눌렀는지 (캐시 용도)                               | field: `gameId`<br>value: `like` / `dislike` / `none`<br>`HSET like:status:{userId} {gameId} {like/dislike/none}`<br>TTL 지정 |
| `fail:kafka:{like/play/download}`             | MongoDB에 CountData Upsert 실패한 UgcIds 저장<br>자세한 로직은 시퀀스 다이어그램 참고 | value: Set (gameIds)<br>TTL 지정 X                                                                                            |
