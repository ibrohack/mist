package com.mist.model;

import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Objects;

/**
 * A player's rating and review of a game ("valoración"), stored in the {@code reviews} collection.
 *
 * <p>Reviews grow without bound and are read in pages, so they are not embedded in the game; the game keeps only the
 * count and sum of ratings in its {@link GameStats}. A player reviews each game once, which a unique index on
 * {@code gameId} and {@code playerId} enforces.</p>
 */
@Entity
@Table(name = "reviews")
public class Review {

    /** Lowest rating a review can give. */
    public static final int MIN_RATING = 1;
    /** Highest rating a review can give. */
    public static final int MAX_RATING = 5;
    /** Longest comment accepted. */
    public static final int MAX_COMMENT_LENGTH = 2000;

    @Id
    @ObjectIdGenerator
    private ObjectId id;

    private ObjectId playerId;
    private ObjectId gameId;
    private int rating;
    private String comment;
    private Instant createdAt;

    /** Used by Hibernate. */
    protected Review() {
    }

    private Review(ObjectId playerId, ObjectId gameId, int rating, String comment, Instant createdAt) {
        this.playerId = playerId;
        this.gameId = gameId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    /**
     * Writes a review.
     *
     * @param playerId the player who owns the game
     * @param gameId   the game reviewed
     * @param rating   the rating, 1 to 5
     * @param comment  the review text; blank means a rating without text
     * @param now      the current time
     * @return the new review
     * @throws IllegalArgumentException with {@link ModelKeys#REVIEW_RATING} or {@link ModelKeys#REVIEW_COMMENT}
     */
    public static Review of(ObjectId playerId, ObjectId gameId, int rating, String comment, Instant now) {
        Checks.require(rating >= MIN_RATING && rating <= MAX_RATING, ModelKeys.REVIEW_RATING);
        return new Review(Objects.requireNonNull(playerId, "playerId"), Objects.requireNonNull(gameId, "gameId"),
                rating, Checks.optionalText(comment, MAX_COMMENT_LENGTH, ModelKeys.REVIEW_COMMENT),
                Objects.requireNonNull(now, "now"));
    }

    /**
     * Returns the document id.
     *
     * @return the id, or {@code null} before the review is first saved
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Returns the author.
     *
     * @return the id of the player in {@code users}
     */
    public ObjectId getPlayerId() {
        return playerId;
    }

    /**
     * Returns the game reviewed.
     *
     * @return the id of the game in {@code games}
     */
    public ObjectId getGameId() {
        return gameId;
    }

    /**
     * Returns the rating.
     *
     * @return the rating, 1 to 5
     */
    public int getRating() {
        return rating;
    }

    /**
     * Returns the review text.
     *
     * @return the comment, or {@code null} for a rating without text
     */
    public String getComment() {
        return comment;
    }

    /**
     * Returns when the review was written.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
