package com.cryptoinc.orderboard.controller;

import com.cryptoinc.orderboard.model.Order;
import com.cryptoinc.orderboard.model.OrderSummaryEntry;
import com.cryptoinc.orderboard.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
public class OrderController {
    private final OrderService service;

    @PostMapping("/order/add")
    public void addOrder(@RequestBody Order order) {
        service.addOrder(order);
    }

    @PostMapping("/order/cancel")
    public void cancelOrder(@RequestBody Order order) {
        service.cancelOrder(order);
    }

    @GetMapping("/order/summary")
    @ResponseBody
    public List<OrderSummaryEntry> orderSummary() {
        return service.getOrderSummary();
    }
}
