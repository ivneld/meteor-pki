package com.meteor.api;

import com.meteor.api.request.OrderPlaceRequest;
import com.meteor.domain.Address;
import com.meteor.domain.Money;
import com.meteor.domain.Order;
import com.meteor.domain.OrderReader;
import com.meteor.domain.OrderWriter;
import com.meteor.enums.OrderStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import com.meteor.support.web.ApiControllerAdvice;
import com.meteor.test.api.RestDocsTest;
import com.meteor.usecase.OrderUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import static com.meteor.support.docs.ProblemFields.problem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest extends RestDocsTest {

    private static final Address ADDRESS = new Address("Seoul", "Teheran-ro 1", "06000");

    private OrderUseCase orderUseCase;

    private OrderReader orderReader;

    private OrderWriter orderWriter;

    @BeforeEach
    void setUp() {
        orderUseCase = mock(OrderUseCase.class);
        orderReader = mock(OrderReader.class);
        orderWriter = mock(OrderWriter.class);
        mockMvc = mockController(new OrderController(orderUseCase, orderReader, orderWriter),
                new ApiControllerAdvice());
    }

    @Test
    void place() throws Exception {
        when(orderUseCase.place(any())).thenReturn(order(OrderStatus.CREATED));

        OrderPlaceRequest request = new OrderPlaceRequest(1L, "keyboard", 2, 50_000,
                new OrderPlaceRequest.AddressRequest("Seoul", "Teheran-ro 1", "06000"));

        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(json(request)))
            .andExpect(status().isOk())
            .andDo(document("order-place", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                            fieldWithPath("productName").type(JsonFieldType.STRING).description("상품명"),
                            fieldWithPath("quantity").type(JsonFieldType.NUMBER).description("수량"),
                            fieldWithPath("unitPrice").type(JsonFieldType.NUMBER).description("단가(원)"),
                            fieldWithPath("shippingAddress.city").type(JsonFieldType.STRING).description("시"),
                            fieldWithPath("shippingAddress.street").type(JsonFieldType.STRING).description("도로명"),
                            fieldWithPath("shippingAddress.zipCode").type(JsonFieldType.STRING).description("우편번호")),
                    responseFields(orderFields())));
    }

    @Test
    void getNotFound() throws Exception {
        when(orderReader.find(eq(999L)))
            .thenThrow(new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", 999L));

        mockMvc.perform(get("/api/v1/orders/{orderId}", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("O001"))
            .andDo(document("order-get-not-found", preprocessResponse(prettyPrint()),
                    pathParameters(parameterWithName("orderId").description("주문 ID")),
                    problem(fieldWithPath("orderId").type(JsonFieldType.NUMBER).description("조회한 주문 ID"))));
    }

    @Test
    void cancelPaidOrder() throws Exception {
        when(orderWriter.cancel(eq(1L)))
            .thenThrow(new CoreException(ErrorCode.ORDER_NOT_CANCELLABLE).property("orderId", 1L)
                .property("orderStatus", OrderStatus.PAID));

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", 1L))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("O003"))
            .andDo(document("order-cancel-conflict", preprocessResponse(prettyPrint()),
                    pathParameters(parameterWithName("orderId").description("주문 ID")),
                    problem(fieldWithPath("orderId").type(JsonFieldType.NUMBER).description("주문 ID"),
                            fieldWithPath("orderStatus").type(JsonFieldType.STRING).description("현재 주문 상태"))));
    }

    private static Order order(OrderStatus status) {
        return Order.restore(1L, 1L, "keyboard", 2, Money.of(50_000), ADDRESS, status);
    }

    private static FieldDescriptor[] orderFields() {
        return new FieldDescriptor[] { fieldWithPath("id").type(JsonFieldType.NUMBER).description("주문 ID"),
                fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                fieldWithPath("productName").type(JsonFieldType.STRING).description("상품명"),
                fieldWithPath("quantity").type(JsonFieldType.NUMBER).description("수량"),
                fieldWithPath("unitPrice").type(JsonFieldType.NUMBER).description("단가(원)"),
                fieldWithPath("totalAmount").type(JsonFieldType.NUMBER).description("총액(원)"),
                fieldWithPath("shippingAddress.city").type(JsonFieldType.STRING).description("시"),
                fieldWithPath("shippingAddress.street").type(JsonFieldType.STRING).description("도로명"),
                fieldWithPath("shippingAddress.zipCode").type(JsonFieldType.STRING).description("우편번호"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("주문 상태") };
    }

    private static String json(Object body) {
        return JsonMapper.builder().build().writeValueAsString(body);
    }

}
