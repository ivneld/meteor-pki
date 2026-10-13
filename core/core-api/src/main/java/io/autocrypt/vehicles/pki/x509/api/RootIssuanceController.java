package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.RootIssuanceRequest;
import io.autocrypt.vehicles.pki.x509.usecase.CertIssuanceUseCase;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Root CA 인증서 발급(x509-ra 의
 * {@code cert.presentation.api.v2.controller.RootIssuanceController} 계약).
 */
@RestController
public class RootIssuanceController {

    private final CertIssuanceUseCase certIssuanceUseCase;

    public RootIssuanceController(CertIssuanceUseCase certIssuanceUseCase) {
        this.certIssuanceUseCase = certIssuanceUseCase;
    }

    /** 발급한 Root CA 인증서를 PEM 문자열로 돌려준다. */
    @PostMapping(value = "/api/certificates/root", produces = "application/x-pem-file")
    public ResponseEntity<String> issueRoot(@RequestBody @Valid RootIssuanceRequest request) {
        return ResponseEntity.ok(certIssuanceUseCase.issueRoot(request.toCommand()).getPem());
    }

}
