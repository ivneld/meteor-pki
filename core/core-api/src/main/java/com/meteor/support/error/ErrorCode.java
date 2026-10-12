package com.meteor.support.error;

/**
 * 서비스가 클라이언트에 돌려주는 모든 에러의 목록.
 *
 * <p>
 * 에러 하나는 <b>코드 문자열 + HTTP 상태 + 제목 + 로그 레벨</b>로 정의한다. 어떤 HTTP 상태로 응답할지는 예외를 던지는 쪽이 아니라 에러
 * 코드가 결정한다. 예외를 던지는 가장 안쪽 계층이 domain 이므로 어휘는 프레임워크를 모르는 이 패키지에 둔다(R-01). ProblemDetail 로
 * 바꾸는 일과 프레임워크 오류를 공통 코드에 대응시키는 일은 표현 계층(core-api 의 ProblemDetails)이 맡는다.
 *
 * <p>
 * 코드 문자열은 접두어로 도메인을 구분한다. {@code C} 공통, {@code M} 회원, {@code O} 주문, {@code P} 결제.
 */
public enum ErrorCode {

    // ----- 공통 (C) -----
    INVALID_REQUEST("C001", 400, "Invalid request.", LogLevel.INFO),
    NOT_FOUND("C002", 404, "Resource not found.", LogLevel.INFO),
    METHOD_NOT_ALLOWED("C003", 405, "Method not allowed.", LogLevel.INFO),
    UNSUPPORTED_MEDIA_TYPE("C004", 415, "Unsupported media type.", LogLevel.INFO),
    INTERNAL_ERROR("C999", 500, "An unexpected error has occurred.", LogLevel.ERROR),

    // ----- 회원 (M) -----
    MEMBER_NOT_FOUND("M001", 404, "Member not found.", LogLevel.INFO),
    MEMBER_ALREADY_WITHDRAWN("M002", 409, "Member has already withdrawn.", LogLevel.INFO),
    MEMBER_NOT_ACTIVE("M003", 409, "Member is not active.", LogLevel.INFO),

    // ----- 주문 (O) -----
    ORDER_NOT_FOUND("O001", 404, "Order not found.", LogLevel.INFO),
    ORDER_NOT_PAYABLE("O002", 409, "Order is not in a payable state.", LogLevel.INFO),
    ORDER_NOT_CANCELLABLE("O003", 409, "Order can no longer be cancelled.", LogLevel.INFO),
    ORDER_LIMIT_EXCEEDED("O004", 409, "Too many orders are awaiting payment.", LogLevel.INFO),

    // ----- 결제 (P) -----
    PAYMENT_NOT_FOUND("P001", 404, "Payment not found.", LogLevel.INFO),
    PAYMENT_NOT_REQUESTED("P002", 409, "Payment is not awaiting approval.", LogLevel.INFO),
    PAYMENT_FAILED("P003", 502, "Payment gateway did not approve the payment.", LogLevel.WARN);

    private final String code;

    private final int status;

    private final String title;

    private final LogLevel logLevel;

    ErrorCode(String code, int status, String title, LogLevel logLevel) {
        this.code = code;
        this.status = status;
        this.title = title;
        this.logLevel = logLevel;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

}
