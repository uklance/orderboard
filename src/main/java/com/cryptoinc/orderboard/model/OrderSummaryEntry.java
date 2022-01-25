package com.cryptoinc.orderboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
public class OrderSummaryEntry {
    private final String coinType;
    private final Side side;
    private final BigDecimal quantity;
    private final String currency;
    private final BigDecimal price;
}
