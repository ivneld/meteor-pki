package com.meteor.pki.api;

import java.util.Base64;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaApiIT extends ApiIT {

    @Test
    void buildsAndQueriesACaChain() throws Exception {
        long rootProfile = createProfile("ca-api-root", "ROOT", "EC-P-384");
        long subProfile = createProfile("ca-api-sub", "SUB", "EC-P-256");

        String rootPem = mockMvc
            .perform(post("/api/certificates/root").contentType(MediaType.APPLICATION_JSON)
                .content("{\"certProfileId\":" + rootProfile
                        + ",\"subjectDn\":\"CN=API Root,O=Meteor,C=KR\",\"validityPeriod\":\"P5Y\"}"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/x-pem-file"))
            .andExpect(content().string(startsWith("-----BEGIN CERTIFICATE-----")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String rootSki = caSkiBySubject("CN=API Root,O=Meteor,C=KR");

        String subResponse = mockMvc
            .perform(post("/api/certificates/sub-ca").contentType(MediaType.APPLICATION_JSON)
                .content("{\"certProfileId\":" + subProfile + ",\"subjectDn\":\"CN=API Sub,O=Meteor,C=KR\""
                        + ",\"akiHex\":\"" + rootSki + "\",\"includeDefaultCrlDp\":true,\"validityPeriod\":\"P1Y\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.x509CertificateDer").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();
        byte[] subDer = Base64.getDecoder().decode((String) JsonPath.read(subResponse, "$.x509CertificateDer"));
        String subSki = caSkiBySubject("CN=API Sub,O=Meteor,C=KR");

        mockMvc.perform(get("/api/certificates/ca/{skiHex}", subSki))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.certType").value("SUB"))
            .andExpect(jsonPath("$.certStatus").value("VALID"))
            .andExpect(jsonPath("$.akiHex").value(rootSki))
            .andExpect(jsonPath("$.keyAlgorithm").value("EC-P-256"))
            .andExpect(jsonPath("$.signatureAlgorithmName").value("SHA384withECDSA"))
            .andExpect(jsonPath("$.keyUsages[0]").value("KEY_CERT_SIGN"));

        String serial = JsonPath.read(mockMvc.perform(get("/api/certificates/ca/{skiHex}", subSki))
            .andReturn()
            .getResponse()
            .getContentAsString(), "$.serialNumberHex");
        mockMvc
            .perform(get("/api/certificates/{akiHex}/download", rootSki).param("serialNumberHex", serial)
                .param("format", "der"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pkix-cert"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + serial + ".der\""))
            .andExpect(content().bytes(subDer));
        mockMvc.perform(get("/api/certificates/{akiHex}/download", rootSki).param("serialNumberHex", serial))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/x-pem-file"));
        mockMvc
            .perform(get("/api/certificates/{akiHex}/download", rootSki).param("serialNumberHex", serial)
                .param("format", "p12"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("CERT_INVALID_INPUT"));

        mockMvc.perform(get("/api/certificates/ca"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].certPem", hasItem(rootPem)));
    }

    @Test
    void caErrorsUseX509RaCodes() throws Exception {
        mockMvc.perform(get("/api/certificates/ca/{skiHex}", "ABCDEF"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("CERT_RESOURCE_NOT_FOUND"));

        long leafProfile = createProfile("ca-api-leaf", "LEAF", "EC-P-256");
        mockMvc
            .perform(post("/api/certificates/root").contentType(MediaType.APPLICATION_JSON)
                .content("{\"certProfileId\":" + leafProfile + ",\"subjectDn\":\"CN=Not Root\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("PROFILE_RESOURCE_NOT_FOUND"));
        mockMvc
            .perform(post("/api/certificates/sub-ca").contentType(MediaType.APPLICATION_JSON)
                .content("{\"certProfileId\":" + leafProfile + ",\"subjectDn\":\"CN=x\",\"akiHex\":\"ZZ\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
    }

    @Test
    void renewsAndRevokesASubCa() throws Exception {
        long rootProfile = createProfile("ca-renew-root", "ROOT", "EC-P-256");
        long subProfile = createProfile("ca-renew-sub", "SUB", "EC-P-256");
        mockMvc
            .perform(post("/api/certificates/root").contentType(MediaType.APPLICATION_JSON)
                .content("{\"certProfileId\":" + rootProfile + ",\"subjectDn\":\"CN=Renew API Root\"}"))
            .andExpect(status().isOk());
        String rootSki = caSkiBySubject("CN=Renew API Root");
        mockMvc
            .perform(post("/api/certificates/sub-ca").contentType(MediaType.APPLICATION_JSON)
                .content("{\"certProfileId\":" + subProfile + ",\"subjectDn\":\"CN=Renew API Sub\",\"akiHex\":\""
                        + rootSki + "\",\"validityPeriod\":\"P11Y\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("CERT_INVALID_INPUT"));
        mockMvc
            .perform(post("/api/certificates/sub-ca").contentType(MediaType.APPLICATION_JSON)
                .content("{\"certProfileId\":" + subProfile + ",\"subjectDn\":\"CN=Renew API Sub\",\"akiHex\":\""
                        + rootSki + "\",\"validityPeriod\":\"P2Y\"}"))
            .andExpect(status().isOk());
        String subSki = caSkiBySubject("CN=Renew API Sub");
        String subSerial = JsonPath.read(mockMvc.perform(get("/api/certificates/ca/{skiHex}", subSki))
            .andReturn()
            .getResponse()
            .getContentAsString(), "$.serialNumberHex");

        mockMvc
            .perform(post("/api/certificates/sub-ca/renewal").contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNumberHex\":\"" + subSerial + "\",\"akiHex\":\"" + rootSki
                        + "\",\"activationStartDaysAgo\":0,\"validityPeriod\":\"P20Y\","
                        + "\"revokeOriginalCertificate\":true,\"useLatestIssuer\":false}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Requested validity is outside the issuer's validity period."));
        mockMvc
            .perform(post("/api/certificates/sub-ca/renewal").contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNumberHex\":\"" + subSerial + "\",\"akiHex\":\"" + rootSki
                        + "\",\"activationStartDaysAgo\":0,\"validityPeriod\":\"P1Y\","
                        + "\"revokeOriginalCertificate\":true,\"useLatestIssuer\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.x509CertificateDer").isString());

        String list = mockMvc.perform(get("/api/certificates/ca")).andReturn().getResponse().getContentAsString();
        java.util.List<String> statuses = JsonPath.read(list, "$[?(@.subjectDn == 'CN=Renew API Sub')].certStatus");
        java.util.List<String> skis = JsonPath.read(list, "$[?(@.subjectDn == 'CN=Renew API Sub')].skiHex");
        org.assertj.core.api.Assertions.assertThat(statuses).containsExactlyInAnyOrder("REVOKED", "VALID");
        String renewedSki = skis.stream().filter(ski -> !ski.equals(subSki)).findFirst().orElseThrow();
        String renewedSerial = JsonPath.read(mockMvc.perform(get("/api/certificates/ca/{skiHex}", renewedSki))
            .andReturn()
            .getResponse()
            .getContentAsString(), "$.serialNumberHex");
        String rootSerial = JsonPath.read(mockMvc.perform(get("/api/certificates/ca/{skiHex}", rootSki))
            .andReturn()
            .getResponse()
            .getContentAsString(), "$.serialNumberHex");

        mockMvc
            .perform(post("/api/certificates/sub-ca/{akiHex}/revocation", rootSki)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNumberHex\":\"" + renewedSerial + "\",\"reason\":7}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Unsupported CRL reason code."));
        mockMvc
            .perform(post("/api/certificates/sub-ca/{akiHex}/revocation", rootSki)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNumberHex\":\"" + renewedSerial + "\",\"reason\":1}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/certificates/ca/{skiHex}", renewedSki)).andExpect(status().isNotFound());
        mockMvc
            .perform(post("/api/certificates/sub-ca/{akiHex}/revocation", rootSki)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNumberHex\":\"" + renewedSerial + "\",\"reason\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Can not revoke certificate"));
        mockMvc
            .perform(post("/api/certificates/sub-ca/{akiHex}/revocation", rootSki)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNumberHex\":\"" + rootSerial + "\",\"reason\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("CERT_REVOKE_ROOT_FORBIDDEN"));
        mockMvc
            .perform(post("/api/certificates/sub-ca/{akiHex}/revocation", rootSki)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNumberHex\":\"0A\",\"reason\":1}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("CERT_RESOURCE_NOT_FOUND"));
    }

    private long createProfile(String name, String type, String keyAlgorithm) throws Exception {
        String body = mockMvc
            .perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                .content("{\"policyName\":\"" + name + "\",\"certType\":\"" + type
                        + "\",\"validityPeriod\":\"P10Y\",\"keyUsages\":[],\"keyAlgorithmSpec\":\"" + keyAlgorithm
                        + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String caSkiBySubject(String subjectDn) throws Exception {
        String body = mockMvc.perform(get("/api/certificates/ca")).andReturn().getResponse().getContentAsString();
        java.util.List<String> skis = JsonPath.read(body, "$[?(@.subjectDn == '" + subjectDn + "')].skiHex");
        return skis.get(0);
    }

}
