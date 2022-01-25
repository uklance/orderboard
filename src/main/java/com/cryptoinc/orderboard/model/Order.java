package com.cryptoinc.orderboard.model;

import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
public class Order {
    private final String userId;
    private final String coinType;
    private final Side side;
    private final BigDecimal quantity;
    private final String currency;
    private final BigDecimal price;
}
