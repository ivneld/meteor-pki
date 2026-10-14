package com.meteor.pki.api;

import com.meteor.pki.api.request.RenewalRequest;
import com.meteor.pki.api.request.RevocationRequest;
import com.meteor.pki.api.response.RenewalResponse;
import com.meteor.pki.domain.CertWriter;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.Revocation;
import com.meteor.pki.domain.SerialNumber;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CertManageController {

    private final CertWriter certWriter;

    public CertManageController(CertWriter certWriter) {
        this.certWriter = certWriter;
    }

    @PostMapping("/api/certificates/leaf/{akiHex}/renewal")
    public ResponseEntity<RenewalResponse> renewLeafCertificate(@PathVariable String akiHex,
            @RequestBody @Valid RenewalRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @PostMapping("/api/certificates/leaf/{akiHex}/revocation")
    public ResponseEntity<Void> revokeLeafCertificate(@PathVariable String akiHex,
            @RequestBody @Valid RevocationRequest request) {
        throw new CoreException(ErrorCode.NOT_IMPLEMENTED);
    }

    @PostMapping("/api/certificates/sub-ca/{akiHex}/revocation")
    public ResponseEntity<Void> revokeSubCaCertificate(@PathVariable String akiHex,
            @RequestBody @Valid RevocationRequest request) {
        certWriter.revoke(KeyIdentifier.of(akiHex), SerialNumber.of(request.serialNumberHex()), CertType.SUB,
                Revocation.reasonOf(request.reason()));
        return ResponseEntity.ok().build();
    }

}
