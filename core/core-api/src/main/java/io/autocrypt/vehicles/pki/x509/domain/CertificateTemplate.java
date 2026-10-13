package io.autocrypt.vehicles.pki.x509.domain;

import java.util.Date;

import io.autocrypt.vehicles.pki.x509.enums.SignatureAlgorithm;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;

/**
 * 서명하기 전의 인증서 내용. 프로파일이 만든다({@link CertProfile#rootTemplate},
 * {@link CertProfile#subordinateTemplate}).
 */
public record CertificateTemplate(DistinguishedName issuerDn, DistinguishedName subjectDn,
        SubjectPublicKey subjectPublicKey, ValidityPeriod validity, Extensions extensions) {

    /** 일련번호와 서명 알고리즘을 정해 서명할 TBSCertificate 를 만든다. 시각은 초 단위로 잘린다. */
    public TbsCertificate toTbs(SerialNumber serialNumber, SignatureAlgorithm signatureAlgorithm) {
        AlgorithmIdentifier algorithm = algorithmIdentifier(signatureAlgorithm);
        V3TBSCertificateGenerator generator = new V3TBSCertificateGenerator();
        generator.setSerialNumber(new ASN1Integer(serialNumber.value()));
        generator.setSignature(algorithm);
        generator.setIssuer(issuerDn.toX500Name());
        generator.setStartDate(new Time(Date.from(validity.notBefore())));
        generator.setEndDate(new Time(Date.from(validity.notAfter())));
        generator.setSubject(subjectDn.toX500Name());
        generator.setSubjectPublicKeyInfo(subjectPublicKey.toSpki());
        generator.setExtensions(extensions);
        TBSCertificate tbs = generator.generateTBSCertificate();
        return new TbsCertificate(tbs, algorithm, signatureAlgorithm);
    }

    /** RSA 는 NULL 파라미터를 붙이는 표준 식별자(RFC 4055), 그 밖은 OID 만 쓴다. */
    private static AlgorithmIdentifier algorithmIdentifier(SignatureAlgorithm signatureAlgorithm) {
        try {
            return new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm.getJcaName());
        }
        catch (IllegalArgumentException ex) {
            return new AlgorithmIdentifier(new ASN1ObjectIdentifier(signatureAlgorithm.getOid()));
        }
    }

}
