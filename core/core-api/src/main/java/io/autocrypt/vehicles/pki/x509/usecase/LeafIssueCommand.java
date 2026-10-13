package io.autocrypt.vehicles.pki.x509.usecase;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.domain.DistinguishedName;
import io.autocrypt.vehicles.pki.x509.domain.IssuanceOptions;
import io.autocrypt.vehicles.pki.x509.domain.KeyIdentifier;
import io.autocrypt.vehicles.pki.x509.domain.SubjectPublicKey;
import io.autocrypt.vehicles.pki.x509.domain.ValidityDuration;

/**
 * 공개키(SubjectPublicKeyInfo)로 요청한 Leaf 발급.
 *
 * @param issuerKeyId 발급자 CA 의 SKI (요청의 akiHex)
 * @param validityDuration 없으면(null) 프로파일의 최대 기간
 * @param notBefore 없으면(null) 현재 시각
 */
public record LeafIssueCommand(Long certProfileId, DistinguishedName subjectDn, KeyIdentifier issuerKeyId,
        SubjectPublicKey publicKey, List<String> crlDps, boolean includeDefaultCrlDp, List<String> aiaCaIssuers,
        List<String> ocspUris, ValidityDuration validityDuration, Instant notBefore) {

    IssuanceOptions options(Instant now) {
        return new IssuanceOptions(notBefore == null ? now : notBefore, validityDuration, crlDps, includeDefaultCrlDp,
                aiaCaIssuers, ocspUris);
    }

}
