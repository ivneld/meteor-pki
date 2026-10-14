package com.meteor.pki.ui;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.meteor.pki.domain.Cert;
import com.meteor.pki.domain.CertReader;
import com.meteor.pki.domain.CertWriter;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.SerialNumber;
import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.CrlReason;
import com.meteor.pki.ui.form.RenewForm;
import com.meteor.pki.ui.form.RevokeForm;
import com.meteor.pki.ui.view.CertView;
import com.meteor.pki.usecase.CertIssuanceUseCase;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UiCertificateController {

    private final CertReader certReader;

    private final CertWriter certWriter;

    private final CertIssuanceUseCase certIssuanceUseCase;

    private final Clock clock;

    public UiCertificateController(CertReader certReader, CertWriter certWriter,
            CertIssuanceUseCase certIssuanceUseCase, Clock clock) {
        this.certReader = certReader;
        this.certWriter = certWriter;
        this.certIssuanceUseCase = certIssuanceUseCase;
        this.clock = clock;
    }

    @GetMapping("/ui/certificates")
    public String list(@RequestParam(required = false) CertType type, @RequestParam(required = false) CertStatus status,
            @RequestParam(required = false) String q, Model model) {
        Instant now = clock.instant();
        String keyword = q == null ? "" : q.trim().toLowerCase();
        List<CertView> certs = certReader.getAll()
            .stream()
            .map(cert -> CertView.of(cert, now))
            .filter(cert -> type == null || cert.certType() == type)
            .filter(cert -> status == null || cert.status() == status)
            .filter(cert -> keyword.isEmpty() || cert.subjectDn().toLowerCase().contains(keyword)
                    || cert.serialHex().toLowerCase().contains(keyword) || cert.skiHex().toLowerCase().contains(keyword)
                    || cert.trackingId().startsWith(keyword))
            .toList();
        model.addAttribute("certs", certs);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("types", CertType.values());
        model.addAttribute("statuses", CertStatus.values());
        return "certificates";
    }

    @GetMapping("/ui/certificates/{akiHex}/{serialHex}")
    public String detail(@PathVariable String akiHex, @PathVariable String serialHex, Model model) {
        Instant now = clock.instant();
        Cert cert = certReader.get(KeyIdentifier.of(akiHex), SerialNumber.of(serialHex));
        List<CertView> lineage = certReader.getLineage(cert.getTrackingId())
            .stream()
            .map(member -> CertView.of(member, now))
            .toList();
        model.addAttribute("cert", CertView.of(cert, now));
        model.addAttribute("lineage", lineage);
        model.addAttribute("reasons",
                Arrays.stream(CrlReason.values()).filter(reason -> reason != CrlReason.REMOVE_FROM_CRL).toList());
        return "certificate";
    }

    @PostMapping("/ui/certificates/{akiHex}/{serialHex}/renewal/root")
    public String renewRoot(@PathVariable String akiHex, @PathVariable String serialHex, @ModelAttribute RenewForm form,
            RedirectAttributes redirect) {
        return UiActions.attempt(redirect, null, detailPath(akiHex, serialHex), () -> renewed(redirect,
                certIssuanceUseCase.renewRoot(form.toRootCommand(akiHex, serialHex)), "Root 인증서를 새 키로 갱신했습니다."));
    }

    @PostMapping("/ui/certificates/{akiHex}/{serialHex}/renewal/sub-ca")
    public String renewSubCa(@PathVariable String akiHex, @PathVariable String serialHex,
            @ModelAttribute RenewForm form, RedirectAttributes redirect) {
        return UiActions.attempt(redirect, null, detailPath(akiHex, serialHex), () -> renewed(redirect,
                certIssuanceUseCase.renewSubCa(form.toSubCaCommand(akiHex, serialHex)), "Sub CA 인증서를 새 키로 갱신했습니다."));
    }

    @PostMapping("/ui/certificates/{akiHex}/{serialHex}/renewal/leaf")
    public String renewLeaf(@PathVariable String akiHex, @PathVariable String serialHex, @ModelAttribute RenewForm form,
            RedirectAttributes redirect) {
        return UiActions.attempt(redirect, null, detailPath(akiHex, serialHex), () -> renewed(redirect,
                certIssuanceUseCase.renewLeaf(form.toLeafCommand(akiHex, serialHex)), "Leaf 인증서를 갱신했습니다."));
    }

    @PostMapping("/ui/certificates/{akiHex}/{serialHex}/revocation")
    public String revoke(@PathVariable String akiHex, @PathVariable String serialHex, @ModelAttribute RevokeForm form,
            RedirectAttributes redirect) {
        return UiActions.attempt(redirect, null, detailPath(akiHex, serialHex), () -> {
            certWriter.revoke(KeyIdentifier.of(akiHex), SerialNumber.of(serialHex), CertType.SUB, form.crlReason());
            redirect.addFlashAttribute("success", "인증서를 폐기했습니다.");
            return "redirect:" + detailPath(akiHex, serialHex);
        });
    }

    private String renewed(RedirectAttributes redirect, Cert renewed, String message) {
        redirect.addFlashAttribute("success", message + " 같은 trackingId 로 이력에 추가되었습니다.");
        return "redirect:" + detailPath(renewed.getAuthorityKeyId().toHex(), renewed.getSerialNumber().toHex());
    }

    static String detailPath(String akiHex, String serialHex) {
        return "/ui/certificates/" + akiHex + "/" + serialHex;
    }

}
