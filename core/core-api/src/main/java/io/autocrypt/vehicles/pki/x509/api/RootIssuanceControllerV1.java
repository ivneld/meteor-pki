package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.RootIssuanceRequestV1;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v1.controller.RootIssuanceControllerV1} 계약. 구현
 * 전이라 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class RootIssuanceControllerV1 {

    @PostMapping(value = "/api/certificates/root", produces = "application/x-pem-file")
    public ResponseEntity<String> issueRoot(@RequestBody @Valid RootIssuanceRequestV1 request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
