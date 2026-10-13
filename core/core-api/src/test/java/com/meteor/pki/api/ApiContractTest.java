package com.meteor.pki.api;

import java.time.Clock;
import java.util.stream.Stream;

import com.meteor.pki.domain.CertProfileReader;
import com.meteor.pki.domain.CertReader;
import com.meteor.pki.domain.EkuReader;
import com.meteor.pki.domain.EkuWriter;
import com.meteor.pki.support.web.ApiControllerAdvice;
import com.meteor.pki.usecase.CertIssuanceUseCase;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import com.meteor.pki.usecase.CertProfileUseCase;
import com.meteor.pki.usecase.SubCaIssueCommand;
import com.meteor.pki.support.web.JacksonConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiContractTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private MockMvc mockMvc;

    private final CertIssuanceUseCase certIssuanceUseCase = mock(CertIssuanceUseCase.class);

    @BeforeEach
    void setUp() {
        JsonMapper.Builder jsonMapper = JsonMapper.builder();
        JacksonConfig.customize(jsonMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(new LeafIssuanceController(certIssuanceUseCase),
                new CertManageController(), new CertQueryController(mock(CertReader.class), Clock.systemUTC()),
                new RootIssuanceController(certIssuanceUseCase), new SubCaIssuanceController(certIssuanceUseCase),
                new CrlController(), new CrlDistributionController(), new CmpController(), new KeyAgreementController(),
                new KeySyncController(), new DeploymentTlsBundleController(),
                new CertProfileController(mock(CertProfileUseCase.class), mock(CertProfileReader.class)),
                new EkuController(mock(EkuReader.class), mock(EkuWriter.class)), new CertIssueRequestStatController(),
                new IssuedCertStatController(), new RevokedCertStatController(), new StatRefreshController())
            .setControllerAdvice(new ApiControllerAdvice())
            .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
                    new JacksonJsonHttpMessageConverter(jsonMapper))
            .build();
    }

    static Stream<Arguments> validRequests() {
        String spki = "\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE\"";
        String stats = "?start=2026-01-01&end=2026-01-31&statType=DAILY";
        return Stream.of(
                Arguments.of("POST", "/api/certificates/leaf/0A/renewal", JSON,
                        "{\"serialNumberHex\":\"01\",\"activationStartDaysAgo\":0,\"revokeOriginalCertificate\":true}"),
                Arguments.of("POST", "/api/certificates/leaf/0A/revocation", JSON,
                        "{\"serialNumberHex\":\"01\",\"reason\":1}"),
                Arguments.of("POST", "/api/certificates/sub-ca/renewal", JSON,
                        "{\"serialNumberHex\":\"01\",\"akiHex\":\"0A\",\"activationStartDaysAgo\":0,"
                                + "\"useExistingPublicKey\":true,\"revokeOriginalCertificate\":false,\"useLatestIssuer\":true}"),
                Arguments.of("GET", "/api/crls/bulk.zip", null, null),
                Arguments.of("GET", "/api/crls/0A.crl", null, null),
                Arguments.of("POST", "/api/crls/0A/issue", null, null),
                Arguments.of("GET", "/api/crl/0A.crl", null, null),
                Arguments.of("POST", "/cmp", "application/pkixcmp", "0\u0000"),
                Arguments.of("POST", "/api/key-agreement/request-public-key", null, null),
                Arguments.of("POST", "/api/key-agreement/operate", JSON,
                        "{\"wrappingAlgorithm\":\"ECDH_AES_256_GCM\",\"initialValue\":\"AA==\","
                                + "\"ephemeralPublicKeySpki\":" + spki + ",\"wrappedKey\":\"AA==\"}"),
                Arguments.of("POST", "/api/key-sync", null, null),
                Arguments.of("POST", "/internal/deployment/tls-bundle", JSON,
                        "{\"subjectAlternativeNames\":\"DNS:ra.example.com\",\"pfxPassword\":\"secret\"}"),
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
        return Stream.of(Arguments.of("POST", "/api/certificates/leaf/spki", "{}"),
                Arguments.of("POST", "/api/certificates/leaf/p10cr", "{}"),
                Arguments.of("POST", "/api/certificates/leaf/0A/renewal", "{\"activationStartDaysAgo\":-1}"),
                Arguments.of("POST", "/api/certificates/leaf/0A/revocation", "{}"),
                Arguments.of("POST", "/api/certificates/root", "{}"),
                Arguments.of("POST", "/api/certificates/sub-ca", "{}"),
                Arguments.of("POST", "/api/certificates/sub-ca/renewal", "{}"),
                Arguments.of("POST", "/api/key-agreement/operate", "{}"),
                Arguments.of("POST", "/internal/deployment/tls-bundle", "{}"),
                Arguments.of("POST", "/api/profiles", "{}"), Arguments.of("POST", "/api/extended-key-usages", "{}"),
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
            .perform(request(HttpMethod.POST, "/api/certificates/root").contentType(JSON)
                .content("{\"certProfileId\":1,\"subjectDn\":\"CN=root\",\"validityPeriod\":\"1Y\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("validityPeriod only allows PnY, PnM, or PnD format."))
            .andExpect(jsonPath("$.errors[*].field", hasItem("validityPeriod")));
    }

    @Test
    void profileExtendedKeyUsagesAreBoundFromExtendKeyUsage() throws Exception {
        mockMvc.perform(request(HttpMethod.POST, "/api/profiles").contentType(JSON)
            .content("{\"policyName\":\"tls\",\"certType\":\"LEAF\",\"validityPeriod\":\"P1Y\","
                    + "\"keyUsages\":[\"DIGITAL_SIGNATURE\"],\"extendKeyUsage\":[\"\"],\"keyAlgorithmSpec\":\"EC_P256\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("extendedKeyUsages[0]"));
    }

    @Test
    void missingPrimitiveFieldsDefaultLikeX509Ra() throws Exception {
        when(certIssuanceUseCase.issueSubCa(any())).thenThrow(new CoreException(ErrorCode.NOT_IMPLEMENTED));

        mockMvc
            .perform(request(HttpMethod.POST, "/api/certificates/sub-ca").contentType(JSON)
                .content("{\"certProfileId\":1,\"subjectDn\":\"CN=sub\",\"akiHex\":\"0A\"}"))
            .andExpect(status().isNotImplemented());
        ArgumentCaptor<SubCaIssueCommand> command = ArgumentCaptor.forClass(SubCaIssueCommand.class);
        verify(certIssuanceUseCase).issueSubCa(command.capture());
        assertThat(command.getValue().includeDefaultCrlDp()).isFalse();
    }

    @Test
    void statsRequireDateRangeAndType() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/stats/issued-cert?start=2026-01-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
    }

    @Test
    void downloadRequiresSerialNumber() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/certificates/0A/download")).andExpect(status().isBadRequest());
    }

    @Test
    void notImplementedUsesX509RaErrorShape() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/crls/0A.crl"))
            .andExpect(status().isNotImplemented())
            .andExpect(jsonPath("$.title").value("PKI-ERROR"))
            .andExpect(jsonPath("$.errorCode").value("COMMON_NOT_IMPLEMENTED"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

}
