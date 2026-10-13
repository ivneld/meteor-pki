package com.meteor.pki.domain;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;

public class CertProfile {

    public static final int MAX_PATH_LENGTH = 5;

    private static final int MAX_NAME_LENGTH = 100;

    private static final Set<KeyUsage> CA_REQUIRED_KEY_USAGES = EnumSet.of(KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN);

    private final Long id;

    private final String name;

    private final CertType certType;

    private final ValidityDuration validityDuration;

    private final Integer pathLenConstraint;

    private final Set<KeyUsage> keyUsages;

    private final boolean keyUsageCritical;

    private final List<ProfileEku> extendedKeyUsages;

    private final CertificatePolicies certificatePolicies;

    private final SubjectAltNameTemplate subjectAltNameTemplate;

    private final KeyAlgorithm keyAlgorithm;

    private final boolean requireApproval;

    private final boolean useDefaultAiaCaIssuer;

    private final List<String> ocspUris;

    private final Instant createdAt;

    private final Instant updatedAt;

    private CertProfile(Long id, String name, CertType certType, ValidityDuration validityDuration,
            Integer pathLenConstraint, Set<KeyUsage> keyUsages, boolean keyUsageCritical,
            List<ProfileEku> extendedKeyUsages, CertificatePolicies certificatePolicies,
            SubjectAltNameTemplate subjectAltNameTemplate, KeyAlgorithm keyAlgorithm, boolean requireApproval,
            boolean useDefaultAiaCaIssuer, List<String> ocspUris, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.certType = certType;
        this.validityDuration = validityDuration;
        this.pathLenConstraint = pathLenConstraint;
        this.keyUsages = keyUsages.isEmpty() ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(keyUsages));
        this.keyUsageCritical = keyUsageCritical;
        this.extendedKeyUsages = List.copyOf(extendedKeyUsages);
        this.certificatePolicies = certificatePolicies;
        this.subjectAltNameTemplate = subjectAltNameTemplate;
        this.keyAlgorithm = keyAlgorithm;
        this.requireApproval = requireApproval;
        this.useDefaultAiaCaIssuer = useDefaultAiaCaIssuer;
        this.ocspUris = List.copyOf(ocspUris);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CertProfile create(CertProfileDraft draft) {
        if (draft.name().length() > MAX_NAME_LENGTH) {
            throw new CoreException(ErrorCode.PROFILE_INVALID_INPUT,
                    "Certificate profile name must be at most 100 characters.");
        }
        if (!draft.customExtensionOids().isEmpty()) {
            throw new CoreException(ErrorCode.PROFILE_INVALID_INPUT, "Custom OID not found or inactive.");
        }
        List<String> collectedOcspUris = collectOcspUris(draft);
        return switch (draft.certType()) {
            case ROOT -> new CertProfile(null, draft.name(), CertType.ROOT, draft.validityDuration(), null,
                    withCaKeyUsages(draft.keyUsages()), true, draft.extendedKeyUsages(), draft.certificatePolicies(),
                    SubjectAltNameTemplate.EMPTY, draft.keyAlgorithm(), false, false, List.of(), null, null);
            case SUB -> new CertProfile(null, draft.name(), CertType.SUB, draft.validityDuration(),
                    subPathLen(draft.pathLenConstraint()), withCaKeyUsages(draft.keyUsages()), true,
                    draft.extendedKeyUsages(), draft.certificatePolicies(), SubjectAltNameTemplate.EMPTY,
                    draft.keyAlgorithm(), false, draft.useDefaultAiaCaIssuer(), collectedOcspUris, null, null);
            case LEAF -> new CertProfile(null, draft.name(), CertType.LEAF, draft.validityDuration(), null,
                    withoutCaKeyUsages(draft.keyUsages()), draft.keyUsageCritical(), draft.extendedKeyUsages(),
                    draft.certificatePolicies(), draft.subjectAltNameTemplate(), draft.keyAlgorithm(),
                    draft.requireApproval(), draft.useDefaultAiaCaIssuer(), collectedOcspUris, null, null);
        };
    }

    public static CertProfile restore(Long id, String name, CertType certType, ValidityDuration validityDuration,
            Integer pathLenConstraint, Set<KeyUsage> keyUsages, boolean keyUsageCritical,
            List<ProfileEku> extendedKeyUsages, CertificatePolicies certificatePolicies,
            SubjectAltNameTemplate subjectAltNameTemplate, KeyAlgorithm keyAlgorithm, boolean requireApproval,
            boolean useDefaultAiaCaIssuer, List<String> ocspUris, Instant createdAt, Instant updatedAt) {
        return new CertProfile(id, name, certType, validityDuration, pathLenConstraint, keyUsages, keyUsageCritical,
                extendedKeyUsages, certificatePolicies, subjectAltNameTemplate, keyAlgorithm, requireApproval,
                useDefaultAiaCaIssuer, ocspUris, createdAt, updatedAt);
    }

