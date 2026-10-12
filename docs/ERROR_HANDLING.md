# 응답 포맷과 에러 코드

성공 응답은 리소스 DTO 를 그대로 내려준다. 오류 응답은 [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
(`application/problem+json`) 형식이며, `code` 확장 속성으로 에러 코드를 함께 전달한다.

```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "type": "urn:meteor:error:O001",
  "title": "Order not found.",
  "status": 404,
  "instance": "/api/v1/orders/999",
  "code": "O001",
  "orderId": 999
}
```

| 필드 | 출처 | 설명 |
|---|---|---|
| `type` | `ProblemDetails` | 에러 코드 URI. `urn:meteor:error:{code}`. 문서 URL 이 생기면 `ProblemDetails.TYPE_PREFIX` 만 바꾼다 |
| `title` | `ErrorCode` | 에러 종류의 제목. 같은 코드면 항상 같다 |
| `status` | `ErrorCode` | HTTP 상태. 응답 상태 라인과 항상 일치한다 |
| `detail` | `CoreException` | 발생 건에 대한 부가 설명. 없으면 생략된다 |
| `instance` | Spring MVC | 요청 경로. 자동으로 채워진다 |
| `code` | `ErrorCode` | 클라이언트가 분기에 사용하는 에러 코드 문자열 |
| 그 외 | `CoreException.property()` | 확장 속성. 최상위에 그대로 노출된다 |

## 구조

에러 코드는 어휘이고 ProblemDetail 은 표현이다. 둘 다 core-api 모듈에 있지만 패키지가 다르다. 어휘(`support.error`)는 예외를 던지는
가장 안쪽 계층인 도메인 모델이 쓰므로 프레임워크를 모르고(R-01), 표현(`support.web`)은 Spring MVC 에 의존한다. 에러 어휘는 다른 모듈이
쓸 일이 없으므로 core-enum 에 두지 않는다. domain 을 core-domain 모듈로 추출할 때 함께 옮긴다.

```
support.error  (프레임워크를 모른다. 도메인 모델이 던진다)
  ErrorCode (enum)         코드 문자열 + HTTP 상태(int) + 제목 + 로그 레벨
  CoreException            domain, usecase 어디서든 던질 수 있는 유일한 예외. errorCode + detail(선택) + properties
  LogLevel                 로깅 프레임워크에 의존하지 않기 위한 자체 enum

support.web    (Spring MVC 표현 계층)
  ProblemDetails           ErrorCode / CoreException → ProblemDetail 렌더링. fromStatus() 로 프레임워크 오류에 공통 코드 선택
  ErrorCodeLogger          에러 코드의 로그 레벨로 기록
  ApiControllerAdvice      모든 예외 → ProblemDetail. CoreException 은 코드대로, Spring MVC 표준 예외는 fromStatus() 코드로,
                           검증 실패는 errors[{field, message}] 추가, 나머지는 INTERNAL_ERROR
```

핵심 규칙은 하나다. **어떤 HTTP 상태로 응답할지는 예외를 던지는 쪽이 아니라 에러 코드가 결정한다.** 애그리거트, 도메인 서비스, usecase 어디서
던져도 같은 코드는 같은 상태·같은 제목으로 나간다.

## 에러 코드 목록

코드 문자열은 접두어로 도메인을 구분한다. 접두어 뒤 숫자는 도메인 안에서 순번이다.

| 접두어 | 영역 | 예 |
|---|---|---|
| `C` | 공통. 특정 도메인에 속하지 않는 오류, Spring MVC 표준 오류, VO 검증 실패 | `C001` 400, `C002` 404, `C003` 405, `C004` 415, `C999` 500 |
| `M` | 회원 | `M001` 없음 (404), `M002` 이미 탈퇴 (409), `M003` 비활성 (409) |
| `O` | 주문 | `O001` 없음 (404), `O002` 결제 불가 상태 (409), `O003` 취소 불가 상태 (409), `O004` 결제 대기 주문 한도 초과 (409) |
| `P` | 결제 | `P001` 없음 (404), `P002` 승인 대기 상태 아님 (409), `P003` PG 미승인 (502) |

`C999` 는 처리되지 않은 모든 예외의 기본값이다. 공통 코드는 `ProblemDetails.fromStatus()` 가 참조하므로 삭제하거나 상태를 바꿀 때 함께 확인한다.

## 신규 에러 코드 추가하기

1. **도메인 접두어 정하기.** 기존 접두어가 있으면 다음 번호를, 새 도메인이면 새 접두어를 쓴다. 접두어를 추가하면 `ErrorCode` 의
   클래스 주석과 이 문서의 표에도 적는다.
2. **`ErrorCode` 에 항목 추가.** 도메인별 구분 주석 아래에 넣는다.

   ```java
   // ----- 주문 (O) -----
   ORDER_NOT_FOUND("O001", 404, "Order not found.", LogLevel.INFO),
   ORDER_ALREADY_PAID("O002", 409, "Order has already been paid.", LogLevel.WARN),
   ORDER_PAYMENT_FAILED("O003", 502, "Payment provider rejected the request.", LogLevel.ERROR);
   ```

   - **코드 문자열**: 한 번 배포된 코드는 바꾸지 않는다. 클라이언트가 분기에 사용한다.
   - **HTTP 상태**: 클라이언트가 고칠 수 있는 문제면 4xx, 서버 쪽 문제면 5xx.
   - **제목**: 에러 *종류* 를 설명하는 문장. 특정 건의 값은 `detail` 이나 `property()` 로.
   - **로그 레벨**: 정상 흐름에서 생길 수 있는 클라이언트 오류는 `INFO`, 운영자가 알아야 하면 `WARN`, 조사가 필요하면 `ERROR`.
3. **`ErrorCodeTest` 실행.** 코드 문자열 중복과 상태 범위를 검사한다.
4. **문서 갱신.** `core/core-api/src/docs/asciidoc/index.adoc` 의 에러 코드 표에 한 줄 추가한다. 응답 모양을 보여줄 필요가
   있으면 컨트롤러 REST Docs 테스트에 오류 케이스를 추가한다 (`OrderControllerTest.getNotFound()` 참고).

## 사용하기

**규칙 위반은 애그리거트가 던진다.** 상태 전이 규칙은 도메인에 있으므로 예외도 거기서 나온다. 여러 애그리거트에 걸친 규칙은
`*Policy` 가 던진다(`OrderLimitPolicy` → `O004`).

```java
// Order (domain)
public void cancel() {
    if (status != OrderStatus.CREATED) {
        throw new CoreException(ErrorCode.ORDER_NOT_CANCELLABLE).property("orderId", id).property("orderStatus", status);
    }
    status = OrderStatus.CANCELLED;
}
```

**없는 리소스는 도메인 서비스가 던진다.** 조회 결과가 없는 것은 도메인 규칙이 아니라 흐름의 문제라 애그리거트가 아니라 `*Reader`/`*Writer` 가 던진다.

```java
// OrderReader (domain)
return orderRepository.findById(orderId)
    .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", orderId));
```

**발생 건의 정보를 붙인다.** 구조화된 값은 `property()` 로, 사람이 읽을 설명은 `detail` 로.

```java
throw new CoreException(ErrorCode.ORDER_ALREADY_PAID, "paidAt=" + order.getPaidAt());
```

`property()` 로 넣은 값은 응답 JSON 최상위에 그대로 노출된다. 민감 정보나 내부 식별자는 넣지 않는다. ProblemDetail 표준 필드 이름
(`type`, `title`, `status`, `detail`, `instance`)은 `CoreException.property()` 가 거부한다. 주문 상태를 싣고 싶으면 `status` 가 아니라
`orderStatus` 처럼 쓴다.

**값 검증 실패는 VO 가 던진다.** VO(domain 의 record)는 외부 의존이 없어 `IllegalArgumentException` 을 던지고, 어드바이스가 `C001` 과
메시지로 응답한다. 요청 DTO 가 Command 나 도메인 서비스 인자로 바뀌는 시점에 `Email.of(...)` 가 실패하면 usecase·도메인 서비스에 들어가기 전에 400 이 나간다.

**외부 연동 실패는 usecase 가 에러 코드로 바꾼다.** client 모듈은 core-api 의 에러 어휘를 모르므로 Feign 예외를 자기 예외(`PgApprovalException`)로
감싸 던지고, 호출한 usecase 가 받아 에러 코드로 바꾼다(`PaymentUseCase` → `P003`). 원인 예외는 cause 로 남겨 로그에 기록하고 응답에는 노출하지 않는다.

**컨트롤러는 아무것도 하지 않는다.** `try/catch` 없이 usecase 나 도메인 서비스를 호출하고 DTO 를 반환한다. 예외는 `ApiControllerAdvice` 가 받는다.

**비동기 메서드도 같다.** `@Async` 안에서 던진 `CoreException` 은 `AsyncExceptionHandler` 가 같은 로그 레벨로 기록한다.

**클라이언트는 `status` 가 아니라 `code` 로 분기한다.** 같은 404 라도 `C002`(경로 없음)와 `O001`(주문 없음)은 다른 상황이다.
