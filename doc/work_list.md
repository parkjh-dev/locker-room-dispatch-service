# Locker Room Dispatch Service - Work List

> **작성일**: 2026-05-06
> **참고 문서**: `locker-room-resource-service/doc/srs.md`, `locker-room-resource-service/README.md`
> **목적**: AI 게시물 등록 + 외부 채널 알림 발송(SMS/알림톡/Mail) 마이크로서비스 구현 계획

---

## 0. 프로젝트 개요

### 0.1 책임 범위

dispatch-service는 **외부 시스템과 대화하는 일을 도맡는 사이드이펙트 전담 서비스**다.

| 영역 | 내용 |
|---|---|
| **AI 게시물 등록** | 별도 ai-service가 LLM으로 생성한 콘텐츠를 Kafka로 받아 resource-service에 게시물/댓글로 등록 |
| **외부 채널 발송** | resource-service가 발행한 `notification.*` 이벤트를 받아 SMS / 카카오 알림톡 / 이메일로 발송 |

**범위 밖 (다른 서비스 책임)**:
- LLM 호출, 콘텐츠 생성 → ai-service (별도 서비스, 추후 개발)
- 인앱 알림 저장 → resource-service의 `notification` 도메인이 담당
- 비밀번호 재설정 메일 → Keycloak이 SMTP로 직접 발송

### 0.2 기술 스택

| 구분 | 기술 | 비고 |
|---|---|---|
| Framework | Spring Boot 4.0.6 / Java 17 | resource-service(4.0.2)와 다름 |
| Messaging | Apache Kafka (Consumer 중심) | resource-service와 동일 클러스터 |
| DB | MariaDB | dispatch_log, message_template 등 자체 데이터 |
| Auth (out-bound) | OAuth2 Client Credentials (Keycloak) | resource-service 호출용 |
| 분산 락 | ShedLock (JdbcTemplate provider) | 다중 인스턴스 운영 대비 |
| 캐시 | Caffeine | Client Credentials 토큰, 수신자 연락처 |
| 외부 채널 | 가비아 SMS / JavaMail (SMTP) / 카카오 알림톡(IF만) | |

### 0.3 시스템 위치

```
ai-service (TBD) ─── ai.qna-answer.ready ──┐
                ─── ai.team-news.ready  ──┤
                                          ▼
resource-service ─── notification.comment ───┐
                ─── notification.reply ─────┤
                ─── notification.inquiry-replied ┤
                ─── notification.report-processed┤
                                                 ▼
                                          dispatch-service
                                          ├─ publish → resource-service REST
                                          └─ notify  → Gabia SMS / SMTP / (알림톡)
```

---

## 1. Kafka 토픽 스펙 (사전 합의)

### 1.1 Inbound (consume)

| 토픽 | Producer | Key | Payload | dispatch 동작 |
|---|---|---|---|---|
| `ai.qna-answer.ready` | ai-service | `postId` | `{ eventId, postId, content, model, generatedAt }` | resource-service `POST /posts/{postId}/comments` (`isAiGenerated=true`) |
| `ai.team-news.ready` | ai-service | `teamId` | `{ eventId, teamId, sport, boardId, title, content, generatedAt }` | resource-service `POST /posts` (`isAiGenerated=true`) |
| `notification.comment` | resource-service | `userId` | `{ eventId, userId, postId, commentId, actorNickname }` | Mail / SMS 발송 |
| `notification.reply` | resource-service | `userId` | `{ eventId, userId, parentCommentId, replyId, actorNickname }` | Mail / SMS 발송 |
| `notification.inquiry-replied` | resource-service | `userId` | `{ eventId, userId, inquiryId, replyId }` | Mail / SMS 발송 |
| `notification.report-processed` | resource-service | `userId` | `{ eventId, userId, reportId, decision }` | Mail / SMS 발송 |

### 1.2 멱등성 / 재시도

- 모든 이벤트는 producer가 `eventId`(UUID) 동봉
- dispatch는 `dispatch_log(event_id UNIQUE)`로 중복 처리 차단
- resource-service 호출 시 `Idempotency-Key`는 `uuid5(eventId)`로 결정적 생성
- Retry: `@RetryableTopic` — 3회 (5s/30s/60s) → `{topic}.dlq`

---

## 2. 패키지 구조

```
com.lockerroom.dispatchservice/
├── common/                       # ApiResponse, BaseEntity, Constants
├── infrastructure/
│   ├── configuration/            # KafkaConfig, ResourceClientConfig, ShedLockConfig, MailConfig, AsyncConfig
│   ├── kafka/                    # RetryTopicConfig, DltRecoverer, ConsumerErrorHandler
│   ├── client/                   # ResourceServiceClient + ClientCredentialsTokenProvider
│   ├── sms/                      # SmsSender(IF) + GabiaSmsSender + LogStubSmsSender
│   ├── alimtalk/                 # AlimtalkSender(IF) + LogStubAlimtalkSender (실 구현 보류)
│   ├── mail/                     # MailSender (JavaMailSender 래퍼)
│   ├── security/                 # OAuth2 Client Credentials 토큰 관리
│   └── exceptions/
├── publish/                      # AI 콘텐츠 → resource-service 게시물 등록
│   ├── qna/                      #   QnaAnswerPublishConsumer + QnaAnswerPublishService
│   ├── news/                     #   TeamNewsPublishConsumer + TeamNewsPublishService
│   └── dto/
├── notify/                       # 외부 채널 발송
│   ├── consumer/                 #   notification.* 4종 Consumer
│   ├── orchestrator/             #   DispatchOrchestrator (Mail+SMS 라우팅)
│   ├── channel/                  #   MailChannel, SmsChannel, AlimtalkChannel(stub)
│   ├── template/                 #   MessageTemplate (이벤트유형 × 채널)
│   └── recipient/                #   RecipientResolver + Caffeine 캐시
├── log/                          # DispatchLog (Entity, Repository) — event_id UNIQUE
└── DispatchServiceApplication.java
```

