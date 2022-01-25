package com.cryptoinc.orderboard.exception;

import com.cryptoinc.orderboard.model.Order;
import lombok.Getter;

public class NoSuchOrderException extends RuntimeException {
    @Getter
    private final Order order;

    public NoSuchOrderException(Order order) {
        super(order.toString());
        this.order = order;
    }
}
