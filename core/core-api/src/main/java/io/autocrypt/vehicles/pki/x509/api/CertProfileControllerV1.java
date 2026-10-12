package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.CertProfileCreationRequestV1;
import io.autocrypt.vehicles.pki.x509.api.response.CertProfileDetailResponseV1;
import io.autocrypt.vehicles.pki.x509.api.response.CertProfilePageResponseV1;
import io.autocrypt.vehicles.pki.x509.api.response.CertProfileResponseV1;
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
 * x509-ra 의 {@code profile.presentation.api.v1.controller.CertProfileControllerV1} 계약. 구현
 * 전이라 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class CertProfileControllerV1 {

    @PostMapping("/api/certpolicies")
    public ResponseEntity<CertProfileResponseV1> create(@RequestBody @Valid CertProfileCreationRequestV1 request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @GetMapping("/api/certpolicies/detail")
    public ResponseEntity<CertProfileDetailResponseV1> getProfileDetail(
            @RequestParam("policyName") String profileName) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    /** 모든 프로파일을 돌려준다. */
    @GetMapping("/api/certpolicies")
    public ResponseEntity<CertProfilePageResponseV1> getCertPolicies(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
