package com.meteor.pki.api;

import com.meteor.pki.api.request.SubCaIssuanceRequest;
import com.meteor.pki.api.request.SubCaRenewalRequest;
import com.meteor.pki.api.response.SubCaIssuanceResponse;
import com.meteor.pki.api.response.SubCaRenewalResponse;
import com.meteor.pki.usecase.CertIssuanceUseCase;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity
            .ok(new SubCaRenewalResponse(certIssuanceUseCase.renewSubCa(request.toCommand()).getDer()));
    }

}
