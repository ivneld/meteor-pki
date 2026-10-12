package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.RenewalRequestV1;
import io.autocrypt.vehicles.pki.x509.api.request.RevocationRequestV1;
import io.autocrypt.vehicles.pki.x509.api.response.RenewalResponseV1;
import io.autocrypt.vehicles.pki.x509.api.response.RevocationResponseV1;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v1.controller.CertManageControllerV1} 계약. 구현 전이라
 * 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class CertManageControllerV1 {

    @PostMapping("/api/certificates/{akiHex}/renewal")
    public ResponseEntity<RenewalResponseV1> renewCertificate(@PathVariable String akiHex,
            @RequestBody @Valid RenewalRequestV1 request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    /**
     * 실패도 ProblemDetail 이 아니라 RevocationResponseV1(revoked=false) 로 알린다. 업무 오류 400, 그 외
     * 500.
     */
    @PostMapping("/api/certificates/{serialNumber}/revocation")
    public ResponseEntity<RevocationResponseV1> revokeCertificate(@PathVariable String serialNumber,
            @RequestBody @Valid RevocationRequestV1 request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
