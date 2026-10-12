package com.meteor.api;

import com.meteor.api.request.PaymentPayRequest;
import com.meteor.domain.Money;
import com.meteor.domain.Payment;
import com.meteor.domain.PaymentReader;
import com.meteor.enums.PaymentMethod;
import com.meteor.enums.PaymentStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import com.meteor.support.web.ApiControllerAdvice;
import com.meteor.test.api.RestDocsTest;
import com.meteor.usecase.PaymentUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static com.meteor.support.docs.ProblemFields.problem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends RestDocsTest {

    private PaymentUseCase paymentUseCase;

    @BeforeEach
    void setUp() {
        paymentUseCase = mock(PaymentUseCase.class);
        mockMvc = mockController(new PaymentController(paymentUseCase, mock(PaymentReader.class)),
                new ApiControllerAdvice());
    }

    @Test
    void pay() throws Exception {
        when(paymentUseCase.pay(any()))
            .thenReturn(Payment.restore(1L, 1L, Money.of(100_000), PaymentMethod.CARD, PaymentStatus.APPROVED, "tx-1"));

        mockMvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(json()))
            .andExpect(status().isOk())
            .andDo(document("payment-pay", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(fieldWithPath("orderId").type(JsonFieldType.NUMBER).description("주문 ID"),
                            fieldWithPath("method").type(JsonFieldType.STRING).description("결제 수단")),
                    responseFields(fieldWithPath("id").type(JsonFieldType.NUMBER).description("결제 ID"),
                            fieldWithPath("orderId").type(JsonFieldType.NUMBER).description("주문 ID"),
                            fieldWithPath("amount").type(JsonFieldType.NUMBER).description("승인 금액(원)"),
                            fieldWithPath("method").type(JsonFieldType.STRING).description("결제 수단"),
                            fieldWithPath("status").type(JsonFieldType.STRING).description("결제 상태"))));
    }

    @Test
    void payFailedAtGateway() throws Exception {
        when(paymentUseCase.pay(any()))
            .thenThrow(new CoreException(ErrorCode.PAYMENT_FAILED).property("paymentId", 1L));

        mockMvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(json()))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("P003"))
            .andDo(document("payment-pay-failed", preprocessResponse(prettyPrint()),
                    problem(fieldWithPath("paymentId").type(JsonFieldType.NUMBER).description("실패로 남은 결제 ID"))));
    }

    private static String json() {
        return JsonMapper.builder().build().writeValueAsString(new PaymentPayRequest(1L, PaymentMethod.CARD));
    }

}
