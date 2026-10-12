package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code cert.presentation.api.v2.controller.CmpController} 계약. 구현 전이라 모든
 * 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 *
 * <p>
 * RFC 9810 CMP over HTTP. 요청·응답 모두 DER PKIMessage. x509-ra 는 처리 실패도 HTTP 200 + error
 * PKIBody 로 돌려주며(CmpExceptionHandler), 그 동작은 구현 단계에서 옮긴다.
 */
@RestController
public class CmpController {

    @PostMapping(path = "/cmp", consumes = "application/pkixcmp", produces = "application/pkixcmp")
    public ResponseEntity<byte[]> processCmpMessage(@RequestBody byte[] pkiMessageDer) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
