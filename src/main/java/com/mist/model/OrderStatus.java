package com.mist.model;

/**
 * State of a purchase.
 */
public enum OrderStatus {

    /** Paid with the player's card; every game in the order is in the player's library. */
    PAID,

    /** Some games of the order were returned and refunded to the card. */
    PARTIALLY_REFUNDED,

    /** Every game of the order was returned and refunded to the card. */
    REFUNDED
}