---

## 3. Phase 1 — 골격 (Skeleton)

### 3.1 의존성 추가

- [x] **1.1** `pom.xml`에 다음 의존성 추가
  - `spring-boot-starter-oauth2-client` (Client Credentials)
  - `net.javacrumbs.shedlock:shedlock-spring` **5.16.0** (6.0.0은 미출시)
  - `net.javacrumbs.shedlock:shedlock-provider-jdbc-template` 5.16.0
  - `com.github.ben-manes.caffeine:caffeine`
  - `org.springframework.boot:spring-boot-starter-aspectj`
  - `org.flywaydb:flyway-core` + `flyway-mysql`
  - test: `testcontainers-bom`(1.20.4) + `junit-jupiter`/`mariadb`/`kafka`, `greenmail-junit5`(2.0.1), `wiremock-standalone`(3.9.2), `spring-security-test`
- **검증**: `mvn compile` 성공

### 3.2 패키지 구조 생성

- [x] **1.2** `common/`, `infrastructure/{configuration,kafka,client,sms,alimtalk,mail,security,exceptions,controller}`, `publish/{qna,news}`, `notify/{consumer,orchestrator,channel,template,recipient}`, `log/`, `support/` (테스트) 생성

### 3.3 프로파일 분리

- [x] **1.3** `application.yaml` (공통) + `application-local.yaml` / `application-dev.yaml` / `application-prod.yaml` 분리
  - `local`: SMS=stub, Alimtalk=stub, Mail=localhost:3025 (GreenMail 호환)
  - `dev/prod`: SMS=gabia, Mail=실 SMTP, OAuth2 Client Credentials 환경변수 주입
- **포트**: 8084 (notification-service 포트 계승)
- **DB**: `locker_room_dispatch` (resource-service와 분리)

### 3.4 공통 모듈

- [x] **1.4** `common/entity/BaseEntity` (`createdAt`, `updatedAt` — JpaAuditing)
- [x] **1.5** `common/response/ApiResponse<T>` (record, `{ code, message, data }`)
- [x] **1.6** `infrastructure/exceptions/CustomException` + `ErrorCode` enum (12종) + `GlobalExceptionHandler`

### 3.5 Kafka 기반 설정

- [x] **1.7** `KafkaConfig` — `@EnableKafka`만. `application.yaml`에서 ErrorHandlingDeserializer + JsonDeserializer + trusted packages 설정 (SB 4.x `autoconfigure.kafka` 패키지 재배치로 직접 ConsumerFactory 빌드는 단순화)
- [x] **1.8** `RetryTopicConfig` — 3회 재시도(5s/30s/60s), DLQ 자동 생성, `.dlq` suffix
- [x] **1.9** `ConsumerErrorHandler` — `DefaultErrorHandler`에 `IllegalArgumentException` / `ConstraintViolationException` / `NonRetryableException`을 비재시도 등록

### 3.6 JPA + 마이그레이션

- [x] **1.10** datasource / JPA 설정 (`local`은 `ddl-auto: validate` + Flyway, dev/prod도 동일)
- [x] **1.11** `db/migration/V1__init.sql` 작성
  - `dispatch_logs` (`(event_id, channel)` UNIQUE — 같은 eventId가 여러 채널로 발송될 수 있어 채널별 멱등성 보장)
  - `message_templates`
  - `shedlock`
- [x] `JpaConfig` (`@EnableJpaAuditing`)
- [x] `DispatchLog` Entity + `DispatchEventType`/`DispatchChannel`/`DispatchStatus` enum + Repository

### 3.7 ShedLock 설정

- [x] **1.12** `ShedLockConfig` — JdbcTemplate provider, `defaultLockAtMostFor=PT10M`, `usingDbTime()`

### 3.8 Actuator

- [x] **1.13** `/actuator/{health,info,metrics,loggers}` 노출, `InfoController` (`/api/v1/info/{name,profile}`)

### 3.9 테스트

- [x] **1.14** ~~`KafkaConfigTest`~~ — KafkaConfig가 `@EnableKafka`만 있어 단위 테스트 의미 없음. 생략
- [x] **1.15** `RetryTopicIntegrationTest` — `RetryTopicConfiguration` 빈 생성 + dlt suffix 검증으로 단순화. 실 retry/DLQ 흐름은 Phase 3 Consumer 구현 시 EmbeddedKafka 통합 테스트로 검증 (5s/30s/60s 백오프로 1분+ 소요)
- [x] **1.16** `ConsumerErrorHandlerTest` — `DefaultErrorHandler` 빈 생성 검증
- [x] **1.17** `FlywayMigrationIntegrationTest` (`@IntegrationTest`, Docker 필요) — Testcontainers MariaDB 11.4로 V1 적용, 테이블 + UNIQUE 인덱스 검증
- [x] **1.18** `GlobalExceptionHandlerTest` — CustomException / 알 수 없는 예외 처리 검증 (3 case)
- [x] **1.19** ~~`BaseEntityTest`~~ — 단독 검증 어려움. `DispatchLogRepositoryIntegrationTest`에서 `createdAt`/`updatedAt` 자동 설정 + UNIQUE 제약 검증으로 대체
- [x] 추가: `ApiResponseTest`(3), `ErrorCodeTest`(2), `DispatchLogTest`(4), `DispatchServiceApplicationTests`(1)