    private static Integer subPathLen(Integer requested) {
        int pathLen = requested == null ? 0 : requested;
        if (pathLen < 0) {
            throw new CoreException(ErrorCode.PROFILE_INVALID_INPUT,
                    "pathLenConstraint must be greater than or equal to 0.");
        }
        if (pathLen > MAX_PATH_LENGTH) {
            throw new CoreException(ErrorCode.PROFILE_INVALID_INPUT,
                    "Exceeded the maximum allowed certificate chain length.");
        }
        return pathLen;
    }

    private static Set<KeyUsage> withCaKeyUsages(Set<KeyUsage> requested) {
        Set<KeyUsage> keyUsages = EnumSet.copyOf(CA_REQUIRED_KEY_USAGES);
        keyUsages.addAll(requested);
        return keyUsages;
    }

    private static Set<KeyUsage> withoutCaKeyUsages(Set<KeyUsage> requested) {
        Set<KeyUsage> keyUsages = EnumSet.noneOf(KeyUsage.class);
        keyUsages.addAll(requested);
        keyUsages.removeAll(CA_REQUIRED_KEY_USAGES);
        return keyUsages;
    }

    private static List<String> collectOcspUris(CertProfileDraft draft) {
        List<String> uris = new ArrayList<>();
        if (draft.useDefaultOcspUri()) {
            uris.add(draft.defaultOcspUri());
        }
        uris.addAll(draft.ocspUris());
        List<String> trimmed = new ArrayList<>();
        for (String uri : uris) {
            if (uri == null || uri.isBlank()) {
                throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "OCSP URI must not be blank.");
            }
            trimmed.add(uri.trim());
        }
        return trimmed.stream().distinct().toList();
    }

    public void ensureIssues(CertType target) {
        if (certType != target) {
            throw new CoreException(ErrorCode.PROFILE_RESOURCE_NOT_FOUND, "Certificate profile not found.")
                .property("certProfileId", id)
                .property("certType", certType);
        }
    }

    public CertificateTemplate rootTemplate(DistinguishedName subjectDn, SubjectPublicKey subjectKey,
            IssuanceOptions options) {
        ensureIssues(CertType.ROOT);
        ensureKeyMatches(subjectKey);
        KeyIdentifier keyId = subjectKey.keyIdentifier();
        ExtensionsGenerator extensions = profileExtensions();
        add(extensions, Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(keyId.value()));
        add(extensions, Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(keyId.value()));
        addCrlDistributionPoints(extensions, cleaned(options.crlDps()));
        addAuthorityInfoAccess(extensions, List.of(), concat(ocspUris, options.ocspUris()));
        return new CertificateTemplate(subjectDn, subjectDn, subjectKey, options.validityWithin(validityDuration),
                extensions.generate());
    }

    public CertificateTemplate subordinateTemplate(DistinguishedName issuerDn, SubjectPublicKey issuerKey,
            DistinguishedName subjectDn, SubjectPublicKey subjectKey, IssuanceOptions options,
            DistributionPoints distributionPoints, Extension requestedSubjectAltName) {
        if (certType == CertType.ROOT) {
            throw new IllegalStateException("Root profile issues self-signed certificates only");
        }
        ensureKeyMatches(subjectKey);
        KeyIdentifier issuerKeyId = issuerKey.keyIdentifier();
        ExtensionsGenerator extensions = profileExtensions();
        if (requestedSubjectAltName == null && !subjectAltNameTemplate.isEmpty()) {
            add(extensions, Extension.subjectAlternativeName, false, subjectAltNameTemplate.toGeneralNames());
        }
        add(extensions, Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(issuerKeyId.value()));
        add(extensions, Extension.subjectKeyIdentifier, false,
                new SubjectKeyIdentifier(subjectKey.keyIdentifier().value()));
        List<String> crlDps = new ArrayList<>(cleaned(options.crlDps()));
        if (options.includeDefaultCrlDp()) {
            crlDps.add(distributionPoints.crlUrl(issuerKeyId));
        }
        addCrlDistributionPoints(extensions, crlDps);
        List<String> caIssuers = new ArrayList<>(options.aiaCaIssuers());
        if (useDefaultAiaCaIssuer) {
            caIssuers.add(distributionPoints.caIssuersUrl(issuerKeyId));
        }
        addAuthorityInfoAccess(extensions, caIssuers, concat(ocspUris, options.ocspUris()));
        if (requestedSubjectAltName != null) {
            extensions.addExtension(requestedSubjectAltName);
        }
        return new CertificateTemplate(issuerDn, subjectDn, subjectKey, options.validityWithin(validityDuration),
                extensions.generate());
    }

    private void ensureKeyMatches(SubjectPublicKey subjectKey) {
        if (!subjectKey.keyAlgorithm().equals(keyAlgorithm)) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "The subject public key does not match the profile's key spec.")
                .property("keyAlgorithm", keyAlgorithm.value());
        }
    }

    private ExtensionsGenerator profileExtensions() {
        ExtensionsGenerator extensions = new ExtensionsGenerator();
        BasicConstraints basicConstraints = !isCa() ? new BasicConstraints(false)
                : pathLenConstraint == null ? new BasicConstraints(true) : new BasicConstraints(pathLenConstraint);
        add(extensions, Extension.basicConstraints, true, basicConstraints);
        if (!keyUsages.isEmpty()) {
            add(extensions, Extension.keyUsage, keyUsageCritical, KeyUsageBits.toExtension(keyUsages));
        }
        if (!extendedKeyUsages.isEmpty()) {
            add(extensions, Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(extendedKeyUsages.stream()
                        .map(ProfileEku::oid)
                        .sorted()
                        .map(oid -> KeyPurposeId.getInstance(new ASN1ObjectIdentifier(oid)))
                        .toArray(KeyPurposeId[]::new)));
        }
        if (certificatePolicies != null) {
            add(extensions, Extension.certificatePolicies, certificatePolicies.critical(),
                    new org.bouncycastle.asn1.x509.CertificatePolicies(certificatePolicies.oids()
                        .stream()
                        .map(oid -> new PolicyInformation(new ASN1ObjectIdentifier(oid)))
                        .toArray(PolicyInformation[]::new)));
        }
        return extensions;
    }

    private static void addCrlDistributionPoints(ExtensionsGenerator extensions, List<String> urls) {
        if (urls.isEmpty()) {
            return;
        }
        add(extensions, Extension.cRLDistributionPoints, false, new CRLDistPoint(urls.stream()
            .map(url -> new DistributionPoint(new DistributionPointName(new GeneralNames(uri(url))), null, null))
            .toArray(DistributionPoint[]::new)));
    }

    private static void addAuthorityInfoAccess(ExtensionsGenerator extensions, List<String> caIssuers,
            List<String> ocsp) {
        List<AccessDescription> descriptions = new ArrayList<>();
        cleaned(caIssuers).stream()
            .distinct()
            .forEach(url -> descriptions.add(new AccessDescription(AccessDescription.id_ad_caIssuers, uri(url))));
        cleaned(ocsp).stream()
            .distinct()
            .forEach(url -> descriptions.add(new AccessDescription(AccessDescription.id_ad_ocsp, uri(url))));
        if (!descriptions.isEmpty()) {
            add(extensions, Extension.authorityInfoAccess, false,
                    new AuthorityInformationAccess(descriptions.toArray(AccessDescription[]::new)));
        }
    }

    private static List<String> cleaned(List<String> values) {
        return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> joined = new ArrayList<>(first);
        joined.addAll(second);
        return joined;
    }

    private static GeneralName uri(String url) {
        return new GeneralName(GeneralName.uniformResourceIdentifier, url);
    }

    private static void add(ExtensionsGenerator extensions, ASN1ObjectIdentifier oid, boolean critical,
            ASN1Encodable value) {
        try {
            extensions.addExtension(oid, critical, value);
        }
        catch (IOException ex) {
            throw new CoreException(ErrorCode.CERT_OPERATION_FAILED, "Failed to encode certificate extension fields.");
        }
    }

    public boolean isNew() {
        return id == null;
    }

    public boolean isCa() {
        return certType != CertType.LEAF;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CertType getCertType() {
        return certType;
    }

    public ValidityDuration getValidityDuration() {
        return validityDuration;
    }

    public Integer getPathLenConstraint() {
        return pathLenConstraint;
    }

    public Set<KeyUsage> getKeyUsages() {
        return keyUsages;
    }

    public boolean isKeyUsageCritical() {
        return keyUsageCritical;
    }

    public List<ProfileEku> getExtendedKeyUsages() {
        return extendedKeyUsages;
    }

    public CertificatePolicies getCertificatePolicies() {
        return certificatePolicies;
    }

    public SubjectAltNameTemplate getSubjectAltNameTemplate() {
        return subjectAltNameTemplate;
    }

    public KeyAlgorithm getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public boolean isRequireApproval() {
        return requireApproval;
    }

    public boolean isUseDefaultAiaCaIssuer() {
        return useDefaultAiaCaIssuer;
    }

    public List<String> getOcspUris() {
        return ocspUris;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
