package com.meteor.pki.ui;

import java.util.Map;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.ui.view.UiError;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(basePackages = "com.meteor.pki.ui")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UiControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(UiControllerAdvice.class);

    @ExceptionHandler(CoreException.class)
    public String handleCoreException(CoreException ex, Model model, HttpServletResponse response) {
        response.setStatus(ex.getErrorCode().getStatus());
        model.addAttribute("error", UiError.of(ex));
        return "error-page";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        model.addAttribute("error", UiError.of(ex));
        return "error-page";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception ex, Model model) {
        log.error("UI request failed", ex);
        model.addAttribute("error", new UiError("요청을 처리하지 못했습니다: " + ex.getMessage(), "SYSTEM_ERROR", Map.of()));
        return "error-page";
    }

}
