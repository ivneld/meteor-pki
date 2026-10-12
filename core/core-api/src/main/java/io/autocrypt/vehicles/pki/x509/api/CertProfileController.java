package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.CertProfileCreationRequest;
import io.autocrypt.vehicles.pki.x509.api.response.CertProfileDetailResponse;
import io.autocrypt.vehicles.pki.x509.api.response.CertProfilePageResponse;
import io.autocrypt.vehicles.pki.x509.api.response.CertProfileResponse;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code profile.presentation.api.v2.controller.CertProfileController} 계약. 구현
 * 전이라 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class CertProfileController {

    @PostMapping("/api/v2/profiles")
    public ResponseEntity<CertProfileResponse> create(@RequestBody @Valid CertProfileCreationRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @GetMapping("/api/v2/profiles/detail")
    public ResponseEntity<CertProfileDetailResponse> getProfileDetail(@RequestParam String profileName) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    /** 발급 가능한 프로파일만 돌려준다. */
    @GetMapping("/api/v2/profiles")
    public ResponseEntity<CertProfilePageResponse> getCertProfiles(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
