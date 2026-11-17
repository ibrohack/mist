package com.mist.dao.hibernate;

import com.mist.dao.ReviewDao;
import com.mist.exception.DataAccessException;
import com.mist.model.Review;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** {@link ReviewDao} implemented with Hibernate ORM on MongoDB. */
public class HibernateReviewDao extends HibernateDao implements ReviewDao {

    /**
     * Creates the DAO.
     *
     * @param transactions the transaction runner whose sessions it uses
     */
    public HibernateReviewDao(HibernateTransactions transactions) {
        super(transactions, "reviews");
    }

    @Override
    public Optional<Review> findByGameAndPlayer(ObjectId gameId, ObjectId playerId) throws DataAccessException {
        Objects.requireNonNull(gameId, "gameId");
        Objects.requireNonNull(playerId, "playerId");
        return execute("FIND", "gameId=" + gameId + ", playerId=" + playerId, session -> session
                .createSelectionQuery("from Review where gameId = :gameId and playerId = :playerId", Review.class)
                .setParameter("gameId", gameId)
                .setParameter("playerId", playerId)
                .setMaxResults(1)
                .getResultList().stream().findFirst());
    }

    @Override
    public List<Review> findByGame(ObjectId gameId, int offset, int limit) throws DataAccessException {
        Objects.requireNonNull(gameId, "gameId");
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException("Invalid page: offset " + offset + ", limit " + limit);
        }
        return execute("FIND", "gameId=" + gameId + ", offset=" + offset + ", limit=" + limit, session -> session
                .createSelectionQuery("from Review where gameId = :gameId order by createdAt desc", Review.class)
                .setParameter("gameId", gameId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList());
    }

    @Override
    public Review insert(Review review) throws DataAccessException {
        Objects.requireNonNull(review, "review");
        if (review.getId() != null) {
            throw new IllegalArgumentException("Review already stored: " + review.getId());
        }
        return execute("INSERT", "gameId=" + review.getGameId() + ", playerId=" + review.getPlayerId()
                + ", rating=" + review.getRating(), session -> persistOrMerge(session, review, true));
    }
}
