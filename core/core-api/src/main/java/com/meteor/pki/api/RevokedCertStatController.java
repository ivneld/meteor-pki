package com.meteor.pki.api;

import java.time.LocalDate;
import java.util.List;

import com.meteor.pki.api.response.RevokedCertStatResponse;
import com.meteor.pki.enums.StatType;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats/revoked-cert")
public class RevokedCertStatController {

    @GetMapping
    public ResponseEntity<List<RevokedCertStatResponse>> getRevokedCertStat(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam("statType") StatType statType) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
