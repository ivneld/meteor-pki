package com.meteor.domain;

import com.meteor.ContextTest;
import com.meteor.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRepositoryIT extends ContextTest {

    private static final Address ADDRESS = new Address("Seoul", "Teheran-ro 1", "06000");

    private final OrderRepository orderRepository;

    OrderRepositoryIT(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Test
    void valueObjectsSurviveRoundTrip() {
        Order saved = orderRepository.save(Order.place(1L, "keyboard", 2, Money.of(50_000), ADDRESS));

        Order found = orderRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getUnitPrice()).isEqualTo(Money.of(50_000));
        assertThat(found.getShippingAddress()).isEqualTo(ADDRESS);
        assertThat(found.totalAmount()).isEqualTo(Money.of(100_000));
    }

    @Test
    void saveExistingAppliesDomainState() {
        Order saved = orderRepository.save(Order.place(1L, "keyboard", 1, Money.of(1), ADDRESS));
        saved.markPaid();

        orderRepository.save(saved);

        assertThat(orderRepository.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void countsOrdersByMemberAndStatus() {
        Order paid = orderRepository.save(Order.place(77L, "keyboard", 1, Money.of(1), ADDRESS));
        paid.markPaid();
        orderRepository.save(paid);
        orderRepository.save(Order.place(77L, "mouse", 1, Money.of(1), ADDRESS));

        assertThat(orderRepository.countByMemberIdAndStatus(77L, OrderStatus.CREATED)).isEqualTo(1);
        assertThat(orderRepository.countByMemberIdAndStatus(77L, OrderStatus.PAID)).isEqualTo(1);
    }

}
