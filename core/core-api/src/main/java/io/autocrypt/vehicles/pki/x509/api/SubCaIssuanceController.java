package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.SubCaIssuanceRequest;
import io.autocrypt.vehicles.pki.x509.api.request.SubCaRenewalRequest;
import io.autocrypt.vehicles.pki.x509.api.response.SubCaIssuanceResponse;
import io.autocrypt.vehicles.pki.x509.api.response.SubCaRenewalResponse;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v2.controller.SubCaIssuanceController} 계약. 구현
 * 전이라 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class SubCaIssuanceController {

    @PostMapping("/api/v2/certificates/sub-ca")
    public ResponseEntity<SubCaIssuanceResponse> issueSubCaCertificate(
            @RequestBody @Valid SubCaIssuanceRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @PostMapping("/api/v2/certificates/sub-ca/renewal")
    public ResponseEntity<SubCaRenewalResponse> renewSubCaCertificate(@RequestBody @Valid SubCaRenewalRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
