package com.meteor.pki.ui;

import java.security.KeyPair;
import java.util.List;

import com.meteor.pki.ContextTest;
import com.meteor.pki.domain.Cert;
import com.meteor.pki.domain.CertReader;
import com.meteor.pki.domain.TestKeys;
import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.CrlReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UiIT extends ContextTest {

    private final CertReader certReader;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    UiIT(CertReader certReader) {
        this.certReader = certReader;
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void pagesRender() throws Exception {
        mockMvc.perform(get("/")).andExpect(redirectedUrl("/ui"));
        for (String path : List.of("/ui", "/ui/certificates", "/ui/profiles", "/ui/profiles/new?type=SUB", "/ui/ekus",
                "/ui/issue/root", "/ui/issue/sub-ca", "/ui/issue/leaf")) {
            mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Meteor PKI")));
        }
    }

    @Test
    void walksTheCertificateLifecycleThroughTheUi() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        createProfile("ui-root-" + suffix, "ROOT", "P10Y");
        createProfile("ui-sub-" + suffix, "SUB", "P5Y");
        createProfile("ui-leaf-" + suffix, "LEAF", "P1Y");
        mockMvc.perform(get("/ui/profiles/detail").param("name", "ui-root-" + suffix))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("이 프로파일로 Root 발급")));

        Cert root = issue("/ui/issue/root", "certProfileId", profileId("ui-root-" + suffix), "subjectDn",
                "CN=UI Root " + suffix);
        Cert sub = issue("/ui/issue/sub-ca", "certProfileId", profileId("ui-sub-" + suffix), "subjectDn",
                "CN=UI Sub " + suffix, "akiHex", root.getSubjectKeyId().toHex(), "includeDefaultCrlDp", "true",
                "validityPeriod", "P2Y");
        KeyPair device = TestKeys.ecP256();
        Cert leaf = issue("/ui/issue/leaf", "mode", "csr", "certProfileId", profileId("ui-leaf-" + suffix), "akiHex",
                sub.getSubjectKeyId().toHex(), "csrPem",
                TestKeys.csrPem("CN=ui.example.com", device, "ui.example.com"));
        assertThat(leaf.getSubjectDn().value()).isEqualTo("CN=ui.example.com");

        mockMvc.perform(get(detail(sub)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("생명주기 이력")))
            .andExpect(content().string(containsString(sub.getTrackingId())));
        mockMvc.perform(get("/ui")).andExpect(content().string(containsString("UI Sub " + suffix)));

        mockMvc.perform(post(detail(leaf) + "/renewal/leaf").param("activationStartDaysAgo", "0"))
            .andExpect(flash().attributeExists("success"));
        assertThat(certReader.getLineage(leaf.getTrackingId())).hasSize(2);

        MvcResult renewed = mockMvc
            .perform(post(detail(sub) + "/renewal/sub-ca").param("activationStartDaysAgo", "0")
                .param("validityPeriod", "P1Y")
                .param("revokeOriginalCertificate", "true"))
            .andExpect(flash().attributeExists("success"))
            .andReturn();
        String renewedUrl = renewed.getResponse().getRedirectedUrl();
        assertThat(renewedUrl).isNotEqualTo(detail(sub));
        assertThat(certReader.getLineage(sub.getTrackingId())).hasSize(2);
        assertThat(certReader.get(root.getSubjectKeyId(), sub.getSerialNumber()).getStatus())
            .isEqualTo(CertStatus.REVOKED);
        mockMvc.perform(get(renewedUrl))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("최초 발급")));

        mockMvc.perform(post(detail(root) + "/renewal/root").param("activationStartDaysAgo", "0"))
            .andExpect(flash().attributeExists("success"));
        assertThat(certReader.getLineage(root.getTrackingId())).hasSize(2)
            .allMatch(cert -> cert.getCertType() == CertType.ROOT);

        Cert renewedSub = certReader.getLineage(sub.getTrackingId()).get(1);
        mockMvc.perform(post(detail(renewedSub) + "/revocation").param("reason", "5"))
            .andExpect(redirectedUrl(detail(renewedSub)))
            .andExpect(flash().attributeExists("success"));
        assertThat(
                certReader.get(renewedSub.getAuthorityKeyId(), renewedSub.getSerialNumber()).getRevocation().reason())
            .isEqualTo(CrlReason.CESSATION_OF_OPERATION);
    }

    @Test
    void failuresAreShownWithTheEnteredValues() throws Exception {
        mockMvc
            .perform(post("/ui/issue/sub-ca").param("certProfileId", "999999")
                .param("subjectDn", "CN=Kept")
                .param("akiHex", "00FF"))
            .andExpect(redirectedUrl("/ui/issue/sub-ca"))
            .andExpect(flash().attributeExists("error", "form"));
        mockMvc.perform(post("/ui/profiles").param("name", "").param("certType", "LEAF"))
            .andExpect(redirectedUrl("/ui/profiles/new"))
            .andExpect(flash().attributeExists("error"));
        mockMvc.perform(get("/ui/certificates/00FF/01"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Certificate not found.")));
    }

    private void createProfile(String name, String type, String validity) throws Exception {
        var request = post("/ui/profiles").param("name", name)
            .param("certType", type)
            .param("validityPeriod", validity)
            .param("keyAlgorithmSpec", "EC-P-256");
        if (type.equals("LEAF")) {
            request.param("keyUsages", "DIGITAL_SIGNATURE").param("ekuOids", "1.3.6.1.5.5.7.3.1");
        }
        mockMvc.perform(request).andExpect(flash().attributeExists("success"));
    }

    private String profileId(String name) throws Exception {
        String body = mockMvc.perform(get("/ui/profiles/detail").param("name", name))
            .andReturn()
            .getResponse()
            .getContentAsString();
        int index = body.indexOf("certProfileId=");
        return body.substring(index + "certProfileId=".length()).split("\"")[0];
    }

    private Cert issue(String path, String... params) throws Exception {
        var request = post(path);
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        MvcResult result = mockMvc.perform(request).andExpect(flash().attributeExists("success")).andReturn();
        String[] parts = result.getResponse().getRedirectedUrl().split("/");
        return certReader.get(com.meteor.pki.domain.KeyIdentifier.of(parts[3]),
                com.meteor.pki.domain.SerialNumber.of(parts[4]));
    }

    private static String detail(Cert cert) {
        return "/ui/certificates/" + cert.getAuthorityKeyId().toHex() + "/" + cert.getSerialNumber().toHex();
    }

}
