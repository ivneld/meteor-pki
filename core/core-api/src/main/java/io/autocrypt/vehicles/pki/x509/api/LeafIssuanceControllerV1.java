package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.LeafIssuanceRequestV1;
import io.autocrypt.vehicles.pki.x509.api.request.P10crIssuanceRequestV1;
import io.autocrypt.vehicles.pki.x509.api.response.LeafIssuanceResponseV1;
import io.autocrypt.vehicles.pki.x509.api.response.LeafP10CrIssuanceResponseV1;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v1.controller.LeafIssuanceControllerV1} 계약. 구현
 * 전이라 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class LeafIssuanceControllerV1 {

    @PostMapping("/api/certificates")
    public ResponseEntity<LeafIssuanceResponseV1> issueBySpki(@RequestBody @Valid LeafIssuanceRequestV1 request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @PostMapping("/api/certificates/p10cr")
    public ResponseEntity<LeafP10CrIssuanceResponseV1> issueByP10cr(
            @RequestBody @Valid P10crIssuanceRequestV1 request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
