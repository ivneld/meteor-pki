package com.meteor.pki.ui;

import com.meteor.pki.domain.EkuReader;
import com.meteor.pki.domain.EkuWriter;
import com.meteor.pki.ui.form.EkuForm;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UiEkuController {

    private final EkuReader ekuReader;

    private final EkuWriter ekuWriter;

    public UiEkuController(EkuReader ekuReader, EkuWriter ekuWriter) {
        this.ekuReader = ekuReader;
        this.ekuWriter = ekuWriter;
    }

    @GetMapping("/ui/ekus")
    public String list(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", EkuForm.empty());
        }
        model.addAttribute("ekus", ekuReader.findAll());
        return "ekus";
    }

    @PostMapping("/ui/ekus")
    public String register(@ModelAttribute EkuForm form, RedirectAttributes redirect) {
        return UiActions.attempt(redirect, form, "/ui/ekus", () -> {
            ekuWriter.register(form.trimmedOid(), form.trimmedPurpose());
            redirect.addFlashAttribute("success", "EKU " + form.trimmedOid() + " 를 등록했습니다.");
            return "redirect:/ui/ekus";
        });
    }

}
