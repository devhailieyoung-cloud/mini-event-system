# Kafka Listener vs Kafka Streams

## 비교 배경

이 프로젝트의 핵심 Consumer 작업은 아래 흐름이다.

```
이벤트 수신 → key별 카운트 누적 → 조건(수량/시간) 만족 시 MongoDB flush
```

이는 단순한 이벤트 처리가 아니라 **stateful aggregation** 이다.  
같은 요구사항을 Listener와 Streams로 각각 구현하며 차이를 비교한다.

---

## Kafka Listener

### 구현 방식

`@KafkaListener`로 메시지를 수신하고, 상태(버퍼)를 직접 관리한다.

```
@KafkaListener → ConcurrentHashMap 버퍼 적재 → threshold 체크
                                                    ↓ (100건 도달)
                                              MongoDB $inc flush

@Scheduled (10초) → 버퍼 전체 drain → MongoDB $inc flush
```

### 직접 구현이 필요한 것들

| 관심사 | 구현 방법 | 한계 |
|--------|----------|------|
| 상태 저장 | `ConcurrentHashMap` | in-memory → 크래시 시 유실 |
| 스레드 안전성 | `merge()`, `remove(key, value)` CAS | 직접 책임 |
| 시간 기반 flush | `@Scheduled` | Kafka 처리 스레드와 별개 |
| rebalance 대응 | 없음 | 파티션 재할당 시 버퍼 손실 |
| 내구성 | Redis로 별도 보완 | 설계 복잡도 증가 |

### 적합한 케이스

상태가 없는 단순 처리에 잘 맞는다.

```
메시지 수신 → 변환/검증 → DB 저장 or 외부 API 호출
```

이 프로젝트에서는 `game-like-error-redis` 같은 에러 토픽 처리,  
단건 알림 발송 등이 Listener에 더 적합하다.

---

## Kafka Streams

### 구현 방식

Processor API의 `KeyValueStore`와 `Punctuator`를 사용한다.

```
Kafka Topic
   ↓
Streams Processor
   ├─ KeyValueStore (gameId → count) 적재
   │       ↓ (100건 도달)
   │  MongoDB $inc flush (즉시)
   │
   └─ Punctuator (10초 주기)
          ↓
     KeyValueStore 전체 순회 → MongoDB $inc flush
```

### Listener 대비 내장된 것들

| Listener로 직접 구현 | Streams에 내장 |
|-------------------|--------------|
| `ConcurrentHashMap` 버퍼 | `KeyValueStore` (RocksDB 기반) |
| `@Scheduled` flush | `Punctuator` (stream-time 기반) |
| 크래시 복구 없음 | changelog topic으로 자동 복구 |
| rebalance 대응 없음 | state store 체크포인트 + 복원 |
| 수동 스레드 안전성 처리 | 파티션-태스크 1:1 보장 |

### 내구성 보장 원리

Kafka Streams는 `KeyValueStore`의 변경사항을 **changelog topic**에 기록한다.  
서버가 죽거나 파티션이 재할당되더라도, 재시작 시 changelog topic을 replay해 상태를 복원한다.

```
KeyValueStore 변경
   ↓ (자동 기록)
changelog topic (Kafka에 저장)
   ↓ (재시작/rebalance 시 자동 복원)
KeyValueStore 상태 복구
```

이 구조 덕분에 Listener 방식에서 Redis로 별도 보완했던 내구성 문제가 자연스럽게 해결된다.

### 파티션-태스크 1:1 보장

Streams는 파티션 단위로 task를 생성하고, 각 task가 자신의 파티션 데이터와 state store를 함께 담당한다.  
같은 key는 항상 같은 파티션으로 라우팅되므로, 특정 gameId의 카운트는 항상 하나의 task에서만 처리된다.  
스레드 안전성 문제를 애초에 설계 단계에서 제거한다.

---

## 비교 요약

| | Listener | Streams |
|--|---------|---------|
| 이 use case (버퍼 + 집계) | △ 직접 구현 필요 | ✅ 내장 |
| 단순 이벤트 처리 | ✅ 간단 | △ 오버스펙 |
| 상태 내구성 | 직접 책임 (Redis 보완) | changelog topic 자동 보장 |
| rebalance 대응 | 직접 책임 | 자동 체크포인트 + 복원 |
| 스레드 안전성 | 직접 책임 | 파티션-태스크 1:1로 회피 |
| 학습 곡선 | 낮음 | 높음 |
| 운영 복잡도 | 낮음 | 높음 (RocksDB, changelog topic) |

---

## 결론

**stateful aggregation에는 Kafka Streams가 더 적합하다.**

Listener 방식은 단순성이라는 장점이 있지만,  
버퍼링 + 집계 + 내구성이 요구되는 이 use case에서는  
직접 해결해야 할 문제들이 Streams의 학습 비용보다 크다.

다만 프로젝트 내 에러 토픽 처리처럼 상태가 필요 없는 처리는  
Listener를 그대로 유지하는 것이 적합하다.

> **이 프로젝트의 접근 방식**  
> Listener로 먼저 구현한 뒤 동일 로직을 Streams로 전환하며,  
> 두 방식의 차이를 코드 수준에서 비교한다.
