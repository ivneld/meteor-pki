package com.meteor.pki.usecase;

import java.util.List;
import java.util.Set;

import com.meteor.pki.ContextTest;
import com.meteor.pki.domain.CertProfile;
import com.meteor.pki.domain.KeyAlgorithm;
import com.meteor.pki.domain.ProfileEku;
import com.meteor.pki.domain.SubjectAltNameTemplate;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertProfileUseCaseIT extends ContextTest {

    private final CertProfileUseCase certProfileUseCase;

    CertProfileUseCaseIT(CertProfileUseCase certProfileUseCase) {
        this.certProfileUseCase = certProfileUseCase;
    }

    @Test
    void copiesRegisteredEkusAndDefaultOcspIntoTheProfile() {
        CertProfile profile = certProfileUseCase.create(command("usecase-leaf", List.of("1.3.6.1.5.5.7.3.1")));

        assertThat(profile.getId()).isNotNull();
        assertThat(profile.getExtendedKeyUsages())
            .containsExactly(new ProfileEku("1.3.6.1.5.5.7.3.1", "TLS Web Server Authentication"));
        assertThat(profile.getOcspUris()).containsExactly("http://127.0.0.1/api/ocsp");
    }

    @Test
    void unknownEkuIsRejected() {
        assertThatThrownBy(() -> certProfileUseCase.create(command("usecase-unknown-eku", List.of("1.2.3.4.5"))))
            .isInstanceOf(CoreException.class)
            .hasMessage("Unknown EKU OID.");
    }

    @Test
    void nameMustBeUnique() {
        certProfileUseCase.create(command("usecase-dup", List.of()));

        assertThatThrownBy(() -> certProfileUseCase.create(command("usecase-dup", List.of())))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.PROFILE_INVALID_INPUT))
            .hasMessage("Certificate profile with the same name already exists.");
    }

    private static CertProfileCreateCommand command(String name, List<String> ekuOids) {
        return new CertProfileCreateCommand(name, CertType.LEAF, ValidityDuration.from("P1Y"), null,
                SubjectAltNameTemplate.EMPTY, Set.of(KeyUsage.DIGITAL_SIGNATURE), false, ekuOids, null, List.of(), true,
                false, false, KeyAlgorithm.of("EC-P-256"), List.of());
    }

}
