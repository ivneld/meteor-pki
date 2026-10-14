package com.meteor.pki.ui;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;

import com.meteor.pki.domain.Cert;
import com.meteor.pki.domain.CertProfile;
import com.meteor.pki.domain.CertProfileReader;
import com.meteor.pki.domain.CertReader;
import com.meteor.pki.domain.PageQuery;
import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.ui.form.LeafIssueForm;
import com.meteor.pki.ui.form.RootIssueForm;
import com.meteor.pki.ui.form.SubCaIssueForm;
import com.meteor.pki.ui.view.CertView;
import com.meteor.pki.usecase.CertIssuanceUseCase;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UiIssuanceController {

    private static final PageQuery ALL_PROFILES = new PageQuery(0, 1000);

    private final CertIssuanceUseCase certIssuanceUseCase;

    private final CertProfileReader certProfileReader;

    private final CertReader certReader;

    private final Clock clock;

    public UiIssuanceController(CertIssuanceUseCase certIssuanceUseCase, CertProfileReader certProfileReader,
            CertReader certReader, Clock clock) {
        this.certIssuanceUseCase = certIssuanceUseCase;
        this.certProfileReader = certProfileReader;
        this.certReader = certReader;
        this.clock = clock;
    }

    @GetMapping("/ui/issue/root")
    public String rootForm(@RequestParam(required = false) Long certProfileId, Model model) {
        if (!model.containsAttribute("form")) {
            RootIssueForm empty = RootIssueForm.empty();
            model.addAttribute("form", new RootIssueForm(certProfileId, empty.subjectDn(), empty.validityPeriod(),
                    empty.crlDps(), empty.ocspUris(), empty.notBefore()));
        }
        model.addAttribute("profiles", profiles(CertType.ROOT));
        return "issue-root";
    }

    @PostMapping("/ui/issue/root")
    public String issueRoot(@ModelAttribute RootIssueForm form, RedirectAttributes redirect) {
        return UiActions.attempt(redirect, form, "/ui/issue/root",
                () -> issued(redirect, certIssuanceUseCase.issueRoot(form.toCommand()), "Root CA 를 발급했습니다."));
    }

    @GetMapping("/ui/issue/sub-ca")
    public String subCaForm(@RequestParam(required = false) String akiHex,
            @RequestParam(required = false) Long certProfileId, Model model) {
        if (!model.containsAttribute("form")) {
            SubCaIssueForm empty = SubCaIssueForm.empty(akiHex);
            model.addAttribute("form",
                    new SubCaIssueForm(certProfileId, empty.subjectDn(), akiHex, empty.crlDps(),
                            empty.includeDefaultCrlDp(), empty.aiaCaIssuers(), empty.ocspUris(), empty.validityPeriod(),
                            empty.notBefore()));
        }
        model.addAttribute("profiles", profiles(CertType.SUB));
        model.addAttribute("issuers", issuers(CertView::canIssueCa));
        return "issue-sub-ca";
    }

    @PostMapping("/ui/issue/sub-ca")
    public String issueSubCa(@ModelAttribute SubCaIssueForm form, RedirectAttributes redirect) {
        return UiActions.attempt(redirect, form, "/ui/issue/sub-ca",
                () -> issued(redirect, certIssuanceUseCase.issueSubCa(form.toCommand()), "Sub CA 를 발급했습니다."));
    }

    @GetMapping("/ui/issue/leaf")
    public String leafForm(@RequestParam(required = false) String akiHex,
            @RequestParam(required = false) Long certProfileId, Model model) {
        if (!model.containsAttribute("form")) {
            LeafIssueForm empty = LeafIssueForm.empty(akiHex);
            model.addAttribute("form",
                    new LeafIssueForm(empty.mode(), certProfileId, akiHex, empty.subjectDn(), empty.publicKeyPem(),
                            empty.csrPem(), empty.crlDps(), empty.includeDefaultCrlDp(), empty.aiaCaIssuers(),
                            empty.ocspUris(), empty.validityPeriod(), empty.notBefore()));
        }
        model.addAttribute("profiles", profiles(CertType.LEAF));
        model.addAttribute("issuers", issuers(CertView::canIssueLeaf));
        return "issue-leaf";
    }

    @PostMapping("/ui/issue/leaf")
    public String issueLeaf(@ModelAttribute LeafIssueForm form, RedirectAttributes redirect) {
        return UiActions.attempt(redirect, form, "/ui/issue/leaf", () -> {
            Cert leaf = form.usesCsr() ? certIssuanceUseCase.issueLeafFromP10cr(form.toCsrCommand())
                    : certIssuanceUseCase.issueLeaf(form.toKeyCommand());
            return issued(redirect, leaf, leaf.getStatus() == CertStatus.PENDING
                    ? "Leaf 인증서를 발급했습니다. 승인이 필요한 프로파일이라 PENDING 으로 저장되었습니다." : "Leaf 인증서를 발급했습니다.");
        });
    }

    private List<CertProfile> profiles(CertType type) {
        return certProfileReader.getPage(ALL_PROFILES)
            .profiles()
            .stream()
            .filter(profile -> profile.getCertType() == type)
            .toList();
    }

    private List<CertView> issuers(Predicate<CertView> canIssue) {
        Instant now = clock.instant();
        return certReader.getLatestCasPerChain().stream().map(ca -> CertView.of(ca, now)).filter(canIssue).toList();
    }

    private static String issued(RedirectAttributes redirect, Cert cert, String message) {
        redirect.addFlashAttribute("success", message);
        return "redirect:"
                + UiCertificateController.detailPath(cert.getAuthorityKeyId().toHex(), cert.getSerialNumber().toHex());
    }

}
