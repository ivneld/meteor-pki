package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.KeyAgreementRequest;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v2.controller.KeyAgreementController} 계약. 구현 전이라
 * 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 *
 * <p>
 * key-agreement.role=server 일 때만 등록된다. 200 성공, 208 이미 완료, 500 실패.
 */
@ConditionalOnProperty(prefix = "key-agreement", name = "role", havingValue = "server")
@RestController
@RequestMapping("/api/key-agreement")
public class KeyAgreementController {

    /** 키 합의용 공개키(바이트)를 발급한다. */
    @PostMapping("/request-public-key")
    public ResponseEntity<byte[]> issueKeyAgreementPublicKey() {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @PostMapping("/operate")
    public ResponseEntity<Void> operateKeyAgreement(@RequestBody @Valid KeyAgreementRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
