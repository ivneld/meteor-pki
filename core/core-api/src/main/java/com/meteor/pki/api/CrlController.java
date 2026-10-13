package com.meteor.pki.api;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CrlController {

    @GetMapping(value = "/api/crls/bulk.zip", produces = "application/zip")
    public ResponseEntity<byte[]> downloadBulkCrlZip() {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @GetMapping(value = "/api/crls/{caSkiHex}.crl", produces = "application/pkix-crl")
    public ResponseEntity<byte[]> downloadLatest(@PathVariable String caSkiHex) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @PostMapping(value = "/api/crls/{caSkiHex}/issue", produces = "application/pkix-crl")
    public ResponseEntity<byte[]> issueAndDownload(@PathVariable String caSkiHex) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
