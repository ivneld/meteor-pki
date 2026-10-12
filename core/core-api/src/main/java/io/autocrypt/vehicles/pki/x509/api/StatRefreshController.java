package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.api.request.CertStatRefreshRequest;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code stats.presentation.api.controller.StatRefreshController} 계약. 구현 전이라 모든
 * 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
@RequestMapping("/api")
public class StatRefreshController {

    /** 성공하면 200 빈 본문. */
    @PostMapping("/cert-stat/recalculation")
    public void refreshCertStat(@RequestBody @Valid CertStatRefreshRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
