package com.meteor.pki.usecase;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import com.meteor.pki.ContextTest;
import com.meteor.pki.domain.Cert;
import com.meteor.pki.domain.CertProfile;
import com.meteor.pki.domain.CertReader;
import com.meteor.pki.domain.CertificationRequest;
import com.meteor.pki.domain.DistinguishedName;
import com.meteor.pki.domain.KeyAlgorithm;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.SubjectAltNameTemplate;
import com.meteor.pki.domain.TestKeys;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertIssuanceUseCaseIT extends ContextTest {

    private final CertProfileUseCase certProfileUseCase;

    private final CertIssuanceUseCase certIssuanceUseCase;

    private final CertReader certReader;

    CertIssuanceUseCaseIT(CertProfileUseCase certProfileUseCase, CertIssuanceUseCase certIssuanceUseCase,
            CertReader certReader) {
        this.certProfileUseCase = certProfileUseCase;
        this.certIssuanceUseCase = certIssuanceUseCase;
        this.certReader = certReader;
    }

    @Test
    void rootAndTwoSubCasFormAValidChain() throws Exception {
        CertProfile rootProfile = profile("chain-root", CertType.ROOT, null, "EC-P-384");
        CertProfile subProfile = profile("chain-sub", CertType.SUB, 1, "EC-P-256");
        CertProfile issuingProfile = profile("chain-issuing", CertType.SUB, 0, "RSA-2048");

        Cert root = certIssuanceUseCase.issueRoot(rootCommand(rootProfile, "CN=Chain Root,O=Meteor,C=KR"));
        Cert sub = certIssuanceUseCase.issueSubCa(subCommand(subProfile, root.getSubjectKeyId(), "CN=Chain Sub"));
        Cert issuing = certIssuanceUseCase
            .issueSubCa(subCommand(issuingProfile, sub.getSubjectKeyId(), "CN=Chain Issuing"));

        assertThat(root.getId()).isNotNull();
        assertThat(root.getPrivateKeyId()).isNotNull();
        assertThat(issuing.getAuthorityKeyId()).isEqualTo(sub.getSubjectKeyId());
        assertThat(issuing.getKeyAlgorithm().value()).isEqualTo("RSA-2048");
        assertThat(certReader.getCa(issuing.getSubjectKeyId()).getId()).isEqualTo(issuing.getId());
        validateChain(root, sub, issuing);
    }

    @Test
    void issuerWithPathLengthZeroCannotIssueAnotherCa() {
        CertProfile rootProfile = profile("path-root", CertType.ROOT, null, "EC-P-256");
        CertProfile leafCaProfile = profile("path-sub", CertType.SUB, 0, "EC-P-256");
        Cert root = certIssuanceUseCase.issueRoot(rootCommand(rootProfile, "CN=Path Root"));
        Cert sub = certIssuanceUseCase.issueSubCa(subCommand(leafCaProfile, root.getSubjectKeyId(), "CN=Path Sub"));

        assertThatThrownBy(
                () -> certIssuanceUseCase.issueSubCa(subCommand(leafCaProfile, sub.getSubjectKeyId(), "CN=Too Deep")))
            .isInstanceOf(CoreException.class)
            .hasMessage("Issuer certificate cannot issue: pathLenConstraint is 0.");
    }

    @Test
    void profileOfAnotherTypeAndUnknownIssuerAreNotFound() {
        CertProfile subProfile = profile("wrong-type-sub", CertType.SUB, 0, "EC-P-256");

        assertThatThrownBy(() -> certIssuanceUseCase.issueRoot(rootCommand(subProfile, "CN=Wrong")))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROFILE_RESOURCE_NOT_FOUND));
        assertThatThrownBy(
                () -> certIssuanceUseCase.issueSubCa(subCommand(subProfile, KeyIdentifier.of("0A0B0C"), "CN=Orphan")))
            .isInstanceOf(CoreException.class)
            .hasMessage("Failed to find a CA certificate matching the AKI.");
    }

    @Test
    void leafCompletesTheChainAndFollowsTheProfileApproval() throws Exception {
        CertProfile rootProfile = profile("leaf-root", CertType.ROOT, null, "EC-P-256");
        CertProfile issuingProfile = profile("leaf-issuing", CertType.SUB, 0, "EC-P-256");
        Cert root = certIssuanceUseCase.issueRoot(rootCommand(rootProfile, "CN=Leaf Root"));
        Cert issuing = certIssuanceUseCase
            .issueSubCa(subCommand(issuingProfile, root.getSubjectKeyId(), "CN=Leaf Issuing"));
        CertProfile tls = leafProfile("leaf-tls", false);
        CertProfile approval = leafProfile("leaf-approval", true);
        KeyPair deviceKeys = TestKeys.ecP256();

        Cert leaf = certIssuanceUseCase.issueLeaf(leafCommand(tls, issuing.getSubjectKeyId(), deviceKeys));
        Cert pending = certIssuanceUseCase.issueLeaf(leafCommand(approval, issuing.getSubjectKeyId(), deviceKeys));
        Cert fromCsr = certIssuanceUseCase.issueLeafFromP10cr(new LeafP10crIssueCommand(tls.getId(),
                CertificationRequest.fromPem(TestKeys.csrPem("CN=device-csr", deviceKeys, "device.example.com")),
                issuing.getSubjectKeyId(), List.of(), false, List.of(), List.of(), ValidityDuration.from("P30D"),
                null));

        assertThat(leaf.getStatus()).isEqualTo(CertStatus.VALID);
        assertThat(pending.getStatus()).isEqualTo(CertStatus.PENDING);
        assertThat(fromCsr.getSubjectDn().value()).isEqualTo("CN=device-csr");
        assertThat(fromCsr.getNotAfter()).isEqualTo(fromCsr.getNotBefore().plus(java.time.Duration.ofDays(30)));
        validateChain(root, issuing, leaf);
        validateChain(root, issuing, fromCsr);
    }

    @Test
    void leafKeyMustMatchTheProfile() {
        CertProfile rootProfile = profile("mismatch-root", CertType.ROOT, null, "EC-P-256");
        Cert root = certIssuanceUseCase.issueRoot(rootCommand(rootProfile, "CN=Mismatch Root"));
        CertProfile rsaLeaf = certProfileUseCase.create(new CertProfileCreateCommand("mismatch-leaf", CertType.LEAF,
                ValidityDuration.from("P1Y"), null, SubjectAltNameTemplate.EMPTY, Set.of(), false, List.of(), null,
                List.of(), false, false, false, KeyAlgorithm.of("RSA-2048"), List.of()));

        assertThatThrownBy(
                () -> certIssuanceUseCase.issueLeaf(leafCommand(rsaLeaf, root.getSubjectKeyId(), TestKeys.ecP256())))
            .isInstanceOf(CoreException.class)
            .hasMessage("The subject public key does not match the profile's key spec.");
    }

    @Test
    void issuanceMustStayWithinTheIssuerValidity() {
        CertProfile rootProfile = profile("window-root", CertType.ROOT, null, "EC-P-256");
        CertProfile subProfile = profile("window-sub", CertType.SUB, 0, "EC-P-256");
        Cert root = certIssuanceUseCase.issueRoot(new RootIssueCommand(rootProfile.getId(),
                DistinguishedName.of("CN=Window Root"), ValidityDuration.from("P3Y"), List.of(), List.of(), null));
        Cert sub = certIssuanceUseCase.issueSubCa(
                new SubCaIssueCommand(subProfile.getId(), DistinguishedName.of("CN=Window Sub"), root.getSubjectKeyId(),
                        List.of(), false, List.of(), List.of(), ValidityDuration.from("P2Y"), null));

        assertThatThrownBy(
                () -> certIssuanceUseCase.issueSubCa(subCommand(subProfile, root.getSubjectKeyId(), "CN=Too Long")))
            .isInstanceOf(CoreException.class)
            .hasMessage("Requested validity is outside the issuer's validity period.");
        assertThatThrownBy(() -> certIssuanceUseCase.issueSubCa(new SubCaIssueCommand(subProfile.getId(),
                DistinguishedName.of("CN=Too Early"), root.getSubjectKeyId(), List.of(), false, List.of(), List.of(),
                ValidityDuration.from("P1Y"), root.getNotBefore().minusSeconds(60))))
            .hasMessage("Requested validity is outside the issuer's validity period.");
        assertThatThrownBy(
                () -> certIssuanceUseCase.issueLeaf(new LeafIssueCommand(leafProfile("window-leaf", false).getId(),
                        DistinguishedName.of("CN=device"), sub.getSubjectKeyId(), TestKeys.publicKey(TestKeys.ecP256()),
                        List.of(), false, List.of(), List.of(), ValidityDuration.from("P1Y"),
                        sub.getNotAfter().minus(java.time.Duration.ofDays(30)))))
            .hasMessage("Requested validity is outside the issuer's validity period.");
    }

    private CertProfile leafProfile(String name, boolean requireApproval) {
        return certProfileUseCase.create(new CertProfileCreateCommand(name, CertType.LEAF, ValidityDuration.from("P1Y"),
                null, SubjectAltNameTemplate.of("DNS:device.example.com"), Set.of(KeyUsage.DIGITAL_SIGNATURE), true,
                List.of("1.3.6.1.5.5.7.3.2"), null, List.of(), true, requireApproval, true, KeyAlgorithm.of("EC-P-256"),
                List.of()));
    }

    private static LeafIssueCommand leafCommand(CertProfile profile, KeyIdentifier issuer, KeyPair keys) {
        return new LeafIssueCommand(profile.getId(), DistinguishedName.of("CN=device"), issuer,
                TestKeys.publicKey(keys), List.of(), true, List.of(), List.of(), null, null);
    }

    private CertProfile profile(String name, CertType type, Integer pathLen, String keyAlgorithm) {
        return certProfileUseCase.create(new CertProfileCreateCommand(name, type, ValidityDuration.from("P10Y"),
                pathLen, SubjectAltNameTemplate.EMPTY, Set.of(), false, List.of(), null, List.of(), true, false, true,
                KeyAlgorithm.of(keyAlgorithm), List.of()));
    }

    private static RootIssueCommand rootCommand(CertProfile profile, String subjectDn) {
        return new RootIssueCommand(profile.getId(), DistinguishedName.of(subjectDn), null, List.of(), List.of(), null);
    }

    private static SubCaIssueCommand subCommand(CertProfile profile, KeyIdentifier issuer, String subjectDn) {
        return new SubCaIssueCommand(profile.getId(), DistinguishedName.of(subjectDn), issuer, List.of(), true,
                List.of(), List.of(), ValidityDuration.from("P5Y"), null);
    }

    private static void validateChain(Cert root, Cert... chain) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate anchor = toX509(factory, root);
        List<X509Certificate> path = new java.util.ArrayList<>();
        for (int i = chain.length - 1; i >= 0; i--) {
            path.add(toX509(factory, chain[i]));
        }
        PKIXParameters parameters = new PKIXParameters(Set.of(new TrustAnchor(anchor, null)));
        parameters.setRevocationEnabled(false);
        CertPathValidator.getInstance("PKIX").validate(factory.generateCertPath(path), parameters);
    }

    private static X509Certificate toX509(CertificateFactory factory, Cert cert) throws Exception {
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(cert.getDer()));
    }

}
