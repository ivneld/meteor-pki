package com.meteor.pki.ui;

import java.util.Arrays;

import com.meteor.pki.domain.CertProfile;
import com.meteor.pki.domain.CertProfileReader;
import com.meteor.pki.domain.EkuReader;
import com.meteor.pki.domain.PageQuery;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyAlgorithmSpec;
import com.meteor.pki.enums.KeyUsage;
import com.meteor.pki.ui.form.ProfileForm;
import com.meteor.pki.usecase.CertProfileUseCase;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class UiProfileController {

    private final CertProfileUseCase certProfileUseCase;

    private final CertProfileReader certProfileReader;

    private final EkuReader ekuReader;

    public UiProfileController(CertProfileUseCase certProfileUseCase, CertProfileReader certProfileReader,
            EkuReader ekuReader) {
        this.certProfileUseCase = certProfileUseCase;
        this.certProfileReader = certProfileReader;
        this.ekuReader = ekuReader;
    }

    @GetMapping("/ui/profiles")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", certProfileReader.getPage(new PageQuery(Math.max(page, 0), 20)));
        return "profiles";
    }

    @GetMapping("/ui/profiles/new")
    public String newForm(@RequestParam(required = false) CertType type, Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", ProfileForm.empty(type == null ? CertType.LEAF : type));
        }
        model.addAttribute("ekus", ekuReader.findAll());
        model.addAttribute("types", CertType.values());
        model.addAttribute("keyUsages", KeyUsage.values());
        model.addAttribute("keyAlgorithms",
                Arrays.stream(KeyAlgorithmSpec.values()).map(KeyAlgorithmSpec::getValue).toList());
        return "profile-new";
    }

    @PostMapping("/ui/profiles")
    public String create(@ModelAttribute ProfileForm form, RedirectAttributes redirect) {
        return UiActions.attempt(redirect, form, "/ui/profiles/new", () -> {
            CertProfile profile = certProfileUseCase.create(form.toCommand());
            redirect.addFlashAttribute("success", "프로파일 '" + profile.getName() + "' 을 만들었습니다.");
            return "redirect:" + detailPath(profile.getName());
        });
    }

    @GetMapping("/ui/profiles/detail")
    public String detail(@RequestParam String name, Model model) {
        model.addAttribute("profile", certProfileReader.getByName(name));
        return "profile";
    }

    private static String detailPath(String name) {
        return UriComponentsBuilder.fromPath("/ui/profiles/detail").queryParam("name", name).encode().toUriString();
    }

}
