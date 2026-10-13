package com.meteor.pki.support.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiControllerAdvice extends ResponseEntityExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ProblemDetail> handleCoreException(CoreException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorCodeLogger.log(log, errorCode, "CoreException [" + errorCode.getCode() + "] " + e.getMessage(), e);
        return ResponseEntity.status(errorCode.getStatus()).body(ProblemDetails.of(e));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        ErrorCodeLogger.log(log, errorCode, "Invalid value: " + e.getMessage(), null);
        return ResponseEntity.status(errorCode.getStatus()).body(ProblemDetails.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception e) {
        ErrorCode errorCode = ErrorCode.SYSTEM_ERROR;
        ErrorCodeLogger.log(log, errorCode, "Unhandled exception: " + e.getMessage(), e);
        return ResponseEntity.status(errorCode.getStatus()).body(ProblemDetails.of(errorCode, null));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        String detail = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .filter(message -> message != null)
            .findFirst()
            .orElse("Invalid request");
        ProblemDetail problemDetail = ProblemDetails.of(errorCode, detail);
        problemDetail.setProperty("errors", toFieldErrors(ex.getBindingResult().getFieldErrors()));
        return handleExceptionInternal(ex, problemDetail, headers, HttpStatusCode.valueOf(errorCode.getStatus()),
                request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ErrorCode errorCode = ProblemDetails.fromStatus(statusCode.value());
        ErrorCodeLogger.log(log, errorCode, "Spring MVC exception: " + ex.getMessage(), null);
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
            WebRequest request) {
        if (body instanceof ProblemDetail problemDetail && !ProblemDetails.hasCode(problemDetail)) {
            ProblemDetails.apply(ProblemDetails.fromStatus(statusCode.value()), problemDetail);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private List<Map<String, Object>> toFieldErrors(List<FieldError> fieldErrors) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (FieldError fieldError : fieldErrors) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("field", fieldError.getField());
            error.put("message", fieldError.getDefaultMessage());
            errors.add(error);
        }
        return errors;
    }

}
