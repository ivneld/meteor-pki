package com.meteor.pki.api;

import com.meteor.pki.api.request.DeploymentTlsBundleRequest;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeploymentTlsBundleController {

    @PostMapping(value = "/internal/deployment/tls-bundle", produces = "application/zip")
    public ResponseEntity<byte[]> issue(@RequestBody @Valid DeploymentTlsBundleRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
