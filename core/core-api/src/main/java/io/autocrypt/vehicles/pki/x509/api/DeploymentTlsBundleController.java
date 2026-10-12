package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.DeploymentTlsBundleRequest;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.internal.DeploymentTlsBundleController} 계약. 구현 전이라
 * 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 *
 * <p>
 * 배포 스크립트용 내부 API. tls-leaf.pfx, subca.pem, rootca.pem 을 담은 ZIP(deployment-tls.zip,
 * Cache-Control: no-store).
 */
@RestController
public class DeploymentTlsBundleController {

    @PostMapping(value = "/internal/deployment/tls-bundle", produces = "application/zip")
    public ResponseEntity<byte[]> issue(@RequestBody @Valid DeploymentTlsBundleRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
