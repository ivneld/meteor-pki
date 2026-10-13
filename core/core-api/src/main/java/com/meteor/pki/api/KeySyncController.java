package com.meteor.pki.api;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(prefix = "pki.features.key-sync", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/api")
public class KeySyncController {

    @PostMapping("/key-sync")
    public ResponseEntity<Void> syncKeys() {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

}
