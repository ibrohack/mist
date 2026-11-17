package com.mist.model;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Struct;

import java.math.BigDecimal;
import java.util.OptionalDouble;

/**
 * Sales and rating summary of a game, embedded in its {@code games} document.
 *
 * <p>The purchase, return and review transactions keep it in step with {@code orders} and {@code reviews}, so the store
 * never aggregates those collections to show a game. The average rating is computed from the count and the sum, so it
 * never drifts.</p>
 *
 * @param unitsSold   copies sold minus copies returned
 * @param revenue     money from sales minus refunds, at scale 2
 * @param ratingCount number of reviews
 * @param ratingSum   sum of every review's rating (1 to 5 each)
 */
@Embeddable
@Struct(name = "GameStats")
public record GameStats(int unitsSold, BigDecimal revenue, int ratingCount, int ratingSum) {

    /** Statistics of a game that was never sold or rated. */
    public static final GameStats EMPTY = new GameStats(0, BigDecimal.ZERO, 0, 0);

    /**
     * Checks that the counters are consistent.
     *
     * @throws IllegalArgumentException if a counter is negative or the rating sum is impossible for the count
     */
    public GameStats {
        revenue = Checks.money(revenue, ModelKeys.AMOUNT);
        if (unitsSold < 0 || ratingCount < 0 || ratingSum < ratingCount || ratingSum > 5 * ratingCount) {
            throw new IllegalArgumentException("Inconsistent game statistics: units " + unitsSold
                    + ", ratings " + ratingCount + ", rating sum " + ratingSum);
        }
    }

    /**
     * Returns the statistics after one more copy was sold.
     *
     * @param price the price paid
     * @return the updated statistics
     * @throws IllegalArgumentException with {@link ModelKeys#AMOUNT} if the price is missing or negative
     */
    public GameStats withSale(BigDecimal price) {
        return new GameStats(unitsSold + 1, revenue.add(Checks.money(price, ModelKeys.AMOUNT)), ratingCount,
                ratingSum);
    }

    /**
     * Returns the statistics after one copy was returned.
     *
     * @param price the price refunded
     * @return the updated statistics
     * @throws IllegalArgumentException with {@link ModelKeys#AMOUNT} if the price is missing, negative or larger than
     *                                  the revenue
     * @throws IllegalStateException    if no copy was sold
     */
    public GameStats withRefund(BigDecimal price) {
        BigDecimal refund = Checks.money(price, ModelKeys.AMOUNT);
        Checks.require(refund.compareTo(revenue) <= 0, ModelKeys.AMOUNT);
        if (unitsSold == 0) {
            throw new IllegalStateException("No sold copy to refund");
        }
        return new GameStats(unitsSold - 1, revenue.subtract(refund), ratingCount, ratingSum);
    }

    /**
     * Returns the statistics after one more review.
     *
     * @param rating the review's rating, 1 to 5
     * @return the updated statistics
     * @throws IllegalArgumentException with {@link ModelKeys#REVIEW_RATING} if the rating is out of range
     */
    public GameStats withRating(int rating) {
        Checks.require(rating >= Review.MIN_RATING && rating <= Review.MAX_RATING, ModelKeys.REVIEW_RATING);
        return new GameStats(unitsSold, revenue, ratingCount + 1, ratingSum + rating);
    }

    /**
     * Returns the average rating shown as "overall reviews".
     *
     * @return the average from 1 to 5, or empty if the game has no reviews
     */
    public OptionalDouble averageRating() {
        return ratingCount == 0 ? OptionalDouble.empty() : OptionalDouble.of((double) ratingSum / ratingCount);
    }
}
