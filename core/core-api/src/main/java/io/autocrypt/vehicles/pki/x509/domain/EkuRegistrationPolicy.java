package io.autocrypt.vehicles.pki.x509.domain;

import java.util.Collection;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

/**
 * EKU 의 OID 는 한 번만 등록한다. 이미 등록된 OID 는 {@link EkuWriter} 가 저장소에서 꺼내 넘긴다(S-04).
 */
public final class EkuRegistrationPolicy {

    private EkuRegistrationPolicy() {
    }

    public static void ensureNotRegistered(String oid, boolean registered) {
        if (registered) {
            throw new CoreException(ErrorCode.PROFILE_INVALID_INPUT,
                    "Extended Key Usage with the same OID already exists.")
                .property("oid", oid);
        }
    }

    /** 표준 EKU 중 아직 등록되지 않은 것. */
    public static List<Eku> missingDefaults(Collection<String> registeredOids) {
        return Eku.defaults().stream().filter(eku -> !registeredOids.contains(eku.getOid())).toList();
    }

}
