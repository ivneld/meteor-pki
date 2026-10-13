package com.meteor.pki.domain;

import java.io.IOException;
import java.security.Provider;

import com.meteor.pki.enums.SignatureAlgorithm;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
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

public record TbsCertificate(TBSCertificate tbs, AlgorithmIdentifier algorithmIdentifier,
        SignatureAlgorithm signatureAlgorithm) {

    private static final Provider PROVIDER = new BouncyCastleProvider();

    public byte[] toBeSigned() {
        try {
            return tbs.getEncoded();
        }
        catch (IOException ex) {
            throw new CoreException(ErrorCode.CERT_OPERATION_FAILED, "Failed to encode certificate.");
        }
    }

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
