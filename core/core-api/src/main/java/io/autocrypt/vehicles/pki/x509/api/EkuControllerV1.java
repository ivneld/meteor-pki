package io.autocrypt.vehicles.pki.x509.api;

import java.util.List;

import io.autocrypt.vehicles.pki.x509.api.request.EkuCreateRequestV1;
import io.autocrypt.vehicles.pki.x509.api.response.EkuResponseV1;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code profile.presentation.api.v1.controller.EkuControllerV1} 계약. 구현 전이라 모든
 * 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
@RequestMapping("/api/extended-key-usages")
public class EkuControllerV1 {

    /** 성공하면 200 빈 본문. */
    @PostMapping
    public ResponseEntity<Void> createExtendedKeyUsage(@RequestBody @Valid EkuCreateRequestV1 request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @GetMapping
    public ResponseEntity<List<EkuResponseV1>> getExtendedKeyUsages() {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
