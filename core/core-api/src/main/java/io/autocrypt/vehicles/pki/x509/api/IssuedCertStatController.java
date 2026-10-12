package io.autocrypt.vehicles.pki.x509.api;

import java.time.LocalDate;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.api.response.IssuedCertStatResponse;
import io.autocrypt.vehicles.pki.x509.enums.StatType;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * x509-ra 의 {@code stats.presentation.api.controller.IssuedCertStatController} 계약. 구현 전이라
 * 모든 엔드포인트가 501(COMMON_NOT_IMPLEMENTED)로 응답한다.
 */
@RestController
@RequestMapping("/api/stats/issued-cert")
public class IssuedCertStatController {

    @GetMapping
    public ResponseEntity<List<IssuedCertStatResponse>> getIssuedCertStat(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam("statType") StatType statType) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
