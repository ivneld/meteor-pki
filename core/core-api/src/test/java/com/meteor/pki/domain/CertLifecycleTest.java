package com.meteor.pki.domain;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.CrlReason;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");

    private static final DistributionPoints DP = new DistributionPoints("http://dp.example.com",
            "http://dp.example.com", "http://dp.example.com", "/api/ocsp", "/api/ca", "/api/crl");

    private final KeyPair rootKeys = TestKeys.ecP256();

    private final KeyPair subKeys = TestKeys.ecP256();

    @Test
    void issuerValidityMustCoverTheRequestedPeriod() {
        Cert root = issueRoot(rootKeys, "CN=Root");

        root.ensureCanIssue(CertType.SUB, new ValidityPeriod(NOW, root.getNotAfter()), NOW);
        assertThatThrownBy(() -> root.ensureCanIssue(CertType.SUB,
                new ValidityPeriod(NOW, root.getNotAfter().plusSeconds(1)), NOW))
            .isInstanceOf(CoreException.class)
            .hasMessage("Requested validity is outside the issuer's validity period.");
        assertThatThrownBy(() -> root.ensureCanIssue(CertType.LEAF,
                new ValidityPeriod(NOW.minusSeconds(1), NOW.plus(Duration.ofDays(1))), NOW))
            .hasMessage("Requested validity is outside the issuer's validity period.");
    }

    @Test
    void subCaIsRevokedOnceWithAReason() {
        Cert root = issueRoot(rootKeys, "CN=Root");
        Cert sub = saved(issueSub(root, rootKeys, subKeys));

        Cert revoked = sub.revoke(CertType.SUB, CrlReason.KEY_COMPROMISE, NOW);

        assertThat(revoked.getStatus()).isEqualTo(CertStatus.REVOKED);
        assertThat(revoked.getStatusAt(NOW)).isEqualTo(CertStatus.REVOKED);
        assertThat(revoked.getRevocation()).isEqualTo(new Revocation(CrlReason.KEY_COMPROMISE, NOW));
        assertThat(revoked.getId()).isEqualTo(sub.getId());
        assertThat(revoked.getTrackingId()).isEqualTo(sub.getTrackingId());
        assertThatThrownBy(() -> revoked.revoke(CertType.SUB, CrlReason.KEY_COMPROMISE, NOW))
            .hasMessage("Can not revoke certificate");
        assertThatThrownBy(() -> sub.revoke(CertType.SUB, CrlReason.UNSPECIFIED, Instant.parse("2040-01-01T00:00:00Z")))
            .hasMessage("Can not revoke certificate");
    }

    @Test
    void revocationRejectsRootsOtherTypesAndUnusableReasons() {
        Cert root = issueRoot(rootKeys, "CN=Root");
        Cert sub = saved(issueSub(root, rootKeys, subKeys));

        assertThatThrownBy(() -> root.revoke(CertType.SUB, CrlReason.KEY_COMPROMISE, NOW))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode())
                .isEqualTo(ErrorCode.CERT_REVOKE_ROOT_FORBIDDEN));
        assertThatThrownBy(() -> sub.revoke(CertType.LEAF, CrlReason.KEY_COMPROMISE, NOW))
            .hasMessage("Certificate type does not match the revocation.");
        assertThatThrownBy(() -> sub.revoke(CertType.SUB, CrlReason.REMOVE_FROM_CRL, NOW))
            .hasMessage("removeFromCRL cannot be used as a revocation reason.");
        assertThat(Revocation.reasonOf(4)).isEqualTo(CrlReason.SUPERSEDED);
        assertThatThrownBy(() -> Revocation.reasonOf(7)).hasMessage("Unsupported CRL reason code.");
    }

    private static Cert saved(Cert cert) {
        return Cert.restore(10L, cert.getDer(), cert.getCertType(), cert.getStatus(), cert.getPrivateKeyId(), false,
                NOW, null);
    }

    private static Cert issueRoot(KeyPair keys, String subjectDn) {
        CertificateTemplate template = profile(CertType.ROOT, null).rootTemplate(DistinguishedName.of(subjectDn),
                TestKeys.publicKey(keys), new IssuanceOptions(NOW, null, List.of(), false, List.of(), List.of()));
        TbsCertificate tbs = template.toTbs(SerialNumber.random(new SecureRandom()), TestKeys.ecdsa());
        return Cert.issuedRoot(tbs.withSignature(TestKeys.sign(tbs, keys), TestKeys.publicKey(keys)),
                new PrivateKeyId("k-root"));
    }

    private static Cert issueSub(Cert root, KeyPair rootKeys, KeyPair subKeys) {
        IssuanceOptions options = new IssuanceOptions(NOW, ValidityDuration.from("P5Y"), List.of(), true, List.of(),
                List.of());
        CertificateTemplate template = profile(CertType.SUB, 0).subordinateTemplate(root.getSubjectDn(),
                root.getSubjectPublicKey(), DistinguishedName.of("CN=Sub,O=Meteor,C=KR"), TestKeys.publicKey(subKeys),
                options, DP, null);
        TbsCertificate tbs = template.toTbs(SerialNumber.random(new SecureRandom()), TestKeys.ecdsa());
        return Cert.issuedSubCa(tbs.withSignature(TestKeys.sign(tbs, rootKeys), root.getSubjectPublicKey()),
                new PrivateKeyId("k-sub"));
    }

    private static CertProfile profile(CertType type, Integer pathLen) {
        return CertProfile.create(new CertProfileDraft(type + "-lifecycle", type, ValidityDuration.from("P10Y"),
                pathLen, SubjectAltNameTemplate.EMPTY, Set.of(), false, List.of(), null,
                List.of("http://ocsp.example.com"), false, "", false, true, KeyAlgorithm.of("EC-P-256"), List.of()));
    }

}
