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
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.X509CertificateHolder;
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
    void validityPeriodIsKeptInWholeSeconds() {
        ValidityPeriod period = new ValidityPeriod(NOW.plusMillis(700), NOW.plusSeconds(10).plusMillis(300));

        assertThat(period.notBefore()).isEqualTo(NOW);
        assertThat(period.length()).isEqualTo(Duration.ofSeconds(10));
        assertThat(period.contains(new ValidityPeriod(NOW, NOW.plusSeconds(10)))).isTrue();
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

    @Test
    void renewalKeepsTheSubjectAndExtensionsWithANewKey() throws Exception {
        Cert root = issueRoot(rootKeys, "CN=Root");
        Cert sub = saved(issueSub(root, rootKeys, subKeys));
        KeyPair newKeys = TestKeys.ecP256();

        ValidityPeriod validity = sub.renewalValidity(new RenewalOptions(2, null), NOW);
        Cert renewed = renew(sub, root, rootKeys, newKeys, validity);

        X509CertificateHolder original = new X509CertificateHolder(sub.getDer());
        X509CertificateHolder holder = new X509CertificateHolder(renewed.getDer());
        assertThat(validity.notBefore()).isEqualTo(NOW.minus(Duration.ofDays(2)));
        assertThat(validity.length()).isEqualTo(sub.getValidityPeriod().length());
        assertThat(renewed.getSubjectDn()).isEqualTo(sub.getSubjectDn());
        assertThat(renewed.getSubjectKeyId()).isEqualTo(TestKeys.publicKey(newKeys).keyIdentifier())
            .isNotEqualTo(sub.getSubjectKeyId());
        assertThat(renewed.getAuthorityKeyId()).isEqualTo(root.getSubjectKeyId());
        assertThat(renewed.getTrackingId()).isEqualTo(sub.getTrackingId());
        assertThat(renewed.getPrivateKeyId()).isEqualTo(new PrivateKeyId("k-renewed"));
        assertThat(renewed.getPathLenConstraint()).isZero();
        assertThat(holder.getExtension(Extension.keyUsage)).isEqualTo(original.getExtension(Extension.keyUsage));
        assertThat(holder.getExtension(Extension.cRLDistributionPoints))
            .isEqualTo(original.getExtension(Extension.cRLDistributionPoints));
        assertThat(sub.renewalValidity(new RenewalOptions(0, ValidityDuration.from("P1Y")), NOW).notAfter())
            .isEqualTo(Instant.parse("2027-10-13T00:00:00Z"));
        assertThatThrownBy(() -> Cert.renewedSubCa(sub.getDer(), new PrivateKeyId("k-same"), sub))
            .hasMessage("Renewed CA certificate must use a new key");
    }

    @Test
    void renewalUnderANewIssuerPointsDefaultDistributionPointsToIt() throws Exception {
        Cert root = issueRoot(rootKeys, "CN=Root");
        Cert sub = saved(issueSub(root, rootKeys, subKeys));
        KeyPair nextRootKeys = TestKeys.ecP256();
        Cert nextRoot = issueRoot(nextRootKeys, "CN=Root");

        Cert renewed = renew(sub, nextRoot, nextRootKeys, TestKeys.ecP256(),
                sub.renewalValidity(new RenewalOptions(0, ValidityDuration.from("P1Y")), NOW));

        X509CertificateHolder holder = new X509CertificateHolder(renewed.getDer());
        String issuerSki = nextRoot.getSubjectKeyId().toHex();
        assertThat(renewed.getAuthorityKeyId()).isEqualTo(nextRoot.getSubjectKeyId());
        assertThat(CRLDistPoint.fromExtensions(holder.getExtensions()).getDistributionPoints()[0].getDistributionPoint()
            .getName()
            .toString()).contains("http://dp.example.com/api/crl/" + issuerSki + ".crl");
        AuthorityInformationAccess aia = AuthorityInformationAccess.fromExtensions(holder.getExtensions());
        assertThat(aia.getAccessDescriptions()[0].getAccessLocation()).isEqualTo(new GeneralName(
                GeneralName.uniformResourceIdentifier, "http://dp.example.com/api/ca/" + issuerSki + ".crt"));
        assertThat(aia.getAccessDescriptions()[1].getAccessLocation())
            .isEqualTo(new GeneralName(GeneralName.uniformResourceIdentifier, "http://ocsp.example.com"));
    }

    @Test
    void onlyValidCertificatesOfTheExpectedTypeAreRenewed() {
        Cert root = issueRoot(rootKeys, "CN=Root");
        Cert sub = saved(issueSub(root, rootKeys, subKeys));

        sub.ensureRenewable(CertType.SUB, NOW);
        assertThatThrownBy(() -> root.ensureRenewable(CertType.SUB, NOW))
            .hasMessage("Certificate type does not match the renewal.");
        assertThatThrownBy(() -> sub.revoke(CertType.SUB, CrlReason.SUPERSEDED, NOW).ensureRenewable(CertType.SUB, NOW))
            .hasMessage("Cannot renew invalid certificate.");
        assertThatThrownBy(() -> new RenewalOptions(-1, null))
            .hasMessage("activationStartDaysAgo must not be negative.");
    }

    @Test
    void rootRenewalIsSelfSignedWithANewKeyAndKeepsTheLineage() throws Exception {
        Cert root = saved(issueRoot(rootKeys, "CN=Root"));
        KeyPair newKeys = TestKeys.ecP256();
        SubjectPublicKey newKey = TestKeys.publicKey(newKeys);

        root.ensureRenewable(CertType.ROOT, NOW);
        CertificateTemplate template = root.rootRenewalTemplate(newKey,
                root.renewalValidity(new RenewalOptions(0, ValidityDuration.from("P20Y")), NOW), DP);
        TbsCertificate tbs = template.toTbs(SerialNumber.random(new SecureRandom()), TestKeys.ecdsa());
        Cert renewed = Cert.renewedRoot(tbs.withSignature(TestKeys.sign(tbs, newKeys), newKey),
                new PrivateKeyId("k-root-2"), root);

        X509CertificateHolder holder = new X509CertificateHolder(renewed.getDer());
        assertThat(renewed.getCertType()).isEqualTo(CertType.ROOT);
        assertThat(renewed.getSubjectDn()).isEqualTo(root.getSubjectDn());
        assertThat(renewed.getIssuerDn()).isEqualTo(root.getSubjectDn());
        assertThat(renewed.getSubjectKeyId()).isEqualTo(newKey.keyIdentifier()).isNotEqualTo(root.getSubjectKeyId());
        assertThat(renewed.getAuthorityKeyId()).isEqualTo(renewed.getSubjectKeyId());
        assertThat(renewed.getTrackingId()).isEqualTo(root.getTrackingId());
        assertThat(renewed.getNotAfter()).isEqualTo(Instant.parse("2046-10-13T00:00:00Z"));
        assertThat(holder.isSignatureValid(
                new org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder().build(newKeys.getPublic())))
            .isTrue();
        assertThat(holder.getExtension(Extension.basicConstraints))
            .isEqualTo(new X509CertificateHolder(root.getDer()).getExtension(Extension.basicConstraints));
        assertThatThrownBy(() -> Cert.renewedRoot(root.getDer(), new PrivateKeyId("k-same"), root))
            .hasMessage("Renewed CA certificate must use a new key");
    }

    @Test
    void leafRenewalReusesOrReplacesTheClientKey() throws Exception {
        Cert root = issueRoot(rootKeys, "CN=Root");
        Cert sub = saved(issueSub(root, rootKeys, subKeys));
        Cert leaf = saved(issueLeaf(sub, subKeys));
        ValidityPeriod validity = leaf.renewalValidity(new RenewalOptions(0, null), NOW);

        assertThat(leaf.leafRenewalKey(null)).isEqualTo(leaf.getSubjectPublicKey());
        Cert sameKey = renewLeaf(leaf, sub, subKeys, leaf.leafRenewalKey(null), validity);
        SubjectPublicKey newKey = TestKeys.publicKey(TestKeys.ecP256());
        Cert newKeyRenewal = renewLeaf(leaf, sub, subKeys, leaf.leafRenewalKey(newKey), validity);

        assertThat(sameKey.getCertType()).isEqualTo(CertType.LEAF);
        assertThat(sameKey.getStatus()).isEqualTo(CertStatus.VALID);
        assertThat(sameKey.getSubjectKeyId()).isEqualTo(leaf.getSubjectKeyId());
        assertThat(sameKey.getSerialNumber()).isNotEqualTo(leaf.getSerialNumber());
        assertThat(sameKey.getTrackingId()).isEqualTo(leaf.getTrackingId());
        assertThat(newKeyRenewal.getSubjectKeyId()).isEqualTo(newKey.keyIdentifier());
        assertThat(newKeyRenewal.getTrackingId()).isEqualTo(leaf.getTrackingId());
        assertThat(new X509CertificateHolder(newKeyRenewal.getDer()).getExtension(Extension.subjectAlternativeName))
            .isEqualTo(new X509CertificateHolder(leaf.getDer()).getExtension(Extension.subjectAlternativeName));
        assertThat(validity.length()).isEqualTo(leaf.getValidityPeriod().length());
        assertThatThrownBy(() -> leaf.leafRenewalKey(rsaKey()))
            .hasMessage("The renewal public key must use the original key algorithm.");
        assertThatThrownBy(() -> Cert.renewedLeaf(issueLeaf(sub, subKeys, "CN=other").getDer(), leaf))
            .hasMessage("Renewed certificate must keep the subject");
    }

    private Cert renewLeaf(Cert original, Cert issuer, KeyPair issuerKeys, SubjectPublicKey key,
            ValidityPeriod validity) {
        CertificateTemplate template = original.renewalTemplate(issuer, key, validity, DP);
        TbsCertificate tbs = template.toTbs(SerialNumber.random(new SecureRandom()), TestKeys.ecdsa());
        return Cert.renewedLeaf(tbs.withSignature(TestKeys.sign(tbs, issuerKeys), issuer.getSubjectPublicKey()),
                original);
    }

    private static SubjectPublicKey rsaKey() throws Exception {
        java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return TestKeys.publicKey(generator.generateKeyPair());
    }

    private static Cert issueLeaf(Cert issuer, KeyPair issuerKeys) {
        return issueLeaf(issuer, issuerKeys, "CN=device");
    }

    private static Cert issueLeaf(Cert issuer, KeyPair issuerKeys, String subjectDn) {
        IssuanceOptions options = new IssuanceOptions(NOW, ValidityDuration.from("P1Y"), List.of(), true, List.of(),
                List.of());
        CertProfile profile = CertProfile.create(new CertProfileDraft("leaf-lifecycle", CertType.LEAF,
                ValidityDuration.from("P1Y"), null, SubjectAltNameTemplate.of("DNS:device.example.com"), Set.of(),
                false, List.of(), null, List.of(), false, "", false, true, KeyAlgorithm.of("EC-P-256"), List.of()));
        CertificateTemplate template = profile.subordinateTemplate(issuer.getSubjectDn(), issuer.getSubjectPublicKey(),
                DistinguishedName.of(subjectDn), TestKeys.publicKey(TestKeys.ecP256()), options, DP, null);
        TbsCertificate tbs = template.toTbs(SerialNumber.random(new SecureRandom()), TestKeys.ecdsa());
        return Cert.issuedLeaf(tbs.withSignature(TestKeys.sign(tbs, issuerKeys), issuer.getSubjectPublicKey()), false);
    }

    private Cert renew(Cert original, Cert issuer, KeyPair issuerKeys, KeyPair newKeys, ValidityPeriod validity) {
        CertificateTemplate template = original.renewalTemplate(issuer, TestKeys.publicKey(newKeys), validity, DP);
        TbsCertificate tbs = template.toTbs(SerialNumber.random(new SecureRandom()), TestKeys.ecdsa());
        return Cert.renewedSubCa(tbs.withSignature(TestKeys.sign(tbs, issuerKeys), issuer.getSubjectPublicKey()),
                new PrivateKeyId("k-renewed"), original);
    }

    private static Cert saved(Cert cert) {
        return Cert.restore(10L, cert.getDer(), cert.getCertType(), cert.getStatus(), cert.getPrivateKeyId(), false,
                NOW, cert.getTrackingId(), null);
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
