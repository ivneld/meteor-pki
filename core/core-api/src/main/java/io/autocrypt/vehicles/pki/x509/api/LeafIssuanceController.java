package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.LeafIssuanceRequest;
import io.autocrypt.vehicles.pki.x509.api.request.P10crIssuanceRequest;
import io.autocrypt.vehicles.pki.x509.api.response.LeafIssuanceResponse;
import io.autocrypt.vehicles.pki.x509.api.response.LeafP10CrIssuanceResponse;
import io.autocrypt.vehicles.pki.x509.usecase.CertIssuanceUseCase;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Leaf 인증서 발급(x509-ra 의
 * {@code cert.presentation.api.v2.controller.LeafIssuanceController} 계약).
 *
 * <p>
 * 발급 승인 정책은 프로파일을 따른다(ApprovalStrategy.FOLLOW_PROFILE). 승인이 필요한 프로파일이어도 응답은 같고, 인증서는
 * PENDING 으로 저장된다.
 */
@RestController
public class LeafIssuanceController {

    private final CertIssuanceUseCase certIssuanceUseCase;

    public LeafIssuanceController(CertIssuanceUseCase certIssuanceUseCase) {
        this.certIssuanceUseCase = certIssuanceUseCase;
    }

    /** SubjectPublicKeyInfo(DER)로 leaf 인증서를 발급한다. */
    @PostMapping("/api/certificates/leaf/spki")
    public ResponseEntity<LeafIssuanceResponse> issueBySpki(@RequestBody @Valid LeafIssuanceRequest request) {
        return ResponseEntity.ok(new LeafIssuanceResponse(certIssuanceUseCase.issueLeaf(request.toCommand()).getDer()));
    }

    /** PKCS#10 CSR(PEM)로 leaf 인증서를 발급한다. */
    @PostMapping("/api/certificates/leaf/p10cr")
    public ResponseEntity<LeafP10CrIssuanceResponse> issueByP10cr(@RequestBody @Valid P10crIssuanceRequest request) {
        return ResponseEntity
            .ok(LeafP10CrIssuanceResponse.from(certIssuanceUseCase.issueLeafFromP10cr(request.toCommand())));
    }

}
