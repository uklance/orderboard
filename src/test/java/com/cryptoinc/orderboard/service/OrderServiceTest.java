package com.cryptoinc.orderboard.service;

import com.cryptoinc.orderboard.IOUtil;
import com.cryptoinc.orderboard.exception.NoSuchOrderException;
import com.cryptoinc.orderboard.model.Order;
import com.cryptoinc.orderboard.model.OrderSummaryEntry;
import com.cryptoinc.orderboard.model.Side;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrderServiceTest {
    private ObjectMapper objectMapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
    private OrderService service;

    @BeforeEach
    public void beforeEach() {
        service = new OrderService();
    }

    @Test
    public void testValuesFromSpec() throws IOException {
        assertOrderboard("testValuesFromSpec-orders.json", "testValuesFromSpec-orderboard.json");
    }

    @Test
    public void testBuyAndSell() throws IOException {
        assertOrderboard("testBuyAndSell-orders.json", "testBuyAndSell-orderboard.json");
    }

    @Test
    public void testCancel() {
        // given
        Order order1 = Order.builder()
                .userId("user1")
                .coinType("ETH")
                .side(Side.BUY)
                .currency("GBP")
                .price(BigDecimal.valueOf(100))
                .quantity(BigDecimal.valueOf(10))
                .build();

        Order order2 = Order.builder()
                .userId("user2")
                .coinType("BTC")
                .side(Side.SELL)
                .currency("GBP")
                .price(BigDecimal.valueOf(200))
                .quantity(BigDecimal.valueOf(20))
                .build();

        // when
        service.addOrder(order1.toBuilder().build());
        service.addOrder(order1.toBuilder().build());
        service.addOrder(order2.toBuilder().build());
        service.addOrder(order2.toBuilder().build());

        List<OrderSummaryEntry> summary1 = service.getOrderSummary();

        assertThatThrownBy(() -> service.cancelOrder(order1.toBuilder().userId("user2").build()))
                .isInstanceOf(NoSuchOrderException.class);

        assertThatThrownBy(() -> service.cancelOrder(order1.toBuilder().coinType("BTC").build()))
                .isInstanceOf(NoSuchOrderException.class);

        assertThatThrownBy(() -> service.cancelOrder(order1.toBuilder().side(Side.SELL).build()))
                .isInstanceOf(NoSuchOrderException.class);

        assertThatThrownBy(() -> service.cancelOrder(order1.toBuilder().price(BigDecimal.valueOf(101)).build()))
                .isInstanceOf(NoSuchOrderException.class);

        assertThatThrownBy(() -> service.cancelOrder(order1.toBuilder().quantity(BigDecimal.valueOf(11)).build()))
                .isInstanceOf(NoSuchOrderException.class);

        service.cancelOrder(order1);

        List<OrderSummaryEntry> summary2 = service.getOrderSummary();

        service.cancelOrder(order1);

        List<OrderSummaryEntry> summary3 = service.getOrderSummary();

        assertThatThrownBy(() -> service.cancelOrder(order1)).isInstanceOf(NoSuchOrderException.class);

        // then
        Predicate<OrderSummaryEntry> predicate = entry -> "ETH".equals(entry.getCoinType());
        Optional<OrderSummaryEntry> found1 = summary1.stream().filter(predicate).findFirst();
        Optional<OrderSummaryEntry> found2 = summary2.stream().filter(predicate).findFirst();
        Optional<OrderSummaryEntry> found3 = summary3.stream().filter(predicate).findFirst();

        assertThat(found1.get().getQuantity()).isEqualTo(BigDecimal.valueOf(20));
        assertThat(found2.get().getQuantity()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(found3.isPresent()).isFalse();
    }

    private void assertOrderboard(String orderJsonPath, String orderboardJsonPath) throws IOException {
        // given
        Resource orderResource = new ClassPathResource("OrderboardServiceTest/" + orderJsonPath);
        Resource orderboardResource = new ClassPathResource("OrderboardServiceTest/" + orderboardJsonPath);
        String expectedJson = IOUtil.readString(orderboardResource);
        Order[] orders = objectMapper.readValue(orderResource.getInputStream(), Order[].class);

        // when
        Arrays.stream(orders).parallel().forEach(service::addOrder);
        List<OrderSummaryEntry> orderSummary = service.getOrderSummary();
        String actualJson = objectMapper.writeValueAsString(orderSummary);

        // then
        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }
}