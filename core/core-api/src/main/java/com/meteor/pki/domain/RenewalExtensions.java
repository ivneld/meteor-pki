package com.meteor.pki.domain;

import java.io.IOException;
import java.util.Arrays;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1IA5String;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;

final class RenewalExtensions {

    private RenewalExtensions() {
    }

    static Extensions of(Extensions original, KeyIdentifier previousIssuerKeyId, KeyIdentifier issuerKeyId,
            KeyIdentifier subjectKeyId, DistributionPoints distributionPoints) {
        String previousCrlUrl = distributionPoints.crlUrl(previousIssuerKeyId);
        String crlUrl = distributionPoints.crlUrl(issuerKeyId);
        String previousCaIssuersUrl = distributionPoints.caIssuersUrl(previousIssuerKeyId);
        String caIssuersUrl = distributionPoints.caIssuersUrl(issuerKeyId);
        ExtensionsGenerator generator = new ExtensionsGenerator();
        add(generator, Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(issuerKeyId.value()));
        add(generator, Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(subjectKeyId.value()));
        for (ASN1ObjectIdentifier oid : original.getExtensionOIDs()) {
            Extension extension = original.getExtension(oid);
            if (oid.equals(Extension.authorityKeyIdentifier) || oid.equals(Extension.subjectKeyIdentifier)) {
                continue;
            }
            if (oid.equals(Extension.cRLDistributionPoints)) {
                add(generator, oid, extension.isCritical(),
                        retarget(CRLDistPoint.getInstance(extension.getParsedValue()), previousCrlUrl, crlUrl));
            }
            else if (oid.equals(Extension.authorityInfoAccess)) {
                add(generator, oid, extension.isCritical(),
                        retarget(AuthorityInformationAccess.getInstance(extension.getParsedValue()),
                                previousCaIssuersUrl, caIssuersUrl));
            }
            else {
                generator.addExtension(extension);
            }
        }
        return generator.generate();
    }

    private static CRLDistPoint retarget(CRLDistPoint distPoint, String from, String to) {
        return new CRLDistPoint(Arrays.stream(distPoint.getDistributionPoints()).map(point -> {
            DistributionPointName name = point.getDistributionPoint();
            if (name == null || name.getType() != DistributionPointName.FULL_NAME) {
                return point;
            }
            GeneralName[] names = Arrays.stream(GeneralNames.getInstance(name.getName()).getNames())
                .map(generalName -> retarget(generalName, from, to))
                .toArray(GeneralName[]::new);
            return new DistributionPoint(new DistributionPointName(new GeneralNames(names)), point.getReasons(),
                    point.getCRLIssuer());
        }).toArray(DistributionPoint[]::new));
    }

    private static AuthorityInformationAccess retarget(AuthorityInformationAccess access, String from, String to) {
        return new AuthorityInformationAccess(Arrays.stream(access.getAccessDescriptions())
            .map(description -> description.getAccessMethod().equals(AccessDescription.id_ad_caIssuers)
                    ? new AccessDescription(description.getAccessMethod(),
                            retarget(description.getAccessLocation(), from, to))
                    : description)
            .toArray(AccessDescription[]::new));
    }

    private static GeneralName retarget(GeneralName name, String from, String to) {
        if (name.getTagNo() == GeneralName.uniformResourceIdentifier
                && ASN1IA5String.getInstance(name.getName()).getString().equals(from)) {
            return new GeneralName(GeneralName.uniformResourceIdentifier, to);
        }
        return name;
    }

    private static void add(ExtensionsGenerator generator, ASN1ObjectIdentifier oid, boolean critical,
            ASN1Encodable value) {
        try {
            generator.addExtension(oid, critical, value);
        }
        catch (IOException ex) {
            throw new CoreException(ErrorCode.CERT_OPERATION_FAILED, "Failed to encode certificate extension fields.");
        }
    }

}
