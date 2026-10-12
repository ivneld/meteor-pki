package io.autocrypt.vehicles.pki.x509.api;

import java.util.List;

import io.autocrypt.vehicles.pki.x509.api.request.DownloadRequest;
import io.autocrypt.vehicles.pki.x509.api.response.CaCertDetailResponse;
import io.autocrypt.vehicles.pki.x509.api.response.CertResponse;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v2.controller.CertQueryController} 계약. 구현 전이라 모든
 * 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class CertQueryController {

    /**
     * format=pem(기본)이면 application/x-pem-file, der 이면 application/pkix-cert. 파일명은
     * {serialNumberHex}.{pem|der}. 그 외 format 은 400.
     */
    @GetMapping(value = "/api/v2/certificates/{akiHex}/download",
            produces = { "application/x-pem-file", "application/pkix-cert" })
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable String akiHex, @Valid DownloadRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @GetMapping("/api/v2/certificates/ca/{skiHex}")
    public ResponseEntity<CertResponse> getCaCertificate(@PathVariable String skiHex) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    /** 체인별 최신 CA 인증서(숨김 제외). */
    @GetMapping("/api/v2/certificates/ca")
    public ResponseEntity<List<CaCertDetailResponse>> getCaCertificates() {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
