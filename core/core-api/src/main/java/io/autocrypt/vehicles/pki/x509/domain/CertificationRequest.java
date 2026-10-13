package io.autocrypt.vehicles.pki.x509.domain;

import java.io.IOException;
import java.security.Provider;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;

/**
 * 인증서 서명 요청(PKCS#10 CSR). 만들 때 요청자의 서명을 검증해 공개키에 맞는 개인키를 가졌는지(proof of possession) 확인한다.
 *
 * <p>
 * 발급에 쓰는 것은 주체 DN, 공개키, 요청한 SAN 확장뿐이다. 그 밖에 요청한 확장(키 용도, EKU ...)은 프로파일이 정하므로
 * 무시한다(x509-ra 와 같다).
 */
public record CertificationRequest(byte[] der) {

    private static final Provider PROVIDER = new BouncyCastleProvider();

    private static final Pattern PEM = Pattern
        .compile("-----BEGIN (.+?)-----\\s+([A-Za-z0-9+/=\\r\\n]+?)-----END \\1-----");

    public CertificationRequest {
        der = der.clone();
        verifySignature(parse(der));
    }

    public static CertificationRequest fromPem(String pem) {
        Matcher matcher = PEM.matcher(pem == null ? "" : pem.trim());
        if (!matcher.find()) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Invalid PEM format");
        }
        byte[] der;
        try {
            der = Base64.getMimeDecoder().decode(matcher.group(2));
        }
        catch (IllegalArgumentException ex) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Invalid PEM format");
        }
        return new CertificationRequest(der);
    }

    @Override
    public byte[] der() {
        return der.clone();
    }

    public DistinguishedName subjectDn() {
        return DistinguishedName.from(parse(der).getSubject());
    }

    public SubjectPublicKey publicKey() {
        return SubjectPublicKey.from(parse(der).getSubjectPublicKeyInfo());
    }

    /** CSR 이 요청한 SAN 확장. 없으면 null. */
    public Extension requestedSubjectAltName() {
        Extensions extensions = parse(der).getRequestedExtensions();
        return extensions == null ? null : extensions.getExtension(Extension.subjectAlternativeName);
    }

    private static PKCS10CertificationRequest parse(byte[] der) {
        try {
            return new PKCS10CertificationRequest(der);
        }
        catch (IOException | RuntimeException ex) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Failed to convert PEM to object.");
        }
    }

    private static void verifySignature(PKCS10CertificationRequest request) {
        boolean valid;
        try {
            valid = request.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider(PROVIDER)
                .build(request.getSubjectPublicKeyInfo()));
        }
        catch (OperatorCreationException | PKCSException | RuntimeException ex) {
            valid = false;
        }
        if (!valid) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Failed to verify P10CR signature.");
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CertificationRequest that && Arrays.equals(der, that.der);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(der);
    }

    @Override
    public String toString() {
        return "CertificationRequest[" + subjectDn().value() + "]";
    }

}
