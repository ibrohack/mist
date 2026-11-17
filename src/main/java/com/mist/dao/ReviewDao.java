package com.mist.dao;

import com.mist.exception.DataAccessException;
import com.mist.model.Review;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

/**
 * Data access for the {@code reviews} collection.
 *
 * <p>A unique index on {@code {gameId, playerId}} allows one review per player and game.</p>
 */
public interface ReviewDao {

    /**
     * Finds the review a player wrote for a game.
     *
     * @param gameId   the game
     * @param playerId the player
     * @return the review, or empty if the player has not reviewed the game
     * @throws DataAccessException if the database fails
     */
    Optional<Review> findByGameAndPlayer(ObjectId gameId, ObjectId playerId) throws DataAccessException;

    /**
     * Reads one page of a game's reviews.
     *
     * @param gameId the game
     * @param offset how many reviews to skip
     * @param limit  how many reviews to return at most
     * @return the reviews, newest first
     * @throws DataAccessException if the database fails
     */
    List<Review> findByGame(ObjectId gameId, int offset, int limit) throws DataAccessException;

    /**
     * Stores a new review. Reviews never change once written.
     *
     * @param review the review, without an id
     * @return the stored review, with its id
     * @throws com.mist.exception.DuplicateEntityException if the player already reviewed the game
     * @throws DataAccessException                        if the database fails
     */
    Review insert(Review review) throws DataAccessException;
}
