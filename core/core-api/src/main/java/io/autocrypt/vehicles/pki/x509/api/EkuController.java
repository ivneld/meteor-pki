package io.autocrypt.vehicles.pki.x509.api;

import java.util.List;

import io.autocrypt.vehicles.pki.x509.api.request.EkuCreateRequest;
import io.autocrypt.vehicles.pki.x509.api.response.EkuResponse;
import io.autocrypt.vehicles.pki.x509.domain.EkuReader;
import io.autocrypt.vehicles.pki.x509.domain.EkuWriter;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 확장 키 용도(EKU) 등록과 조회. EKU 하나로 끝나므로 도메인 서비스를 바로 부른다(x509-ra 의
 * {@code profile.presentation.api.v2.controller.EkuController} 계약).
 */
@RestController
@RequestMapping("/api/extended-key-usages")
public class EkuController {

    private final EkuReader ekuReader;

    private final EkuWriter ekuWriter;

    public EkuController(EkuReader ekuReader, EkuWriter ekuWriter) {
        this.ekuReader = ekuReader;
        this.ekuWriter = ekuWriter;
    }

    /** 성공하면 200 빈 본문. */
    @PostMapping
    public ResponseEntity<Void> createExtendedKeyUsage(@RequestBody @Valid EkuCreateRequest request) {
        ekuWriter.register(request.oid(), request.purpose());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<EkuResponse>> getExtendedKeyUsages() {
        return ResponseEntity.ok(ekuReader.findAll().stream().map(EkuResponse::from).toList());
    }

}
