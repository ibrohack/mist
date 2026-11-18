package com.mist.ui.component;

import com.mist.model.Order;
import com.mist.model.OrderItem;

/**
 * A row of the orders tree: an order, or one of its games under it.
 *
 * @param order      the order
 * @param item       the game of the row, or {@code null} for the order's own row
 * @param returnable whether the game of the row can still be returned
 */
public record OrderRow(Order order, OrderItem item, boolean returnable) {

    /**
     * Tells whether the row is a game of an order.
     *
     * @return {@code true} for a game row, {@code false} for an order row
     */
    public boolean isItem() {
        return item != null;
    }
}
