package io.autocrypt.vehicles.pki.x509.api;

import java.util.List;

import io.autocrypt.vehicles.pki.x509.api.request.DownloadRequestV1;
import io.autocrypt.vehicles.pki.x509.api.response.CaCertDetailResponseV1;
import io.autocrypt.vehicles.pki.x509.api.response.CertResponseV1;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v1.controller.CertQueryControllerV1} 계약. 구현 전이라
 * 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class CertQueryControllerV1 {

    /** format=pem(기본) 또는 der. */
    @GetMapping(value = "/api/certificates/{akiHex}/download",
            produces = { "application/x-pem-file", "application/pkix-cert" })
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable String akiHex, @Valid DownloadRequestV1 request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    /** SKI 로 CA 인증서를 조회한다. */
    @GetMapping("/api/certificates/{skiHex}")
    public ResponseEntity<CertResponseV1> getCertificate(@PathVariable String skiHex) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @GetMapping("/api/certificates/ca")
    public ResponseEntity<List<CaCertDetailResponseV1>> getCaCertificates() {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
