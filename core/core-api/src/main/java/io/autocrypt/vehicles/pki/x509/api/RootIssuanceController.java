package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.RootIssuanceRequest;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v2.controller.RootIssuanceController} 계약. 구현 전이라
 * 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
public class RootIssuanceController {

    /** 발급한 Root CA 인증서를 PEM 문자열로 돌려준다. */
    @PostMapping(value = "/api/v2/certificates/root", produces = "application/x-pem-file")
    public ResponseEntity<String> issueRoot(@RequestBody @Valid RootIssuanceRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
