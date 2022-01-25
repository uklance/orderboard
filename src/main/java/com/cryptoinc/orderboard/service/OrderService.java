package com.cryptoinc.orderboard.service;

import com.cryptoinc.orderboard.exception.NoSuchOrderException;
import com.cryptoinc.orderboard.model.Order;
import com.cryptoinc.orderboard.model.OrderSummaryEntry;
import com.cryptoinc.orderboard.model.Side;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.stream.Collectors.toList;

@Component
public class OrderService {
    private static final Comparator<OrderSummaryEntry> ORDER_SUMMARY_ENTRY_COMPARATOR = Comparator
            .comparing(OrderSummaryEntry::getCoinType)
            .thenComparing(OrderSummaryEntry::getCurrency)
            .thenComparing(os -> os.getSide() == Side.SELL ? os.getPrice() : os.getPrice().negate());

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private final Map<Order, Integer> orderCounts = new HashMap<>(); // count per order as we can have duplicate orders
    private final Map<String, OrderSummaryEntry> orderSummaryEntries = new HashMap<>();

    /**
     * Adds an order and maintains the summary
     * @param order order to add
     */
    public void addOrder(Order order) {
        writeLock.lock();
        try {
            Integer prevCount = orderCounts.get(order);
            orderCounts.put(order, prevCount == null ? 1 : prevCount + 1);
            updateOrderSummary(order.getCoinType(), order.getSide(), order.getQuantity(), order.getCurrency(), order.getPrice());
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Cancels an existing order and maintains the summary
     * @param order order to cancel
     * @throws NoSuchOrderException if order cannot be found
     */
    public void cancelOrder(Order order) throws NoSuchOrderException {
        writeLock.lock();
        try {
            Integer orderCount = orderCounts.get(order);
            if (orderCount == null) {
                throw new NoSuchOrderException(order);
            }
            if (orderCount == 1) {
                orderCounts.remove(order);
            } else {
                orderCounts.put(order, orderCount - 1);
            }

            // remove the order from the order summary by applying the opposite side
            Side oppositeSide = order.getSide() == Side.BUY ? Side.SELL : Side.BUY;
            updateOrderSummary(order.getCoinType(), oppositeSide, order.getQuantity(), order.getCurrency(), order.getPrice());
        } finally {
            writeLock.unlock();
        }
    }

    public List<OrderSummaryEntry> getOrderSummary() {
        readLock.lock();
        try {
            return orderSummaryEntries.values().stream().sorted(ORDER_SUMMARY_ENTRY_COMPARATOR).collect(toList());
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Update the order summary
     */
    private void updateOrderSummary(String coinType, Side side, BigDecimal quantity, String currency, BigDecimal price) {
        String key = String.format("%s|%s|%s", coinType, currency, price.stripTrailingZeros().toPlainString());
        OrderSummaryEntry prevEntry = orderSummaryEntries.get(key);
        Side newSide;
        BigDecimal newQuantity;
        if (prevEntry == null) {
            newSide = side;
            newQuantity = quantity;
        } else {
            // convert prev and current to BUY then add
            BigDecimal newBuyQuantity = asBuy(prevEntry.getSide(), prevEntry.getQuantity())
                            .add(asBuy(side, quantity));
            newSide = newBuyQuantity.signum() < 0 ? Side.SELL : Side.BUY;
            newQuantity = newBuyQuantity.abs();
        }
        if (newQuantity.signum() == 0) {
            orderSummaryEntries.remove(key);
        } else {
            OrderSummaryEntry newEntry = OrderSummaryEntry
                    .builder()
                    .coinType(coinType)
                    .side(newSide)
                    .quantity(newQuantity)
                    .currency(currency)
                    .price(price)
                    .build();

            orderSummaryEntries.put(key, newEntry);
        }
    }

    /**
     * Represent the quantity as a BUY (eg negate SELL)
     */
    private BigDecimal asBuy(Side side, BigDecimal quantity) {
        return side == Side.BUY ? quantity : quantity.negate();
    }
}
