package io.autocrypt.vehicles.pki.x509.api;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.api.request.DownloadRequest;
import io.autocrypt.vehicles.pki.x509.api.response.CaCertDetailResponse;
import io.autocrypt.vehicles.pki.x509.api.response.CertResponse;
import io.autocrypt.vehicles.pki.x509.domain.Cert;
import io.autocrypt.vehicles.pki.x509.domain.CertReader;
import io.autocrypt.vehicles.pki.x509.domain.DownloadFormat;
import io.autocrypt.vehicles.pki.x509.domain.KeyIdentifier;
import io.autocrypt.vehicles.pki.x509.domain.SerialNumber;
import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증서 조회와 다운로드(x509-ra 의 {@code cert.presentation.api.v2.controller.CertQueryController}
 * 계약).
 */
@RestController
public class CertQueryController {

    private final CertReader certReader;

    private final Clock clock;

    public CertQueryController(CertReader certReader, Clock clock) {
        this.certReader = certReader;
        this.clock = clock;
    }

    /**
     * format=pem(기본)이면 application/x-pem-file, der 이면 application/pkix-cert. 파일명은
     * {serialNumberHex}.{pem|der}. 그 외 format 은 400.
     */
    @GetMapping(value = "/api/certificates/{akiHex}/download",
            produces = { "application/x-pem-file", "application/pkix-cert" })
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable String akiHex, @Valid DownloadRequest request) {
        DownloadFormat format = DownloadFormat.of(request.format());
        SerialNumber serialNumber = SerialNumber.of(request.serialNumberHex());
        Cert cert = certReader.get(KeyIdentifier.of(akiHex), serialNumber);
        byte[] body = format.der() ? cert.getDer() : cert.getPem().getBytes(StandardCharsets.US_ASCII);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(format.der() ? "application/pkix-cert" : "application/x-pem-file"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                        .filename(serialNumber.toHex() + "." + format.extension())
                        .build()
                        .toString())
            .body(body);
    }

    @GetMapping("/api/certificates/ca/{skiHex}")
    public ResponseEntity<CertResponse> getCaCertificate(@PathVariable String skiHex) {
        return ResponseEntity.ok(CertResponse.from(certReader.getCa(KeyIdentifier.of(skiHex)), clock.instant()));
    }

    /** 체인별 최신 CA 인증서(숨김 제외). */
    @GetMapping("/api/certificates/ca")
    public ResponseEntity<List<CaCertDetailResponse>> getCaCertificates() {
        return ResponseEntity.ok(certReader.getLatestCasPerChain()
            .stream()
            .map(cert -> CaCertDetailResponse.from(cert, clock.instant()))
            .toList());
    }

}
