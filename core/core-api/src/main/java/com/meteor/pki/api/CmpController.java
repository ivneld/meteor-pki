package com.meteor.pki.api;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CmpController {

    @PostMapping(path = "/cmp", consumes = "application/pkixcmp", produces = "application/pkixcmp")
    public ResponseEntity<byte[]> processCmpMessage(@RequestBody byte[] pkiMessageDer) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
