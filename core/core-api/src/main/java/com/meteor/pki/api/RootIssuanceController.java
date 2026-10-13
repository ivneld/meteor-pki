package com.meteor.pki.api;

import com.meteor.pki.api.request.RootIssuanceRequest;
import com.meteor.pki.usecase.CertIssuanceUseCase;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootIssuanceController {

    private final CertIssuanceUseCase certIssuanceUseCase;

    public RootIssuanceController(CertIssuanceUseCase certIssuanceUseCase) {
        this.certIssuanceUseCase = certIssuanceUseCase;
    }

    @PostMapping(value = "/api/certificates/root", produces = "application/x-pem-file")
    public ResponseEntity<String> issueRoot(@RequestBody @Valid RootIssuanceRequest request) {
        return ResponseEntity.ok(certIssuanceUseCase.issueRoot(request.toCommand()).getPem());
    }

}
