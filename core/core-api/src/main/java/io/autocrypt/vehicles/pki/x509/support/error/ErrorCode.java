package io.autocrypt.vehicles.pki.x509.support.error;

/**
 * 서비스가 클라이언트에 돌려주는 모든 에러의 목록.
 *
 * <p>
 * 에러 하나는 <b>코드 문자열 + HTTP 상태 + 제목 + 로그 레벨</b>로 정의한다. 어떤 HTTP 상태로 응답할지는 예외를 던지는 쪽이 아니라 에러
 * 코드가 결정한다. 예외를 던지는 가장 안쪽 계층이 domain 이므로 어휘는 프레임워크를 모르는 이 패키지에 둔다(R-01). ProblemDetail 로
 * 바꾸는 일과 프레임워크 오류를 공통 코드에 대응시키는 일은 표현 계층(core-api 의 ProblemDetails)이 맡는다.
 *
 * <p>
 * 코드 문자열은 x509-ra 의 규약({@code <접두어>_<이름>})을 따른다. 응답의 {@code errorCode} 로 그대로 나간다. 접두어는
 * {@code COMMON} 공통이고, 도메인 에러가 생기면 x509-ra 와 같은 접두어({@code CERT}, {@code PROFILE},
 * {@code STATS} ...)를 쓴다.
 */
public enum ErrorCode {

    // ----- 공통 (COMMON) -----
    INVALID_INPUT("COMMON_INVALID_INPUT", 400, "Invalid input.", LogLevel.INFO),
    RESOURCE_NOT_FOUND("COMMON_RESOURCE_NOT_FOUND", 404, "Resource not found.", LogLevel.INFO),
    METHOD_NOT_ALLOWED("COMMON_METHOD_NOT_ALLOWED", 405, "Method not allowed.", LogLevel.INFO),
    UNSUPPORTED_MEDIA_TYPE("COMMON_UNSUPPORTED_MEDIA_TYPE", 415, "Unsupported media type.", LogLevel.INFO),
    SYSTEM_ERROR("COMMON_SYSTEM_ERROR", 500, "Unexpected error", LogLevel.ERROR),
    NOT_IMPLEMENTED("COMMON_NOT_IMPLEMENTED", 501, "Not implemented yet.", LogLevel.WARN);

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
