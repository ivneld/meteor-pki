package com.meteor.pki.ui;

import java.util.function.Supplier;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.ui.view.UiError;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

final class UiActions {

    private UiActions() {
    }

    static String attempt(RedirectAttributes redirect, Object form, String failurePath, Supplier<String> action) {
        try {
            return action.get();
        }
        catch (CoreException | IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", UiError.of(ex));
            if (form != null) {
                redirect.addFlashAttribute("form", form);
            }
            return "redirect:" + failurePath;
        }
    }

}
