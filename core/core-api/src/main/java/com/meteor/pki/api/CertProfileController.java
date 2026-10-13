package com.meteor.pki.api;

import com.meteor.pki.api.request.CertProfileCreationRequest;
import com.meteor.pki.api.response.CertProfileDetailResponse;
import com.meteor.pki.api.response.CertProfilePageResponse;
import com.meteor.pki.api.response.CertProfileResponse;
import com.meteor.pki.domain.CertProfileReader;
import com.meteor.pki.domain.PageQuery;
import com.meteor.pki.usecase.CertProfileUseCase;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CertProfileController {

    private final CertProfileUseCase certProfileUseCase;

    private final CertProfileReader certProfileReader;

    public CertProfileController(CertProfileUseCase certProfileUseCase, CertProfileReader certProfileReader) {
        this.certProfileUseCase = certProfileUseCase;
        this.certProfileReader = certProfileReader;
    }

    @PostMapping("/api/profiles")
    public ResponseEntity<CertProfileResponse> create(@RequestBody @Valid CertProfileCreationRequest request) {
        return ResponseEntity.ok(CertProfileResponse.from(certProfileUseCase.create(request.toCommand())));
    }

    @GetMapping("/api/profiles/detail")
    public ResponseEntity<CertProfileDetailResponse> getProfileDetail(@RequestParam String profileName) {
        return ResponseEntity.ok(CertProfileDetailResponse.from(certProfileReader.getByName(profileName)));
    }

    @GetMapping("/api/profiles")
    public ResponseEntity<CertProfilePageResponse> getCertProfiles(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(CertProfilePageResponse.from(certProfileReader.getPage(new PageQuery(page, size))));
    }

}
