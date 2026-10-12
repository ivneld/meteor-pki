# 응답 포맷과 에러 코드

성공 응답은 리소스 DTO 나 파일(PEM, DER, ZIP)을 그대로 내려준다. 오류 응답은 [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
(`application/problem+json`)이며, 모양은 x509-ra 의 `GlobalExceptionHandler` 와 같다. 예외는 `/cmp` 로, 처리 실패도 HTTP 200 과
error PKIBody 로 돌려준다([X509_RA_API.md](X509_RA_API.md#cmp-키-합의-키-동기화-내부-api)).

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "PKI-ERROR",
  "status": 400,
  "detail": "validityPeriod only allows PnY, PnM, or PnD format.",
  "instance": "/api/v2/certificates/root",
  "errorCode": "COMMON_INVALID_INPUT",
  "timestamp": "2026-10-12T12:58:01.469217Z",
  "errors": [ { "field": "validityPeriod", "message": "validityPeriod only allows PnY, PnM, or PnD format." } ]
}
```

| 필드 | 출처 | 설명 |
|---|---|---|
| `title` | `ProblemDetails.TITLE` | 항상 `PKI-ERROR`. 에러 종류는 `errorCode` 로 구분한다 |
| `status` | `ErrorCode` | HTTP 상태. 응답 상태 라인과 항상 일치한다 |
| `detail` | `CoreException` / 검증 메시지 | 발생 건의 설명. 없으면 `ErrorCode` 의 제목. 검증 실패면 첫 필드 오류 메시지 |
| `instance` | Spring MVC | 요청 경로 |
| `errorCode` | `ErrorCode` | 클라이언트가 분기에 사용하는 코드. `<접두어>_<이름>` |
| `timestamp` | `ProblemDetails` | 발생 시각(ISO-8601) |
| `errors` | `ApiControllerAdvice` | 검증 실패일 때만. `[{field, message}]` |
| 그 외 | `CoreException.property()` | 확장 속성. 최상위에 그대로 노출된다 |

## 구조

에러 코드는 어휘이고 ProblemDetail 은 표현이다. 어휘(`support.error`)는 예외를 던지는 가장 안쪽 계층인 도메인 모델이 쓰므로 프레임워크를
모르고(R-01), 표현(`support.web`)은 Spring MVC 에 의존한다.

```
support.error  (프레임워크를 모른다. 도메인 모델이 던진다)
  ErrorCode (enum)         코드 문자열 + HTTP 상태(int) + 제목 + 로그 레벨
  CoreException            domain, usecase 어디서든 던질 수 있는 유일한 예외. errorCode + detail(선택) + properties
  LogLevel                 로깅 프레임워크에 의존하지 않기 위한 자체 enum

support.web    (Spring MVC 표현 계층)
  ProblemDetails           ErrorCode / CoreException → ProblemDetail 렌더링. fromStatus() 로 프레임워크 오류에 공통 코드 선택
  ErrorCodeLogger          에러 코드의 로그 레벨로 기록
  ApiControllerAdvice      모든 예외 → ProblemDetail. CoreException 은 코드대로, Spring MVC 표준 예외는 fromStatus() 코드로,
                           검증 실패는 errors[{field, message}] 추가, 나머지는 SYSTEM_ERROR
  JacksonConfig            x509-ra 와 같은 JSON 표현 (Instant 초 단위, 빠진 primitive 는 기본값)
```

핵심 규칙은 하나다. **어떤 HTTP 상태로 응답할지는 예외를 던지는 쪽이 아니라 에러 코드가 결정한다.**

## 에러 코드

코드 문자열은 x509-ra 의 규약 `<접두어>_<이름>` 을 따른다. x509-ra 의 도메인 에러를 옮길 때는 원본의 접두어와 이름을 그대로 써서 클라이언트가
같은 코드로 분기하게 한다.

| 접두어 | 영역 | 현재 코드 |
|---|---|---|
| `COMMON` | 특정 도메인에 속하지 않는 오류, Spring MVC 표준 오류, VO 검증 실패 | `INVALID_INPUT` 400, `RESOURCE_NOT_FOUND` 404, `METHOD_NOT_ALLOWED` 405, `UNSUPPORTED_MEDIA_TYPE` 415, `SYSTEM_ERROR` 500, `NOT_IMPLEMENTED` 501 |
| `CERT`, `PROFILE`, `STATS` ... | x509-ra 의 도메인 에러 | 구현할 때 옮긴다 |

x509-ra 는 에러 범주로 HTTP 상태를 정한다. 옮길 때 같은 상태로 정의한다.

| x509-ra `ErrorCategory` | HTTP 상태 |
|---|---|
| `BUSINESS_RULE_VIOLATION` | 400 |
| `NOT_FOUND` | 404 |
| `UNAUTHORIZED` | 403 |
| `INTERNAL_ERROR` | 500 |

`COMMON_NOT_IMPLEMENTED` 는 계약만 옮긴 엔드포인트가 쓴다. 엔드포인트를 구현하면 사라져야 한다.

## 신규 에러 코드 추가하기

1. **`ErrorCode` 에 항목 추가.** 접두어별 구분 주석 아래에 넣는다.

   ```java
   // ----- 인증서 (CERT) -----
   CERT_RESOURCE_NOT_FOUND("CERT_RESOURCE_NOT_FOUND", 404, "Resource not found.", LogLevel.INFO),
   CERT_INVALID_INPUT("CERT_INVALID_INPUT", 400, "Invalid input.", LogLevel.INFO),
   ```

   - **코드 문자열**: x509-ra 에 같은 에러가 있으면 그 코드를 그대로 쓴다. 한 번 배포된 코드는 바꾸지 않는다.
   - **HTTP 상태**: 위 범주 표를 따른다.
   - **제목**: 에러 *종류* 를 설명하는 문장. 특정 건의 값은 `detail` 이나 `property()` 로.
   - **로그 레벨**: 정상 흐름에서 생길 수 있는 클라이언트 오류는 `INFO`, 운영자가 알아야 하면 `WARN`, 조사가 필요하면 `ERROR`.
2. **`ErrorCodeTest` 실행.** 코드 문자열 중복과 상태 범위를 검사한다.
3. **문서 갱신.** 이 문서의 표와 `core/core-api/src/docs/asciidoc/index.adoc` 의 에러 코드 표에 한 줄 추가한다.

## 사용하기

**규칙 위반은 애그리거트가 던진다.** 여러 애그리거트에 걸친 규칙은 `*Policy` 가 던진다.

**없는 리소스는 도메인 서비스가 던진다.** 조회 결과가 없는 것은 도메인 규칙이 아니라 흐름의 문제라 `*Reader`/`*Writer` 가 던진다.

```java
throw new CoreException(ErrorCode.RESOURCE_NOT_FOUND).property("akiHex", akiHex);
```

**발생 건의 정보를 붙인다.** 구조화된 값은 `property()` 로, 사람이 읽을 설명은 `detail` 로. `property()` 로 넣은 값은 응답 JSON 최상위에
그대로 노출되므로 민감 정보(개인키, 비밀번호, PFX 암호)는 넣지 않는다. ProblemDetail 표준 필드 이름(`type`, `title`, `status`, `detail`,
`instance`)은 `CoreException.property()` 가 거부한다.

**값 검증 실패는 VO 가 던진다.** VO 는 외부 의존이 없어 `IllegalArgumentException` 을 던지고, 어드바이스가 `COMMON_INVALID_INPUT` 과
메시지로 응답한다.

**외부 연동 실패는 usecase 가 에러 코드로 바꾼다.** client 모듈은 core-api 의 에러 어휘를 모르므로 자기 예외로 감싸 던지고, 호출한 usecase 가
받아 에러 코드로 바꾼다. 원인 예외는 cause 로 남겨 로그에 기록하고 응답에는 노출하지 않는다.

**컨트롤러는 아무것도 하지 않는다.** `try/catch` 없이 usecase 나 도메인 서비스를 호출하고 DTO 를 반환한다. 예외는 `ApiControllerAdvice` 가 받는다.
단, v1 폐기(`/api/certificates/{serialNumber}/revocation`)는 x509-ra 계약상 실패를 `RevocationResponseV1` 본문으로 알리므로 그 엔드포인트만 예외를 응답 본문으로 바꾼다.

**클라이언트는 `status` 가 아니라 `errorCode` 로 분기한다.**
