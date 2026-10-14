package com.meteor.pki.api;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import com.meteor.pki.domain.TestKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeafApiIT extends ApiIT {

    private String issuingSki;

    private X509Certificate issuing;

    private long leafProfile;

    @BeforeEach
    void buildCa() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        long rootProfile = createProfile("{\"policyName\":\"leaf-api-root-" + suffix
                + "\",\"certType\":\"ROOT\",\"validityPeriod\":\"P10Y\",\"keyUsages\":[],\"keyAlgorithmSpec\":\"EC-P-256\"}");
        long subProfile = createProfile("{\"policyName\":\"leaf-api-sub-" + suffix
                + "\",\"certType\":\"SUB\",\"validityPeriod\":\"P5Y\",\"keyUsages\":[],\"keyAlgorithmSpec\":\"EC-P-256\"}");
        leafProfile = createProfile("{\"policyName\":\"leaf-api-tls-" + suffix
                + "\",\"certType\":\"LEAF\",\"validityPeriod\":\"P1Y\",\"keyUsages\":[\"DIGITAL_SIGNATURE\"],"
                + "\"extendKeyUsage\":[\"1.3.6.1.5.5.7.3.1\"],\"subjectAltName\":\"DNS:www.example.com\","
                + "\"useDefaultAiaCaIssuer\":true,\"keyAlgorithmSpec\":\"EC-P-256\"}");
        String rootDn = "CN=Leaf API Root " + suffix;
        mockMvc
            .perform(post("/api/certificates/root").contentType(MediaType.APPLICATION_JSON)
                .content("{\"certProfileId\":" + rootProfile + ",\"subjectDn\":\"" + rootDn + "\"}"))
            .andExpect(status().isOk());
        String rootSki = skiOf(rootDn);
        String sub = mockMvc
            .perform(post("/api/certificates/sub-ca").contentType(MediaType.APPLICATION_JSON)
                .content("{\"certProfileId\":" + subProfile + ",\"subjectDn\":\"CN=Leaf API Issuing " + suffix
                        + "\",\"akiHex\":\"" + rootSki + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        issuing = x509(JsonPath.read(sub, "$.x509CertificateDer"));
        issuingSki = skiOf("CN=Leaf API Issuing " + suffix);
    }

    @Test
    void issuesLeafFromPublicKey() throws Exception {
        KeyPair device = TestKeys.ecP256();
        String body = mockMvc
            .perform(post("/api/certificates/leaf/spki").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectDn\":\"CN=www.example.com\",\"akiHex\":\"" + issuingSki
                        + "\",\"subjectPublicKeyInfoDer\":\""
                        + Base64.getEncoder().encodeToString(device.getPublic().getEncoded()) + "\",\"certProfileId\":"
                        + leafProfile + ",\"includeDefaultCrlDp\":true}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        X509Certificate leaf = x509(JsonPath.read(body, "$.x509CertificateDer"));
        leaf.verify(issuing.getPublicKey());
        assertThat(leaf.getBasicConstraints()).isEqualTo(-1);
        assertThat(leaf.getExtendedKeyUsage()).containsExactly("1.3.6.1.5.5.7.3.1");
        assertThat(leaf.getSubjectAlternativeNames()).extracting(name -> (Object) name.get(1))
            .containsExactly("www.example.com");
        assertThat(leaf.getPublicKey()).isEqualTo(device.getPublic());
    }

    @Test
    void issuesLeafFromCsrWithItsRequestedSan() throws Exception {
        KeyPair device = TestKeys.ecP256();
        String csr = TestKeys.csrPem("CN=api.example.com,O=Meteor", device, "api.example.com", "api2.example.com");

        String body = mockMvc
            .perform(post("/api/certificates/leaf/p10cr").contentType(MediaType.APPLICATION_JSON)
                .content("{\"p10crPem\":" + quote(csr) + ",\"certProfileId\":" + leafProfile + ",\"akiHex\":\""
                        + issuingSki + "\",\"validityPeriod\":\"P90D\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subjectDn").value("CN=api.example.com,O=Meteor"))
            .andExpect(jsonPath("$.akiHex").value(issuingSki))
            .andExpect(jsonPath("$.signatureAlgorithmName").value("SHA256withECDSA"))
            .andExpect(jsonPath("$.keyUsages[0]").value("DIGITAL_SIGNATURE"))
            .andExpect(jsonPath("$.extendedKeyUsages[0]").value("1.3.6.1.5.5.7.3.1"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        X509Certificate leaf = x509(JsonPath.read(body, "$.x509CertificateDer"));
        leaf.verify(issuing.getPublicKey());
        assertThat(leaf.getSubjectAlternativeNames()).extracting(name -> (Object) name.get(1))
            .containsExactly("api.example.com", "api2.example.com");
    }

    @Test
    void leafErrorsUseX509RaCodes() throws Exception {
        String rsaKey = Base64.getEncoder().encodeToString(rsaPublicKey());
        mockMvc
            .perform(post("/api/certificates/leaf/spki").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectDn\":\"CN=x\",\"akiHex\":\"" + issuingSki + "\",\"subjectPublicKeyInfoDer\":\""
                        + rsaKey + "\",\"certProfileId\":" + leafProfile + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("CERT_INVALID_INPUT"))
            .andExpect(jsonPath("$.detail").value("The subject public key does not match the profile's key spec."));

        mockMvc
            .perform(post("/api/certificates/leaf/p10cr").contentType(MediaType.APPLICATION_JSON)
                .content("{\"p10crPem\":\"garbage\",\"certProfileId\":" + leafProfile + ",\"akiHex\":\"" + issuingSki
                        + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid PEM format"));

        mockMvc
            .perform(post("/api/certificates/leaf/spki").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectDn\":\"CN=x\",\"akiHex\":\"0102\",\"subjectPublicKeyInfoDer\":\""
                        + Base64.getEncoder().encodeToString(TestKeys.ecP256().getPublic().getEncoded())
                        + "\",\"certProfileId\":" + leafProfile + "}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("CERT_RESOURCE_NOT_FOUND"));
    }

    @Test
    void renewsALeafWithTheSameOrANewPublicKey() throws Exception {
        KeyPair device = TestKeys.ecP256();
        X509Certificate original = x509(JsonPath.read(mockMvc
            .perform(post("/api/certificates/leaf/spki").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectDn\":\"CN=www.example.com\",\"akiHex\":\"" + issuingSki
                        + "\",\"subjectPublicKeyInfoDer\":\""
                        + Base64.getEncoder().encodeToString(device.getPublic().getEncoded()) + "\",\"certProfileId\":"
                        + leafProfile + "}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(), "$.x509CertificateDer"));
        String serial = original.getSerialNumber().toString(16).toUpperCase();

        String sameKey = mockMvc.perform(post("/api/certificates/leaf/{akiHex}/renewal", issuingSki)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"serialNumberHex\":\"" + serial
                    + "\",\"activationStartDaysAgo\":0,\"revokeOriginalCertificate\":false,\"validityPeriod\":\"\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subjectDn").value("CN=www.example.com"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        X509Certificate renewed = x509(JsonPath.read(sameKey, "$.certificateDerBase64"));
        renewed.verify(issuing.getPublicKey());
        assertThat(renewed.getPublicKey()).isEqualTo(device.getPublic());
        assertThat(renewed.getSerialNumber().toString(16).toUpperCase())
            .isEqualTo(JsonPath.read(sameKey, "$.serialNumberHex"));
        assertThat(renewed.getSubjectAlternativeNames())
            .containsExactlyElementsOf(original.getSubjectAlternativeNames());

        KeyPair next = TestKeys.ecP256();
        String rekeyed = mockMvc
            .perform(post("/api/certificates/leaf/{akiHex}/renewal", issuingSki).contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNumberHex\":\"" + serial
                        + "\",\"activationStartDaysAgo\":0,\"revokeOriginalCertificate\":true,"
                        + "\"subjectPublicKeyInfoDer\":\""
                        + Base64.getEncoder().encodeToString(next.getPublic().getEncoded())
                        + "\",\"validityPeriod\":\"P90D\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(x509(JsonPath.read(rekeyed, "$.certificateDerBase64")).getPublicKey()).isEqualTo(next.getPublic());

        mockMvc
            .perform(post("/api/certificates/leaf/{akiHex}/renewal", issuingSki).contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNumberHex\":\"" + serial + "\",\"activationStartDaysAgo\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Cannot renew invalid certificate."));
        mockMvc
            .perform(post("/api/certificates/leaf/{akiHex}/renewal", issuingSki).contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNumberHex\":\"" + renewed.getSerialNumber().toString(16).toUpperCase()
                        + "\",\"activationStartDaysAgo\":0,\"subjectPublicKeyInfoDer\":\""
                        + Base64.getEncoder().encodeToString(rsaPublicKey()) + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("The renewal public key must use the original key algorithm."));
    }

    private long createProfile(String json) throws Exception {
        String body = mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String skiOf(String subjectDn) throws Exception {
        String body = mockMvc.perform(get("/api/certificates/ca")).andReturn().getResponse().getContentAsString();
        List<String> skis = JsonPath.read(body, "$[?(@.subjectDn == '" + subjectDn + "')].skiHex");
        return skis.get(0);
    }

    private static X509Certificate x509(String base64) throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
            .generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
    }

    private static byte[] rsaPublicKey() throws Exception {
        java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair().getPublic().getEncoded();
    }

    private static String quote(String text) {
        return "\"" + text.replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }

}
