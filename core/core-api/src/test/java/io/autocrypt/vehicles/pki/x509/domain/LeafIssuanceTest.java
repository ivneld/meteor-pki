package io.autocrypt.vehicles.pki.x509.domain;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import io.autocrypt.vehicles.pki.x509.enums.CertStatus;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Leaf 인증서 본문과 CSR 처리. CSR 은 서명을 검증하고 주체 DN, 공개키, SAN 만 쓴다.
 */
class LeafIssuanceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");

    private static final DistributionPoints DP = new DistributionPoints("http://dp.example.com",
            "http://dp.example.com", "http://dp.example.com", "/api/ocsp", "/api/ca", "/api/crl");

    private final KeyPair issuerKeys = TestKeys.ecP256();

    private final KeyPair subjectKeys = TestKeys.ecP256();

    @Test
    void csrSignatureIsVerifiedAndItsSubjectKeyAndSanAreRead() {
        CertificationRequest request = CertificationRequest
            .fromPem(TestKeys.csrPem("CN=device-1,O=Autocrypt", subjectKeys, "device-1.example.com"));

        assertThat(request.subjectDn().value()).isEqualTo("CN=device-1,O=Autocrypt");
        assertThat(request.publicKey()).isEqualTo(TestKeys.publicKey(subjectKeys));
        assertThat(request.requestedSubjectAltName().getExtnId()).isEqualTo(Extension.subjectAlternativeName);
        assertThat(CertificationRequest.fromPem(TestKeys.csrPem("CN=no-san", subjectKeys)).requestedSubjectAltName())
            .isNull();
    }

    @Test
    void malformedOrTamperedCsrIsRejected() {
        assertThatThrownBy(() -> CertificationRequest.fromPem("not a pem")).hasMessage("Invalid PEM format");
        assertThatThrownBy(() -> CertificationRequest
            .fromPem("-----BEGIN CERTIFICATE REQUEST-----\nAAAA\n-----END CERTIFICATE REQUEST-----"))
            .hasMessage("Failed to convert PEM to object.");

        String pem = TestKeys.csrPem("CN=device-1", subjectKeys);
        byte[] der = Base64.getMimeDecoder()
            .decode(pem.replace("-----BEGIN CERTIFICATE REQUEST-----", "")
                .replace("-----END CERTIFICATE REQUEST-----", "")
                .trim());
        der[der.length - 5] ^= 0x01;
        assertThatThrownBy(() -> new CertificationRequest(der)).hasMessage("Failed to verify P10CR signature.");
    }

    @Test
    void leafUsesProfileSanUnlessTheCsrRequestsOne() throws Exception {
        CertProfile profile = leafProfile("DNS:profile.example.com", false);
        CertificationRequest request = CertificationRequest
            .fromPem(TestKeys.csrPem("CN=device-1", subjectKeys, "csr.example.com"));

        X509CertificateHolder fromProfile = new X509CertificateHolder(
                issue(profile, TestKeys.publicKey(subjectKeys), null));
        X509CertificateHolder fromCsr = new X509CertificateHolder(
                issue(profile, request.publicKey(), request.requestedSubjectAltName()));

        assertThat(GeneralNames.fromExtensions(fromProfile.getExtensions(), Extension.subjectAlternativeName)
            .getNames()[0]).isEqualTo(new GeneralName(GeneralName.dNSName, "profile.example.com"));
        assertThat(GeneralNames.fromExtensions(fromCsr.getExtensions(), Extension.subjectAlternativeName).getNames())
            .containsExactly(new GeneralName(GeneralName.dNSName, "csr.example.com"));
        List<?> oids = fromCsr.getExtensionOIDs();
        assertThat(oids.get(oids.size() - 1)).isEqualTo(Extension.subjectAlternativeName);
    }

    @Test
    void leafIsAnEndEntityPendingWhenTheProfileRequiresApproval() {
        Cert approved = Cert.issuedLeaf(issue(leafProfile("", false), TestKeys.publicKey(subjectKeys), null), false);
        Cert pending = Cert.issuedLeaf(issue(leafProfile("", true), TestKeys.publicKey(subjectKeys), null), true);

        assertThat(approved.isCa()).isFalse();
        assertThat(approved.getKeyUsages()).containsExactly(KeyUsage.DIGITAL_SIGNATURE);
        assertThat(approved.getExtendedKeyUsages()).containsExactly("1.3.6.1.5.5.7.3.1", "1.3.6.1.5.5.7.3.2");
        assertThat(approved.getAuthorityKeyId()).isEqualTo(TestKeys.publicKey(issuerKeys).keyIdentifier());
        assertThat(approved.getStatus()).isEqualTo(CertStatus.VALID);
        assertThat(pending.getStatus()).isEqualTo(CertStatus.PENDING);
    }

    @Test
    void subjectKeyMustMatchTheProfileKeyAlgorithm() {
        CertProfile rsaLeaf = CertProfile.create(new CertProfileDraft("rsa-leaf", CertType.LEAF,
                ValidityDuration.from("P1Y"), null, SubjectAltNameTemplate.EMPTY, Set.of(), false, List.of(), null,
                List.of(), false, "", false, false, KeyAlgorithm.of("RSA-2048"), List.of()));

        assertThatThrownBy(() -> issue(rsaLeaf, TestKeys.publicKey(subjectKeys), null))
            .hasMessage("The subject public key does not match the profile's key spec.");
    }

    private byte[] issue(CertProfile profile, SubjectPublicKey subjectKey, Extension requestedSan) {
        SubjectPublicKey issuerKey = TestKeys.publicKey(issuerKeys);
        CertificateTemplate template = profile.subordinateTemplate(DistinguishedName.of("CN=Issuing CA"), issuerKey,
                DistinguishedName.of("CN=device-1"), subjectKey,
                new IssuanceOptions(NOW, null, List.of(), true, List.of(), List.of()), DP, requestedSan);
        TbsCertificate tbs = template.toTbs(SerialNumber.random(new SecureRandom()), TestKeys.ecdsa());
        return tbs.withSignature(TestKeys.sign(tbs, issuerKeys), issuerKey);
    }

    private static CertProfile leafProfile(String san, boolean requireApproval) {
        return CertProfile.create(new CertProfileDraft("leaf", CertType.LEAF, ValidityDuration.from("P1Y"), null,
                SubjectAltNameTemplate.of(san), Set.of(KeyUsage.DIGITAL_SIGNATURE), true,
                List.of(new ProfileEku("1.3.6.1.5.5.7.3.2", "client"), new ProfileEku("1.3.6.1.5.5.7.3.1", "server")),
                null, List.of(), false, "", requireApproval, false, KeyAlgorithm.of("EC-P-256"), List.of()));
    }

}
