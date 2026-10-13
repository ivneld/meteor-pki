package io.autocrypt.vehicles.pki.x509.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * CA 인증서 목록에서 체인마다 가장 최근 것을 고른다. 체인은 (AKI, SKI) 쌍이다. 같은 키로 갱신한 인증서는 하나로 모이고, 키를 바꾸거나 발급자
 * 키가 다르면 다른 체인이 된다(x509-ra 와 같다).
 */
public final class CaCertificates {

    private static final Comparator<Cert> NEWEST_LAST = Comparator.comparing(Cert::getCreatedAt)
        .thenComparing(Cert::getId);

    private CaCertificates() {
    }

    /** 만든 순서로 정렬해 돌려준다. */
    public static List<Cert> latestPerChain(Collection<Cert> cas) {
        Map<String, Cert> latest = cas.stream()
            .collect(Collectors.toMap(cert -> cert.getAuthorityKeyId() + ":" + cert.getSubjectKeyId(), cert -> cert,
                    BinaryOperator.maxBy(NEWEST_LAST)));
        return latest.values().stream().sorted(NEWEST_LAST).toList();
    }

}
