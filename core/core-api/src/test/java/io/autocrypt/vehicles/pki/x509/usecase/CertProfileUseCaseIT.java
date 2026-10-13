package io.autocrypt.vehicles.pki.x509.usecase;

import java.util.List;
import java.util.Set;

import io.autocrypt.vehicles.pki.x509.ContextTest;
import io.autocrypt.vehicles.pki.x509.domain.CertProfile;
import io.autocrypt.vehicles.pki.x509.domain.KeyAlgorithm;
import io.autocrypt.vehicles.pki.x509.domain.ProfileEku;
import io.autocrypt.vehicles.pki.x509.domain.SubjectAltNameTemplate;
import io.autocrypt.vehicles.pki.x509.domain.ValidityDuration;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 등록된 EKU 확인(EkuReader)과 프로파일 생성(CertProfileWriter)을 usecase 가 엮는지 확인한다. 표준 EKU 는 기동할 때
 * 등록된다.
 */
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
