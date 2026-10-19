package com.mist.model;

/**
 * State of a purchase.
 */
public enum OrderStatus {

    /** Paid from the wallet; the game is in the buyer's library. */
    PAID,

    /** Refunded to the wallet. */
    REFUNDED
}
