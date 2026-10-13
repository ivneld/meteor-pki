package com.meteor.pki.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public final class CaCertificates {

    private static final Comparator<Cert> NEWEST_LAST = Comparator.comparing(Cert::getCreatedAt)
        .thenComparing(Cert::getId);

    private CaCertificates() {
    }

    public static List<Cert> latestPerChain(Collection<Cert> cas) {
        Map<String, Cert> latest = cas.stream()
            .collect(Collectors.toMap(cert -> cert.getAuthorityKeyId() + ":" + cert.getSubjectKeyId(), cert -> cert,
                    BinaryOperator.maxBy(NEWEST_LAST)));
        return latest.values().stream().sorted(NEWEST_LAST).toList();
    }

}
