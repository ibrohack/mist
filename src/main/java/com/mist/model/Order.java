package com.mist.model;

import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A purchase, stored in the {@code orders} collection.
 *
 * <p>An order embeds its items, each with the game's name and price at purchase time, and a copy of the card it was
 * paid with, so the receipt never changes afterwards. Players can return single games: a returned item keeps its line
 * with a refund time, and the order becomes {@link OrderStatus#PARTIALLY_REFUNDED} or {@link OrderStatus#REFUNDED}.</p>
 */
@Entity
@Table(name = "orders")
public class Order {

    /** Most games one order can hold. */
    public static final int MAX_ITEMS = 50;

    /** The document id. */
    @Id
    @ObjectIdGenerator
    private ObjectId id;

    /** The buyer. */
    private ObjectId playerId;
    /** The games bought, with their price at the time. */
    private List<OrderItem> items;
    /** What was charged. */
    private BigDecimal total;

    /** Paid, partially or fully refunded. */
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /** The card charged, masked. */
    private PaymentCard paidWith;
    /** When it was placed. */
    private Instant createdAt;

    /** Used by Hibernate. */
    protected Order() {
    }

    /**
     * Creates a paid order.
     *
     * @param playerId  the buyer
     * @param items     the games bought
     * @param total     what was charged
     * @param paidWith  the card charged
     * @param createdAt when it was placed
     */
    private Order(ObjectId playerId, List<OrderItem> items, BigDecimal total, PaymentCard paidWith,
                  Instant createdAt) {
        this.playerId = playerId;
        this.items = items;
        this.total = total;
        this.status = OrderStatus.PAID;
        this.paidWith = paidWith;
        this.createdAt = createdAt;
    }

    /**
     * Places a paid order. The card is charged by simulation, so placing the order means it was paid.
     *
     * @param playerId the buyer
     * @param items    the games bought, usually the cart; none refunded and none repeated
     * @param card     the card it is paid with
     * @param now      the current time
     * @return the paid order, whose total is the sum of the item prices
     * @throws IllegalArgumentException with {@link ModelKeys#ORDER_ITEMS}, {@link ModelKeys#ORDER_DUPLICATE_GAME} or
     *                                  {@link ModelKeys#PLAYER_CARD} if the order is invalid
     */
    public static Order place(ObjectId playerId, List<OrderItem> items, PaymentCard card, Instant now) {
        Objects.requireNonNull(playerId, "playerId");
        Checks.require(items != null && !items.isEmpty() && items.size() <= MAX_ITEMS, ModelKeys.ORDER_ITEMS);
        Set<ObjectId> games = new HashSet<>();
        for (OrderItem item : items) {
            Checks.require(!item.isRefunded(), ModelKeys.ORDER_ITEMS);
            Checks.require(games.add(item.gameId()), ModelKeys.ORDER_DUPLICATE_GAME);
        }
        BigDecimal total = items.stream().map(OrderItem::price).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
        return new Order(playerId, List.copyOf(items), total, Checks.present(card, ModelKeys.PLAYER_CARD),
                Objects.requireNonNull(now, "now"));
    }

    /**
     * Returns the document id.
     *
     * @return the id, or {@code null} before the order is first saved
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Returns the buyer.
     *
     * @return the id of the player in {@code users}
     */
    public ObjectId getPlayerId() {
        return playerId;
    }

    /**
     * Returns the games bought.
     *
     * @return an unmodifiable view of the items, in cart order
     */
    public List<OrderItem> getItems() {
        return items == null ? List.of() : Collections.unmodifiableList(items);
    }

    /**
     * Finds the line of a game in this order.
     *
     * @param gameId the game
     * @return the item, or empty if the game is not in the order
     */
    public Optional<OrderItem> item(ObjectId gameId) {
        return getItems().stream().filter(item -> item.gameId().equals(gameId)).findFirst();
    }

    /**
     * Returns the amount charged when the order was placed.
     *
     * @return the total at scale 2; refunds do not change it
     */
    public BigDecimal getTotal() {
        return total;
    }

    /**
     * Returns the amount given back for returned games.
     *
     * @return the sum of the refunded items' prices, at scale 2
     */
    public BigDecimal refundedAmount() {
        return getItems().stream().filter(OrderItem::isRefunded).map(OrderItem::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    /**
     * Returns the state of the order.
     *
     * @return the status
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Returns the card the order was paid with, as it was then.
     *
     * @return the card, without its number
     */
    public PaymentCard getPaidWith() {
        return paidWith;
    }

    /**
     * Returns when the order was placed and paid.
     *
     * @return the purchase time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Tells whether a game of this order can still be returned.
     *
     * @param gameId the game
     * @param now    the current time
     * @param window how long after the purchase a return is allowed; the rule belongs to the service
     * @return {@code true} if the game is in the order, not yet refunded, and the window has not closed
     */
    public boolean isRefundable(ObjectId gameId, Instant now, Duration window) {
        return item(gameId).filter(item -> !item.isRefunded()).isPresent()
                && now.isBefore(createdAt.plus(window));
    }

    /**
     * Records the return of one game and refunds its price to the card.
     *
     * @param gameId the game returned
     * @param now    the current time
     * @return the amount refunded, which is the price paid for the game
     * @throws IllegalArgumentException with {@link ModelKeys#ORDER_GAME_NOT_IN_ORDER} if the game is not in the order
     * @throws IllegalStateException    with {@link ModelKeys#ORDER_ALREADY_REFUNDED} if it was already refunded
     */
    public BigDecimal refund(ObjectId gameId, Instant now) {
        OrderItem item = item(gameId).orElseThrow(
                () -> new IllegalArgumentException(ModelKeys.ORDER_GAME_NOT_IN_ORDER));
        Checks.requireState(!item.isRefunded(), ModelKeys.ORDER_ALREADY_REFUNDED);
        OrderItem refunded = item.refunded(now);
        items = getItems().stream().map(line -> line.equals(item) ? refunded : line).toList();
        status = getItems().stream().allMatch(OrderItem::isRefunded)
                ? OrderStatus.REFUNDED
                : OrderStatus.PARTIALLY_REFUNDED;
        return item.price();
    }
}
