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
import com.meteor.pki.domain.CertWriter;
import com.meteor.pki.domain.CertificationRequest;
import com.meteor.pki.domain.DistinguishedName;
import com.meteor.pki.domain.KeyAlgorithm;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.SubjectAltNameTemplate;
import com.meteor.pki.domain.SubjectPublicKey;
import com.meteor.pki.domain.TestKeys;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.CrlReason;
import com.meteor.pki.enums.KeyUsage;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertIssuanceUseCaseIT extends ContextTest {

    private final CertProfileUseCase certProfileUseCase;

    private final CertIssuanceUseCase certIssuanceUseCase;

    private final CertReader certReader;

    private final CertWriter certWriter;

    private final PlatformTransactionManager transactionManager;

    CertIssuanceUseCaseIT(CertProfileUseCase certProfileUseCase, CertIssuanceUseCase certIssuanceUseCase,
            CertReader certReader, CertWriter certWriter, PlatformTransactionManager transactionManager) {
        this.certProfileUseCase = certProfileUseCase;
        this.certIssuanceUseCase = certIssuanceUseCase;
        this.certReader = certReader;
        this.certWriter = certWriter;
        this.transactionManager = transactionManager;
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
    void subCaRenewalUsesANewKeyAndSupersedesTheOriginal() throws Exception {
        CertProfile rootProfile = profile("renew-root", CertType.ROOT, null, "EC-P-384");
        CertProfile subProfile = profile("renew-sub", CertType.SUB, 0, "RSA-2048");
        Cert root = certIssuanceUseCase.issueRoot(rootCommand(rootProfile, "CN=Renew Root"));
        Cert sub = certIssuanceUseCase.issueSubCa(subCommand(subProfile, root.getSubjectKeyId(), "CN=Renew Sub"));

        Cert renewed = certIssuanceUseCase.renewSubCa(new SubCaRenewCommand(sub.getSerialNumber(),
                root.getSubjectKeyId(), 0, ValidityDuration.from("P2Y"), true, false));

        Cert original = certReader.get(root.getSubjectKeyId(), sub.getSerialNumber());
        assertThat(renewed.getSubjectDn()).isEqualTo(sub.getSubjectDn());
        assertThat(renewed.getSubjectKeyId()).isNotEqualTo(sub.getSubjectKeyId());
        assertThat(renewed.getPrivateKeyId()).isNotEqualTo(sub.getPrivateKeyId());
        assertThat(renewed.getKeyAlgorithm().value()).isEqualTo("RSA-2048");
        assertThat(renewed.getTrackingId()).isEqualTo(sub.getTrackingId());
        assertThat(renewed.getNotAfter())
            .isEqualTo(renewed.getNotBefore().atZone(java.time.ZoneOffset.UTC).plusYears(2).toInstant());
        assertThat(original.getStatus()).isEqualTo(CertStatus.REVOKED);
        assertThat(original.getRevocation().reason()).isEqualTo(CrlReason.SUPERSEDED);
        validateChain(root, renewed);
        Cert leaf = certIssuanceUseCase
            .issueLeaf(leafCommand(leafProfile("renew-leaf", false), renewed.getSubjectKeyId(), TestKeys.ecP256()));
        validateChain(root, renewed, leaf);
        assertThatThrownBy(() -> certIssuanceUseCase
            .renewSubCa(new SubCaRenewCommand(sub.getSerialNumber(), root.getSubjectKeyId(), 0, null, false, false)))
            .hasMessage("Cannot renew invalid certificate.");
        assertThatThrownBy(() -> certIssuanceUseCase
            .renewSubCa(new SubCaRenewCommand(root.getSerialNumber(), root.getSubjectKeyId(), 0, null, false, false)))
            .hasMessage("Certificate type does not match the renewal.");
    }

    @Test
    void latestIssuerInTheLineageSignsTheRenewal() throws Exception {
        CertProfile rootProfile = profile("lineage-root", CertType.ROOT, null, "EC-P-256");
        CertProfile midProfile = profile("lineage-mid", CertType.SUB, 1, "EC-P-256");
        CertProfile issuingProfile = profile("lineage-issuing", CertType.SUB, 0, "EC-P-256");
        Cert root = certIssuanceUseCase.issueRoot(rootCommand(rootProfile, "CN=Lineage Root"));
        Cert mid = certIssuanceUseCase.issueSubCa(subCommand(midProfile, root.getSubjectKeyId(), "CN=Lineage Mid"));
        Cert issuing = certIssuanceUseCase
            .issueSubCa(new SubCaIssueCommand(issuingProfile.getId(), DistinguishedName.of("CN=Lineage Issuing"),
                    mid.getSubjectKeyId(), List.of(), true, List.of(), List.of(), ValidityDuration.from("P2Y"), null));
        Cert renewedMid = certIssuanceUseCase.renewSubCa(new SubCaRenewCommand(mid.getSerialNumber(),
                root.getSubjectKeyId(), 0, ValidityDuration.from("P4Y"), false, false));

        Cert sameIssuer = certIssuanceUseCase.renewSubCa(new SubCaRenewCommand(issuing.getSerialNumber(),
                mid.getSubjectKeyId(), 0, ValidityDuration.from("P1Y"), false, false));
        Cert latestIssuer = certIssuanceUseCase.renewSubCa(new SubCaRenewCommand(issuing.getSerialNumber(),
                mid.getSubjectKeyId(), 0, ValidityDuration.from("P1Y"), false, true));

        assertThat(sameIssuer.getAuthorityKeyId()).isEqualTo(mid.getSubjectKeyId());
        assertThat(latestIssuer.getAuthorityKeyId()).isEqualTo(renewedMid.getSubjectKeyId());
        assertThat(new String(latestIssuer.getDer(), java.nio.charset.StandardCharsets.ISO_8859_1))
            .contains("/api/crl/" + renewedMid.getSubjectKeyId().toHex() + ".crl")
            .doesNotContain(mid.getSubjectKeyId().toHex() + ".crl");
        validateChain(root, mid, sameIssuer);
        validateChain(root, renewedMid, latestIssuer);
    }

    @Test
    void issuanceAndRenewalMustStayWithinTheIssuerValidity() {
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
        assertThatThrownBy(() -> certIssuanceUseCase.renewSubCa(new SubCaRenewCommand(sub.getSerialNumber(),
                root.getSubjectKeyId(), 0, ValidityDuration.from("P4Y"), false, false)))
            .hasMessage("Requested validity is outside the issuer's validity period.");
        assertThatThrownBy(() -> certIssuanceUseCase
            .renewSubCa(new SubCaRenewCommand(sub.getSerialNumber(), root.getSubjectKeyId(), 1, null, false, false)))
            .hasMessage("Requested validity is outside the issuer's validity period.");
        assertThatThrownBy(
                () -> certIssuanceUseCase.issueLeaf(new LeafIssueCommand(leafProfile("window-leaf", false).getId(),
                        DistinguishedName.of("CN=device"), sub.getSubjectKeyId(), TestKeys.publicKey(TestKeys.ecP256()),
                        List.of(), false, List.of(), List.of(), ValidityDuration.from("P1Y"),
                        sub.getNotAfter().minus(java.time.Duration.ofDays(30)))))
            .hasMessage("Requested validity is outside the issuer's validity period.");
        assertThat(certReader.get(root.getSubjectKeyId(), sub.getSerialNumber()).getStatus())
            .isEqualTo(CertStatus.VALID);
    }

    @Test
    void revokedSubCaCanNoLongerIssue() {
        CertProfile rootProfile = profile("revoke-root", CertType.ROOT, null, "EC-P-256");
        CertProfile subProfile = profile("revoke-sub", CertType.SUB, 0, "EC-P-256");
        Cert root = certIssuanceUseCase.issueRoot(rootCommand(rootProfile, "CN=Revoke Root"));
        Cert sub = certIssuanceUseCase.issueSubCa(subCommand(subProfile, root.getSubjectKeyId(), "CN=Revoke Sub"));

        Cert revoked = certWriter.revoke(root.getSubjectKeyId(), sub.getSerialNumber(), CertType.SUB,
                CrlReason.KEY_COMPROMISE);

        assertThat(revoked.getStatus()).isEqualTo(CertStatus.REVOKED);
        assertThat(certReader.get(root.getSubjectKeyId(), sub.getSerialNumber()).getRevocation())
            .isEqualTo(revoked.getRevocation());
        assertThatThrownBy(() -> certIssuanceUseCase
            .issueLeaf(leafCommand(leafProfile("revoke-leaf", false), sub.getSubjectKeyId(), TestKeys.ecP256())))
            .hasMessage("Failed to find a CA certificate matching the AKI.");
        assertThatThrownBy(() -> certWriter.revoke(root.getSubjectKeyId(), root.getSerialNumber(), CertType.SUB,
                CrlReason.KEY_COMPROMISE))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode())
                .isEqualTo(ErrorCode.CERT_REVOKE_ROOT_FORBIDDEN));
        assertThatThrownBy(() -> certWriter.revoke(root.getSubjectKeyId(), sub.getSerialNumber(), CertType.SUB,
                CrlReason.KEY_COMPROMISE))
            .hasMessage("Can not revoke certificate");
    }

    @Test
    void concurrentRevocationOfTheSameCertificateIsRejected() {
        CertProfile rootProfile = profile("lock-root", CertType.ROOT, null, "EC-P-256");
        CertProfile subProfile = profile("lock-sub", CertType.SUB, 0, "EC-P-256");
        Cert root = certIssuanceUseCase.issueRoot(rootCommand(rootProfile, "CN=Lock Root"));
        Cert sub = certIssuanceUseCase.issueSubCa(subCommand(subProfile, root.getSubjectKeyId(), "CN=Lock Sub"));
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            certReader.get(root.getSubjectKeyId(), sub.getSerialNumber());
            inner.executeWithoutResult(other -> certWriter.revoke(root.getSubjectKeyId(), sub.getSerialNumber(),
                    CertType.SUB, CrlReason.KEY_COMPROMISE));
            certWriter.revoke(root.getSubjectKeyId(), sub.getSerialNumber(), CertType.SUB, CrlReason.SUPERSEDED);
        })).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(certReader.get(root.getSubjectKeyId(), sub.getSerialNumber()).getRevocation().reason())
            .isEqualTo(CrlReason.KEY_COMPROMISE);
    }

    @Test
    void rootRenewalUsesANewKeyAndBecomesTheLatestIssuerInTheLineage() throws Exception {
        CertProfile rootProfile = profile("root-renew-root", CertType.ROOT, null, "EC-P-256");
        CertProfile subProfile = profile("root-renew-sub", CertType.SUB, 0, "EC-P-256");
        Cert root = certIssuanceUseCase.issueRoot(rootCommand(rootProfile, "CN=Root Renew"));
        Cert sub = certIssuanceUseCase.issueSubCa(subCommand(subProfile, root.getSubjectKeyId(), "CN=Root Renew Sub"));

        Cert renewedRoot = certIssuanceUseCase.renewRoot(
                new RootRenewCommand(root.getSerialNumber(), root.getSubjectKeyId(), 0, ValidityDuration.from("P20Y")));

        assertThat(renewedRoot.getCertType()).isEqualTo(CertType.ROOT);
        assertThat(renewedRoot.getSubjectDn()).isEqualTo(root.getSubjectDn());
        assertThat(renewedRoot.getSubjectKeyId()).isNotEqualTo(root.getSubjectKeyId());
        assertThat(renewedRoot.getAuthorityKeyId()).isEqualTo(renewedRoot.getSubjectKeyId());
        assertThat(renewedRoot.getPrivateKeyId()).isNotEqualTo(root.getPrivateKeyId());
        assertThat(renewedRoot.getTrackingId()).isEqualTo(root.getTrackingId());
        assertThat(certReader.get(root.getSubjectKeyId(), root.getSerialNumber()).getStatus())
            .isEqualTo(CertStatus.VALID);
        validateChain(renewedRoot);
        Cert subUnderNewRoot = certIssuanceUseCase.renewSubCa(new SubCaRenewCommand(sub.getSerialNumber(),
                root.getSubjectKeyId(), 0, ValidityDuration.from("P5Y"), true, true));
        assertThat(subUnderNewRoot.getAuthorityKeyId()).isEqualTo(renewedRoot.getSubjectKeyId());
        validateChain(renewedRoot, subUnderNewRoot);
        assertThatThrownBy(() -> certIssuanceUseCase
            .renewRoot(new RootRenewCommand(sub.getSerialNumber(), root.getSubjectKeyId(), 0, null)))
            .hasMessage("Certificate type does not match the renewal.");
    }

    @Test
    void leafRenewalKeepsTheLineageAndOptionallySupersedesTheOriginal() throws Exception {
        CertProfile rootProfile = profile("leaf-renew-root", CertType.ROOT, null, "EC-P-256");
        CertProfile subProfile = profile("leaf-renew-sub", CertType.SUB, 0, "EC-P-256");
        Cert root = certIssuanceUseCase.issueRoot(rootCommand(rootProfile, "CN=Leaf Renew Root"));
        Cert sub = certIssuanceUseCase.issueSubCa(subCommand(subProfile, root.getSubjectKeyId(), "CN=Leaf Renew Sub"));
        Cert leaf = certIssuanceUseCase
            .issueLeaf(leafCommand(leafProfile("leaf-renew-leaf", false), sub.getSubjectKeyId(), TestKeys.ecP256()));

        Cert sameKey = certIssuanceUseCase
            .renewLeaf(new LeafRenewCommand(leaf.getSerialNumber(), sub.getSubjectKeyId(), 0, null, false, null));
        SubjectPublicKey newKey = TestKeys.publicKey(TestKeys.ecP256());
        Cert rekeyed = certIssuanceUseCase.renewLeaf(new LeafRenewCommand(leaf.getSerialNumber(), sub.getSubjectKeyId(),
                0, ValidityDuration.from("P6M"), true, newKey));

        assertThat(sameKey.getSubjectKeyId()).isEqualTo(leaf.getSubjectKeyId());
        assertThat(sameKey.getTrackingId()).isEqualTo(leaf.getTrackingId());
        assertThat(rekeyed.getSubjectKeyId()).isEqualTo(newKey.keyIdentifier());
        assertThat(rekeyed.getTrackingId()).isEqualTo(leaf.getTrackingId());
        assertThat(rekeyed.getSubjectDn()).isEqualTo(leaf.getSubjectDn());
        assertThat(certReader.get(sub.getSubjectKeyId(), leaf.getSerialNumber()).getRevocation().reason())
            .isEqualTo(CrlReason.SUPERSEDED);
        assertThat(certReader.get(sub.getSubjectKeyId(), sameKey.getSerialNumber()).getStatus())
            .isEqualTo(CertStatus.VALID);
        validateChain(root, sub, rekeyed);
        assertThatThrownBy(() -> certIssuanceUseCase
            .renewLeaf(new LeafRenewCommand(leaf.getSerialNumber(), sub.getSubjectKeyId(), 0, null, false, null)))
            .hasMessage("Cannot renew invalid certificate.");
        assertThatThrownBy(() -> certIssuanceUseCase.renewLeaf(new LeafRenewCommand(sameKey.getSerialNumber(),
                sub.getSubjectKeyId(), 0, ValidityDuration.from("P10Y"), false, null)))
            .hasMessage("Requested validity is outside the issuer's validity period.");
        assertThatThrownBy(() -> certIssuanceUseCase
            .renewLeaf(new LeafRenewCommand(sub.getSerialNumber(), root.getSubjectKeyId(), 0, null, false, null)))
            .hasMessage("Certificate type does not match the renewal.");
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
