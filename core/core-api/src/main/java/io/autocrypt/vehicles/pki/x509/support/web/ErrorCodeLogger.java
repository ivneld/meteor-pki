package io.autocrypt.vehicles.pki.x509.support.web;

import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import org.slf4j.Logger;

/**
 * 에러 코드에 정해진 로그 레벨로 기록한다.
 */
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
