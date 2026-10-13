# 아키텍처 규칙

이 문서는 템플릿이 강제하는 규칙의 단일 출처다. 규칙마다 ID 가 있고, ArchUnit 테스트(`core-api` 의 `ArchitectureRules`)와
리뷰 에이전트 체크리스트([REVIEW_AGENT.md](REVIEW_AGENT.md))가 같은 ID 를 쓴다. 에러 코드는 [ERROR_HANDLING.md](ERROR_HANDLING.md), 구조를
키우는 시점과 순서는 [SCALING.md](SCALING.md) 에 있다. 규칙을 바꾸려면 이 문서를 먼저 고치고 테스트와 체크리스트를 따라 고친다.

> 이 문서는 [meteor-template](https://github.com/ivneld/meteor-template) 에서 가져왔다. 본문의 `Order`, `Member`, `Payment`,
> `PgClient` 예시는 템플릿의 레퍼런스 코드를 가리키며 이 저장소에는 없다. 이 저장소의 본보기는 [새 기능 추가](#새-기능-추가)의 x509 코드다.

- `R-xx` 구조 규칙. 결정적이며 ArchUnit 이 CI 에서 차단한다.
- `S-xx` 의미 규칙. 코드 구조만으로 판단할 수 없어 리뷰 에이전트와 사람이 본다.

## 설계 원칙

규칙은 모두 아래 원칙에서 나온다. 규칙이 상황에 맞지 않을 때는 원칙으로 돌아가 판단한다.

### 1. 애플리케이션을 시작하기 위한 구조다. 계층은 셋이다

이 템플릿은 애플리케이션의 초기 시작을 위한 것이다. 도메인마다 패키지를 나누는 버티컬 슬라이스, 컨텍스트 사이의 Facade·이벤트 통로 같은
장치는 두지 않는다. 코드는 `domain`, `usecase`, `api` 세 계층으로만 나누고, 각 계층 안에서는 도메인별 하위 패키지를 만들지 않는다.
한 패키지가 감당하기 어려울 만큼 커지면 그때 나눈다([SCALING.md](SCALING.md)).

### 2. 규칙은 도메인 모델 한 곳에 모은다

"CREATED 일 때만 결제할 수 있다" 같은 규칙은 애그리거트에, 여러 애그리거트에 걸친 규칙은 `*Policy` 에 둔다. 도메인 모델은 Spring 과
JPA 를 모르는 순수 객체라서 목 없이 단위 테스트한다. 도메인 서비스와 usecase 는 사실을 꺼내 넘기고 저장할 뿐 스스로 판단하지 않는다.
규칙을 바꿀 때 고칠 곳이 하나여야 한다.

### 3. 도메인 서비스는 애그리거트 하나를 맡는다

리포지토리 호출과 공통 도메인 처리는 도메인 서비스(`*Reader`, `*Writer`)에 모은다. 도메인 서비스가 커지는 길은 대부분 다른 애그리거트의
저장소나 다른 도메인 서비스를 부르는 데서 열린다. 그래서 도메인 서비스 하나는 자기 애그리거트와 자기 `*Repository` 만 다루고, 여러
애그리거트를 엮거나 외부 시스템을 부르는 일은 전부 usecase 로 올린다. 이름은 역할이 드러나는 닫힌 어휘만 쓴다. `*Service` 는 무엇을
하는 클래스인지 드러내지 못하므로 쓰지 않는다.

### 4. 트랜잭션 경계는 코드에서 보여야 한다

도메인 서비스와 usecase 모두 트랜잭션을 선언할 수 있다. 도메인 서비스는 기본 전파(`REQUIRED`)만 써서 혼자 불리면 독립된 트랜잭션이
되고, usecase 가 연 트랜잭션 안에서 불리면 거기에 참여한다. 경계를 조절하는 일(여러 도메인 서비스를 한 트랜잭션으로 묶기, 의도적으로
나누기, `REQUIRES_NEW`)은 usecase 만 한다. 바깥 트랜잭션을 빠뜨려 생기는 부분 커밋은 ArchUnit(R-08)이 막는다.

### 5. 영속화 기술은 JPA 로 고정한다. 엔티티와 애그리거트는 그래도 분리한다

영속화 기술은 JPA 로 고정이고, 한번 DB 에 두기로 한 데이터를 다른 저장소로 옮기는 일은 드물다. 그래서 저장소를 갈아끼우기 위한
인터페이스는 두지 않는다. `*Repository` 는 Spring Data 를 감싼 구체 클래스 하나다.

그런데도 JPA 엔티티와 애그리거트를 분리하는 이유는 두 가지다.

- **애그리거트와 값 객체를 JPA 의 간섭 없이 쓰기 위해서.** 기본 생성자, 가변 필드, 프록시, 지연 로딩, 더티 체킹 같은 JPA 의 요구가
  도메인 모델에 스며들지 않는다. 애그리거트는 `final` 필드와 정적 팩토리, 불변 record 값 객체로 규칙에만 집중한다.
- **애그리거트와 값 객체를 결과로 그대로 돌려주기 위해서.** 영속성 컨텍스트에 묶인 엔티티는 트랜잭션 밖으로 내보내기 어렵지만 순수
  애그리거트는 그대로 돌려줄 수 있다. 그래서 별도 결과 객체를 반드시 만들 필요가 없다.

자세한 득실은 [영속화 결정](#영속화-결정).

### 6. API 계약은 도메인 모델과 분리한다

요청·응답 객체에는 도메인 타입을 쓰지 않는다(R-12). 값 객체는 원시 타입이나 api 전용 record 로 풀고, enum 은 core-enum 의 것을 그대로
쓴다. 돌려받은 애그리거트에서는 getter 만 읽어 응답을 만든다(R-10). API 계약이 도메인 모델에 묶이면 둘 중 하나를 못 바꾸고, 컨트롤러가
애그리거트의 행위를 부르면 트랜잭션 밖에서 상태가 바뀐다.

### 7. 지금도 이득을 주는 대비만 한다

미래를 위한 장치는 매일 복잡도라는 이자를 낸다. 배치처럼 도메인을 함께 쓰는 두 번째 실행 모듈이 생기면 `domain` 을 core-domain 모듈로
추출한다. 저장소 어댑터를 domain 에 두고 도메인 모델이 프레임워크를 모르게 해 둔 것은 그 추출을 파일 이동으로 끝내기 위해서이고, 그 외의
대비는 하지 않는다.

## 계층과 모듈

```
core-enum       com.meteor.pki.enums. 모든 도메인 enum. 어떤 모듈·라이브러리에도 의존하지 않는다
core-api        유일한 실행 모듈. com.meteor.pki.domain / usecase / api + support(error, web, async)
storage/db-core com.meteor.pki.storage(JPA 엔티티, Spring Data 인터페이스) + support.storage(DataSource, JPA 설정, BaseEntity).
                도메인을 모른다. enum 은 core-enum 의 것을 @Enumerated(STRING) 컬럼으로, 값 객체는 컬럼으로 펼쳐 저장한다
clients/*       외부 시스템 어댑터. com.meteor.pki.client.<이름>. 예: client-crypto(키 생성과 서명, HSM/KMS 자리)
support/*       logging, monitoring
```

```
core-api ──► storage/db-core ──► core-enum
   ├──► clients/*
   └─────────────────────────────▲
```

```
 api ──────────► usecase ──────────► domain ──────► storage
  │                 │                  ▲  (*Repository 만)
  │                 └──► clients       │
  └────────────────────────────────────┘
     (애그리거트 하나로 끝나는 흐름은 api 가 *Reader / *Writer 를 바로 부른다)

 모든 계층과 storage ──► enums (core-enum)
 domain ──► support.error
```

모듈 의존은 `core-api → storage/db-core → core-enum` 한 방향이다. storage 는 도메인을 모르지만 core-enum 은 알므로 상태는 enum 컬럼으로,
값 객체는 컬럼으로 펼쳐 저장하고, domain 의 `*Repository` 어댑터가 엔티티를 애그리거트로 바꾼다. storage 와 clients 가 core-api 를
모른다는 것은 Gradle 이 컴파일 단계에서 막는다. 모듈로 막을 수 없는 세부 규칙은 ArchUnit(`ArchitectureRules`)이 `./gradlew test` 에서 잡는다.

### 패키지 규약

```
com.meteor.pki.enums              모든 도메인 enum (상태, 종류)                                                        (core-enum)
com.meteor.pki.domain             애그리거트, 값 객체(record), *Policy           ← 도메인 모델. 순수 자바             (core-api)
                              *Reader, *Writer                               ← 도메인 서비스. Spring 빈
                              *Repository                                    ← 저장소 어댑터. 엔티티 ↔ 애그리거트
com.meteor.pki.usecase            *UseCase, *Command, *Result                                                        (core-api)
com.meteor.pki.api                *Controller                                                                        (core-api)
  api.request / api.response  요청·응답 DTO
com.meteor.pki.storage            *Entity, *JpaRepository. 도메인을 모르며 상태는 enum 컬럼                          (storage/db-core)
com.meteor.pki.client.<이름>      외부 연동 어댑터                                                                    (clients/*)
com.meteor.pki.support            어느 계층에도 속하지 않는 공통 영역
  support.error                 ErrorCode, CoreException, LogLevel. 프레임워크를 모른다                          (core-api)
  support.web / support.async   어드바이스, ProblemDetails, 설정                                                  (core-api)
  support.storage               DataSource/JPA 설정, BaseEntity                                                   (storage/db-core)
```

### 계층별 책임

| 계층 | 하는 일 | 하지 않는 것 |
|---|---|---|
| `api` | HTTP 요청 → Command 나 도메인 서비스 인자로 변환, 애그리거트 → 응답 DTO 변환. 엔드포인트 하나는 `*UseCase` 나 `*Reader`/`*Writer` 의 메서드 하나만 호출 | `@Transactional`, `*Repository`·`*Policy`·storage·client 접근, 애그리거트의 상태 변경 메서드 호출, DTO 필드에 도메인 타입 |
| `usecase` | 여러 도메인 서비스를 엮는 흐름, 외부 시스템 호출, 트랜잭션 경계 조절(묶기, 나누기, `REQUIRES_NEW`) | 규칙 판단, 도메인 상태로 분기, `*Repository` 직접 사용, 다른 `*UseCase` 호출 |
| `domain` 도메인 서비스 | 애그리거트 하나의 조회(`*Reader`)와 변경(`*Writer`). 꺼내고, 애그리거트·Policy 에 넘기고, 저장한다 | 다른 애그리거트의 `*Repository`, 다른 도메인 서비스, 외부 시스템, usecase·api 타입, 기본 전파 외의 트랜잭션 설정 |
| `domain` 도메인 모델 | 모든 규칙과 상태 전이. 애그리거트, 값 객체, `*Policy` | Spring, JPA, 저장소, 도메인 서비스 |
| `domain` 저장소 어댑터 | `*Repository` 가 storage 의 Spring Data 를 감싸 엔티티 ↔ 애그리거트 변환을 가둔다 | 비즈니스 규칙, 엔티티를 밖으로 내보내기 |
| `enums` | 상태·종류 enum. 모든 계층과 storage 가 같은 타입을 쓴다 | 다른 모듈 의존, 로직 |
| `storage` / `clients` | JPA 엔티티와 Spring Data 인터페이스, 외부 연동 | 도메인 참조, 비즈니스 규칙, 트랜잭션 경계 |

## 도메인 서비스

### 이름

domain 의 클래스 종류는 닫혀 있다. 목록 밖의 이름(`*Service`, `*Manager`, `*Handler`, `*Processor`, `*Helper`)은 역할을 드러내지 못하므로
쓰지 않는다.

| 종류 | 이름 | 트랜잭션 | 예 |
|---|---|---|---|
| 애그리거트 | 도메인 명사 | 없음 | `Order`, `Member` |
| 값 객체 | 도메인 명사 (record) | 없음 | `Money`, `Address`, `Email` |
| 순수 규칙 | `*Policy` | 없음 | `OrderLimitPolicy` |
| 조회 | `*Reader` | 클래스에 `@Transactional(readOnly = true)` | `OrderReader.find`, `MemberReader.ensureActive` |
| 생성·변경 | `*Writer` | 클래스에 `@Transactional` | `OrderWriter.place`, `OrderWriter.cancel` |
| 저장소 어댑터 | `*Repository` | 없음 (호출자의 트랜잭션) | `OrderRepository` |

`*Reader` 와 `*Writer` 는 이름이 곧 트랜잭션 속성이라 R-07 로 그대로 검사한다. `*Writer` 가 커지면 `OrderCanceller` 처럼 동사 단위로
쪼개지 말고 먼저 규칙이 Writer 에 새어 있지 않은지(S-04) 본다. 그래도 크면 `*Writer` 를 둘로 나누되 이름은 `*Writer` 로 끝낸다.

### 범위

| 규칙 | 이유 | 강제 |
|---|---|---|
| 자기 애그리거트의 `*Repository` 만 쓴다 (`OrderWriter` → `OrderRepository`) | 다른 애그리거트의 데이터를 읽거나 쓰기 시작하면 서비스가 흐름 전체를 떠안는다 | R-04 |
| 다른 도메인 서비스를 부르지 않는다 | 서비스끼리 엮이는 연쇄가 가장 흔한 비대화 경로다. 엮는 일은 usecase 가 한다 | R-04 |
| 외부 시스템(`client`)을 부르지 않는다 | 외부 I/O 가 트랜잭션 안으로 들어오고 경계를 나눌 수 없게 된다 | R-01 |
| usecase·api 타입(`*Command`, request)을 받지 않는다 | 도메인 연산이 화면·API 모양을 따라가지 않게 한다. 인자는 원시 타입과 도메인 타입 | R-01 |
| 트랜잭션은 기본 전파만 쓴다 | 경계 조절은 usecase 한 곳에서 한다 | R-07 |
| 규칙 판단은 애그리거트와 `*Policy` 에 두고, 서비스는 꺼내고 → 넘기고 → 저장한다 | 리포지토리 없이 판단할 수 있는 규칙은 순수 객체에 있어야 단위 테스트가 쉽다 | S-01, S-04 |

판단이 애매할 때.

- **다른 애그리거트의 값 하나만 필요하다.** 리포지토리를 직접 읽지 않는다. usecase 가 상대 도메인 서비스에서 값을 받아 인자로 넘긴다.
  결제 금액을 `OrderReader.payableAmount()` 에서 받아 `PaymentWriter.request(...)` 에 넘기는 것이 본보기다.
- **usecase 가 여러 서비스를 엮는 흐름이 여러 군데서 반복된다.** 그 흐름에 이름을 붙여 usecase 메서드 하나로 만든다. usecase 끼리는
  부르지 않는다(R-09).
- **Policy 가 리포지토리를 직접 쓰면 편하겠다.** Policy 는 순수하게 둔다. 사실을 꺼내는 일은 서비스가, 판단은 Policy 가 한다.

### usecase 가 필요한 때

| 흐름 | 호출 | 본보기 |
|---|---|---|
| 애그리거트 하나로 끝난다 | api → `*Reader` / `*Writer` | 회원 가입·탈퇴, 주문 조회·취소 |
| 여러 도메인 서비스를 엮는다 | api → `*UseCase` (바깥 트랜잭션) → 도메인 서비스들 | `OrderUseCase.place` (회원 확인 + 주문 생성) |
| 외부 시스템을 부르거나 트랜잭션을 나눈다 | api → `*UseCase` (`TransactionTemplate`) → 도메인 서비스들, client | `PaymentUseCase.pay` |

한 엔드포인트에서 usecase·도메인 서비스를 여러 개 부르지 않는다(R-11). 호출마다 트랜잭션이 따로 열려 부분 커밋이 생기고, 흐름을
컨트롤러가 조율하게 된다. 묶어야 하면 usecase 메서드 하나로 만든다.

usecase 의 반환 타입은 상황에 맞게 고른다. 애그리거트 하나를 돌려주면 충분하면 그대로 돌려주고(`OrderUseCase.place` → `Order`), 여러
애그리거트를 합치거나 계산 결과를 담아야 하면 `*Result` 를 둔다. 식별자 하나처럼 단순한 값은 원시 타입으로 돌려준다(`MemberWriter.register` → `Long`).

## 트랜잭션

### 선언하는 곳

| 위치 | 허용 | 강제 |
|---|---|---|
| `*Reader` | 클래스에 `@Transactional(readOnly = true)`. 메서드에 다시 붙인다면 기본 전파만 | R-06, R-07 |
| `*Writer` | 클래스에 `@Transactional`. 메서드에 다시 붙인다면 기본 전파만 | R-06, R-07 |
| `*UseCase` | `@Transactional` (모든 속성), `TransactionTemplate` | R-06 |
| 그 외 (api, `*Repository`, `*Policy`, storage, client) | 없음 | R-06 |

### 묶기

도메인 서비스를 둘 이상 부르고 그중 `*Writer` 가 있는 usecase 메서드는 바깥 트랜잭션을 연다(R-08). 도메인 서비스는 기본 전파라 그 트랜잭션에
참여하므로 함께 커밋되고 함께 롤백된다. 바깥 트랜잭션을 빠뜨리면 서비스마다 따로 커밋해, 두 번째가 실패했을 때 첫 번째만 남는다.
컴파일도 런타임도 알려주지 않으므로 구조 규칙으로 막는다.

readOnly 트랜잭션의 usecase 메서드는 `*Writer` 를 부르지 않는다(R-08). 읽기 전용 트랜잭션 안에서의 쓰기는 플러시되지 않거나 에러가 난다.

### 나누기

외부 API 호출 동안 DB 커넥션을 잡고 있지 않으려는 것처럼 의도적으로 트랜잭션을 나눠야 하는 유스케이스는 메서드에 `@Transactional` 을
붙이지 않고 `TransactionTemplate` 으로 구간을 감싼다. 경계가 한 메서드 안에 그대로 보인다.

```java
public Payment pay(PaymentPayCommand command) {
    Payment requested = transaction.execute(status ->                     // 트랜잭션 1
            paymentWriter.request(command.orderId(), orderReader.payableAmount(command.orderId()), command.method()));

    PgApproval approval = pgClient.approve(requested.paymentKey(), ...);   // 트랜잭션 밖

    return transaction.execute(status -> {                                // 트랜잭션 2
        Payment approved = paymentWriter.approve(requested.getId(), approval.transactionKey());
        orderWriter.markPaid(command.orderId());
        return approved;
    });
}
```

다른 방법은 쓰지 않는다.

- **같은 클래스의 `@Transactional` 메서드를 `this` 로 부르기.** Spring 트랜잭션은 프록시로 동작하므로 자기 호출에는 트랜잭션이 적용되지
  않는다. 컴파일도 테스트도 조용히 통과한다.
- **구간을 별도 빈으로 빼기.** 그 빈이 usecase 면 R-09, 도메인 서비스면 경계가 domain 으로 샌다.
- **바깥 트랜잭션 안에서 `REQUIRES_NEW` 로 열기.** 바깥 커넥션을 쥔 채 하나를 더 써서 부하 시 커넥션 풀이 고갈되고, 어느 구간이 언제
  커밋되는지 메서드만 봐서는 알 수 없다.

나눈다는 것은 앞 구간은 커밋됐는데 뒤 구간이 실패하는 상태를 받아들인다는 뜻이다. 그래서 함께 설계한다(S-06).

1. **앞 구간이 남긴 상태가 의미를 갖게 한다.** `Payment` 는 PG 호출 전에 `REQUESTED` 로 저장되어, PG 가 거절하면 `FAILED`, 승인 뒤 처리가
   실패하면 `REQUESTED` 로 남아 대사할 수 있다.
2. **뒤 구간을 다시 실행해도 안전하게 한다.** 애그리거트가 상태로 거절하므로(`Payment.approve` 는 `REQUESTED` 에서만) 재시도가 결과를 바꾸지 않는다.

ArchUnit 은 람다 본문의 호출도 감싸는 메서드의 것으로 세므로, `TransactionTemplate` 을 쓰는 메서드에서 쓰기가 모두 템플릿 안에 있는지는
구조 규칙으로 판별하지 못한다. 그 부분은 리뷰(S-06)가 본다.

### 함정

- **rollback-only.** 바깥 트랜잭션에 참여한 도메인 서비스가 예외를 던지면 트랜잭션 전체에 rollback-only 표시가 붙는다. usecase 가 그 예외를
  잡고 계속 진행하면 커밋 시점에 `UnexpectedRollbackException` 이 난다. 트랜잭션 안에서 도메인 서비스의 예외를 잡아 삼키지 않는다(S-07).
  트랜잭션 밖(외부 호출 실패 등)에서 잡아 처리하는 것은 괜찮다.
- **readOnly 무시.** `*Reader` 의 `readOnly = true` 는 바깥 쓰기 트랜잭션에 참여하면 무시된다. 이것은 의도된 동작이다.

## 값 타입의 자리

```
새 타입이 생겼다
  │
  ├─ 식별자가 있고 상태가 바뀐다 ──▶ 애그리거트 (com.meteor.pki.domain)
  │
  ├─ enum 이다 ──▶ com.meteor.pki.enums (core-enum)     예: OrderStatus, PaymentMethod
  │                storage 의 엔티티도 같은 타입으로 저장하므로 모듈이 따로 있다
  │
  └─ record 다 ──▶ com.meteor.pki.domain                예: Money, Address, Email
```

## 영속화 결정

JPA 를 쓰되 **도메인 모델로 쓰지 않고 영속 엔진으로만 쓴다.** 애그리거트는 순수 자바로 `com.meteor.pki.domain` 에, JPA 엔티티는 storage 모듈의
`com.meteor.pki.storage` 에 있고, 둘 사이 변환은 domain 의 `*Repository` 어댑터가 한다. storage 모듈은 도메인을 모르지만 core-enum 은 알므로
상태는 enum 컬럼으로, 값 객체는 컬럼으로 펼쳐 저장하고 어댑터가 record 로 조립한다. 엔티티와 Spring Data 를 참조할 수 있는 곳은 storage 와
`*Repository` 뿐이고(R-05), 도메인 모델이 JPA 를 모르는 것은 R-01 이 지킨다. 그 결과 JPA 의 이점 중 일부는 포기하고 일부는 유지한다.

| 포기하는 것 (애그리거트 중심 설계가 원래 피하는 것) | 유지하는 것 (영속 모델에 붙는 인프라 기능) |
|---|---|
| 더티 체킹. 저장은 `save()` 로 명시한다 (S-05) | `@TenantId` 로 모든 쿼리에 테넌트 조건 자동 적용 |
| 지연 로딩과 연관관계 탐색. 애그리거트는 통째로 로딩한다 | `@Version` 낙관적 락, Envers 감사 이력, 2차 캐시 |
| cascade. 저장은 애그리거트 단위로 끝난다 | 스키마 매핑과 `validate`, 배치 insert, 방언 추상화 |

변환 코드는 애그리거트마다 `*Repository` 의 `save` / `toDomain` 과 애그리거트의 `restore` 로 고정되어 있어 비용이 예측 가능하다.
`@Version` 을 쓸 때 애그리거트가 트랜잭션을 넘어 전달된다면 `restore()` 에 version 을 포함시켜 `*Repository` 가 복원 시 넘기도록 한다.

애그리거트끼리는 ID 로만 참조한다. 다른 애그리거트를 필드로 갖거나 엔티티 사이에 JPA 연관관계를 걸지 않는다. 애그리거트가 통째로
로딩·저장되는 단위이고, 연관관계를 따라가면 그 단위가 무너진다.

## 구조 규칙 (R)

| ID | 규칙 | 이유 |
|---|---|---|
| R-01 | 도메인 모델(애그리거트, 값 객체, `*Policy`)은 Spring, JPA, storage, client, usecase, api, `*Repository`, `*Reader`, `*Writer` 에 의존하지 않는다. domain 전체는 usecase, api, client 를 모르고, `support` 에서는 에러 어휘(`support.error`)만 쓴다. 에러 어휘도 프레임워크를 모른다 | 규칙이 프레임워크와 분리되어야 순수 단위 테스트가 가능하다. 도메인 서비스가 외부 시스템과 상위 계층 타입을 모르게 해 범위를 지킨다 |
| R-02 | usecase 는 api 를 모른다. storage 와 client 는 domain, usecase, api 를 모른다 | 의존은 바깥에서 안쪽으로만 흐른다. storage 가 도메인을 모르는 것은 모듈 의존이 1차로 막고 이 규칙이 다시 확인한다 |
| R-03 | `*Service` 접미어는 쓰지 않는다. domain 의 Spring 빈은 `*Reader`, `*Writer`, `*Repository` 뿐이다. usecase 의 클래스는 `*UseCase`, `*Command`, `*Result` 중 하나다 | 이름만으로 역할이 드러나야 규칙과 흐름과 트랜잭션이 한 클래스에 섞이지 않는다 |
| R-04 | `*Reader` / `*Writer` 는 같은 이름의 `*Repository` 만 쓰고, 다른 `*Repository`, `*Reader`, `*Writer`, storage 를 쓰지 않는다 | 도메인 서비스가 애그리거트 하나를 넘어서면 흐름 전체를 떠안고 비대해진다 |
| R-05 | JPA 엔티티는 storage 에만 있고, storage 를 참조하는 것은 domain 의 `*Repository` 뿐이다 | 영속 모델이 API 계약이나 도메인이 되는 것을 막는다. 변환은 어댑터 안에서 끝난다 |
| R-06 | `@Transactional` 은 usecase 와 `*Reader`/`*Writer` 에만 있다. `TransactionTemplate` 같은 프로그래밍 방식 트랜잭션은 usecase 에만 있다 | 트랜잭션을 여는 곳이 두 종류뿐이어야 "이 코드가 어느 트랜잭션 안인가"를 추적할 수 있다 |
| R-07 | `*Reader` 는 클래스에 `@Transactional(readOnly = true)`, `*Writer` 는 클래스에 `@Transactional` 을 붙이고, 도메인 서비스는 기본 전파(`REQUIRED`)만 쓴다 | 도메인 서비스는 혼자면 독립 트랜잭션, usecase 안이면 참여한다. 경계 조절은 usecase 한 곳에서만 한다 |
| R-08 | 도메인 서비스를 둘 이상 부르고 그중 `*Writer` 가 있는 usecase 메서드는 `@Transactional` 이거나 `TransactionTemplate` 을 쓴다. readOnly usecase 메서드는 `*Writer` 를 부르지 않는다 | 바깥 트랜잭션 누락으로 생기는 조용한 부분 커밋을 막는다 |
| R-09 | usecase 는 `*Repository` 와 storage 를 쓰지 않고, 다른 `*UseCase` 를 호출하지 않는다 | 저장소 접근은 도메인 서비스 한 곳으로 모은다. usecase 끼리 부르면 트랜잭션 경계가 중첩된다 |
| R-10 | api 는 `*Repository`, `*Policy`, storage, client 를 쓰지 않는다. 돌려받은 애그리거트에서는 getter 만 읽는다 | 컨트롤러가 애그리거트의 상태 변경 메서드를 부르면 트랜잭션 밖에서 상태가 바뀌고 저장이 누락된다 |
| R-11 | 엔드포인트 하나는 `*UseCase` 나 `*Reader`/`*Writer` 의 메서드를 하나만 부른다 | 여러 개를 부르면 호출마다 트랜잭션이 따로 열려 부분 커밋이 생기고 흐름을 컨트롤러가 조율하게 된다 |
| R-12 | api 의 요청·응답 객체(`api.request`, `api.response`)는 domain 타입을 필드로 갖지 않는다 | API 계약이 도메인 모델에 묶이면 둘 중 하나를 못 바꾼다. 값 객체는 원시 타입이나 api 전용 record 로 푼다. enum 은 core-enum 이라 허용 |
| R-13 | core-enum 의 enum 은 다른 패키지와 프레임워크에 의존하지 않는다 | 모든 계층과 storage 가 참조하므로 의존이 생기면 전파된다 |

## 의미 규칙 (S)

ArchUnit 이 볼 수 없는 규칙. 리뷰 에이전트가 PR 마다 확인한다.

| ID | 규칙 | 신호 |
|---|---|---|
| S-01 | usecase 와 도메인 서비스는 도메인 상태로 분기하지 않는다 | `if (order.getStatus() == ...)`. 애그리거트 메서드로 내린다 |
| S-02 | 규칙이 있는 애그리거트는 빈약하지 않다 | getter 만 있고 상태 변경이 밖에서 일어남. setter 존재. 단, 상태 전이·계산·정책이 없는 설정·코드 관리 기능은 단순한 모델을 허용한다 |
| S-03 | 컨트롤러는 VO 를 생성·변환만 하고 연산하지 않는다 | 컨트롤러에서 `money.times(...)` 같은 계산 |
| S-04 | 여러 애그리거트에 걸친 규칙은 `*Policy` 에 둔다. 도메인 서비스와 usecase 는 사실을 꺼내 넘기기만 한다 | `*Writer` 나 usecase 가 저장소 조회 결과(건수, 목록)를 직접 비교해 거절·분기함 |
| S-05 | 저장은 명시적이다 | 애그리거트를 바꾸고 `repository.save()` 를 호출하지 않는 `*Writer` |
| S-06 | 트랜잭션을 나눈 유스케이스는 중간 상태와 재실행 안전성을 설계하고, 외부 I/O 를 트랜잭션 안에 두지 않는다 | `@Transactional` usecase 메서드 안의 client 호출. `TransactionTemplate` 밖에서 쓰기를 엮음. 앞 구간이 남긴 상태가 아무 의미도 없음(실패 시 정리·대사 불가) |
| S-07 | 트랜잭션 안에서 도메인 서비스의 예외를 잡아 삼키지 않는다 | `@Transactional` usecase 메서드나 `TransactionTemplate` 람다 안에서 도메인 서비스 호출을 try-catch 로 감싸고 계속 진행함 |
| S-08 | `*Repository` 어댑터와 client 는 변환만 하고 규칙을 갖지 않는다 | 어댑터 안의 비즈니스 분기, 계산 |

## 예외 처리

규칙을 지킬 수 없는 상황은 있다. 그때는 우회하지 말고 기록한다.

1. ArchUnit 위반이면 `FreezingArchRule` 로 해당 위반만 동결한다. 동결 파일은 저장소에 커밋한다.
2. `docs/adr/` 에 ADR 한 장을 쓴다. 규칙 ID, 이유, 만료 조건(날짜 또는 상황), 되돌리는 방법.
3. 분기마다 동결 목록과 ADR 을 리뷰해 만료된 것을 되돌린다.

### 값 타입의 검증 실패

VO 는 외부 의존이 없으므로 생성자의 검증 실패는 `IllegalArgumentException` 으로 던진다. `ApiControllerAdvice` 가 이를 `C001` 로 바꾼다.
도메인 규칙 위반은 `CoreException` 으로 던진다. 둘의 구분은 "값 자체가 성립하지 않는가"(`Money(-1)`)와 "값은 맞지만 지금 상태에서
허용되지 않는가"(`order.cancel()` on PAID)다.

## 새 기능 추가

`member`, `order`, `payment` 세 도메인이 레퍼런스다(meteor-template). 사람은 문서보다 옆 코드를 복사하므로, 레퍼런스가 규칙을 어기지 않게 유지하는 것이
가장 강한 강제 수단이다.

| 본보기 | 템플릿 코드 | 이 저장소 |
|---|---|---|
| usecase 없이 api 가 도메인 서비스를 바로 부름 | `MemberController` → `MemberReader`, `MemberWriter` | `EkuController` → `EkuReader`, `EkuWriter`, `CertQueryController` → `CertReader` |
| 여러 애그리거트에 걸친 규칙 | `OrderLimitPolicy` (사실은 `OrderWriter` 가 꺼냄) | `CertProfileNamePolicy` (사실은 `CertProfileWriter` 가 꺼냄) |
| 여러 도메인 서비스를 한 트랜잭션으로 묶는 usecase | `OrderUseCase.place` | `CertProfileUseCase.create` (EKU 확인 + 프로파일 저장) |
| 외부 호출을 사이에 두고 트랜잭션을 나누는 usecase | `PaymentUseCase.pay` | `CertIssuanceUseCase` (키 생성·서명은 트랜잭션 밖, 저장만 `TransactionTemplate`) |

1. core-enum 의 `com.meteor.pki.enums` 에 상태·종류 enum 을 둔다.
2. `com.meteor.pki.domain` 에 애그리거트와 값 객체를 두고 순수 단위 테스트를 같이 쓴다. 상태 전이나 정책이 없는 기능이면 단순한 모델로 시작한다(S-02).
3. storage/db-core 의 `com.meteor.pki.storage` 에 `*Entity` 와 `*JpaRepository` 를 둔다. 둘 다 public 이고 도메인을 모른다. 상태는 enum 컬럼(`@Enumerated(STRING)`), 값 객체는 컬럼으로 저장한다.
4. `com.meteor.pki.domain` 에 `*Repository` 어댑터를 둔다. Spring Data 를 감싸 엔티티 ↔ 애그리거트 변환을 하고 애그리거트만 돌려준다. 왕복 테스트(`*RepositoryIT`)를 같이 쓴다.
5. `com.meteor.pki.domain` 에 `*Reader` 와 `*Writer` 를 둔다. 자기 `*Repository` 만 쓴다.
6. 흐름이 애그리거트 하나로 끝나면 `com.meteor.pki.api` 의 컨트롤러가 도메인 서비스를 바로 부른다. 여러 도메인 서비스를 엮거나 외부 시스템을 부르면 `com.meteor.pki.usecase` 에 `*UseCase` 와 `*Command` 를 둔다.
7. 응답 DTO 는 getter 만 읽어 만들고 필드에는 도메인 타입을 두지 않는다(R-12).
8. `ErrorCode` 에 접두어를 추가한다.
9. `./gradlew test` 로 R 규칙을 통과하는지 확인한다.

## 측정

분기 리뷰에서 보는 숫자.

- 동결된 ArchUnit 위반 수
- `TransactionTemplate` 으로 트랜잭션을 나눈 usecase 메서드 수와 각각의 중간 상태 처리(대사·재시도) 여부
- domain 패키지의 클래스 수 (한 패키지가 감당하기 어려워지는 시점을 본다)
- `*Writer` 별 public 메서드 수 (비대해지는 서비스를 본다)
