package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;
import java.util.Set;

import io.autocrypt.vehicles.pki.x509.ContextTest;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;
import io.autocrypt.vehicles.pki.x509.enums.ValidityUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CertProfileRepositoryIT extends ContextTest {

    private final CertProfileRepository certProfileRepository;

    private final CertProfileReader certProfileReader;

    CertProfileRepositoryIT(CertProfileRepository certProfileRepository, CertProfileReader certProfileReader) {
        this.certProfileRepository = certProfileRepository;
        this.certProfileReader = certProfileReader;
    }

    @Test
    void valueObjectsAndCollectionsSurviveRoundTrip() {
        CertProfile saved = certProfileRepository.save(CertProfile.create(new CertProfileDraft("repo-leaf",
                CertType.LEAF, ValidityDuration.from("P90D"), null, SubjectAltNameTemplate.of("DNS:a.example.com"),
                Set.of(KeyUsage.DIGITAL_SIGNATURE, KeyUsage.KEY_AGREEMENT), true,
                List.of(new ProfileEku("1.3.6.1.5.5.7.3.2", "client"), new ProfileEku("1.3.6.1.5.5.7.3.1", "server")),
                CertificatePolicies.ofNullable(List.of("2.23.140.1.2.1", "1.2.3.4")), List.of("http://ocsp.b.com"),
                true, "http://ocsp.a.com", true, true, KeyAlgorithm.of("RSA-2048"), List.of())));

        // 컬렉션은 지연 로딩이므로 Reader 의 트랜잭션 안에서 복원한다
        CertProfile found = certProfileReader.get(saved.getId());

        assertThat(found.getValidityDuration()).isEqualTo(new ValidityDuration(ValidityUnit.DAYS, 90));
        assertThat(found.getKeyUsages()).containsExactlyInAnyOrder(KeyUsage.DIGITAL_SIGNATURE, KeyUsage.KEY_AGREEMENT);
        assertThat(found.getExtendedKeyUsages()).extracting(ProfileEku::oid)
            .containsExactly("1.3.6.1.5.5.7.3.2", "1.3.6.1.5.5.7.3.1");
        assertThat(found.getCertificatePolicies().oids()).containsExactly("2.23.140.1.2.1", "1.2.3.4");
        assertThat(found.getOcspUris()).containsExactly("http://ocsp.a.com", "http://ocsp.b.com");
        assertThat(found.getSubjectAltNameTemplate().csv()).isEqualTo("DNS:a.example.com");
        assertThat(found.getKeyAlgorithm().value()).isEqualTo("RSA-2048");
        assertThat(found.isRequireApproval()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
    }

}
