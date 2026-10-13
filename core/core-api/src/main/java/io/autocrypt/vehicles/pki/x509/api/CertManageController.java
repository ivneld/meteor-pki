package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.RenewalRequest;
import io.autocrypt.vehicles.pki.x509.api.request.RevocationRequest;
import io.autocrypt.vehicles.pki.x509.api.response.RenewalResponse;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v2.controller.CertManageController} 계약. 구현 전이라
 * 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class CertManageController {

    @PostMapping("/api/certificates/leaf/{akiHex}/renewal")
    public ResponseEntity<RenewalResponse> renewLeafCertificate(@PathVariable String akiHex,
            @RequestBody @Valid RenewalRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    /** 성공하면 200 빈 본문. */
    @PostMapping("/api/certificates/leaf/{akiHex}/revocation")
    public ResponseEntity<Void> revokeLeafCertificate(@PathVariable String akiHex,
            @RequestBody @Valid RevocationRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
