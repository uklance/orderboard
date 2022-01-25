package com.cryptoinc.orderboard.controller;

import com.cryptoinc.orderboard.model.Order;
import com.cryptoinc.orderboard.model.OrderSummaryEntry;
import com.cryptoinc.orderboard.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private OrderService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Test
    public void testAdd() throws Exception {
        String orderJson = objectMapper.writeValueAsString(Order.builder().coinType("ETH").build());
        mvc.perform(post("/order/add").content(orderJson).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service, times(1)).addOrder(orderCaptor.capture());

        assertThat(orderCaptor.getValue().getCoinType()).isEqualTo("ETH");
    }

    @Test
    public void testOrderSummary() throws Exception {
        List<OrderSummaryEntry> entries = List.of(OrderSummaryEntry.builder().coinType("BTC").build());
        when(service.getOrderSummary()).thenReturn(entries);
        MvcResult result = mvc.perform(get("/order/summary"))
                .andExpect(status().isOk())
                .andReturn();

        verify(service, times(1)).getOrderSummary();

        String responseJson = result.getResponse().getContentAsString();
        assertThat(responseJson).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(entries));
    }
}