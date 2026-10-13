package io.autocrypt.vehicles.pki.x509.domain;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

/**
 * 프로파일 이름은 서버 안에서 하나뿐이다. 프로파일 한 건은 다른 프로파일의 이름을 모르므로 이 규칙은 여기 둔다. 같은 이름이 있는지는
 * {@link CertProfileWriter} 가 저장소에서 꺼내 넘긴다(S-04).
 */
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
