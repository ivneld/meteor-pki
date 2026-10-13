package com.meteor.pki.support.async;

import java.lang.reflect.Method;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import com.meteor.pki.support.web.ErrorCodeLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void handleUncaughtException(Throwable e, Method method, Object... params) {
        ErrorCode errorCode = (e instanceof CoreException coreException) ? coreException.getErrorCode()
                : ErrorCode.SYSTEM_ERROR;
        ErrorCodeLogger.log(log, errorCode, "Async failed [" + errorCode.getCode() + "] method=" + method.getName(), e);
    }

}
