package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EkuTest {

    @Test
    void oidMustBeWellFormed() {
        assertThat(Eku.register("1.3.6.1.5.5.7.3.1", "server").getOid()).isEqualTo("1.3.6.1.5.5.7.3.1");
        assertThatThrownBy(() -> Eku.register("3.1", "bad first arc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Eku.register(" 1.2.3", "untrimmed")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sameOidCannotBeRegisteredTwice() {
        assertThatThrownBy(() -> EkuRegistrationPolicy.ensureNotRegistered("1.2.3", true))
            .isInstanceOf(CoreException.class)
            .hasMessage("Extended Key Usage with the same OID already exists.");
    }

    @Test
    void onlyMissingDefaultsAreRegistered() {
        List<Eku> missing = EkuRegistrationPolicy.missingDefaults(List.of("1.3.6.1.5.5.7.3.1", "1.3.6.1.5.5.7.3.2"));

        assertThat(missing).extracting(Eku::getOid)
            .containsExactly("1.3.6.1.5.5.7.3.3", "1.3.6.1.5.5.7.3.4", "1.3.6.1.5.5.7.3.8", "1.3.6.1.5.5.7.3.9");
    }

}
