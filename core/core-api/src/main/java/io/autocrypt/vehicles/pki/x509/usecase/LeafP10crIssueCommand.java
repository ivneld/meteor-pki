package io.autocrypt.vehicles.pki.x509.usecase;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.domain.CertificationRequest;
import io.autocrypt.vehicles.pki.x509.domain.IssuanceOptions;
import io.autocrypt.vehicles.pki.x509.domain.KeyIdentifier;
import io.autocrypt.vehicles.pki.x509.domain.ValidityDuration;

/**
 * PKCS#10 CSR 로 요청한 Leaf 발급. 주체 DN 과 공개키는 CSR 에서 꺼낸다.
 *
 * @param issuerKeyId 발급자 CA 의 SKI (요청의 akiHex)
 * @param validityDuration 없으면(null) 프로파일의 최대 기간
 * @param notBefore 없으면(null) 현재 시각
 */
public record LeafP10crIssueCommand(Long certProfileId, CertificationRequest certificationRequest,
        KeyIdentifier issuerKeyId, List<String> crlDps, boolean includeDefaultCrlDp, List<String> aiaCaIssuers,
        List<String> ocspUris, ValidityDuration validityDuration, Instant notBefore) {

    IssuanceOptions options(Instant now) {
        return new IssuanceOptions(notBefore == null ? now : notBefore, validityDuration, crlDps, includeDefaultCrlDp,
                aiaCaIssuers, ocspUris);
    }

}
