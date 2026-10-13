package io.autocrypt.vehicles.pki.x509.api;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import io.autocrypt.vehicles.pki.x509.domain.TestKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Root → Sub CA 를 만든 뒤 Leaf 를 공개키와 CSR 로 발급하고, 발급자 공개키로 서명을 검증한다.
 */
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
        String csr = TestKeys.csrPem("CN=api.example.com,O=Autocrypt", device, "api.example.com", "api2.example.com");

        String body = mockMvc
            .perform(post("/api/certificates/leaf/p10cr").contentType(MediaType.APPLICATION_JSON)
                .content("{\"p10crPem\":" + quote(csr) + ",\"certProfileId\":" + leafProfile + ",\"akiHex\":\""
                        + issuingSki + "\",\"validityPeriod\":\"P90D\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subjectDn").value("CN=api.example.com,O=Autocrypt"))
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
