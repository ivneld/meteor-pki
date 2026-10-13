package io.autocrypt.vehicles.pki.x509.domain;

import java.io.IOException;
import java.security.Provider;

import io.autocrypt.vehicles.pki.x509.enums.SignatureAlgorithm;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.cert.CertException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;

/**
 * 서명할 인증서 본문(TBSCertificate). 서명은 키를 가진 쪽(키 보관소)이 하고, 받은 서명으로 인증서를 완성한다.
 */
public record TbsCertificate(TBSCertificate tbs, AlgorithmIdentifier algorithmIdentifier,
        SignatureAlgorithm signatureAlgorithm) {

    private static final Provider PROVIDER = new BouncyCastleProvider();

    /** 서명할 바이트(DER). */
    public byte[] toBeSigned() {
        try {
            return tbs.getEncoded();
        }
        catch (IOException ex) {
            throw new CoreException(ErrorCode.CERT_OPERATION_FAILED, "Failed to encode certificate.");
        }
    }

    /**
     * 서명을 붙여 인증서 DER 를 만든다. 발급자 공개키로 서명을 검증해, 키 보관소가 엉뚱한 키로 서명했으면 거절한다.
     */
    public byte[] withSignature(byte[] signature, SubjectPublicKey issuerPublicKey) {
        ASN1EncodableVector sequence = new ASN1EncodableVector();
        sequence.add(tbs);
        sequence.add(algorithmIdentifier);
        sequence.add(new DERBitString(signature));
        X509CertificateHolder certificate = new X509CertificateHolder(
                Certificate.getInstance(new DERSequence(sequence)));
        try {
            if (!certificate.isSignatureValid(
                    new JcaContentVerifierProviderBuilder().setProvider(PROVIDER).build(issuerPublicKey.toSpki()))) {
                throw new CoreException(ErrorCode.CERT_OPERATION_FAILED,
                        "Issued certificate signature verification failed");
            }
            return certificate.getEncoded();
        }
        catch (CertException | OperatorCreationException | IOException ex) {
            CoreException failure = new CoreException(ErrorCode.CERT_OPERATION_FAILED,
                    "Issued certificate signature verification failed");
            failure.initCause(ex);
            throw failure;
        }
    }

}
