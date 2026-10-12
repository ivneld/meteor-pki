package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v1.controller.CaDistributionControllerV1} 계약. 구현
 * 전이라 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 *
 * <p>
 * 인증 없이 열려 있는 배포 경로(AIA caIssuers).
 */
@RestController
public class CaDistributionControllerV1 {

    @GetMapping(value = "/api/ca/{skiHex}.crt", produces = "application/pkix-cert")
    public ResponseEntity<byte[]> downloadCaCertificate(@PathVariable String skiHex) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