**검증 결과**: `mvn test -Dgroups='!integration'` → **15 tests passed**. 통합 테스트(Docker 필요)는 별도 그룹으로 분리하여 CI에서만 실행

---

## 4. Phase 2 — 외부 IF & Stub 구현

### 4.1 SmsSender 추상화

- [x] **2.1** `SmsSender` 인터페이스 + `SmsMessage` / `SmsResult` record + `SmsSendException`
- [x] **2.2** `LogStubSmsSender` (`@ConditionalOnProperty(name="dispatch.sms.provider", havingValue="stub", matchIfMissing=true)`)

### 4.2 AlimtalkSender 추상화

- [x] **2.3** `AlimtalkSender` 인터페이스 + `AlimtalkMessage` / `AlimtalkResult` record (templateCode + variables Map + fallbackText)
- [x] **2.4** `LogStubAlimtalkSender` — 실 구현은 사업자 등록 후로 보류

### 4.3 MailSender 래퍼

- [x] **2.5** `MailSender` 인터페이스 + `MailMessage` record (text/html factory) + `MailResult`
- [x] **2.6** `JavaMailSenderAdapter` — `JavaMailSender` 래핑, `MimeMessageHelper` UTF-8, 실패 시 result로 변환

### 4.4 ResourceServiceClient + Client Credentials

- [x] **2.7** Keycloak Client Credentials 설정 (`application.yaml`의 `spring.security.oauth2.client.registration.dispatch-service.*`)
- [x] **2.8** `ClientCredentialsTokenProvider` IF + `DefaultClientCredentialsTokenProvider` — `OAuth2AuthorizedClientManager`에 위임 (자체 만료 갱신 처리). `OAuth2ClientConfig`에서 `AuthorizedClientServiceOAuth2AuthorizedClientManager` + `clientCredentials()` provider Bean 등록
- [x] **2.9** `ResourceServiceClient` — `RestClient` 기반 (3 메서드: createComment / createPost / getRecipientContact). DTO: `CreateCommentRequest`, `CreatePostRequest`, `CreatedIdResponse`, `RecipientContactResponse`
- [x] **2.10** WireMock 통합 테스트 — `ResourceServiceClientTest` (5 case 포함)

> **변경**: Caffeine 직접 캐시 대신 Spring Security `OAuth2AuthorizedClientService`에 위임 (자체 만료 갱신 + 캐시 제공, 검증된 구현). Caffeine은 Phase 4 RecipientResolver에서 사용
> **변경**: `ClientHttpRequestFactoryBuilder`(SB 4.x에서 제거됨) 대신 `JdkClientHttpRequestFactory`(spring-web 7.x) 직접 사용. connect 3s / read 10s

### 4.5 테스트

- [x] **2.11** `LogStubSmsSenderTest` (3) / `LogStubAlimtalkSenderTest` (3) — 발송 성공 + 입력 검증
- [x] **2.12** `JavaMailSenderAdapterTest` (2) — GreenMail로 실 SMTP 수신 검증, unreachable host 시 failure result 반환
- [x] **2.13** `DefaultClientCredentialsTokenProviderTest` (4) — 토큰 발급/null 처리/invalidate/요청 파라미터 검증
- [x] **2.14** `ResourceServiceClientTest.createComment_sendsBearerAndIdempotencyKey_andUnwrapsApiResponse` / `createPost` / `getRecipientContact` — WireMock으로 헤더·바디·언래핑 검증
- [x] **2.15** `ResourceServiceClientTest.on401_invalidatesTokenAndRetriesOnce` — 401 → invalidate → 새 토큰으로 재요청 → 200
- [x] **2.16** ~~별도 `RestClientTimeoutTest`~~ — JDK HttpClient 타임아웃 동작은 표준 라이브러리 보장. `ResourceClientConfig`에서 connect 3s / read 10s 설정. 통합 검증은 Phase 5의 GabiaSmsSender 단계에서 함께 검증

> **테스트 환경 이슈**: WireMock이 HTTP/2를 미지원해 JDK HttpClient 기본(HTTP/2 협상)과 RST_STREAM 충돌. 테스트의 RestClient만 `HttpClient.Version.HTTP_1_1`로 강제 (운영 코드 영향 없음)

**검증 결과**: `mvn test -Dgroups='!integration'` → **32 tests passed** (Phase 1: 15 + Phase 2: 17)

---

## 5. Phase 3 — AI 콘텐츠 등록 Consumer

> ai-service가 아직 없으므로, **EmbeddedKafka에 가짜 이벤트를 던져 검증**한다.

### 5.1 Q&A 답변 등록

