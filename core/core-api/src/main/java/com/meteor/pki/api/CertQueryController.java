package com.meteor.pki.api;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

import com.meteor.pki.api.request.DownloadRequest;
import com.meteor.pki.api.response.CaCertDetailResponse;
import com.meteor.pki.api.response.CertResponse;
import com.meteor.pki.domain.Cert;
import com.meteor.pki.domain.CertReader;
import com.meteor.pki.domain.DownloadFormat;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.SerialNumber;
import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CertQueryController {

    private final CertReader certReader;

    private final Clock clock;

    public CertQueryController(CertReader certReader, Clock clock) {
        this.certReader = certReader;
        this.clock = clock;
    }

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

    @GetMapping("/api/certificates/ca")
    public ResponseEntity<List<CaCertDetailResponse>> getCaCertificates() {
        return ResponseEntity.ok(certReader.getLatestCasPerChain()
            .stream()
            .map(cert -> CaCertDetailResponse.from(cert, clock.instant()))
            .toList());
    }

}
