package com.meteor.pki.ui.view;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.meteor.pki.domain.Cert;
import com.meteor.pki.domain.Revocation;
import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;

public record CertView(Long id, CertType certType, CertStatus status, String subjectDn, String issuerDn,
        String serialHex, String akiHex, String skiHex, String notBefore, String notAfter, long daysLeft,
        String keyAlgorithm, String signatureAlgorithm, String trackingId, Integer pathLenConstraint, boolean ca,
        boolean selfSigned, List<KeyUsage> keyUsages, List<String> extendedKeyUsages, String revocationReason,
        String revokedAt, String createdAt, String pem) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault());

    public static CertView of(Cert cert, Instant now) {
        Revocation revocation = cert.getRevocation();
        return new CertView(cert.getId(), cert.getCertType(), cert.getStatusAt(now), cert.getSubjectDn().value(),
                cert.getIssuerDn().value(), cert.getSerialNumber().toHex(), cert.getAuthorityKeyId().toHex(),
                cert.getSubjectKeyId().toHex(), format(cert.getNotBefore()), format(cert.getNotAfter()),
                Duration.between(now, cert.getNotAfter()).toDays(), cert.getKeyAlgorithm().value(),
                cert.getSignatureAlgorithm().getJcaName(), cert.getTrackingId(), cert.getPathLenConstraint(),
                cert.isCa(), cert.getAuthorityKeyId().equals(cert.getSubjectKeyId()), cert.getKeyUsages(),
                cert.getExtendedKeyUsages(), revocation == null ? null : revocation.reason().name(),
                revocation == null ? null : format(revocation.revokedAt()),
                cert.getCreatedAt() == null ? null : format(cert.getCreatedAt()), cert.getPem());
    }

    private static String format(Instant instant) {
        return FORMAT.format(instant);
    }

    public String detailUrl() {
        return "/ui/certificates/" + akiHex + "/" + serialHex;
    }

    public String downloadUrl(String format) {
        return "/api/certificates/" + akiHex + "/download?serialNumberHex=" + serialHex + "&format=" + format;
    }

    public String commonName() {
        for (String part : subjectDn.split(",")) {
            String trimmed = part.trim();
            if (trimmed.regionMatches(true, 0, "CN=", 0, 3)) {
                return trimmed.substring(3);
            }
        }
        return subjectDn;
    }

    public String shortTrackingId() {
        return trackingId.length() > 12 ? trackingId.substring(0, 12) : trackingId;
    }

    public boolean valid() {
        return status == CertStatus.VALID;
    }

    public boolean expiringSoon() {
        return valid() && daysLeft <= 30;
    }

    public boolean canIssueCa() {
        return valid() && ca && (pathLenConstraint == null || pathLenConstraint > 0);
    }

    public boolean canIssueLeaf() {
        return valid() && ca;
    }

}
