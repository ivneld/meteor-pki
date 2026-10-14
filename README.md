# x509-pki

`v2x-pki/x509` 의 기능(x509-crypto, x509-domain, x509-ra, x509-support, x509-va)을
[meteor-template](https://github.com/ivneld/meteor-template) 구조 위에 다시 구현하는 프로젝트. 뷰 없이 API 만 제공한다.

## 진행 상황

| 단계 | 상태 |
|---|---|
| x509-ra API 계약 (경로, 메서드, 미디어 타입, 요청·응답 DTO, 검증, 오류 응답 모양) | 완료 |
| x509-ra 동작: 인증서 프로파일·EKU, CA 체인(Root, Sub CA, 조회, 다운로드), Leaf 발급(SPKI, CSR) | 완료 |
| x509-ra 동작: Sub CA 갱신(새 키)·폐기, 발급자 유효기간 안에서만 발급·갱신 | 완료 |
| x509-ra 동작: Leaf 갱신·폐기, CRL, CMP, 키 합의·동기화, 통계, 보안 | 예정. 지금은 501 |
| x509-va, x509-crypto, x509-domain, x509-support | 예정 |

옮긴 계약의 전체 목록, 구현 현황, 원본과의 차이는 [docs/X509_RA_API.md](docs/X509_RA_API.md). 계약은 `ApiContractTest` 가, 구현한
동작은 `*ApiIT` 가 지킨다. 프로파일 → Root → Sub CA → Leaf → Sub CA 갱신·폐기를 차례로 호출해 보는 순서는 [docs/API_SCENARIOS.md](docs/API_SCENARIOS.md)
와 `scripts/api-scenario.sh`.

## 모듈 구성

```
x509-pki
├── core
│   ├── core-enum       com.meteor.pki.enums. API 와 저장소가 함께 쓰는 enum (CertType, KeyUsage, StatType ...)
│   └── core-api        유일한 실행 모듈. domain / usecase / api + support(에러 어휘, 웹 설정, 어드바이스)
├── clients
│   └── client-crypto   client.crypto. 키 생성과 서명(HSM/KMS 자리). 개인키는 이 모듈 밖으로 나가지 않는다
├── storage
│   └── db-core         storage(JPA 엔티티, Spring Data) + 저장소 인프라. core-enum 만 의존하고 도메인을 모른다
├── support
│   ├── logging         프로필별 logback, OpenTelemetry, Sentry
│   └── monitoring      Actuator + Prometheus
├── tests
│   └── api-docs        Spring REST Docs 테스트 베이스
└── docs
    ├── X509_RA_API.md    x509-ra 에서 옮긴 API 계약과 구현 현황
    ├── API_SCENARIOS.md  REST API 호출 시나리오 (scripts/api-scenario.sh 로 실행)
    ├── ARCHITECTURE.md   규칙, 계층, 도메인 서비스, 트랜잭션 (템플릿)
    ├── ERROR_HANDLING.md 에러 코드와 오류 응답
    ├── SCALING.md        구조를 키우는 시점과 순서 (템플릿)
    └── REVIEW_AGENT.md   리뷰 에이전트 하네스 (템플릿)
```

계층과 규칙은 템플릿을 그대로 따른다. 코드는 `domain`, `usecase`, `api` 세 계층이고, 규칙은 애그리거트와 `*Policy` 에, 저장소 호출은
`*Reader` / `*Writer` 에, 여러 도메인 서비스를 엮거나 외부 시스템을 부르는 흐름은 `*UseCase` 에 둔다. 전체 규칙은
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), 구조 규칙은 `./gradlew test` 의 `ArchitectureRules` 가 검사한다.

## 원본과 맞춘 것

- **경로와 미디어 타입.** x509-ra 의 v2 API 를 경로에서 `/v2` 만 빼고 옮겼다(`/api/v2/certificates/root` → `/api/certificates/root`).
  v1 API 는 옮기지 않았고, 인증서의 CRL DP 에 들어가는 공개 CRL 배포 경로 `GET /api/crl/{caSkiHex}.crl` 만 원본 그대로 남겼다.
  `/cmp`, 키 합의·동기화, 통계, `/internal/deployment/tls-bundle` 도 원본 경로 그대로다.
- **JSON.** `Instant` 는 초 단위 ISO-8601, `byte[]` 는 Base64, 요청에 없는 `boolean`/`int` 는 `false`/`0` (`support.web.JacksonConfig`).
- **오류 응답.** `title: PKI-ERROR`, `errorCode`, `timestamp`, `errors[{field, message}]` 를 가진 ProblemDetail ([ERROR_HANDLING.md](docs/ERROR_HANDLING.md)).
- **조건부 엔드포인트.** 키 합의는 `key-agreement.role=server`, 키 동기화는 `pki.features.key-sync.enabled=true` 일 때만 등록된다.

## 실행

```bash
./gradlew :core:core-api:bootRun
scripts/api-scenario.sh              # 다른 터미널에서. 프로파일 → Root → Sub CA → Leaf → 조회·다운로드 → 오류 응답
```

```bash
./gradlew test                       # CI 기본. ArchUnit, API 계약 포함
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
| `unitTest` | 태그 없음 | 외부 의존 없는 빠른 단위 테스트 (도메인 모델, ArchUnit, API 계약) |
| `contextTest` | `context` | Spring 컨텍스트를 올리는 통합 테스트 |
| `restDocsTest` | `restdocs` | REST Docs 스니펫 생성 |
| `developTest` | `develop` | CI 에서 돌리지 않는 개발용 테스트 |

## 권장 IDE 설정 (IntelliJ IDEA)

- `Build, Execution, Deployment > Build Tools > Gradle > Run tests using` 을 `IntelliJ IDEA` 로 설정하면 테스트 실행이 빠르다.
- 포맷터는 [Spring Java Format](https://github.com/spring-io/spring-javaformat#intellij-idea) 플러그인을 사용한다.

## 관련 문서

| 문서 | 언제 보는가 |
|---|---|
| [docs/X509_RA_API.md](docs/X509_RA_API.md) | x509-ra 의 어떤 API 를 옮겼는지, 원본과 무엇이 다른지, 무엇이 남았는지 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 설계 원칙, 코드를 어디에 둘지, usecase 가 필요한지, 트랜잭션을 묶거나 나눌 때, 규칙을 어겨야 할 때, 새 기능을 추가할 때 |
| [docs/ERROR_HANDLING.md](docs/ERROR_HANDLING.md) | 에러 코드를 추가하거나 예외를 던질 때, 오류 응답 형식을 알아야 할 때 |
| [docs/SCALING.md](docs/SCALING.md) | 트래픽·코드가 커져 구조를 바꿔야 하는지, 배치 같은 두 번째 실행 모듈을 만들 때, 다른 앱과 연동할 때 |
| [docs/REVIEW_AGENT.md](docs/REVIEW_AGENT.md) | 코드리뷰 요청 전 `/arch-review` 를 돌릴 때, 결과를 해석할 때, 프롬프트를 바꿀 때 |
