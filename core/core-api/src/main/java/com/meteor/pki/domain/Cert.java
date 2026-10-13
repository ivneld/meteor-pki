package com.meteor.pki.domain;

import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;
import com.meteor.pki.enums.SignatureAlgorithm;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

public class Cert {

    public static final Set<CertType> CA_TYPES = EnumSet.of(CertType.ROOT, CertType.SUB);

    private final Long id;

    private final byte[] der;

    private final X509CertificateHolder holder;

    private final CertType certType;

    private final CertStatus status;

    private final PrivateKeyId privateKeyId;

    private final boolean hidden;

    private final Instant createdAt;

    private Cert(Long id, byte[] der, CertType certType, CertStatus status, PrivateKeyId privateKeyId, boolean hidden,
            Instant createdAt) {
        this.id = id;
        this.der = der.clone();
        this.holder = parse(der);
        this.certType = certType;
        this.status = status;
        this.privateKeyId = privateKeyId;
        this.hidden = hidden;
        this.createdAt = createdAt;
    }

    public static Cert issuedRoot(byte[] der, PrivateKeyId privateKeyId) {
        Cert cert = new Cert(null, der, CertType.ROOT, CertStatus.VALID, privateKeyId, false, null);
        BasicConstraints basicConstraints = cert.basicConstraints();
        if (basicConstraints == null || !basicConstraints.isCA() || basicConstraints.getPathLenConstraint() != null) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "BasicConstraints must indicate an unconstrained CA");
        }
        if (!cert.getIssuerDn().equals(cert.getSubjectDn())
                || !cert.getAuthorityKeyId().equals(cert.getSubjectKeyId())) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "Self-signed certificate must have the same issuer and subject");
        }
        return cert;
    }

    public static Cert issuedSubCa(byte[] der, PrivateKeyId privateKeyId) {
        Cert cert = new Cert(null, der, CertType.SUB, CertStatus.VALID, privateKeyId, false, null);
        if (!cert.isCa()) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "BasicConstraints indicates non-CA certificate (cA=false or absent)");
        }
        return cert;
    }

    public static Cert issuedLeaf(byte[] der, boolean pendingApproval) {
        Cert cert = new Cert(null, der, CertType.LEAF, pendingApproval ? CertStatus.PENDING : CertStatus.VALID, null,
                false, null);
        if (cert.isCa()) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "End-entity certificate expected, but BasicConstraints cA=true");
        }
        return cert;
    }

    public static Cert restore(Long id, byte[] der, CertType certType, CertStatus status, PrivateKeyId privateKeyId,
            boolean hidden, Instant createdAt) {
        return new Cert(id, der, certType, status, privateKeyId, hidden, createdAt);
    }

    public void ensureCanIssue(CertType target, Instant now) {
        if (getStatusAt(now) != CertStatus.VALID) {
            throw notEligible("Only VALID certificates can issue certificates.");
        }
        if (!CA_TYPES.contains(certType) || !isCa()) {
            throw notEligible("Only CA certificates can issue.");
        }
        Integer pathLen = getPathLenConstraint();
        if (target != CertType.LEAF && pathLen != null && pathLen == 0) {
            throw notEligible("Issuer certificate cannot issue: pathLenConstraint is 0.");
        }
        if (privateKeyId == null) {
            throw notEligible("Issuer private key is not registered.");
        }
    }

    private CoreException notEligible(String message) {
        return new CoreException(ErrorCode.CERT_INVALID_INPUT, message).property("skiHex", getSubjectKeyId().toHex());
    }

    public CertStatus getStatusAt(Instant now) {
        return now.isAfter(getNotAfter()) ? CertStatus.EXPIRED : status;
    }

    public boolean isNew() {
        return id == null;
    }

    public boolean isCa() {
        BasicConstraints basicConstraints = basicConstraints();
        return basicConstraints != null && basicConstraints.isCA();
    }

    public Long getId() {
        return id;
    }

    public byte[] getDer() {
        return der.clone();
    }

    public String getPem() {
        StringWriter writer = new StringWriter();
        try (PemWriter pem = new PemWriter(writer)) {
            pem.writeObject(new PemObject("CERTIFICATE", der));
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to encode PEM", ex);
        }
        return writer.toString();
    }

    public CertType getCertType() {
        return certType;
    }

    public CertStatus getStatus() {
        return status;
    }

    public PrivateKeyId getPrivateKeyId() {
        return privateKeyId;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public SerialNumber getSerialNumber() {
        return new SerialNumber(holder.getSerialNumber());
    }

    public DistinguishedName getIssuerDn() {
        return DistinguishedName.from(holder.getIssuer());
    }

    public DistinguishedName getSubjectDn() {
        return DistinguishedName.from(holder.getSubject());
    }

    public Instant getNotBefore() {
        return holder.getNotBefore().toInstant();
    }

    public Instant getNotAfter() {
        return holder.getNotAfter().toInstant();
    }

    public SubjectPublicKey getSubjectPublicKey() {
        return SubjectPublicKey.from(holder.getSubjectPublicKeyInfo());
    }

    public KeyAlgorithm getKeyAlgorithm() {
        return getSubjectPublicKey().keyAlgorithm();
    }

    public KeyIdentifier getSubjectKeyId() {
        SubjectKeyIdentifier ski = SubjectKeyIdentifier.fromExtensions(holder.getExtensions());
        return ski == null ? getSubjectPublicKey().keyIdentifier() : new KeyIdentifier(ski.getKeyIdentifier());
    }

    public KeyIdentifier getAuthorityKeyId() {
        AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.fromExtensions(holder.getExtensions());
        return aki == null || aki.getKeyIdentifier() == null ? getSubjectKeyId()
                : new KeyIdentifier(aki.getKeyIdentifier());
    }

    public Integer getPathLenConstraint() {
        BasicConstraints basicConstraints = basicConstraints();
        return basicConstraints == null || !basicConstraints.isCA() || basicConstraints.getPathLenConstraint() == null
                ? null : basicConstraints.getPathLenConstraint().intValueExact();
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        String oid = holder.getSignatureAlgorithm().getAlgorithm().getId();
        return Arrays.stream(SignatureAlgorithm.values())
            .filter(algorithm -> algorithm.getOid().equals(oid))
            .findFirst()
            .orElseThrow(
                    () -> new CoreException(ErrorCode.CERT_INVALID_INPUT, "Unsupported signature algorithm: " + oid));
    }

    public List<KeyUsage> getKeyUsages() {
        org.bouncycastle.asn1.x509.KeyUsage keyUsage = org.bouncycastle.asn1.x509.KeyUsage
            .fromExtensions(holder.getExtensions());
        return keyUsage == null ? List.of() : KeyUsageBits.fromExtension(keyUsage);
    }

    public List<String> getExtendedKeyUsages() {
        ExtendedKeyUsage eku = ExtendedKeyUsage.fromExtensions(holder.getExtensions());
        if (eku == null) {
            return List.of();
        }
        List<String> oids = new ArrayList<>();
        for (KeyPurposeId purpose : eku.getUsages()) {
            oids.add(purpose.getId());
        }
        return oids.stream().sorted().toList();
    }

    public String getTrackingId() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(der));
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private BasicConstraints basicConstraints() {
        return BasicConstraints.fromExtensions(holder.getExtensions());
    }

    private static X509CertificateHolder parse(byte[] der) {
        try {
            return new X509CertificateHolder(der);
        }
        catch (IOException ex) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Invalid certificate.");
        }
    }

}
