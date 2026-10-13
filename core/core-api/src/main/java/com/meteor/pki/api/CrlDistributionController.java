package com.meteor.pki.api;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CrlDistributionController {

    @GetMapping(value = "/api/crl/{caSkiHex}.crl", produces = "application/pkix-crl")
    public ResponseEntity<byte[]> downloadLatest(@PathVariable String caSkiHex) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
