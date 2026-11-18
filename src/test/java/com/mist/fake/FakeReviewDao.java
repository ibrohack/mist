package com.mist.fake;

import com.mist.dao.DaoKeys;
import com.mist.dao.ReviewDao;
import com.mist.exception.DataAccessException;
import com.mist.exception.DuplicateEntityException;
import com.mist.model.Review;
import org.bson.types.ObjectId;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * {@link ReviewDao} on the in-memory database, with the unique game and player pair of the real index.
 */
public final class FakeReviewDao implements ReviewDao {

    /** The shared in-memory database. */
    private final FakeDatabase db;

    /**
     * Creates the DAO.
     *
     * @param db the shared in-memory database
     */
    public FakeReviewDao(FakeDatabase db) {
        this.db = db;
    }

    @Override
    public Optional<Review> findByGameAndPlayer(ObjectId gameId, ObjectId playerId) throws DataAccessException {
        db.check();
        return db.reviews.values().stream()
                .filter(review -> review.getGameId().equals(gameId) && review.getPlayerId().equals(playerId))
                .findFirst();
    }

    @Override
    public List<Review> findByGame(ObjectId gameId, int offset, int limit) throws DataAccessException {
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException("Invalid page: offset " + offset + ", limit " + limit);
        }
        db.check();
        return db.reviews.values().stream()
                .filter(review -> review.getGameId().equals(gameId))
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public Review insert(Review review) throws DataAccessException {
        db.check();
        boolean duplicate = db.reviews.values().stream().anyMatch(other ->
                other.getGameId().equals(review.getGameId()) && other.getPlayerId().equals(review.getPlayerId()));
        if (duplicate) {
            throw new DuplicateEntityException(DaoKeys.DUPLICATE);
        }
        db.reviews.put(FakeDatabase.assignId(review), review);
        return review;
    }
}
