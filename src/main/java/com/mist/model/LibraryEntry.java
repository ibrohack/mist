package com.mist.model;

import jakarta.persistence.Embeddable;
import org.bson.types.ObjectId;
import org.hibernate.annotations.Struct;

import java.time.Instant;
import java.util.Objects;

/**
 * A game in a player's library. Embedded in the player's {@code users} document.
 *
 * @param gameId     the game, in {@code games}
 * @param orderId    the order it was bought with, in {@code orders}
 * @param acquiredAt when the order was paid
 */
@Embeddable
@Struct(name = "LibraryEntry")
public record LibraryEntry(ObjectId gameId, ObjectId orderId, Instant acquiredAt) {

    /**
     * Checks that every part is present.
     *
     * @throws NullPointerException if a part is missing
     */
    public LibraryEntry {
        Objects.requireNonNull(gameId, "gameId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(acquiredAt, "acquiredAt");
    }
}
