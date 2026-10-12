package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.LeafIssuanceRequest;
import io.autocrypt.vehicles.pki.x509.api.request.P10crIssuanceRequest;
import io.autocrypt.vehicles.pki.x509.api.response.LeafIssuanceResponse;
import io.autocrypt.vehicles.pki.x509.api.response.LeafP10CrIssuanceResponse;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v2.controller.LeafIssuanceController} 계약. 구현 전이라
 * 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 *
 * <p>
 * 발급 승인 정책은 프로파일을 따른다(ApprovalStrategy.FOLLOW_PROFILE).
 */
@RestController
public class LeafIssuanceController {

    /** SubjectPublicKeyInfo(DER)로 leaf 인증서를 발급한다. */
    @PostMapping("/api/v2/certificates/leaf/spki")
    public ResponseEntity<LeafIssuanceResponse> issueBySpki(@RequestBody @Valid LeafIssuanceRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    /** PKCS#10 CSR(PEM)로 leaf 인증서를 발급한다. */
    @PostMapping("/api/v2/certificates/leaf/p10cr")
    public ResponseEntity<LeafP10CrIssuanceResponse> issueByP10cr(@RequestBody @Valid P10crIssuanceRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
