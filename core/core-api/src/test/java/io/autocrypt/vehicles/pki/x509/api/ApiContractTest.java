package io.autocrypt.vehicles.pki.x509.api;

import java.util.stream.Stream;

import io.autocrypt.vehicles.pki.x509.support.web.ApiControllerAdvice;
import io.autocrypt.vehicles.pki.x509.support.web.JacksonConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * x509-ra 에서 옮겨 온 API 계약을 확인한다. 구현 전이므로 올바른 요청은 501, 잘못된 요청은 400 이어야 한다. 404·405·415 가
 * 나오면 경로, 메서드, 미디어 타입이 원본 계약과 어긋난 것이다.
 */
class ApiContractTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper.Builder jsonMapper = JsonMapper.builder();
        JacksonConfig.customize(jsonMapper);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new LeafIssuanceController(), new CertManageController(), new CertQueryController(),
                    new RootIssuanceController(), new SubCaIssuanceController(), new CrlController(),
                    new CmpController(), new KeyAgreementController(), new KeySyncController(),
                    new DeploymentTlsBundleController(), new CaDistributionControllerV1(),
                    new LeafIssuanceControllerV1(), new CertManageControllerV1(), new CertQueryControllerV1(),
                    new CrlControllerV1(), new RootIssuanceControllerV1(), new SubCaIssuanceControllerV1(),
                    new CertProfileController(), new CertProfileControllerV1(), new EkuController(),
                    new EkuControllerV1(), new CertIssueRequestStatController(), new IssuedCertStatController(),
                    new RevokedCertStatController(), new StatRefreshController())
            .setControllerAdvice(new ApiControllerAdvice())
            .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
                    new JacksonJsonHttpMessageConverter(jsonMapper))
            .build();
    }

    static Stream<Arguments> validRequests() {
        String spki = "\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE\"";
        String leaf = "{\"subjectDn\":\"CN=leaf\",\"akiHex\":\"0A\",\"subjectPublicKeyInfoDer\":" + spki
                + ",\"certProfileId\":1,\"validityPeriod\":\"P1Y\"}";
        String p10cr = "{\"p10crPem\":\"-----BEGIN CERTIFICATE REQUEST-----\",\"certProfileId\":1,\"akiHex\":\"0A\"}";
        String profile = "{\"policyName\":\"tls\",\"certType\":\"LEAF\",\"validityPeriod\":\"P1Y\","
                + "\"keyUsages\":[\"DIGITAL_SIGNATURE\"],\"extendKeyUsage\":[\"1.3.6.1.5.5.7.3.1\"],"
                + "\"keyAlgorithmSpec\":\"EC_P256\"}";
        String stats = "?start=2026-01-01&end=2026-01-31&statType=DAILY";
        return Stream.of(
                // v2 인증서
                Arguments.of("POST", "/api/v2/certificates/leaf/spki", JSON, leaf),
                Arguments.of("POST", "/api/v2/certificates/leaf/p10cr", JSON, p10cr),
                Arguments.of("POST", "/api/v2/certificates/leaf/0A/renewal", JSON,
                        "{\"serialNumberHex\":\"01\",\"activationStartDaysAgo\":0,\"revokeOriginalCertificate\":true}"),
                Arguments.of("POST", "/api/v2/certificates/leaf/0A/revocation", JSON,
                        "{\"serialNumberHex\":\"01\",\"reason\":1}"),
                Arguments.of("GET", "/api/v2/certificates/0A/download?serialNumberHex=01&format=der", null, null),
                Arguments.of("GET", "/api/v2/certificates/ca/0A", null, null),
                Arguments.of("GET", "/api/v2/certificates/ca", null, null),
                Arguments.of("POST", "/api/v2/certificates/root", JSON,
                        "{\"certProfileId\":1,\"subjectDn\":\"CN=root\"}"),
                Arguments.of("POST", "/api/v2/certificates/sub-ca", JSON,
                        "{\"certProfileId\":1,\"subjectDn\":\"CN=sub\",\"akiHex\":\"0A\"}"),
                Arguments.of("POST", "/api/v2/certificates/sub-ca/renewal", JSON,
                        "{\"serialNumberHex\":\"01\",\"akiHex\":\"0A\",\"activationStartDaysAgo\":0,"
                                + "\"useExistingPublicKey\":true,\"revokeOriginalCertificate\":false,\"useLatestIssuer\":true}"),
                Arguments.of("GET", "/api/v2/crls/bulk.zip", null, null),
                Arguments.of("GET", "/api/v2/crls/0A.crl", null, null),
                Arguments.of("POST", "/api/v2/crls/0A/issue", null, null),
                Arguments.of("POST", "/cmp", "application/pkixcmp", "0\u0000"),
                Arguments.of("POST", "/api/key-agreement/request-public-key", null, null),
                Arguments.of("POST", "/api/key-agreement/operate", JSON,
                        "{\"wrappingAlgorithm\":\"ECDH_AES_256_GCM\",\"initialValue\":\"AA==\","
                                + "\"ephemeralPublicKeySpki\":" + spki + ",\"wrappedKey\":\"AA==\"}"),
                Arguments.of("POST", "/api/key-sync", null, null),
                Arguments.of("POST", "/internal/deployment/tls-bundle", JSON,
                        "{\"subjectAlternativeNames\":\"DNS:ra.example.com\",\"pfxPassword\":\"secret\"}"),
                // v1 인증서
                Arguments.of("GET", "/api/ca/0A.crt", null, null),
                Arguments.of("POST", "/api/certificates", JSON, leaf),
                Arguments.of("POST", "/api/certificates/p10cr", JSON,
                        "{\"p10crPem\":\"-----BEGIN CERTIFICATE REQUEST-----\",\"certProfileId\":1,\"issuerKeyIdHex\":\"0A\"}"),
                Arguments.of("POST", "/api/certificates/0A/renewal", JSON,
                        "{\"serialNumberHex\":\"01\",\"activationStartDaysAgo\":0,\"revokeOriginalCertificate\":true}"),
                Arguments.of("POST", "/api/certificates/01/revocation", JSON, "{\"akiHex\":\"0A\",\"reason\":1}"),
                Arguments.of("GET", "/api/certificates/0A/download?serialNumberHex=01", null, null),
                Arguments.of("GET", "/api/certificates/0A", null, null),
                Arguments.of("GET", "/api/certificates/ca", null, null),
                Arguments.of("GET", "/api/crl/0A.crl", null, null),
                Arguments.of("GET", "/api/crl/bulk.zip", null, null),
                Arguments.of("POST", "/api/certificates/root", JSON, "{\"certProfileId\":1,\"subjectDn\":\"CN=root\"}"),
                Arguments.of("POST", "/api/certificates/sub", JSON,
                        "{\"certProfileId\":1,\"subjectDn\":\"CN=sub\",\"akiHex\":\"0A\"}"),
                // 프로파일
                Arguments.of("POST", "/api/v2/profiles", JSON, profile),
                Arguments.of("GET", "/api/v2/profiles/detail?profileName=tls", null, null),
                Arguments.of("GET", "/api/v2/profiles?page=0&size=20", null, null),
                Arguments.of("POST", "/api/certpolicies", JSON, profile),
                Arguments.of("GET", "/api/certpolicies/detail?policyName=tls", null, null),
                Arguments.of("GET", "/api/certpolicies", null, null),
                Arguments.of("POST", "/api/v2/extended-key-usages", JSON,
                        "{\"oid\":\"1.3.6.1.5.5.7.3.1\",\"purpose\":\"serverAuth\"}"),
                Arguments.of("GET", "/api/v2/extended-key-usages", null, null),
                Arguments.of("POST", "/api/extended-key-usages", JSON,
                        "{\"oid\":\"1.3.6.1.5.5.7.3.1\",\"purpose\":\"serverAuth\"}"),
                Arguments.of("GET", "/api/extended-key-usages", null, null),
                // 통계
                Arguments.of("GET", "/api/stats/issue-cert-request" + stats, null, null),
                Arguments.of("GET", "/api/stats/issued-cert" + stats, null, null),
                Arguments.of("GET", "/api/stats/revoked-cert" + stats, null, null), Arguments.of("POST",
                        "/api/cert-stat/recalculation", JSON, "{\"start\":\"2026-01-01\",\"end\":\"2026-01-31\"}"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("validRequests")
    void validRequestReachesTheEndpoint(String method, String uri, String contentType, String body) throws Exception {
        var builder = request(HttpMethod.valueOf(method), uri);
        if (contentType != null) {
            builder.contentType(contentType).content(body);
        }

        mockMvc.perform(builder).andExpect(status().isNotImplemented());
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(Arguments.of("POST", "/api/v2/certificates/leaf/spki", "{}"),
                Arguments.of("POST", "/api/v2/certificates/leaf/p10cr", "{}"),
                Arguments.of("POST", "/api/v2/certificates/leaf/0A/renewal", "{\"activationStartDaysAgo\":-1}"),
                Arguments.of("POST", "/api/v2/certificates/leaf/0A/revocation", "{}"),
                Arguments.of("POST", "/api/v2/certificates/root", "{}"),
                Arguments.of("POST", "/api/v2/certificates/sub-ca", "{}"),
                Arguments.of("POST", "/api/v2/certificates/sub-ca/renewal", "{}"),
                Arguments.of("POST", "/api/key-agreement/operate", "{}"),
                Arguments.of("POST", "/internal/deployment/tls-bundle", "{}"),
                Arguments.of("POST", "/api/certificates", "{}"), Arguments.of("POST", "/api/certificates/p10cr", "{}"),
                Arguments.of("POST", "/api/certificates/01/revocation", "{}"),
                Arguments.of("POST", "/api/certificates/root", "{}"),
                Arguments.of("POST", "/api/certificates/sub", "{}"), Arguments.of("POST", "/api/v2/profiles", "{}"),
                Arguments.of("POST", "/api/certpolicies", "{}"),
                Arguments.of("POST", "/api/v2/extended-key-usages", "{}"),
                Arguments.of("POST", "/api/extended-key-usages", "{}"),
                Arguments.of("POST", "/api/cert-stat/recalculation", "{}"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("invalidRequests")
    void invalidBodyIsRejectedInX509RaErrorShape(String method, String uri, String body) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), uri).contentType(JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("PKI-ERROR"))
            .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"))
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void validityPeriodMustBeIsoPeriodOfYearsMonthsOrDays() throws Exception {
        mockMvc
            .perform(request(HttpMethod.POST, "/api/v2/certificates/root").contentType(JSON)
                .content("{\"certProfileId\":1,\"subjectDn\":\"CN=root\",\"validityPeriod\":\"1Y\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("validityPeriod only allows PnY, PnM, or PnD format."))
            .andExpect(jsonPath("$.errors[*].field", hasItem("validityPeriod")));
    }

    @Test
    void profileExtendedKeyUsagesAreBoundFromExtendKeyUsage() throws Exception {
        // x509-ra 계약은 extendKeyUsage 다. 빈 문자열이 들어오면 List<@NotBlank String> 검증에 걸려야 한다
        mockMvc.perform(request(HttpMethod.POST, "/api/v2/profiles").contentType(JSON)
            .content("{\"policyName\":\"tls\",\"certType\":\"LEAF\",\"validityPeriod\":\"P1Y\","
                    + "\"keyUsages\":[\"DIGITAL_SIGNATURE\"],\"extendKeyUsage\":[\"\"],\"keyAlgorithmSpec\":\"EC_P256\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("extendedKeyUsages[0]"));
    }

    @Test
    void missingPrimitiveFieldsDefaultLikeX509Ra() throws Exception {
        // includeDefaultCrlDp(boolean) 를 빼도 x509-ra 처럼 false 로 받아 검증을 통과해야 한다
        mockMvc
            .perform(request(HttpMethod.POST, "/api/v2/certificates/sub-ca").contentType(JSON)
                .content("{\"certProfileId\":1,\"subjectDn\":\"CN=sub\",\"akiHex\":\"0A\"}"))
            .andExpect(status().isNotImplemented());
    }

    @Test
    void statsRequireDateRangeAndType() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/stats/issued-cert?start=2026-01-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
    }

    @Test
    void downloadRequiresSerialNumber() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v2/certificates/0A/download")).andExpect(status().isBadRequest());
    }

    @Test
    void notImplementedUsesX509RaErrorShape() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v2/certificates/ca"))
            .andExpect(status().isNotImplemented())
            .andExpect(jsonPath("$.title").value("PKI-ERROR"))
            .andExpect(jsonPath("$.errorCode").value("COMMON_NOT_IMPLEMENTED"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

}
