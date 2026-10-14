package com.meteor.pki.ui;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.meteor.pki.domain.CertReader;
import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.ui.view.CaNode;
import com.meteor.pki.ui.view.CertView;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiDashboardController {

    private final CertReader certReader;

    private final Clock clock;

    public UiDashboardController(CertReader certReader, Clock clock) {
        this.certReader = certReader;
        this.clock = clock;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/ui";
    }

    @GetMapping("/ui")
    public String dashboard(Model model) {
        Instant now = clock.instant();
        List<CertView> all = certReader.getAll().stream().map(cert -> CertView.of(cert, now)).toList();
        List<CertView> cas = all.stream().filter(CertView::ca).toList();
        List<CertView> leaves = all.stream().filter(cert -> cert.certType() == CertType.LEAF).toList();
        model.addAttribute("forest", CaNode.forest(cas, leaves));
        model.addAttribute("recent", all.stream().limit(8).toList());
        model.addAttribute("expiring", all.stream().filter(CertView::expiringSoon).toList());
        model.addAttribute("stats",
                Map.of("ca", (long) cas.size(), "leaf", (long) leaves.size(), "valid", count(all, CertStatus.VALID),
                        "pending", count(all, CertStatus.PENDING), "revoked", count(all, CertStatus.REVOKED), "expired",
                        count(all, CertStatus.EXPIRED)));
        return "dashboard";
    }

    private static long count(List<CertView> certs, CertStatus status) {
        return certs.stream().filter(cert -> cert.status() == status).count();
    }

}
