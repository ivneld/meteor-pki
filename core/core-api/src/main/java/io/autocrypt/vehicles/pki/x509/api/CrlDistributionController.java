package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 없이 열려 있는 CRL 배포 경로(CRL DP). x509-ra 의 {@code CrlControllerV1} 에서 이 엔드포인트만 남겼다. 발급한
 * 인증서의 CRL Distribution Points 확장에 {@code {base}/api/crl/{issuerSki}.crl} 이 들어가므로 경로를 바꾸면
 * 안 된다. 구현 전이라 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class CrlDistributionController {

    /** 최신 CRL(DER). */
    @GetMapping(value = "/api/crl/{caSkiHex}.crl", produces = "application/pkix-crl")
    public ResponseEntity<byte[]> downloadLatest(@PathVariable String caSkiHex) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
