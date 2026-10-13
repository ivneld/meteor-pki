package io.autocrypt.vehicles.pki.x509.usecase;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

import io.autocrypt.vehicles.pki.x509.client.crypto.CryptoClient;
import io.autocrypt.vehicles.pki.x509.client.crypto.CryptoClientException;
import io.autocrypt.vehicles.pki.x509.domain.Cert;
import io.autocrypt.vehicles.pki.x509.domain.CertProfile;
import io.autocrypt.vehicles.pki.x509.domain.CertProfileReader;
import io.autocrypt.vehicles.pki.x509.domain.CertReader;
import io.autocrypt.vehicles.pki.x509.domain.CertWriter;
import io.autocrypt.vehicles.pki.x509.domain.CertificateTemplate;
import io.autocrypt.vehicles.pki.x509.domain.CertificationRequest;
import io.autocrypt.vehicles.pki.x509.domain.DistinguishedName;
import io.autocrypt.vehicles.pki.x509.domain.DistributionPoints;
import io.autocrypt.vehicles.pki.x509.domain.IssuanceOptions;
import io.autocrypt.vehicles.pki.x509.domain.KeyAlgorithm;
import io.autocrypt.vehicles.pki.x509.domain.KeyIdentifier;
import io.autocrypt.vehicles.pki.x509.domain.PrivateKeyId;
import io.autocrypt.vehicles.pki.x509.domain.SerialNumber;
import io.autocrypt.vehicles.pki.x509.domain.SubjectPublicKey;
import io.autocrypt.vehicles.pki.x509.domain.TbsCertificate;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import org.bouncycastle.asn1.x509.Extension;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 인증서 발급 유스케이스. 프로파일·발급자 인증서(두 애그리거트)와 키 보관소(외부 시스템)를 엮는다.
 *
 * <p>
 * 키 생성과 서명은 HSM/KMS 자리의 외부 호출이므로 트랜잭션 밖에서 하고, 다 만든 인증서를 저장하는 구간만
 * {@link TransactionTemplate} 으로 감싼다. 조회는 각 Reader 의 읽기 전용 트랜잭션에서 끝난다.
 *
 * <pre>
 * 트랜잭션(읽기)  프로파일 확인, 발급자 확인
 * 트랜잭션 밖     (CA 면) 키 생성 → 인증서 본문 → 서명 → 서명 검증
 *                 (Leaf 는 신청자의 공개키를 쓰고 키를 만들지 않는다)
 * 트랜잭션        인증서 저장
 * </pre>
 *
 * 서명 전에는 아무것도 저장하지 않으므로 실패하면 다시 요청하면 된다. 저장이 실패하면 새로 만든 CA 키만 키 보관소에 남는다. 이 키는 어떤 인증서에도
 * 묶이지 않아 쓰이지 않는다(x509-ra 와 같다).
 */
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

    /** 새 키로 자기 서명 Root CA 인증서를 발급한다. */
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

    /** 새 키로 Sub CA 인증서를 만들어 발급자 CA 의 키로 서명한다. */
    public Cert issueSubCa(SubCaIssueCommand command) {
        Instant now = clock.instant();
        Cert issuer = certReader.getIssuer(command.issuerKeyId(), CertType.SUB, now);
        CertProfile profile = certProfileReader.getForIssuance(command.certProfileId(), CertType.SUB);

        PrivateKeyId keyId = PrivateKeyId.generate(now);
        SubjectPublicKey publicKey = generateKeyPair(keyId, profile.getKeyAlgorithm());
        CertificateTemplate template = profile.subordinateTemplate(issuer.getSubjectDn(), issuer.getSubjectPublicKey(),
                command.subjectDn(), publicKey, command.options(now), distributionPoints, null);
        byte[] der = sign(template, issuer.getPrivateKeyId(), issuer.getSubjectPublicKey());
        Cert subCa = Cert.issuedSubCa(der, keyId);

        return transaction.execute(status -> certWriter.register(subCa));
    }

    /**
     * 신청자의 공개키로 Leaf 인증서를 발급한다. 개인키는 신청자가 갖고 있으므로 키를 만들지 않는다. 프로파일이 승인을 요구하면 서명까지 하고
     * PENDING 으로 저장한다.
     */
    public Cert issueLeaf(LeafIssueCommand command) {
        return issueLeaf(command.certProfileId(), command.issuerKeyId(), command.subjectDn(), command.publicKey(),
                command.options(clock.instant()), null);
    }

    /** CSR 로 Leaf 인증서를 발급한다. 주체 DN 과 공개키는 CSR 의 것을, SAN 은 CSR 이 요청했으면 그것을 쓴다. */
    public Cert issueLeafFromP10cr(LeafP10crIssueCommand command) {
        CertificationRequest request = command.certificationRequest();
        return issueLeaf(command.certProfileId(), command.issuerKeyId(), request.subjectDn(), request.publicKey(),
                command.options(clock.instant()), request.requestedSubjectAltName());
    }

    private Cert issueLeaf(Long certProfileId, KeyIdentifier issuerKeyId, DistinguishedName subjectDn,
            SubjectPublicKey publicKey, IssuanceOptions options, Extension requestedSubjectAltName) {
        Cert issuer = certReader.getIssuer(issuerKeyId, CertType.LEAF, clock.instant());
        CertProfile profile = certProfileReader.getForIssuance(certProfileId, CertType.LEAF);

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

    /** 서명 키의 알고리즘으로 본문에 서명하고, 서명 키의 공개키로 검증한 인증서 DER 를 돌려준다. */
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
