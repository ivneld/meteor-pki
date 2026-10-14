package com.meteor.pki.domain;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;
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

class CertIssuanceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");

    private static final DistributionPoints DP = new DistributionPoints("http://dp.example.com",
            "http://dp.example.com", "http://dp.example.com", "/api/ocsp", "/api/ca", "/api/crl");

    private static final ValidityPeriod ONE_YEAR = new ValidityPeriod(NOW, NOW.plus(java.time.Duration.ofDays(365)));

    private static final PrivateKeyId ROOT_KEY = new PrivateKeyId("k-root");

    private final KeyPair rootKeys = TestKeys.ecP256();

    private final KeyPair subKeys = TestKeys.ecP256();

    @Test
    void rootIsSelfSignedUnconstrainedCa() throws Exception {
        Cert root = issueRoot();

        X509CertificateHolder holder = new X509CertificateHolder(root.getDer());
        assertThat(root.getIssuerDn()).isEqualTo(root.getSubjectDn());
        assertThat(root.getAuthorityKeyId()).isEqualTo(root.getSubjectKeyId());
        assertThat(root.getSubjectKeyId()).isEqualTo(TestKeys.publicKey(rootKeys).keyIdentifier());
        assertThat(root.isCa()).isTrue();
        assertThat(root.getPathLenConstraint()).isNull();
        assertThat(root.getKeyUsages()).containsExactly(KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN);
        assertThat(holder.getExtension(Extension.keyUsage).isCritical()).isTrue();
        assertThat(holder.getExtension(Extension.basicConstraints).isCritical()).isTrue();
        assertThat(holder.getExtension(Extension.cRLDistributionPoints)).isNull();
        assertThat(root.getNotAfter()).isEqualTo(Instant.parse("2036-10-13T00:00:00Z"));
        assertThat(root.getSerialNumber().toHex()).hasSize(40);
        assertThat(root.getStatusAt(NOW)).isEqualTo(CertStatus.VALID);
    }

    @Test
    void subCaPointsToItsIssuerAndDefaultDistributionPoints() throws Exception {
        Cert root = issueRoot();
        Cert sub = issueSub(root, profile(CertType.SUB, 0, true), true);

        X509CertificateHolder holder = new X509CertificateHolder(sub.getDer());
        String issuerSki = root.getSubjectKeyId().toHex();
        assertThat(sub.getIssuerDn()).isEqualTo(root.getSubjectDn());
        assertThat(sub.getAuthorityKeyId()).isEqualTo(root.getSubjectKeyId());
        assertThat(sub.getPathLenConstraint()).isZero();
        assertThat(CRLDistPoint.fromExtensions(holder.getExtensions()).getDistributionPoints()[0].getDistributionPoint()
            .getName()
            .toString()).contains("http://dp.example.com/api/crl/" + issuerSki + ".crl");
        AuthorityInformationAccess aia = AuthorityInformationAccess.fromExtensions(holder.getExtensions());
        assertThat(aia.getAccessDescriptions()).hasSize(2);
        assertThat(aia.getAccessDescriptions()[0].getAccessLocation()).isEqualTo(new GeneralName(
                GeneralName.uniformResourceIdentifier, "http://dp.example.com/api/ca/" + issuerSki + ".crt"));
        assertThat(aia.getAccessDescriptions()[1].getAccessLocation())
            .isEqualTo(new GeneralName(GeneralName.uniformResourceIdentifier, "http://ocsp.example.com"));
    }

    @Test
    void issuerMustBeValidCaWithKeyAndPathLength() {
        Cert root = issueRoot();
        Cert sub = issueSub(root, profile(CertType.SUB, 0, false), false);

        root.ensureCanIssue(CertType.SUB, ONE_YEAR, NOW);
        assertThatThrownBy(() -> root.ensureCanIssue(CertType.SUB, ONE_YEAR, Instant.parse("2037-01-01T00:00:00Z")))
            .hasMessage("Only VALID certificates can issue certificates.");
        Cert subWithKey = Cert.restore(1L, sub.getDer(), CertType.SUB, CertStatus.VALID, new PrivateKeyId("k-sub"),
                false, NOW, null, null);
        assertThatThrownBy(() -> subWithKey.ensureCanIssue(CertType.SUB, ONE_YEAR, NOW))
            .hasMessage("Issuer certificate cannot issue: pathLenConstraint is 0.");
        subWithKey.ensureCanIssue(CertType.LEAF, ONE_YEAR, NOW);
        Cert withoutKey = Cert.restore(2L, root.getDer(), CertType.ROOT, CertStatus.VALID, null, false, NOW, null,
                null);
        assertThatThrownBy(() -> withoutKey.ensureCanIssue(CertType.SUB, ONE_YEAR, NOW))
            .hasMessage("Issuer private key is not registered.");
        Cert revoked = Cert.restore(3L, root.getDer(), CertType.ROOT, CertStatus.REVOKED, ROOT_KEY, false, NOW, null,
                null);
        assertThatThrownBy(() -> revoked.ensureCanIssue(CertType.SUB, ONE_YEAR, NOW)).isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.CERT_INVALID_INPUT));
    }

    @Test
    void profileMustMatchTheRequestedTypeAndKey() {
        CertProfile sub = profile(CertType.SUB, 0, false);

        assertThatThrownBy(() -> sub.ensureIssues(CertType.ROOT)).isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROFILE_RESOURCE_NOT_FOUND));
        CertProfile rsaRoot = CertProfile.create(new CertProfileDraft("rsa-root", CertType.ROOT,
                ValidityDuration.from("P10Y"), null, SubjectAltNameTemplate.EMPTY, Set.of(), false, List.of(), null,
                List.of(), false, "", false, false, KeyAlgorithm.of("RSA-2048"), List.of()));
        assertThatThrownBy(() -> rsaRoot.rootTemplate(DistinguishedName.of("CN=Root"), TestKeys.publicKey(rootKeys),
                options(null)))
            .hasMessage("The subject public key does not match the profile's key spec.");
    }

    @Test
    void requestedValidityCannotExceedTheProfile() {
        CertProfile root = profile(CertType.ROOT, null, false);

        assertThatThrownBy(() -> root.rootTemplate(DistinguishedName.of("CN=Root"), TestKeys.publicKey(rootKeys),
                options(ValidityDuration.from("P11Y"))))
            .hasMessage("Validity is outside allowed policy range.");
    }

    @Test
    void latestCertificatePerChainIsSelected() {
        Cert root = issueRoot();
        Cert older = Cert.restore(1L, root.getDer(), CertType.ROOT, CertStatus.VALID, ROOT_KEY, false, NOW, null, null);
        Cert newer = Cert.restore(2L, root.getDer(), CertType.ROOT, CertStatus.VALID, ROOT_KEY, false,
                NOW.plusSeconds(1), null, null);
        Cert sub = Cert.restore(3L, issueSub(root, profile(CertType.SUB, 0, false), false).getDer(), CertType.SUB,
                CertStatus.VALID, null, false, NOW, null, null);

        assertThat(CaCertificates.latestPerChain(List.of(newer, older, sub))).extracting(Cert::getId)
            .containsExactly(3L, 2L);
    }

    @Test
    void valueObjectsFollowX509RaFormats() {
        SerialNumber serial = SerialNumber.random(new SecureRandom());
        assertThat(serial.value().toByteArray()).hasSize(20);
        assertThat(SerialNumber.of(serial.toHex().toLowerCase())).isEqualTo(serial);
        assertThat(DownloadFormat.of(null).der()).isFalse();
        assertThat(DownloadFormat.of("DER").der()).isTrue();
        assertThatThrownBy(() -> DownloadFormat.of("p12")).hasMessage("Unsupported certificate download format.");
        assertThat(TestKeys.publicKey(rootKeys).keyAlgorithm().value()).isEqualTo("EC-P-256");
        assertThatThrownBy(() -> DistinguishedName.of(" ")).hasMessage("DistinguishedName must not be blank.");
        assertThat(PrivateKeyId.generate(NOW).value()).startsWith("k-");
    }

    private Cert issueRoot() {
        CertificateTemplate template = profile(CertType.ROOT, null, false).rootTemplate(
                DistinguishedName.of("CN=Test Root,O=Meteor,C=KR"), TestKeys.publicKey(rootKeys), options(null));
        TbsCertificate tbs = template.toTbs(SerialNumber.random(new SecureRandom()), TestKeys.ecdsa());
        return Cert.issuedRoot(tbs.withSignature(TestKeys.sign(tbs, rootKeys), TestKeys.publicKey(rootKeys)), ROOT_KEY);
    }

    private Cert issueSub(Cert root, CertProfile profile, boolean includeDefaultCrlDp) {
        IssuanceOptions options = new IssuanceOptions(NOW, null, List.of(), includeDefaultCrlDp, List.of(), List.of());
        CertificateTemplate template = profile.subordinateTemplate(root.getSubjectDn(), root.getSubjectPublicKey(),
                DistinguishedName.of("CN=Test Sub,O=Meteor,C=KR"), TestKeys.publicKey(subKeys), options, DP, null);
        TbsCertificate tbs = template.toTbs(SerialNumber.random(new SecureRandom()), TestKeys.ecdsa());
        return Cert.issuedSubCa(tbs.withSignature(TestKeys.sign(tbs, rootKeys), root.getSubjectPublicKey()),
                new PrivateKeyId("k-sub"));
    }

    private static IssuanceOptions options(ValidityDuration requested) {
        return new IssuanceOptions(NOW, requested, List.of(), false, List.of(), List.of());
    }

    private static CertProfile profile(CertType type, Integer pathLen, boolean useDefaultAia) {
        return CertProfile.create(new CertProfileDraft(type + "-test", type, ValidityDuration.from("P10Y"), pathLen,
                SubjectAltNameTemplate.EMPTY, Set.of(), false, List.of(), null, List.of("http://ocsp.example.com"),
                false, "", false, useDefaultAia, KeyAlgorithm.of("EC-P-256"), List.of()));
    }

}