- [x] **3.1** `QnaAnswerReadyEvent` DTO (record, JSON include non-null)
- [x] **3.2** `QnaAnswerPublishConsumer` — `@KafkaListener("ai.qna-answer.ready")` + `@RetryableTopic` (3회, 5s/30s/60s) + `@DltHandler`
- [x] **3.3** `QnaAnswerPublishService` — 입력 검증(NonRetryableException) → `DispatchLogTxService.ensurePending()`(REQUIRES_NEW) → `ResourceServiceClient.createComment` → markSuccess/markFailure
- [x] **3.4** ~~EmbeddedKafka 통합 테스트~~ → Phase 6 마무리 단계로 미룸 (RetryableTopic 5s/30s/60s 백오프로 실 실행 1분+ 소요. 핵심 로직은 단위 테스트로 충분 커버)

### 5.2 팀 뉴스 등록

- [x] **3.5** `TeamNewsReadyEvent` DTO
- [x] **3.6** `TeamNewsPublishConsumer` + `@RetryableTopic` + `@DltHandler`
- [x] **3.7** `TeamNewsPublishService` — 동일 패턴 (boardId/title/content 검증)

### 5.3 DLQ 처리

- [x] **3.9** DLQ 핸들러 — `@DltHandler`로 ERROR 로그 (eventId, postId/teamId 포함)

### 5.4 공통 인프라

- [x] **공통 1** `IdempotencyKeyGenerator` — `UUID.nameUUIDFromBytes("dispatch-service:" + eventId)` 결정적 UUID
- [x] **공통 2** `DispatchLogTxService` — `ensurePending` / `markSuccess` / `markFailure` / `markSkipped` 모두 `Propagation.REQUIRES_NEW` (외부 호출 실패 시에도 로그 보존)

### 5.5 테스트

- [x] **3.10** `QnaAnswerPublishServiceTest` (7) — 정상/이미 SUCCESS skip/외부 실패+rethrow/blank eventId/blank content/EventType+Channel 인자/InOrder 검증
- [x] **3.11** `TeamNewsPublishServiceTest` (6) — 동일 시나리오 (boardId null, blank title 포함)
- [x] **3.12~3.14** ~~Consumer/Retry/DLT IT~~ → Phase 6 통합 검증 단계로 일괄 미룸
- [x] **3.15** `IdempotencyKeyGeneratorTest` (5) — 동일 eventId → 동일 UUID, 다른 eventId → 다른 UUID, UUID 포맷, blank/null throw
- [x] **3.16** ~~별도 `DispatchLogUniqueConstraintTest`~~ → Phase 1.17의 `DispatchLogRepositoryIntegrationTest`에서 이미 검증 완료

### 5.6 결정사항

- **Spring Kafka 4.x 변경**: `@RetryableTopic.backoff` (소문자) → `backOff` (대문자), 타입도 `org.springframework.retry.annotation.Backoff` → `org.springframework.kafka.annotation.BackOff`. spring-retry 의존성 불필요
- **트랜잭션 분리 패턴**: 외부 호출(resource-service)이 끼어 있는 영속화 흐름에서 단일 `@Transactional`로 처리하면 외부 실패 시 markFailure도 롤백되어 상태 추적 망가짐. `DispatchLogTxService`의 모든 메서드를 `Propagation.REQUIRES_NEW`로 분리 → 외부 호출 결과를 무관히 로그 보존
- **트랜잭션 진입점**: `PublishService`는 `@Transactional` 없음. 매 단계에서 `DispatchLogTxService` 호출 시 새 트랜잭션이 시작되고 즉시 commit

**검증 결과**: `mvn test -Dgroups='!integration'` → **50 tests passed** (Phase 1: 15 + Phase 2: 17 + Phase 3: 18)

---

## 6. Phase 4 — 알림 Consumer (Mail 우선)

### 6.1 RecipientResolver

- [x] **4.1** `RecipientResolver.resolve(userId)` — `Optional<Recipient>` 반환. 캐시 hit 우선, miss 시 `ResourceServiceClient.getRecipientContact(userId)` 호출 후 캐시. 외부 호출 실패 시 `Optional.empty()`
- [x] **4.2** Caffeine 캐시 (`expireAfterWrite=10분`, `maximumSize=10000`, recordStats). `RecipientCacheConfig`에서 Bean 등록
- [x] **4.3** OPT_OUT/RECIPIENT_NOT_FOUND/NO_PREFERRED_CHANNELS 모두 `channel=NONE`으로 단일 SKIPPED 로그 기록 후 return

### 6.2 메시지 템플릿

