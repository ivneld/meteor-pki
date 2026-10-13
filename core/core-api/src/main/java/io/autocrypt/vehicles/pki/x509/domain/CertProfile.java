package io.autocrypt.vehicles.pki.x509.domain;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
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

/**
 * 인증서 프로파일. 어떤 종류(ROOT, SUB, LEAF)의 인증서를 어떤 키, 유효기간, 확장으로 발급할지 정한다. 한번 만들면 바뀌지 않는다.
 *
 * <p>
 * 종류별 규칙은 모두 {@link #create} 에, 발급할 인증서의 유효기간과 확장을 정하는 규칙은 {@link #rootTemplate},
 * {@link #subordinateTemplate} 에 있다. CA 프로파일은 keyCertSign·cRLSign 과 critical 키 용도를 강제하고
 * SAN, 승인, OCSP 를 쓰지 않는다. LEAF 는 CA 용 키 용도를 조용히 버린다(x509-ra 와 같다).
 */
public class CertProfile {

    /** SUB 프로파일이 가질 수 있는 가장 큰 pathLenConstraint. */
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
        // 사용자 정의 확장은 등록·활성화된 custom OID 만 쓸 수 있다. 아직 custom OID 를 등록하는 기능이 없으므로 모두 거절한다
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

    /** 저장소에서 복원한다. CertProfileRepository 만 호출한다. */
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

    /**
     * 이 프로파일로 target 종류의 인증서를 발급하는지 확인한다. 종류가 다르면 없는 프로파일과 같이 404 다(x509-ra 와 같다).
     */
    public void ensureIssues(CertType target) {
        if (certType != target) {
            throw new CoreException(ErrorCode.PROFILE_RESOURCE_NOT_FOUND, "Certificate profile not found.")
                .property("certProfileId", id)
                .property("certType", certType);
        }
    }

    /**
     * 자기 서명 Root CA 인증서의 내용. 발급자와 주체가 같고 AKI 는 자기 SKI 다. 기본 CRL·CA 배포 지점은 넣지 않고 요청한
     * CRL·OCSP 주소만 넣는다.
     */
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

    /**
     * 발급자가 서명하는 Sub CA·Leaf 인증서의 내용. AKI 는 발급자 키, 기본 CRL 주소({@code includeDefaultCrlDp})와
     * 기본 발급자 인증서 주소({@link #isUseDefaultAiaCaIssuer()})는 발급자 SKI 로 만든다. OCSP 는 프로파일 주소 뒤에
     * 요청 주소를 붙인다.
     * @param requestedSubjectAltName CSR 이 요청한 SAN 확장. 있으면 프로파일 SAN 대신 맨 뒤에 그대로 넣는다. 없으면
     * null
     */
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

    /**
     * 프로파일이 정한 확장: basicConstraints, keyUsage, extendedKeyUsage, certificatePolicies 순서.
     */
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

    /** 발급자 인증서 주소를 먼저, OCSP 주소를 뒤에 둔다. 둘 다 없으면 넣지 않는다. */
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

    /** 앞뒤 공백을 지우고 빈 값을 버린다. */
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

    /** SUB 의 경로 길이 제한. ROOT(무제한)와 LEAF 는 null. */
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

    /** 없으면 null. */
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
