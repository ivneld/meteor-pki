package com.meteor.pki.api;

import com.meteor.pki.api.request.KeyAgreementRequest;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(prefix = "key-agreement", name = "role", havingValue = "server")
@RestController
@RequestMapping("/api/key-agreement")
public class KeyAgreementController {

    @PostMapping("/request-public-key")
    public ResponseEntity<byte[]> issueKeyAgreementPublicKey() {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @PostMapping("/operate")
    public ResponseEntity<Void> operateKeyAgreement(@RequestBody @Valid KeyAgreementRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