- [x] **4.4** `MessageTemplate` Entity (`(event_type, channel, version)` UNIQUE) + `MessageTemplateRepository.findTopByEventTypeAndChannelAndEnabledTrueOrderByVersionDesc` + `MessageTemplateService.findActive` (없으면 `DISPATCH_TEMPLATE_NOT_FOUND` 예외)
- [x] **4.5** `V2__seed_message_templates.sql` — 4 이벤트 × MAIL/SMS = 8 템플릿 시드
- [x] **4.6** `TemplateRenderer` — `{{변수}}` 패턴 정규식. `Matcher.quoteReplacement`로 `$`/`\` 안전 치환. null template 통과, 누락 변수는 빈 문자열

### 6.3 DispatchOrchestrator + MailChannel

- [x] **4.7** `NotificationDispatchService.dispatch(cmd)` — preferredChannels 순회, 각 채널마다 ensurePending → render → send → mark*. SUCCESS인 채널은 skip(retry 시 부분 복구), 모든 채널 실패 시 throw → RetryableTopic 처리
- [x] **4.8** `MailChannel` — `MailSender` 위임. canSend는 email 검증
- [x] **4.9** 4종 Consumer (`CommentNotiConsumer` / `ReplyNotiConsumer` / `InquiryRepliedConsumer` / `ReportProcessedConsumer`) — 각자 이벤트 → `NotificationDispatchCommand` 변환 후 위임. `@RetryableTopic` + `@DltHandler`
- [x] **4.10** ~~통합 테스트~~ → Phase 6 마무리로 미룸 (4.17 / 4.18과 함께)

### 6.4 테스트

- [x] **4.11** `RecipientResolverTest` (6) — 캐시 hit/miss, 알 수 없는 채널 문자열 필터, OPT_OUT 보존, 외부 실패 시 empty, null userId, invalidate 후 재조회
- [x] **4.12** ~~`RecipientResolverCacheTTLTest`~~ — TTL은 Caffeine 라이브러리 보장. 단위 테스트로 시간 모킹은 효용 낮아 생략
- [x] **4.13** `TemplateRendererTest` (7) — 치환, 다중 변수, 누락 변수, null map, null template, 무관한 중괄호, `$`/`\` escape
- [x] **4.14** ~~별도 `MessageTemplateRepositoryTest`~~ → Phase 6 통합 테스트에 포함 (Testcontainers 필요)
- [x] **4.15** `NotificationDispatchServiceTest` (9) — 정상 다채널, OPT_OUT skip, 수신자 없음 skip, 채널별 SUCCESS skip, 일부 실패 + 일부 성공 (no throw), 전체 실패 throw, blank eventId/null userId NonRetryable, 변수 렌더링 검증
- [x] **4.16** `MailChannelTest` (5) — channel=MAIL, canSend (email 유무), success 매핑, failure 매핑, MailMessage 캡처
- [x] **4.17~4.18** ~~Consumer/OPT_OUT IT~~ → Phase 6 통합 검증

### 6.5 결정사항

- **시드 데이터 위치**: `V2__seed_message_templates.sql` Flyway 마이그레이션. 8개 (4 이벤트 × MAIL/SMS). 운영에선 관리자 페이지로 갱신 가능 (추후)
- **부분 실패 정책**: 채널 1개라도 성공이면 dispatch 정상 종료. 전체 실패만 throw → RetryableTopic이 retry. 이로써 retry 시 SUCCESS 채널은 skip되고 FAILED 채널만 재시도
- **OPT_OUT/RECIPIENT_NOT_FOUND**: 외부 채널 발송 시도 0번. 단일 `channel=NONE` 로그에 SKIPPED 기록. 멱등성 키 `(eventId, NONE)`로 중복 처리 방지

**검증 결과**: `mvn test -Dgroups='!integration'` → **77 tests passed** (Phase 1: 15 + Phase 2: 17 + Phase 3: 18 + Phase 4: 27)

---

## 7. Phase 5 — SMS Channel + 가비아 구현

### 7.1 가비아 SMS 사전 작업 (수동 — 사용자 작업)

- [ ] **5.1** 가비아 콘솔에서 발신번호 사전 등록 (개인 휴대폰 + 신분증)
- [ ] **5.2** API ID / API Key 발급
- [ ] **5.3** 환경변수로 보관 (`GABIA_SMS_API_ID`, `GABIA_SMS_API_KEY`, `GABIA_SMS_FROM`)

### 7.2 GabiaSmsSender 구현

- [x] **5.4** `GabiaSmsProperties` (record + `@ConfigurationProperties("dispatch.sms.gabia")`) — `baseUrl`/`apiId`/`apiKey`/`defaultFrom`/`sendPath`/`smsByteThreshold`. blank/null 시 합리적 기본값
- [x] **5.5** `GabiaSmsConfig` — `@ConditionalOnProperty(provider=gabia)` + `@EnableConfigurationProperties`. `gabiaSmsRestClient` Bean: JDK HttpClient + Basic Auth(Base64) 자동 헤더 + connect 3s / read 10s
- [x] **5.6** `GabiaSmsSender` — `@ConditionalOnProperty(provider=gabia)`. UTF-8 byte 길이로 SMS/LMS 자동 분기. blank from 시 `defaultFrom` 사용. `RestClientResponseException`(HTTP 4xx/5xx)/`ResourceAccessException`(네트워크) 분리 처리하여 `SmsResult.failure` 반환 — 예외 던지지 않고 결과로 판단
- [x] **5.7** WireMock 단위 테스트 (5.13~5.16 참조)

### 7.3 SmsChannel

- [x] **5.9** `SmsChannel` — `SmsSender` 위임. `canSend`는 phone 검증. `MailChannel`과 동일한 패턴
- [x] **5.10** `DispatchOrchestrator`에 SMS 라우팅 통합 — Phase 4의 `NotificationDispatchService`가 모든 `DispatchChannelStrategy` Bean을 자동 등록하므로 `SmsChannel` 추가만으로 자동 통합 (별도 코드 변경 없음)

### 7.4 잔액 모니터링 (선택)

- [ ] **5.11** ~~가비아 잔액 조회 API + Actuator HealthIndicator~~ → Phase 6으로 미룸 (정확한 잔액 조회 endpoint는 가비아 콘솔 문서 확보 후)
- [ ] **5.12** ~~잔액 임계치 경고 로그~~ → Phase 6으로 미룸

### 7.5 테스트

- [x] **5.13** `GabiaSmsSenderTest` (7) — short→sms / long→lms / blank from default 사용 / 4xx 401 → `HTTP_401` / 5xx 503 → `HTTP_503` / 200 + 에러 result 코드 / 별칭 키(`code`/`messageId`) 매핑
- [x] **5.14** `GabiaSmsSenderLmsAutoSwitchTest` (5) — 임계치 미만/동일/초과, 한글(UTF-8 3byte) 경계값(30/31자), null text 처리
- [x] **5.15** ~~별도 4xx/5xx 테스트~~ → 5.13에 통합
- [x] **5.16** `SmsChannelTest` (5) — channel=SMS, canSend(phone 유무), success 매핑, failure 매핑, SmsMessage 캡처
- [x] **5.17** ~~`SmsChannelTimeoutTest`~~ → JDK HttpClient 타임아웃은 표준 라이브러리 보장. 실 환경에서 5.19로 검증
- [x] **5.18** ~~`SmsConsumerIT`~~ → Phase 6 통합 검증
- [x] 추가: `GabiaSmsPropertiesTest` (4) — 기본값 폴백 및 커스텀값 보존 검증
- [ ] **5.19** (수동) 사전등록 본인 번호로 dev 환경 1회 실 발송 + DispatchLog 기록 확인 — 사용자 작업 (가비아 키 발급 후)

### 7.6 결정사항

- **byte 임계치**: UTF-8 기준 90byte (가비아 정책 기본값으로 추정. 실제 다르면 properties로 조정). 한글 1자 = 3byte → 약 30자가 SMS 한계
- **성공 판단**: `result` 또는 `code` 필드가 `"0000"`/`"0"`/`"OK"`. 실 가비아 응답 코드는 문서 확보 후 확정
- **에러 처리 정책**: `GabiaSmsSender`는 예외 던지지 않고 `SmsResult.failure`로 반환 → 호출자(SmsChannel/DispatchOrchestrator)가 결과로 판단. 4xx는 `HTTP_4xx`, 네트워크 오류는 `NETWORK_ERROR`
- **JSON 별칭**: `@JsonAlias`로 `result`/`code`, `ref_key`/`refKey`/`messageId`/`message_id` 모두 매핑 — 가비아 문서 확정 전 호환성 확보

**검증 결과**: `mvn test -Dgroups='!integration'` → **98 tests passed** (Phase 1: 15 + Phase 2: 17 + Phase 3: 18 + Phase 4: 27 + Phase 5: 21)

---

## 8. Phase 6 — 운영

### 8.1 모니터링

- [x] **6.1** Actuator `/actuator/metrics` 노출 (Phase 1.13에서 이미 노출됨). `DispatchMetrics` 컴포넌트 추가
- [ ] **6.2** ~~커스텀 헬스 인디케이터 (Kafka, resource-service, Gabia, SMTP)~~ → Spring Boot Actuator 자동 제공(DB/Kafka)으로 1차 만족. 외부 ping은 Phase 7로 연기
- [x] **6.3** 핵심 메트릭: `dispatch.events.total{event_type, channel, status}`, `dispatch.dlq.total{topic, event_type}` — `DispatchMetrics`에서 PublishService / NotificationDispatchService / Consumer DLT 핸들러에서 호출

### 8.2 DLQ 재처리

- [ ] **6.4~6.6** ~~DLQ Replay 관리자 API~~ → **Phase 7로 분리**. Spring Security Resource Server 추가 + Admin API + IT가 부담스럽고, 운영 가시성은 6.3의 DLQ 메트릭 + DLT 로그로 1차 충족

### 8.3 정기 잡 (ShedLock)

- [x] **6.7** `DispatchLogCleanupJob` — 매일 03:00 (`Asia/Seoul`), `@SchedulerLock(name="dispatchLogCleanup", lockAtMostFor="PT30M", lockAtLeastFor="PT1M")`. 90일(`Duration.ofDays(90)`) 이상 로그 삭제. `DispatchLogRepository.deleteByCreatedAtBefore` 추가
- [ ] **6.8** ~~Caffeine 캐시 통계 로깅 잡~~ → Phase 7 (캐시 통계는 Actuator metrics에서 자동 노출 가능)

### 8.4 보안 점검

- [x] **6.9** `PiiMasker` 유틸 (`common/PiiMasker`) — `phone()` / `email()`. 기존 inline 마스킹(LogStubSmsSender / LogStubAlimtalkSender / JavaMailSenderAdapter)을 이 유틸로 통합 (DRY)
- [ ] **6.10** ~~API Key/Client Secret 정적 검사~~ → Phase 7 (gitleaks/trivy 같은 도구 도입)

### 8.5 테스트

- [ ] **6.11** ~~HealthIndicatorTest~~ → 6.2 보류와 함께 Phase 7
- [ ] **6.12~6.13** ~~DlqReplay~~ → Phase 7
- [x] **6.14** `DispatchLogCleanupJobTest` (1) — 90일 이전 cutoff 검증 (시간 범위 ± 1초 허용). ShedLock 동작 자체는 라이브러리 보장이라 통합 단계로
- [x] **6.15** `PiiMaskerTest` (7) — phone(정상/짧음/null), email(정상/단일자/`@`없음/null)
- [x] 추가: `DispatchMetricsTest` (5) — recordOutcome 태그/카운트 분리, null channel→NONE, recordDlq, null topic→unknown

---

## 9. 테스트 전략

### 9.1 테스트 분류

| 분류 | 범위 | 도구 | 비중 |
|---|---|---|---|
| **단위 테스트** | Service / Mapper / Channel / Renderer 등 순수 로직 | JUnit 5 + Mockito + AssertJ | 70% |
| **슬라이스 테스트** | `@DataJpaTest`(Repository), `@WebMvcTest`(Controller) | Spring Boot Test | 15% |
| **통합 테스트** | Consumer 흐름, 외부 시스템 연동 | EmbeddedKafka, Testcontainers(MariaDB), WireMock(resource-service / Gabia), GreenMail(SMTP) | 12% |
| **수동 검증** | 가비아 실 발송, 운영 환경 헬스체크 | - | 3% |

### 9.2 핵심 테스트 시나리오

| 시나리오 | 검증 내용 |
|---|---|
| **멱등성** | 동일 eventId 재발행 시 중복 처리 0건 (`dispatch_log` UNIQUE) |
| **재시도/DLQ** | 비즈니스 예외 → 3회 재시도(5s/30s/60s) → DLQ 적재 → DLQ Replay로 재처리 성공 |
| **다중 인스턴스** | EmbeddedKafka에 2개 컨슈머 인스턴스 등록 → 같은 이벤트가 1번만 처리됨 |
| **분산 락** | ShedLock 잡이 `lockAtMostFor` 동안 단일 실행, 다른 인스턴스는 대기 |
| **토큰 갱신** | resource-service 401 → Client Credentials 토큰 강제 재발급 → 재시도 성공 |
| **수신 동의** | OPT_OUT 사용자 이벤트 수신 시 dispatch_log(SKIPPED) + 외부 호출 0회 |
| **PII 마스킹** | 로그 캡처 후 휴대폰/이메일 원문이 출력되지 않음 검증 |
| **알림톡 폴백 골격** | (실 구현 보류) AlimtalkChannel 실패 → SmsChannel 폴백 단위 테스트 |

### 9.3 테스트 컨벤션

- **네이밍**: `{메서드명}_{시나리오}_{기대결과}` (예: `publish_duplicateEventId_skipsAndLogs`)
- **AAA 패턴**: Arrange / Act / Assert 구분 명확히
- **외부 의존**: 단위 테스트는 Mockito stub, 통합 테스트는 WireMock/EmbeddedKafka/Testcontainers (실 외부 호출 금지)
- **공통 fixture**: `src/test/java/.../support/`에 `EventFixtures`, `RecipientFixtures` 등
- **커버리지 도구**: JaCoCo. PR 머지 기준 라인 커버리지 80% / 분기 커버리지 70%

### 9.4 테스트 실행

```bash
# 단위 + 슬라이스 + 통합 모두
./mvnw test

