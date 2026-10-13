package com.meteor.pki.domain;

import java.util.Collection;
import java.util.List;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

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

    public static List<Eku> missingDefaults(Collection<String> registeredOids) {
        return Eku.defaults().stream().filter(eku -> !registeredOids.contains(eku.getOid())).toList();
    }

}
