# meteor-template

Spring Boot(Java) 애플리케이션을 팀이 함께 시작하기 위한 템플릿.

목표는 단순한 구조로 시작하되 **규칙은 도메인 모델 한 곳에 모으고**, **트랜잭션 경계가 코드에서 보이게** 하는 것이다. 코드는
`domain`, `usecase`, `api` 세 계층으로만 나눈다. 버티컬 슬라이스나 컨텍스트 경계 장치는 두지 않는다. 영속화는 JPA 로 고정하되, JPA 의
간섭 없이 애그리거트·값 객체를 쓰고 그것을 결과로 그대로 돌려주기 위해 엔티티와 애그리거트는 분리한다. 판단 기준은
[설계 원칙](docs/ARCHITECTURE.md#설계-원칙)에 있다.

- 규칙은 애그리거트와 `*Policy`(도메인 모델)에 있다. 도메인 모델은 Spring 과 JPA 를 모른다.
- 저장소 호출은 애그리거트 하나를 맡는 도메인 서비스 `*Reader`(조회)와 `*Writer`(생성·변경)에 모은다. `*Service` 접미어는 쓰지 않는다.
- 여러 도메인 서비스를 엮거나 외부 시스템을 부르거나 트랜잭션을 나눠야 할 때만 `*UseCase` 를 만든다. 애그리거트 하나로 끝나는 흐름은
  api 가 도메인 서비스를 바로 부른다.
- 트랜잭션은 도메인 서비스(기본 전파)와 usecase 에서 선언한다. 경계 조절은 usecase 만 한다. 이 규칙들은 ArchUnit 테스트와 리뷰 에이전트가 강제한다.

## 모듈 구성

```
meteor-template
├── core
│   ├── core-enum       com.meteor.enums. 모든 도메인 enum. 의존 0
│   └── core-api        유일한 실행 모듈. com.meteor.domain / usecase / api
│                       + support(에러 어휘, 웹 설정, 어드바이스)
├── storage
│   └── db-core         com.meteor.storage(JPA 엔티티, Spring Data 인터페이스) + 저장소 인프라(DataSource, JPA 설정, BaseEntity).
│                       core-enum 만 의존하고 도메인을 모른다
├── clients
│   └── client-pg       OpenFeign 기반 PG 연동 어댑터 예시. local 프로필은 PG 없이 승인한다
├── support
│   ├── logging         프로필별 logback, OpenTelemetry, Sentry
│   └── monitoring      Actuator + Prometheus
├── tests
│   └── api-docs        Spring REST Docs 테스트 베이스
└── docs
    ├── ARCHITECTURE.md   규칙, 계층, 도메인 서비스, 트랜잭션
    ├── ERROR_HANDLING.md 에러 코드와 ProblemDetail
    ├── SCALING.md        구조를 키우는 시점과 순서
    └── REVIEW_AGENT.md   리뷰 에이전트 하네스
```

### 계층

```
com.meteor
├── enums       MemberStatus, OrderStatus, PaymentMethod, PaymentStatus                         (core-enum)
├── domain      Member, Order, Payment                       애그리거트          ← 규칙은 여기에만
│               Money, Address, Email                        값 객체(record)
│               OrderLimitPolicy                             여러 애그리거트에 걸친 순수 규칙
│               MemberReader, OrderReader, PaymentReader     조회 도메인 서비스 (readOnly 트랜잭션)
│               MemberWriter, OrderWriter, PaymentWriter     생성·변경 도메인 서비스 (트랜잭션)
│               MemberRepository, OrderRepository, ...       엔티티 ↔ 애그리거트 변환 어댑터
├── usecase     OrderUseCase, PaymentUseCase, *Command       여러 도메인 서비스를 엮는 흐름, 트랜잭션 경계 조절
├── api         *Controller, request/, response/
└── storage     *Entity, *JpaRepository. 도메인을 모르며 enum 컬럼으로 저장                      (storage/db-core)
```

```
 api ──────────► usecase ──────────► domain ──────► storage
  │                 │                  ▲  (*Repository 만)
  │                 └──► clients       │
  └────────────────────────────────────┘
     (애그리거트 하나로 끝나는 흐름은 api 가 *Reader / *Writer 를 바로 부른다)
```

`*Repository` 는 storage 모듈의 Spring Data 인터페이스를 감싸 엔티티를 애그리거트로 바꿔 돌려준다. 영속화 기술은 JPA 로 고정이라 그 위에
별도 인터페이스를 두지 않는다. 모듈 의존은 `core-api → storage/db-core → core-enum` 한 방향이고 storage 는 도메인을 모른다. 계층 사이의
나머지 경계는 ArchUnit(R-01 ~ R-13)이 강제한다. 배치 같은 두 번째 실행 모듈이 생기면 그때 `domain` 과 `support.error` 를 core-domain
모듈로 추출한다([SCALING.md](docs/SCALING.md)).

## 레퍼런스 도메인

구조를 보여주기 위한 최소한의 주문·결제 흐름. 세 가지 호출 모양이 모두 들어 있다.

| 흐름 | 호출 | 트랜잭션 |
|---|---|---|
| 회원 가입·조회·탈퇴, 주문 조회·취소, 결제 조회 | api → `*Reader` / `*Writer` | 도메인 서비스가 연 트랜잭션 하나 |
| 주문 생성 | api → `OrderUseCase.place` → `MemberReader.ensureActive` + `OrderWriter.place` | usecase 의 `@Transactional` 하나에 두 서비스가 참여 |
| 결제 | api → `PaymentUseCase.pay` → `OrderReader`, `PaymentWriter`, `PgClient`, `OrderWriter` | `TransactionTemplate` 으로 둘로 나눔. PG 호출은 트랜잭션 밖 |

```
PaymentUseCase.pay
  트랜잭션 1   OrderReader.payableAmount → PaymentWriter.request         결제 REQUESTED
  트랜잭션 밖  PgClient.approve                                          실패 시 PaymentWriter.fail → FAILED
  트랜잭션 2   PaymentWriter.approve + OrderWriter.markPaid              결제 APPROVED, 주문 PAID (함께 커밋·롤백)
```

| 애그리거트 | 규칙 |
|---|---|
| `Member` | 탈퇴는 한 번만. 활성 회원만 주문할 수 있음 |
| `Order` | 수량 1 이상, CREATED 일 때만 결제·취소 가능. 회원당 결제 대기 주문은 3건까지(`OrderLimitPolicy`) |
| `Payment` | 금액은 0 보다 커야 함. REQUESTED 일 때만 승인·실패 처리 가능 |

애그리거트끼리는 ID(`memberId`, `orderId`)로만 참조한다. 주문 생성은 `OrderUseCaseIT`, 결제 흐름은 `PaymentUseCaseIT`, 나눈 트랜잭션의
경계(PG 실패 시 FAILED, 승인 뒤 실패 시 REQUESTED 유지)는 `PaymentTransactionBoundaryIT` 가 검증한다.

## 핵심 규칙

- 도메인 모델(애그리거트, 값 객체, `*Policy`)은 Spring, JPA, 저장소를 모른다.
- 도메인 서비스는 `*Reader`, `*Writer` 뿐이고 자기 애그리거트의 `*Repository` 만 쓴다. 다른 도메인 서비스와 외부 시스템을 부르지 않는다.
- `@Transactional` 은 usecase 와 도메인 서비스에만 있다. 도메인 서비스는 기본 전파만 쓰고, 경계 조절(`TransactionTemplate`, `REQUIRES_NEW`)은 usecase 만 한다.
- 도메인 서비스를 둘 이상 부르고 그중 Writer 가 있는 usecase 메서드는 바깥 트랜잭션을 연다.
- 엔드포인트 하나는 usecase·도메인 서비스 메서드 하나만 부른다. 응답 DTO 에는 도메인 타입을 두지 않는다.

전체 규칙과 이유는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). 구조 규칙은 `./gradlew test` 의 `ArchitectureRules` 가 검사한다.

