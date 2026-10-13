package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.CertProfileCreationRequest;
import io.autocrypt.vehicles.pki.x509.api.response.CertProfileDetailResponse;
import io.autocrypt.vehicles.pki.x509.api.response.CertProfilePageResponse;
import io.autocrypt.vehicles.pki.x509.api.response.CertProfileResponse;
import io.autocrypt.vehicles.pki.x509.domain.CertProfileReader;
import io.autocrypt.vehicles.pki.x509.domain.PageQuery;
import io.autocrypt.vehicles.pki.x509.usecase.CertProfileUseCase;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증서 프로파일 API. 등록된 EKU 를 엮어야 하는 생성은 usecase 를, 프로파일 하나로 끝나는 조회는
 * {@link CertProfileReader} 를 부른다(x509-ra 의
 * {@code profile.presentation.api.v2.controller.CertProfileController} 계약).
 */
@RestController
public class CertProfileController {

    private final CertProfileUseCase certProfileUseCase;

    private final CertProfileReader certProfileReader;

    public CertProfileController(CertProfileUseCase certProfileUseCase, CertProfileReader certProfileReader) {
        this.certProfileUseCase = certProfileUseCase;
        this.certProfileReader = certProfileReader;
    }

    @PostMapping("/api/profiles")
    public ResponseEntity<CertProfileResponse> create(@RequestBody @Valid CertProfileCreationRequest request) {
        return ResponseEntity.ok(CertProfileResponse.from(certProfileUseCase.create(request.toCommand())));
    }

    @GetMapping("/api/profiles/detail")
    public ResponseEntity<CertProfileDetailResponse> getProfileDetail(@RequestParam String profileName) {
        return ResponseEntity.ok(CertProfileDetailResponse.from(certProfileReader.getByName(profileName)));
    }

    /** 발급 가능한 프로파일만 돌려준다. 이 서버는 모든 키 알고리즘을 지원하므로 모든 프로파일이다. */
    @GetMapping("/api/profiles")
    public ResponseEntity<CertProfilePageResponse> getCertProfiles(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(CertProfilePageResponse.from(certProfileReader.getPage(new PageQuery(page, size))));
    }

}
