package io.autocrypt.vehicles.pki.x509.domain;

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

import io.autocrypt.vehicles.pki.x509.enums.CertStatus;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;
import io.autocrypt.vehicles.pki.x509.enums.SignatureAlgorithm;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * 발급한 인증서. 내용은 DER 가 원본이고 나머지 값은 DER 에서 읽는다. 상태(VALID, PENDING ...)와 서명 키 이름은 이 서버가 관리한다.
 *
 * <p>
 * CA 인증서가 다른 인증서를 발급할 수 있는지는 {@link #ensureCanIssue} 가 판단한다.
 */
public class Cert {

    /** CA 인증서의 종류. */
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

    /** 발급한 Root CA 인증서. 자기 서명이고 경로 길이 제한이 없는 CA 여야 한다. */
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

    /** 발급한 Sub CA 인증서. */
    public static Cert issuedSubCa(byte[] der, PrivateKeyId privateKeyId) {
        Cert cert = new Cert(null, der, CertType.SUB, CertStatus.VALID, privateKeyId, false, null);
        if (!cert.isCa()) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "BasicConstraints indicates non-CA certificate (cA=false or absent)");
        }
        return cert;
    }

    /** 발급한 Leaf 인증서. 승인이 필요하면 PENDING 으로 둔다. 개인키는 신청자가 갖고 있다. */
    public static Cert issuedLeaf(byte[] der, boolean pendingApproval) {
        Cert cert = new Cert(null, der, CertType.LEAF, pendingApproval ? CertStatus.PENDING : CertStatus.VALID, null,
                false, null);
        if (cert.isCa()) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "End-entity certificate expected, but BasicConstraints cA=true");
        }
        return cert;
    }

    /** 저장소에서 복원한다. CertRepository 만 호출한다. */
    public static Cert restore(Long id, byte[] der, CertType certType, CertStatus status, PrivateKeyId privateKeyId,
            boolean hidden, Instant createdAt) {
        return new Cert(id, der, certType, status, privateKeyId, hidden, createdAt);
    }

    /**
     * 이 인증서로 target 종류의 인증서를 발급할 수 있는지 확인한다. 지금 유효한 CA 이고 서명 키가 있어야 하며, CA 를 발급하려면 경로 길이
     * 제한이 0 이 아니어야 한다.
     */
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

    /** now 시점의 상태. 저장된 상태와 무관하게 유효기간이 지났으면 EXPIRED 다. */
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

    /** 저장된 상태. 만료 여부를 반영한 상태는 {@link #getStatusAt}. */
    public CertStatus getStatus() {
        return status;
    }

    /** 서명 키 이름. CA 만 갖는다. */
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

    /** SKI 확장 값. 없으면 공개키의 SHA-1. */
    public KeyIdentifier getSubjectKeyId() {
        SubjectKeyIdentifier ski = SubjectKeyIdentifier.fromExtensions(holder.getExtensions());
        return ski == null ? getSubjectPublicKey().keyIdentifier() : new KeyIdentifier(ski.getKeyIdentifier());
    }

    /** AKI 확장의 keyIdentifier. 없으면(자기 서명) SKI 와 같다. */
    public KeyIdentifier getAuthorityKeyId() {
        AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.fromExtensions(holder.getExtensions());
        return aki == null || aki.getKeyIdentifier() == null ? getSubjectKeyId()
                : new KeyIdentifier(aki.getKeyIdentifier());
    }

    /** CA 의 경로 길이 제한. 제한이 없거나 CA 가 아니면 null. */
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

    /** enum 선언 순서. */
    public List<KeyUsage> getKeyUsages() {
        org.bouncycastle.asn1.x509.KeyUsage keyUsage = org.bouncycastle.asn1.x509.KeyUsage
            .fromExtensions(holder.getExtensions());
        return keyUsage == null ? List.of() : KeyUsageBits.fromExtension(keyUsage);
    }

    /** OID 문자열 정렬. */
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

    /** 인증서 DER 의 SHA-256(소문자 16진수). 같은 인증서를 서버 밖에서 가리킬 때 쓴다. */
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
