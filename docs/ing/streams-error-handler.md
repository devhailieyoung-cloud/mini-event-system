# Consumer Module Streams Retry Logic

- (label, modId) 단위로 단일 재시도 상태 관리
- 커스텀 isRetryable(), 일부 Kafka 예외만 재시도
- 재시도 가능한 예외만 일정 시간 딜레이(+지터) 후 다시 실행되며, 최대 횟수 초과 시 종료
- 중복 예약(key 단위)은 막고, 성공 시 즉시 상태 정리
- 자세한 로직은 AbstractBufferProcessor 참고

### 1. Retry 단위

- Key 단위: (label, modId)
    - label: 이벤트 유형, ex. append-event, handle-redis-failure
- 상태 관리: StreamsRetryState (시도 횟수, 예약 작업, 마지막 에러)

### 2. Retry 진입 시점

- 재시도 진입 지점
    * appendEventToStore(event)
    * handleRedisFailureFallback에서 발생한, flushIfBufferFull(modId, 1)
        * (참고) main-topic에서 발생한 flushIfBufferFull 처리는 `runSafely`로만 수행
            - 이유: `context.schedule(...)` 로 주기적으로 버퍼 만료 여부를 점검하여 flush를 트리거하므로,
              별도의 재시도 예약(runWithRetry)이 불필요함
- 재시도 방식
    * runWithRetry(Runnable task, String label, T event)를 통해 래핑
    * 성공 시 상태 초기화, 실패 시 예외 유형별로 재시도 여부 결정

### 3. Retry 동작 흐름

- 이벤트 처리 시 runWithRetry(task, label, event) 실행
- 성공 시 → 현재 상태 취소 및 제거
- 예외 발생 시 분기:
    * BubblyzKafkaException(retryable=false) → 취소 및 제거
    * BubblyzKafkaException(retryable=true) → 재시도
    * InvalidStateStoreException | TimeoutException | LockException
        * 원인 체인에 TaskMigratedException/CorruptStateException 포함 → 취소 및 제거
        * 그 외 → 재시도
    * 기타 Exception → 취소 및 제거 (무한 재시도 방지)

### 4. Retry 스케줄링

- 최대 횟수
    - triggerSetting.get().getMaxRetry() 초과 시 → 중단 (onMaxRetryExceeded)
- 지연 계산
    * 기본: triggerSetting.get().getRetryDuration()
    * 지터: ±500ms 범위 랜덤 오프셋
- 예약 처리
    * 동일 키/레이블 조합에 기존 예약(scheduled) 있으면 중복 예약 금지
    * append-event 중복은 ERROR 로그, 나머지는 DEBUG 로그
    * 예약 실행(context.schedule)

> **참고**  
> Kafka Streams의 `ProcessorContext.schedule(...)` 메소드를 사용하면  
> 레코드 처리와 punctuator 실행이 경쟁하지 않고 순차적으로 실행  
> 따라서 폭주 방지를 위해 중복 예약 차단과 지터(jitter) 도입을 적용

### 5. 예약 실행 시 동작

- 예약된 콜백 실행 아래와 같은 방식으로 상태 일관성 유지 및 중복 실행 방지

1. 기존 예약 취소(scheduled = null)
2. 동일 태스크에 대해 runWithRetry(...) 재호출

### 6. 상태 수명주기

* 성공 → 상태 삭제
* 재시도 중 → attempts 증가 및 예약 보관
* 최대 재시도 초과 → 로그 후 삭제
* 프로세서 종료 시 → 모든 예약 취소 후 맵 클리어

### [참고] `runSafely()`와 비재시도 로직

* runSafely(Runnable task, String label) = 로깅 후 종료
    * InvalidStateStoreException → WARN
    * BubblyzKafkaException → WARN
    * 기타 Exception → ERROR
* 적용 대상:
    * main-topic 이벤트 처리 과정 중 flushIfBufferFull(modId, maxBufferSize)
    * 주기 스케줄러(flushExpiredKeys(now))
