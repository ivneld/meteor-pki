package com.meteor.pki.support.web;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsTest {

    @Test
    void rendersX509RaErrorShape() {
        ProblemDetail problemDetail = ProblemDetails.of(ErrorCode.RESOURCE_NOT_FOUND, "akiHex=0A");

        assertThat(problemDetail.getStatus()).isEqualTo(404);
        assertThat(problemDetail.getTitle()).isEqualTo("PKI-ERROR");
        assertThat(problemDetail.getDetail()).isEqualTo("akiHex=0A");
        assertThat(problemDetail.getProperties()).containsEntry("errorCode", "COMMON_RESOURCE_NOT_FOUND")
            .containsKey("timestamp");
    }

    @Test
    void detailFallsBackToErrorCodeTitle() {
        assertThat(ProblemDetails.of(ErrorCode.NOT_IMPLEMENTED, null).getDetail()).isEqualTo("Not implemented yet.");
    }

    @Test
    void fromStatusMapsFrameworkStatusesToCommonCodes() {
        assertThat(ProblemDetails.fromStatus(404)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(ProblemDetails.fromStatus(405)).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED);
        assertThat(ProblemDetails.fromStatus(415)).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        assertThat(ProblemDetails.fromStatus(409)).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(ProblemDetails.fromStatus(503)).isEqualTo(ErrorCode.SYSTEM_ERROR);
    }

    @Test
    void coreExceptionPropertiesBecomeExtensions() {
        CoreException exception = new CoreException(ErrorCode.RESOURCE_NOT_FOUND).property("akiHex", "0A");

        ProblemDetail problemDetail = ProblemDetails.of(exception);

        assertThat(problemDetail.getDetail()).isEqualTo("Resource not found.");
        assertThat(problemDetail.getProperties()).containsEntry("errorCode", "COMMON_RESOURCE_NOT_FOUND")
            .containsEntry("akiHex", "0A");
    }

}
