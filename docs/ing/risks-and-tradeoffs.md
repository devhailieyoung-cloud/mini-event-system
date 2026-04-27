# Risks And TradeOffs

## Hot Key 문제

동일 key 기반 파티셔닝 특성상 특정 게임에 트래픽이 집중될 경우 hot partition이 발생할 수 있으며,  
이를 완화하기 위한 key 분산 전략은 집계 단위 및 순차 처리 보장과의 트레이드오프를 가진다.

### (1) Background

Kafka는 key 기준으로 partition을 선택하고, consumer는 partition 단위로 메시지를 처리한다.  
이로 인해 동일 key는 항상 동일 partition으로 라우팅되며, 해당 partition 내에서는 순차 처리가 보장된다.

또한 Kafka Streams에서는 partition 단위로 task가 생성되며,  
각 task가 해당 partition의 데이터 처리와 state store를 함께 담당한다.

### (2) Risk

이러한 구조 하에서 기본 key를 `gameId`로만 구성할 경우,  
특정 인기 콘텐츠 구간에 이벤트가 집중되면  
동일 key 또는 특정 key 범위가 동일 파티션으로 몰리게 된다.

특히 단일 key에 트래픽이 집중되는 hot key 상황이 발생할 경우,  
해당 key가 속한 파티션으로 모든 이벤트가 집중되면서  
해당 파티션을 담당하는 consumer task에 부하가 집중된다.

이로 인해 partition skew가 발생할 수 있으며,  
전체 애플리케이션의 처리량이 아니라 특정 파티션의 처리 속도가 병목이 되는 상황이 발생할 수 있다.

특정 partition의 처리 병목은 consumer lag 증가로 나타날 수 있으며,  
이는 결과적으로 state store 반영 및 downstream(DB flush) 지연으로 이어질 수 있다.

또한 특정 key는 하나의 partition에서만 처리되므로,  
해당 key에 대해서는 병렬 처리 이점을 활용할 수 없다는 한계가 있다.

한편, partition 수를 증가시키더라도 특정 key는 하나의 partition에만 매핑되므로  
key 기반 파티셔닝 구조에서는 hot key로 인한 부하 집중 문제를 근본적으로 해결할 수 없다.

<details>
<summary>partition skew/consumer lag</summary>

> **Partition skew**  
> key 기반 파티셔닝 구조에서 특정 key 또는 key 범위에 트래픽이 집중되어,
> 일부 파티션에 데이터와 처리 부하가 편중되는 현상,
> Hot key는 partition skew를 유발하는 주요 원인 중 하나이다.

> **Consumer lag**  
> Kafka에서 consumer가 아직 처리하지 못한 메시지의 개수,
(= 최신 offset − 소비 완료 offset)

</details>

### Trade-off

기본 key를 `gameId`로 사용하는 대신,  
`interaction-type:gameId` 형태로 key를 확장하여  
이벤트 유형 단위로 key 공간을 분리하였다.

이를 통해 동일 유형 이벤트는 순차 처리 및 집계 일관성을 유지하면서,  
서로 다른 유형 이벤트가 하나의 key에 집중되는 현상을 완화한다.

그러나 이 설계는 다음과 같은 트레이드오프를 가진다.

- 이벤트 유형 단위로 key가 분리됨에 따라, 동일 엔터티 기준의 통합 집계는 스트림 단계에서 보장되지 않는다.
- 유형 간 aggregation이 필요한 경우 downstream 단계에서 추가 aggregation 또는 merge가 필요하다.
- 집계 시점과 방식에 따라 최종 반영 지연 또는 일시적인 불일치가 발생할 수 있다.

### Mitigation

- 동일 유형 내에서는 순차 처리 및 집계 일관성을 유지한다.
- 유형 간 이벤트는 DB 반영 시점에서 통합하여 최종 결과를 구성한다.
- state store에 일정 시간 또는 건수 기준으로 batch를 구성하여 DB로 flush함으로써  
  write 부하를 제어하고 반영 지연을 완화한다.
- consumer lag, processing latency 등의 지표를 수집하고,  
  모니터링 시스템을 통해 임계치 초과 시 alert를 발생시키도록 구성한다.

(2) UserData vs CountData 정합성 깨질 가능성

(3) 각 컴포넌트 장애 시 대처법
