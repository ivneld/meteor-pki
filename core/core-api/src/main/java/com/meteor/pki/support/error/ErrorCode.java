package com.meteor.pki.support.error;

public enum ErrorCode {

    INVALID_INPUT("COMMON_INVALID_INPUT", 400, "Invalid input.", LogLevel.INFO),
    RESOURCE_NOT_FOUND("COMMON_RESOURCE_NOT_FOUND", 404, "Resource not found.", LogLevel.INFO),
    METHOD_NOT_ALLOWED("COMMON_METHOD_NOT_ALLOWED", 405, "Method not allowed.", LogLevel.INFO),
    UNSUPPORTED_MEDIA_TYPE("COMMON_UNSUPPORTED_MEDIA_TYPE", 415, "Unsupported media type.", LogLevel.INFO),
    CONFLICT("COMMON_CONFLICT", 409, "The resource was changed by another request. Try again.", LogLevel.WARN),
    SYSTEM_ERROR("COMMON_SYSTEM_ERROR", 500, "Unexpected error", LogLevel.ERROR),
    NOT_IMPLEMENTED("COMMON_NOT_IMPLEMENTED", 501, "Not implemented yet.", LogLevel.WARN),

    CERT_INVALID_INPUT("CERT_INVALID_INPUT", 400, "Invalid input.", LogLevel.INFO),
    CERT_RESOURCE_NOT_FOUND("CERT_RESOURCE_NOT_FOUND", 404, "Resource not found.", LogLevel.INFO),
    CERT_OPERATION_FAILED("CERT_OPERATION_FAILED", 400, "Certificate operation failed.", LogLevel.ERROR),
    CERT_SIGN_FAILED("CERT_SIGN_FAILED", 500, "Certificate signing failed.", LogLevel.ERROR),
    CERT_REVOKE_ROOT_FORBIDDEN("CERT_REVOKE_ROOT_FORBIDDEN", 400, "Root certificates cannot be revoked.",
            LogLevel.INFO),

    PROFILE_INVALID_INPUT("PROFILE_INVALID_INPUT", 400, "Invalid certificate profile input.", LogLevel.INFO),
    PROFILE_RESOURCE_NOT_FOUND("PROFILE_RESOURCE_NOT_FOUND", 404, "Certificate profile not found.", LogLevel.INFO);

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
