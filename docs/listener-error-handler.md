# Consumer Module Listener Retry Logic

- 자세한 로직은 KafkaErrorHandler와 BubblyzRetryListener 참고

## 요약

### 1. 에러 발생 → 에러 종류 확인

- 즉시 종료 케이스
    - 메시지 변환 실패 (`MessageConversionException`)
    - 역직렬화 실패 (`DeserializationException`)
    - 메서드 파라미터 타입 불일치 (`MethodArgumentTypeMismatchException`)
    - `retryable=false`로 표시된 예외 (`BubblyzKafkaHandlingError`)
- 재시도 케이스
    - 위 조건에 해당하지 않는 모든 예외

> **참고**  
> 특정 오류 발생 시 재시도를 하지 않기를 원한다면  
> `BubblyzKafkaHandlingError`를 던지고 `isRetryable=false`로 설정하면 즉시 종료됨.

### 2. 재시도 진행 시

- 설정된 지연 시간 + 최대 횟수에 따라 다시 시도
- 예: es-mod-save-retry는 2초 간격으로 최대 5번(yml 기반)
- 매번 실패할 때마다 경고 로그 남김, 재시도 하여 작업 성공 시 즉시 종료

### 3. 설정된 횟수만큼 재시도 완료 시

- 원본 토픽이 retry-key를 갖고 있으면 → 지정된 Retry 토픽으로 재발행
- retry-key가 없으면 → 추가 전송 없이 로그만 남기고 종료

### application.yml 설정 예시

메인(`mod-change-stream`)에서 실패   
→ Retry 토픽(`bubblyz-es-retry`)으로 보내어 해당 토픽에서 retry 수행   
→ Retry 토픽에서 또 실패해도 더 이상 재발행 없음

```
kafka:
  binding-infos:
    es-mod-save:
      topic: "${spring.profiles.active}-mod-change-stream"
      group-id: "${spring.profiles.active}-es-mod-save"
      retry-key: "es-mod-save-retry" # 메인 → Retry 토픽 매핑만 제공 (메인 자체의 재시도 설정은 생략)
    es-mod-save-retry:
      topic: "${spring.profiles.active}-bubblyz-es-retry"
      group-id: "${spring.profiles.active}-es-mod-save-retry"
      retry:
        attempts: 5
        delay: 2000ms
        enabled: true`
```

## Detail

### 1. 토픽별 BackOff 적용

- `DefaultErrorHandler`를 상속하고 `setBackOffFunction`을 사용해 토픽별 지연(Delay)·횟수(Attempts) 정책을 지정.
- 즉, 레코드의 topic 단위로 서로 다른 재시도 정책을 적용할 수 있음. (yml 통해 값 지정)

### 2. RetryListener 연동

- `setRetryListeners(bubblyzRetryListener)`로 리스너를 등록.
- 동작:
    - 재시도 실패 시마다 `failedDelivery(...)` 호출 → 실패 로그 기록.
    - 재시도 횟수 소진(재시도 횟수 0 포함) 시 `recovered(...)` 호출 → 후속 처리(예: Retry 토픽 발행)

### 3. 즉시 종료 케이스 처리

- `isKafkaLevelPreListenerFailure(...)`
    - 역직렬화 실패, 메시지 변환 실패, 타입 불일치 → 재시도 없이 종료
- `isNotRetryable(...)`
    - `retryable=false`로 표시된 커스텀 예외 → 재시도 없이 종료
- 위 두 경우에는 `super.handleOne(...)`를 타지 않으므로 실제 재시도 발생하지 않음

### 4. Retry 토픽 발행 구조

- `retry-key`가 지정된 토픽은 실패 시 대응하는 Retry 토픽으로 재발행
- 매핑 구조: `source.topic → retry.topic`
- 이 매핑은 `recovered(...)` 단계에서만 실행됨




