package com.mist.fake;

import com.mist.dao.OrderDao;
import com.mist.exception.DataAccessException;
import com.mist.model.Order;
import org.bson.types.ObjectId;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * {@link OrderDao} on the in-memory database, listing a player's orders newest first as the real query does.
 */
public final class FakeOrderDao implements OrderDao {

    /** The shared in-memory database. */
    private final FakeDatabase db;

    /**
     * Creates the DAO.
     *
     * @param db the shared in-memory database
     */
    public FakeOrderDao(FakeDatabase db) {
        this.db = db;
    }

    @Override
    public Optional<Order> findById(ObjectId id) throws DataAccessException {
        db.check();
        return Optional.ofNullable(db.orders.get(id));
    }

    @Override
    public List<Order> findByPlayer(ObjectId playerId) throws DataAccessException {
        db.check();
        return db.orders.values().stream()
                .filter(order -> order.getPlayerId().equals(playerId))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public Order save(Order order) throws DataAccessException {
        db.check();
        db.orders.put(FakeDatabase.assignId(order), order);
        return order;
    }
}