## 실행

```bash
./gradlew :core:core-api:bootRun
H='Content-Type: application/json'
curl -X POST localhost:8080/api/v1/members  -H "$H" -d '{"email":"kim@example.com","name":"kim"}'
curl -X POST localhost:8080/api/v1/orders   -H "$H" -d '{"memberId":1,"productName":"keyboard","quantity":2,"unitPrice":50000,"shippingAddress":{"city":"Seoul","street":"Teheran-ro 1","zipCode":"06000"}}'
curl -X POST localhost:8080/api/v1/payments -H "$H" -d '{"orderId":1,"method":"CARD"}'   # local 프로필은 PG 없이 승인
curl localhost:8080/api/v1/orders/1                 # status: PAID
curl localhost:8080/api/v1/payments/1               # status: APPROVED
```

```bash
./gradlew test                       # CI 기본. ArchUnit 포함
./gradlew :core:core-api:asciidoctor # REST Docs HTML 생성 (restDocsTest 선행)
./gradlew checkFormat                # 코드 포맷 검사
./gradlew format                     # 코드 포맷 적용
```

## 코드리뷰 요청 전

```bash
npm i -g @earendil-works/pi-coding-agent   # 최초 1회
cp .env.example .env                        # 모델(ARCH_REVIEW_MODEL)과 토큰(ANTHROPIC_API_KEY 등) 입력. .env 는 커밋되지 않는다
pi                                          # 저장소 루트에서 실행해 프로젝트를 신뢰(/trust)한 뒤, 프롬프트에 /arch-review
pi --approve -p --no-session "/arch-review" < /dev/null   # 비대화형. 종료 코드 0 PASS / 1 BLOCK / 2 WARN
```

