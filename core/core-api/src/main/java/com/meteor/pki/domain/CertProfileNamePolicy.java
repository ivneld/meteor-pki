package com.meteor.pki.domain;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

public final class CertProfileNamePolicy {

    private CertProfileNamePolicy() {
    }

    public static void ensureUnique(String name, boolean nameTaken) {
        if (nameTaken) {
            throw new CoreException(ErrorCode.PROFILE_INVALID_INPUT,
                    "Certificate profile with the same name already exists.")
                .property("profileName", name);
        }
    }

}
