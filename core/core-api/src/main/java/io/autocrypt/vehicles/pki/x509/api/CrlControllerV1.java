package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v1.controller.CrlControllerV1} 계약. 구현 전이라 모든
 * 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class CrlControllerV1 {

    /** 인증 없이 열려 있는 배포 경로(CRL DP). */
    @GetMapping(value = "/api/crl/{caSkiHex}.crl", produces = "application/pkix-crl")
    public ResponseEntity<byte[]> downloadLatest(@PathVariable String caSkiHex) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @GetMapping(value = "/api/crl/bulk.zip", produces = "application/zip")
    public ResponseEntity<byte[]> downloadBulkCrlZip() {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