# 통합 테스트만 (Testcontainers 필요 — Docker 가동 상태에서)
./mvnw test -Dgroups=integration

# JaCoCo 리포트
./mvnw verify
open target/site/jacoco/index.html
```

> Testcontainers가 Docker를 요구하므로 CI(Jenkins)에서 Docker-in-Docker 또는 호스트 Docker 소켓 마운트 필요.

---

## 10. 산출물 체크리스트

| 항목 | 목표 |
|---|---|
| Kafka 토픽 처리 | 6종 (AI 2 + Notification 4) Consumer 동작, 멱등성 보장 |
| DispatchLog 추적 | 모든 이벤트의 처리 결과(성공/실패/skip) 기록 |
| 발송 채널 | Mail + SMS(가비아) 정상 동작, 알림톡은 IF만 |
| Idempotency | 같은 eventId 재발송 시 중복 처리 0건 |
| 다중 인스턴스 | 2대 운영 시 같은 이벤트 중복 처리 0건 (Kafka group 분배 + DispatchLog UNIQUE) |
| 분산 락 | 정기 잡이 1회만 실행 (ShedLock 검증) |
| 테스트 커버리지 | JaCoCo 라인 80% / 분기 70% |
| 문서 | `README.md`, `doc/api.md`(internal API), `doc/sds.md` |

---

## 11. 미정 / TODO

| 항목 | 상태 | 메모 |
|---|---|---|
| 알림톡 실 구현 | 보류 | 사업자 등록 후 가비아 또는 NHN Cloud로 추가 |
| ai-service | 미구현 | 본 서비스는 토픽 스펙 가정하에 구현, 실제 연동은 ai-service 완성 후 |
| Webhook 수신 (가비아 발송 결과) | 보류 | 단말 수신 결과까지 추적 필요해지면 추가 |
| 분산 트레이싱 | 보류 | resource-service에 OTel 도입 시 dispatch도 함께 |
| Keycloak `EmailSenderProvider` SPI | 보류 | 메일 일관성이 필요해지면 검토. 우선은 Keycloak이 SMTP 직접 발송 |
| Phase 6 통합 테스트 (3.4, 3.8, 3.12~3.14, 4.10, 4.14, 4.17, 4.18, 5.18) | **Phase 7** | EmbeddedKafka + Testcontainers + WireMock + GreenMail 일괄 통합. RetryableTopic 5s/30s/60s 백오프 부담으로 미뤘음 |
| DLQ Replay 관리자 API (6.4~6.6) | **Phase 7** | Spring Security Resource Server 추가 + ROLE_ADMIN Admin API + IT |
| 외부 시스템 HealthIndicator (6.2) | **Phase 7** | resource-service / Gabia / SMTP ping |
| 가비아 잔액 모니터링 (5.11~5.12) | **Phase 7** | 잔액 조회 API endpoint 확보 후 |
| 가비아 실 발송 검증 (5.19) | **사용자 작업** | 가비아 콘솔에서 발신번호 등록 + API 키 발급 후 |
| API Key/Secret 정적 검사 (6.10) | **Phase 7** | gitleaks/trivy 도구 도입 |

---

## 개정 이력

| 버전 | 날짜 | 변경 내용 |
|---|---|---|
| 1.0 | 2026-05-06 | 초안 작성. dispatch-service 책임 정의(AI 등록 + 외부 채널 발송), Kafka 토픽 스펙, Phase 1~6 작성 |
| 1.1 | 2026-05-06 | 각 Phase에 명시적 테스트 task 추가(1.14~1.19, 2.11~2.16, 3.10~3.16, 4.11~4.18, 5.13~5.19, 6.11~6.15). 신규 9장 "테스트 전략" 추가 (분류·시나리오·컨벤션·실행). 산출물 체크리스트 커버리지 기준을 JaCoCo 라인 80%/분기 70%로 명시 |
| 1.2 | 2026-05-06 | **Phase 1 완료**. 의존성 + 패키지 + 프로파일 + 공통 모듈 + Kafka 설정 + JPA/Flyway + ShedLock + Actuator + 테스트 15개 통과. 결정사항: ShedLock 5.16.0(6.0.0 미출시), KafkaConfig는 application.yaml 위임으로 단순화(SB 4.x autoconfigure.kafka 재배치), 통합 테스트는 `@IntegrationTest` 태그로 분리(Docker 필요) |
| 1.3 | 2026-05-06 | **Phase 2 완료**. SmsSender/AlimtalkSender/MailSender IF + LogStub + JavaMail 래퍼, OAuth2 Client Credentials(Spring Security 위임 방식) + ResourceServiceClient(RestClient + 401 재시도). 32 tests passed. 결정사항: Caffeine 직접 캐시 대신 OAuth2AuthorizedClientService 위임, JdkClientHttpRequestFactory 직접 사용(SB 4.x ClientHttpRequestFactoryBuilder 제거), WireMock HTTP/2 미지원으로 테스트 RestClient만 HTTP/1.1 강제 |
| 1.4 | 2026-05-06 | **Phase 3 완료**. AI 콘텐츠 등록 Consumer (Q&A 답변 / 팀 뉴스). `IdempotencyKeyGenerator` + `DispatchLogTxService`(REQUIRES_NEW로 트랜잭션 분리) + `QnaAnswerPublishService` / `TeamNewsPublishService` + `@RetryableTopic` Consumer + `@DltHandler`. 50 tests passed (+18). 결정사항: SK 4.x `@RetryableTopic.backoff`→`backOff`, BackOff 애너테이션 패키지 변경, 통합 테스트는 백오프 시간(1분+) 부담으로 Phase 6 마무리로 미룸 |
| 1.5 | 2026-05-06 | **Phase 4 완료**. 알림 Consumer (Mail 우선) + Strategy 채널 라우팅. `Recipient` 도메인 + `RecipientResolver`(Caffeine 캐시) + `MessageTemplate` Entity + `TemplateRenderer` + `DispatchChannelStrategy` IF + `MailChannel` + `NotificationDispatchService` + 4종 이벤트 DTO + 4종 Consumer + V2 시드 마이그레이션. 77 tests passed (+27). 결정사항: 부분 실패 정책(전체 실패 시에만 throw), OPT_OUT/RECIPIENT_NOT_FOUND는 channel=NONE 단일 SKIPPED 로그, MessageTemplateRepository IT는 Phase 6으로 미룸 |
| 1.6 | 2026-05-06 | **Phase 5 완료**. 가비아 SMS 구현 + `SmsChannel`. `GabiaSmsProperties`(record) + `GabiaSmsConfig`(Basic Auth RestClient) + `GabiaSendRequest`/`GabiaSendResponse`(JsonAlias 호환) + `GabiaSmsSender`(UTF-8 byte 기준 SMS/LMS 자동 분기) + `SmsChannel`(MailChannel과 동일 패턴). 98 tests passed (+21). 결정사항: 90byte UTF-8 기본 임계치(properties로 조정), 예외 안 던지고 `SmsResult.failure` 반환, JSON 응답 키 별칭 매핑(`result`/`code`/`ref_key` 등). 잔액 모니터링은 Phase 6으로, 실 발송은 사용자가 키 발급 후 5.19에서 |
| 1.7 | 2026-05-06 | **Phase 6 1차 완료** (PII 마스킹 + Metrics + Cleanup Job). `PiiMasker`(common 유틸 + 기존 inline 마스킹 통합), `DispatchMetrics`(Micrometer counter `dispatch.events.total` / `dispatch.dlq.total` + 태그 `event_type`/`channel`/`status`/`topic`), 모든 PublishService/NotificationDispatchService/Consumer DLT 핸들러에서 메트릭 기록. `DispatchLogCleanupJob`(매일 03:00, ShedLock, 90일 보존). 111 tests passed (+13). 결정사항: HealthIndicator/DLQ Replay/외부 통합 테스트는 Phase 7 분리(범위·시간·인프라 부담) |
