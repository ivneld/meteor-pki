package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v2.controller.KeySyncController} 계약. 구현 전이라 모든
 * 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 *
 * <p>
 * pki.features.key-sync.enabled=true 일 때만 등록된다. 성공하면 204.
 */
@ConditionalOnProperty(prefix = "pki.features.key-sync", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/api")
public class KeySyncController {

    @PostMapping("/key-sync")
    public ResponseEntity<Void> syncKeys() {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
