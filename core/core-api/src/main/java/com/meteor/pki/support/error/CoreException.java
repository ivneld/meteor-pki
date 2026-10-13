package com.meteor.pki.support.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.meteor.pki.support.error.ErrorCode;

public class CoreException extends RuntimeException {

    private final ErrorCode errorCode;

    private final String detail;

    private static final Set<String> RESERVED = Set.of("type", "title", "status", "detail", "instance");

    private final Map<String, Object> properties = new LinkedHashMap<>();

    public CoreException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public CoreException(ErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getTitle());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public CoreException property(String name, Object value) {
        if (RESERVED.contains(name)) {
            throw new IllegalArgumentException("reserved problem detail field: " + name);
        }
        this.properties.put(name, value);
        return this;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

}
