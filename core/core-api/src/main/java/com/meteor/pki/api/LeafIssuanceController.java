package com.meteor.pki.api;

import com.meteor.pki.api.request.LeafIssuanceRequest;
import com.meteor.pki.api.request.P10crIssuanceRequest;
import com.meteor.pki.api.response.LeafIssuanceResponse;
import com.meteor.pki.api.response.LeafP10CrIssuanceResponse;
import com.meteor.pki.usecase.CertIssuanceUseCase;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeafIssuanceController {

    private final CertIssuanceUseCase certIssuanceUseCase;

    public LeafIssuanceController(CertIssuanceUseCase certIssuanceUseCase) {
        this.certIssuanceUseCase = certIssuanceUseCase;
    }

    @PostMapping("/api/certificates/leaf/spki")
    public ResponseEntity<LeafIssuanceResponse> issueBySpki(@RequestBody @Valid LeafIssuanceRequest request) {
        return ResponseEntity.ok(new LeafIssuanceResponse(certIssuanceUseCase.issueLeaf(request.toCommand()).getDer()));
    }

    @PostMapping("/api/certificates/leaf/p10cr")
    public ResponseEntity<LeafP10CrIssuanceResponse> issueByP10cr(@RequestBody @Valid P10crIssuanceRequest request) {
        return ResponseEntity
            .ok(LeafP10CrIssuanceResponse.from(certIssuanceUseCase.issueLeafFromP10cr(request.toCommand())));
    }

}
