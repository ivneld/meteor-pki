package com.meteor.pki.usecase;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

import com.meteor.pki.client.crypto.CryptoClient;
import com.meteor.pki.client.crypto.CryptoClientException;
import com.meteor.pki.domain.Cert;
import com.meteor.pki.domain.CertProfile;
import com.meteor.pki.domain.CertProfileReader;
import com.meteor.pki.domain.CertReader;
import com.meteor.pki.domain.CertWriter;
import com.meteor.pki.domain.CertificateTemplate;
import com.meteor.pki.domain.CertificationRequest;
import com.meteor.pki.domain.DistinguishedName;
import com.meteor.pki.domain.DistributionPoints;
import com.meteor.pki.domain.IssuanceOptions;
import com.meteor.pki.domain.KeyAlgorithm;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.PrivateKeyId;
import com.meteor.pki.domain.SerialNumber;
import com.meteor.pki.domain.SubjectPublicKey;
import com.meteor.pki.domain.TbsCertificate;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.bouncycastle.asn1.x509.Extension;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CertIssuanceUseCase {

    private final CertProfileReader certProfileReader;

    private final CertReader certReader;

    private final CertWriter certWriter;

    private final CryptoClient cryptoClient;

    private final DistributionPoints distributionPoints;

    private final TransactionTemplate transaction;

    private final Clock clock;

    private final SecureRandom random = new SecureRandom();

    public CertIssuanceUseCase(CertProfileReader certProfileReader, CertReader certReader, CertWriter certWriter,
            CryptoClient cryptoClient, DistributionPoints distributionPoints, TransactionTemplate transaction,
            Clock clock) {
        this.certProfileReader = certProfileReader;
        this.certReader = certReader;
        this.certWriter = certWriter;
        this.cryptoClient = cryptoClient;
        this.distributionPoints = distributionPoints;
        this.transaction = transaction;
        this.clock = clock;
    }

    public Cert issueRoot(RootIssueCommand command) {
        Instant now = clock.instant();
        CertProfile profile = certProfileReader.getForIssuance(command.certProfileId(), CertType.ROOT);

        PrivateKeyId keyId = PrivateKeyId.generate(now);
        SubjectPublicKey publicKey = generateKeyPair(keyId, profile.getKeyAlgorithm());
        CertificateTemplate template = profile.rootTemplate(command.subjectDn(), publicKey, command.options(now));
        byte[] der = sign(template, keyId, publicKey);
        Cert root = Cert.issuedRoot(der, keyId);

        return transaction.execute(status -> certWriter.register(root));
    }

    public Cert issueSubCa(SubCaIssueCommand command) {
        Instant now = clock.instant();
        Cert issuer = certReader.getIssuer(command.issuerKeyId());
        CertProfile profile = certProfileReader.getForIssuance(command.certProfileId(), CertType.SUB);
        IssuanceOptions options = command.options(now);
        issuer.ensureCanIssue(CertType.SUB, profile.validityFor(options), now);

        PrivateKeyId keyId = PrivateKeyId.generate(now);
        SubjectPublicKey publicKey = generateKeyPair(keyId, profile.getKeyAlgorithm());
        CertificateTemplate template = profile.subordinateTemplate(issuer.getSubjectDn(), issuer.getSubjectPublicKey(),
                command.subjectDn(), publicKey, options, distributionPoints, null);
        byte[] der = sign(template, issuer.getPrivateKeyId(), issuer.getSubjectPublicKey());
        Cert subCa = Cert.issuedSubCa(der, keyId);

        return transaction.execute(status -> certWriter.register(subCa));
    }

    public Cert issueLeaf(LeafIssueCommand command) {
        return issueLeaf(command.certProfileId(), command.issuerKeyId(), command.subjectDn(), command.publicKey(),
                command.options(clock.instant()), null);
    }

    public Cert issueLeafFromP10cr(LeafP10crIssueCommand command) {
        CertificationRequest request = command.certificationRequest();
        return issueLeaf(command.certProfileId(), command.issuerKeyId(), request.subjectDn(), request.publicKey(),
                command.options(clock.instant()), request.requestedSubjectAltName());
    }

    private Cert issueLeaf(Long certProfileId, KeyIdentifier issuerKeyId, DistinguishedName subjectDn,
            SubjectPublicKey publicKey, IssuanceOptions options, Extension requestedSubjectAltName) {
        Cert issuer = certReader.getIssuer(issuerKeyId);
        CertProfile profile = certProfileReader.getForIssuance(certProfileId, CertType.LEAF);
        issuer.ensureCanIssue(CertType.LEAF, profile.validityFor(options), clock.instant());

        CertificateTemplate template = profile.subordinateTemplate(issuer.getSubjectDn(), issuer.getSubjectPublicKey(),
                subjectDn, publicKey, options, distributionPoints, requestedSubjectAltName);
        byte[] der = sign(template, issuer.getPrivateKeyId(), issuer.getSubjectPublicKey());
        Cert leaf = Cert.issuedLeaf(der, profile.isRequireApproval());

        return transaction.execute(status -> certWriter.register(leaf));
    }

    private SubjectPublicKey generateKeyPair(PrivateKeyId keyId, KeyAlgorithm algorithm) {
        try {
            return new SubjectPublicKey(cryptoClient.generateKeyPair(keyId.value(), algorithm.spec()));
        }
        catch (CryptoClientException ex) {
            CoreException failure = new CoreException(ErrorCode.CERT_OPERATION_FAILED, "Failed to generate key pair.");
            failure.initCause(ex);
            throw failure;
        }
    }

    private byte[] sign(CertificateTemplate template, PrivateKeyId signingKeyId, SubjectPublicKey signingPublicKey) {
        TbsCertificate tbs = template.toTbs(SerialNumber.random(random),
                signingPublicKey.keyAlgorithm().signatureAlgorithm());
        byte[] signature;
        try {
            signature = cryptoClient.sign(signingKeyId.value(), tbs.signatureAlgorithm(), tbs.toBeSigned());
        }
        catch (CryptoClientException ex) {
            CoreException failure = new CoreException(ErrorCode.CERT_SIGN_FAILED, "Failed to sign certificate.");
            failure.initCause(ex);
            throw failure;
        }
        return tbs.withSignature(signature, signingPublicKey);
    }

}
