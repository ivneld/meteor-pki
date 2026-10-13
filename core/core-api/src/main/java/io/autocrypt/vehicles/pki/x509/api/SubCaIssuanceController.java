package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.SubCaIssuanceRequest;
import io.autocrypt.vehicles.pki.x509.api.request.SubCaRenewalRequest;
import io.autocrypt.vehicles.pki.x509.api.response.SubCaIssuanceResponse;
import io.autocrypt.vehicles.pki.x509.api.response.SubCaRenewalResponse;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import io.autocrypt.vehicles.pki.x509.usecase.CertIssuanceUseCase;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sub CA 인증서 발급(x509-ra 의
 * {@code cert.presentation.api.v2.controller.SubCaIssuanceController} 계약). 갱신은 x509-ra 에도
 * 구현이 없어 501 로 둔다.
 */
@RestController
public class SubCaIssuanceController {

    private final CertIssuanceUseCase certIssuanceUseCase;

    public SubCaIssuanceController(CertIssuanceUseCase certIssuanceUseCase) {
        this.certIssuanceUseCase = certIssuanceUseCase;
    }

    @PostMapping("/api/certificates/sub-ca")
    public ResponseEntity<SubCaIssuanceResponse> issueSubCaCertificate(
            @RequestBody @Valid SubCaIssuanceRequest request) {
        return ResponseEntity
            .ok(new SubCaIssuanceResponse(certIssuanceUseCase.issueSubCa(request.toCommand()).getDer()));
    }

    @PostMapping("/api/certificates/sub-ca/renewal")
    public ResponseEntity<SubCaRenewalResponse> renewSubCaCertificate(@RequestBody @Valid SubCaRenewalRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
