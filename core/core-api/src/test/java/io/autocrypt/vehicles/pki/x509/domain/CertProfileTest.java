package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;
import java.util.Set;

import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertProfileTest {

    private static final String DEFAULT_OCSP = "http://127.0.0.1/api/ocsp";

    @Test
    void rootIsUnconstrainedCaWithCertAndCrlSigningAndNoOcspSanOrApproval() {
        CertProfile root = CertProfile.create(draft(CertType.ROOT, 3, Set.of(KeyUsage.DIGITAL_SIGNATURE), "DNS:a.com",
                List.of("http://ocsp.example.com"), true, true));

        assertThat(root.isCa()).isTrue();
        assertThat(root.getPathLenConstraint()).isNull();
        assertThat(root.isKeyUsageCritical()).isTrue();
        assertThat(root.getKeyUsages()).containsExactlyInAnyOrder(KeyUsage.DIGITAL_SIGNATURE, KeyUsage.KEY_CERT_SIGN,
                KeyUsage.CRL_SIGN);
        assertThat(root.getSubjectAltNameTemplate().isEmpty()).isTrue();
        assertThat(root.getOcspUris()).isEmpty();
        assertThat(root.isRequireApproval()).isFalse();
        assertThat(root.isUseDefaultAiaCaIssuer()).isFalse();
    }

    @Test
    void subKeepsPathLenAndCollectsDefaultOcspFirst() {
        CertProfile sub = CertProfile.create(
                draft(CertType.SUB, 2, Set.of(), "", List.of(" http://ocsp.example.com ", DEFAULT_OCSP), true, true));

        assertThat(sub.getPathLenConstraint()).isEqualTo(2);
        assertThat(sub.getKeyUsages()).containsExactlyInAnyOrder(KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN);
        assertThat(sub.getOcspUris()).containsExactly(DEFAULT_OCSP, "http://ocsp.example.com");
        assertThat(sub.isUseDefaultAiaCaIssuer()).isTrue();
    }

    @Test
    void subPathLenDefaultsToZeroAndIsBounded() {
        assertThat(CertProfile.create(draft(CertType.SUB, null, Set.of(), "", List.of(), false, false))
            .getPathLenConstraint()).isZero();
        assertThatThrownBy(() -> CertProfile.create(draft(CertType.SUB, -1, Set.of(), "", List.of(), false, false)))
            .hasMessage("pathLenConstraint must be greater than or equal to 0.");
        assertThatThrownBy(() -> CertProfile
            .create(draft(CertType.SUB, CertProfile.MAX_PATH_LENGTH + 1, Set.of(), "", List.of(), false, false)))
            .hasMessage("Exceeded the maximum allowed certificate chain length.");
    }

    @Test
    void leafDropsCaKeyUsagesAndKeepsSanAndApproval() {
        CertProfile leaf = CertProfile.create(
                draft(CertType.LEAF, 9, Set.of(KeyUsage.DIGITAL_SIGNATURE, KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN),
                        "DNS:a.example.com", List.of(), false, true));

        assertThat(leaf.isCa()).isFalse();
        assertThat(leaf.getPathLenConstraint()).isNull();
        assertThat(leaf.getKeyUsages()).containsExactly(KeyUsage.DIGITAL_SIGNATURE);
        assertThat(leaf.isKeyUsageCritical()).isFalse();
        assertThat(leaf.getSubjectAltNameTemplate().csv()).isEqualTo("DNS:a.example.com");
        assertThat(leaf.isRequireApproval()).isTrue();
        assertThat(leaf.getOcspUris()).isEmpty();
    }

    @Test
    void blankOcspUriIsRejected() {
        assertThatThrownBy(
                () -> CertProfile.create(draft(CertType.LEAF, null, Set.of(), "", List.of(" "), false, false)))
            .isInstanceOf(CoreException.class)
            .hasMessage("OCSP URI must not be blank.");
    }

    @Test
    void customExtensionsNeedARegisteredCustomOid() {
        CertProfileDraft draft = new CertProfileDraft("custom", CertType.LEAF, ValidityDuration.from("P1Y"), null,
                SubjectAltNameTemplate.EMPTY, Set.of(), false, List.of(), null, List.of(), false, DEFAULT_OCSP, false,
                false, KeyAlgorithm.of("EC-P-256"), List.of("1.2.3.4"));

        assertThatThrownBy(() -> CertProfile.create(draft)).isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.PROFILE_INVALID_INPUT))
            .hasMessage("Custom OID not found or inactive.");
    }

    @Test
    void keyAlgorithmIsReadFromItsDisplayValue() {
        assertThat(KeyAlgorithm.of("RSA-3072").signatureAlgorithm().getJcaName()).isEqualTo("SHA384withRSA");
        assertThatThrownBy(() -> KeyAlgorithm.of("EC_P256")).hasMessage("Unsupported key algorithm spec.");
    }

    @Test
    void certificatePoliciesNeedAtLeastOneValidOid() {
        assertThat(CertificatePolicies.ofNullable(null)).isNull();
        assertThatThrownBy(() -> CertificatePolicies.ofNullable(List.of()))
            .hasMessage("certificatePolicies must contain at least one policy OID (RFC §4.2.1.4).");
        assertThat(CertificatePolicies.ofNullable(List.of("2.5")).oids()).containsExactly("2.5");
        assertThatThrownBy(() -> CertificatePolicies.ofNullable(List.of("abc"))).hasMessage("Invalid OID.");
    }

    private static CertProfileDraft draft(CertType type, Integer pathLen, Set<KeyUsage> keyUsages, String san,
            List<String> ocspUris, boolean useDefaultOcsp, boolean flags) {
        return new CertProfileDraft(type.name().toLowerCase() + "-profile", type, ValidityDuration.from("P1Y"), pathLen,
                SubjectAltNameTemplate.of(san), keyUsages, false,
                List.of(new ProfileEku("1.3.6.1.5.5.7.3.1", "server")), null, ocspUris, useDefaultOcsp, DEFAULT_OCSP,
                flags, flags, KeyAlgorithm.of("EC-P-256"), List.of());
    }

}
