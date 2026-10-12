package io.autocrypt.vehicles.pki.x509.support.web;

import java.time.Instant;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.http.ProblemDetail;

/**
 * {@link ErrorCode} 를 RFC 9457 ProblemDetail 로 렌더링한다. 응답 모양은 x509-ra 의 오류 응답과 같다.
 *
 * <pre>
 * { "type": "about:blank", "title": "PKI-ERROR", "status": 400, "detail": "...", "instance": "/api/...",
 *   "errorCode": "COMMON_INVALID_INPUT", "timestamp": "2026-01-01T00:00:00Z", "errors": [ {"field", "message"} ] }
 * </pre>
 */
public final class ProblemDetails {

    /** x509-ra 와 같은 고정 제목. 에러 종류는 {@code errorCode} 로 구분한다. */
    public static final String TITLE = "PKI-ERROR";

    private ProblemDetails() {
    }

    public static ProblemDetail of(ErrorCode errorCode, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(errorCode.getStatus());
        problemDetail.setDetail(detail);
        return apply(errorCode, problemDetail);
    }

    public static ProblemDetail of(CoreException exception) {
        ProblemDetail problemDetail = of(exception.getErrorCode(), exception.getDetail());
        exception.getProperties().forEach(problemDetail::setProperty);
        return problemDetail;
    }

    public static ProblemDetail apply(ErrorCode errorCode, ProblemDetail problemDetail) {
        problemDetail.setTitle(TITLE);
        if (problemDetail.getDetail() == null || problemDetail.getDetail().isBlank()) {
            problemDetail.setDetail(errorCode.getTitle());
        }
        problemDetail.setProperty("errorCode", errorCode.getCode());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    /**
     * Spring MVC 가 직접 만들어 내는 표준 오류(404, 405, 415 ...)에 대응하는 공통 코드를 고른다.
     */
    public static ErrorCode fromStatus(int status) {
        if (status == ErrorCode.RESOURCE_NOT_FOUND.getStatus()) {
            return ErrorCode.RESOURCE_NOT_FOUND;
        }
        if (status == ErrorCode.METHOD_NOT_ALLOWED.getStatus()) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (status == ErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus()) {
            return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        }
        if (status >= 400 && status < 500) {
            return ErrorCode.INVALID_INPUT;
        }
        return ErrorCode.SYSTEM_ERROR;
    }

    public static boolean hasCode(ProblemDetail problemDetail) {
        return problemDetail.getProperties() != null && problemDetail.getProperties().containsKey("errorCode");
    }

}
