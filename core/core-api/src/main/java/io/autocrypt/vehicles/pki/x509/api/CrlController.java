package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v2.controller.CrlController} 계약. 구현 전이라 모든
 * 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class CrlController {

    /**
     * 모든 CA 의 최신 CRL 을 {AKI}_{crlNumber}.crl 항목으로 묶은 ZIP. 파일명
     * bulk-crl-yyyyMMdd-HHmmss.zip(UTC). CRL 이 없으면 404.
     */
    @GetMapping(value = "/api/v2/crls/bulk.zip", produces = "application/zip")
    public ResponseEntity<byte[]> downloadBulkCrlZip() {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    /** 최신 CRL(DER). 파일명 {caSkiHex}.crl. */
    @GetMapping(value = "/api/v2/crls/{caSkiHex}.crl", produces = "application/pkix-crl")
    public ResponseEntity<byte[]> downloadLatest(@PathVariable String caSkiHex) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    /** Full CRL 을 즉시 발행하고 DER 로 돌려준다. */
    @PostMapping(value = "/api/v2/crls/{caSkiHex}/issue", produces = "application/pkix-crl")
    public ResponseEntity<byte[]> issueAndDownload(@PathVariable String caSkiHex) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
