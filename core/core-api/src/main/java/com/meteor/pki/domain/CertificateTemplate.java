package com.meteor.pki.domain;

import java.util.Date;

import com.meteor.pki.enums.SignatureAlgorithm;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;

public record CertificateTemplate(DistinguishedName issuerDn, DistinguishedName subjectDn,
        SubjectPublicKey subjectPublicKey, ValidityPeriod validity, Extensions extensions) {

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

    private static AlgorithmIdentifier algorithmIdentifier(SignatureAlgorithm signatureAlgorithm) {
        try {
            return new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm.getJcaName());
        }
        catch (IllegalArgumentException ex) {
            return new AlgorithmIdentifier(new ASN1ObjectIdentifier(signatureAlgorithm.getOid()));
        }
    }

}
