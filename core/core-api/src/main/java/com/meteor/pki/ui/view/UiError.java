package com.meteor.pki.ui.view;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.meteor.pki.support.error.CoreException;

public record UiError(String message, String code, Map<String, String> details) implements Serializable {

    public static UiError of(RuntimeException ex) {
        if (ex instanceof CoreException core) {
            Map<String, String> details = new LinkedHashMap<>();
            core.getProperties().forEach((key, value) -> details.put(key, String.valueOf(value)));
            return new UiError(core.getMessage(), core.getErrorCode().getCode(), details);
        }
        return new UiError(ex.getMessage(), "INVALID_INPUT", Map.of());
    }

}
