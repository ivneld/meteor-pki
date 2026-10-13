package com.meteor.pki.support.web;

import java.time.Instant;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

import org.springframework.http.ProblemDetail;

public final class ProblemDetails {

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
