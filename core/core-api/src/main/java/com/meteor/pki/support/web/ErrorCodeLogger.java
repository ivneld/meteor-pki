package com.meteor.pki.support.web;

import com.meteor.pki.support.error.ErrorCode;
import org.slf4j.Logger;

public final class ErrorCodeLogger {

    private ErrorCodeLogger() {
    }

    public static void log(Logger log, ErrorCode errorCode, String message, Throwable cause) {
        switch (errorCode.getLogLevel()) {
            case ERROR -> log.error(message, cause);
            case WARN -> log.warn(message, cause);
            case DEBUG -> log.debug(message, cause);
            default -> log.info(message, cause);
        }
    }

}
