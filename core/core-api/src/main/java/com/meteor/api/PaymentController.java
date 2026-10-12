package com.meteor.api;

import com.meteor.api.request.PaymentPayRequest;
import com.meteor.api.response.PaymentResponse;
import com.meteor.domain.PaymentReader;
import com.meteor.usecase.PaymentUseCase;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    private final PaymentReader paymentReader;

    public PaymentController(PaymentUseCase paymentUseCase, PaymentReader paymentReader) {
        this.paymentUseCase = paymentUseCase;
        this.paymentReader = paymentReader;
    }

    @PostMapping
    public PaymentResponse pay(@RequestBody @Valid PaymentPayRequest request) {
        return PaymentResponse.from(paymentUseCase.pay(request.toCommand()));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable Long paymentId) {
        return PaymentResponse.from(paymentReader.find(paymentId));
    }

}
