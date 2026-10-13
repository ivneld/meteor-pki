package io.autocrypt.vehicles.pki.x509.domain;

import java.time.Instant;
import java.util.List;

/**
 * 발급 요청이 프로파일에 더하거나 좁히는 값.
 *
 * @param notBefore 유효기간 시작
 * @param requestedDuration 요청한 유효기간 길이. 없으면(null) 프로파일의 최대 기간
 * @param crlDps 추가 CRL 배포 지점
 * @param includeDefaultCrlDp 발급자의 기본 CRL 주소를 넣을지
 * @param aiaCaIssuers 추가 발급자 인증서 주소
 * @param ocspUris 추가 OCSP 주소
 */
public record IssuanceOptions(Instant notBefore, ValidityDuration requestedDuration, List<String> crlDps,
        boolean includeDefaultCrlDp, List<String> aiaCaIssuers, List<String> ocspUris) {

    public IssuanceOptions {
        crlDps = crlDps == null ? List.of() : List.copyOf(crlDps);
        aiaCaIssuers = aiaCaIssuers == null ? List.of() : List.copyOf(aiaCaIssuers);
        ocspUris = ocspUris == null ? List.of() : List.copyOf(ocspUris);
    }

    /** 프로파일이 허용하는 길이 안에서 유효기간을 확정한다. 요청은 기간을 줄일 수만 있다. */
    ValidityPeriod validityWithin(ValidityDuration profileDuration) {
        Instant requestedNotAfter = requestedDuration == null ? null : requestedDuration.maxNotAfter(notBefore);
        return profileDuration.resolve(notBefore, requestedNotAfter);
    }

}
