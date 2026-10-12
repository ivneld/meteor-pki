package com.meteor.api;

import com.meteor.api.request.OrderPlaceRequest;
import com.meteor.api.response.OrderResponse;
import com.meteor.domain.OrderReader;
import com.meteor.domain.OrderWriter;
import com.meteor.usecase.OrderUseCase;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문 API. 회원 확인이 필요한 주문 생성은 usecase 를, 주문 하나로 끝나는 조회·취소는 도메인 서비스를 부른다. 엔드포인트 하나는 그중 하나만
 * 호출한다(R-11).
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderUseCase orderUseCase;

    private final OrderReader orderReader;

    private final OrderWriter orderWriter;

    public OrderController(OrderUseCase orderUseCase, OrderReader orderReader, OrderWriter orderWriter) {
        this.orderUseCase = orderUseCase;
        this.orderReader = orderReader;
        this.orderWriter = orderWriter;
    }

    @PostMapping
    public OrderResponse place(@RequestBody @Valid OrderPlaceRequest request) {
        return OrderResponse.from(orderUseCase.place(request.toCommand()));
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable Long orderId) {
        return OrderResponse.from(orderReader.find(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long orderId) {
        return OrderResponse.from(orderWriter.cancel(orderId));
    }

}
