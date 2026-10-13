package com.meteor.pki.api;

import com.meteor.pki.api.request.CertStatRefreshRequest;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatRefreshController {

    @PostMapping("/cert-stat/recalculation")
    public void refreshCertStat(@RequestBody @Valid CertStatRefreshRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
