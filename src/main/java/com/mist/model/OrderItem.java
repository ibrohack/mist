package com.mist.model;

import jakarta.persistence.Embeddable;
import org.bson.types.ObjectId;
import org.hibernate.annotations.Struct;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A game in an order, embedded in the {@code orders} document.
 *
 * <p>The name and price are copied from the game when the order is placed, so a later rename or price change never
 * alters a past receipt.</p>
 *
 * @param gameId     the game, in {@code games}
 * @param name       the game's name when it was bought
 * @param price      the price paid, at scale 2
 * @param refundedAt when the game was returned, or {@code null} if it was not
 */
@Embeddable
@Struct(name = "OrderItem")
public record OrderItem(ObjectId gameId, String name, BigDecimal price, Instant refundedAt) {

    /**
     * Checks every part.
     *
     * @throws IllegalArgumentException with {@link ModelKeys#GAME_NAME} or {@link ModelKeys#GAME_PRICE}
     * @throws NullPointerException     if the game id is missing
     */
    public OrderItem {
        Objects.requireNonNull(gameId, "gameId");
        name = Checks.text(name, 1, Game.MAX_NAME_LENGTH, ModelKeys.GAME_NAME);
        price = Checks.money(price, ModelKeys.GAME_PRICE);
    }

    /**
     * Copies a saved game's id, name and current price into a new order item.
     *
     * @param game the game being bought; it must have been saved
     * @return the item, not refunded
     */
    public static OrderItem of(Game game) {
        return new OrderItem(game.getId(), game.getName(), game.getPrice(), null);
    }

    /**
     * Tells whether the game was returned.
     *
     * @return {@code true} if it was refunded
     */
    public boolean isRefunded() {
        return refundedAt != null;
    }

    /**
     * Returns this item marked as returned.
     *
     * @param at when the refund happened
     * @return the refunded item
     */
    public OrderItem refunded(Instant at) {
        return new OrderItem(gameId, name, price, Objects.requireNonNull(at, "at"));
    }
}
