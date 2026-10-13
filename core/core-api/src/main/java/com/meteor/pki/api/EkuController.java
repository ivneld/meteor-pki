package com.meteor.pki.api;

import java.util.List;

import com.meteor.pki.api.request.EkuCreateRequest;
import com.meteor.pki.api.response.EkuResponse;
import com.meteor.pki.domain.EkuReader;
import com.meteor.pki.domain.EkuWriter;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/extended-key-usages")
public class EkuController {

    private final EkuReader ekuReader;

    private final EkuWriter ekuWriter;

    public EkuController(EkuReader ekuReader, EkuWriter ekuWriter) {
        this.ekuReader = ekuReader;
        this.ekuWriter = ekuWriter;
    }

    @PostMapping
    public ResponseEntity<Void> createExtendedKeyUsage(@RequestBody @Valid EkuCreateRequest request) {
        ekuWriter.register(request.oid(), request.purpose());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<EkuResponse>> getExtendedKeyUsages() {
        return ResponseEntity.ok(ekuReader.findAll().stream().map(EkuResponse::from).toList());
    }

}
