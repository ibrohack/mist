package com.mist.service;

import com.mist.model.Review;

/**
 * A review as a game's page shows it, with its author's nickname.
 *
 * @param review         the review
 * @param authorNickname the nickname of the player who wrote it, or {@code null} if the account no longer exists
 */
public record GameReview(Review review, String authorNickname) {
}