`/arch-review` 는 저장소에 포함된 Pi 확장(`.pi/extensions`)이다. ArchUnit 이 구조 규칙을, 리뷰 에이전트가 의미 규칙을 본다.
BLOCK 이면 고치거나 ADR 로 예외를 남긴 뒤 리뷰를 요청한다. 푸시마다 자동으로 돌리려면 `git config core.hooksPath .githooks`.
설정과 사용법은 [docs/REVIEW_AGENT.md](docs/REVIEW_AGENT.md).

## 의존성 버전 관리

모든 버전은 `gradle.properties` 에서 관리한다. 새 라이브러리를 추가할 때는 버전을 `gradle.properties` 에 적고 `build.gradle` 에서 참조한다.

## 런타임 프로필

| 프로필 | 용도 |
|---|---|
| `local` | 네트워크 없이 개발. H2 인메모리 DB, DDL auto create |
| `local-dev` | 로컬에서 dev 환경 자원(DB 등)에 접속 |
| `dev` | 개발 서버 배포 |
| `staging` | 스테이징 서버 배포 |
| `live` | 운영 서버 배포 |

로그 설정은 `support/logging/src/main/resources/logback/logback-{profile}.xml` 로 프로필마다 분리된다.

## 테스트 태스크와 태그

| 태스크 | 포함 태그 | 설명 |
|---|---|---|
| `test` | develop, restdocs 제외 | CI 에서 실행하는 기본 묶음. ArchUnit 규칙 포함 |
| `unitTest` | 태그 없음 | 외부 의존 없는 빠른 단위 테스트 (도메인 모델, ArchUnit) |
| `contextTest` | `context` | Spring 컨텍스트를 올리는 통합 테스트 |
| `restDocsTest` | `restdocs` | REST Docs 스니펫 생성 |
| `developTest` | `develop` | CI 에서 돌리지 않는 개발용 테스트 |

## 권장 IDE 설정 (IntelliJ IDEA)

- `Build, Execution, Deployment > Build Tools > Gradle > Run tests using` 을 `IntelliJ IDEA` 로 설정하면 테스트 실행이 빠르다.
- 포맷터는 [Spring Java Format](https://github.com/spring-io/spring-javaformat#intellij-idea) 플러그인을 사용한다.

## 관련 문서

| 문서 | 언제 보는가 |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 설계 원칙, 코드를 어디에 둘지, usecase 가 필요한지, 트랜잭션을 묶거나 나눌 때, 규칙을 어겨야 할 때, 새 기능을 추가할 때 |
| [docs/ERROR_HANDLING.md](docs/ERROR_HANDLING.md) | 에러 코드를 추가하거나 예외를 던질 때, 오류 응답 형식을 알아야 할 때 |
| [docs/SCALING.md](docs/SCALING.md) | 트래픽·코드가 커져 구조를 바꿔야 하는지, 배치 같은 두 번째 실행 모듈을 만들 때, 다른 앱과 연동할 때 |
| [docs/REVIEW_AGENT.md](docs/REVIEW_AGENT.md) | 코드리뷰 요청 전 `/arch-review` 를 돌릴 때, 결과를 해석할 때, 프롬프트를 바꿀 때 |
