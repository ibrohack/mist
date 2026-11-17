package com.mist.dao;

import com.mist.exception.DataAccessException;
import com.mist.model.Order;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

/** Data access for the {@code orders} collection. */
public interface OrderDao {

    /**
     * Finds an order by id.
     *
     * @param id the order id
     * @return the order, or empty if there is none
     * @throws DataAccessException if the database fails
     */
    Optional<Order> findById(ObjectId id) throws DataAccessException;

    /**
     * Lists a player's purchase history.
     *
     * @param playerId the player
     * @return the player's orders, newest first
     * @throws DataAccessException if the database fails
     */
    List<Order> findByPlayer(ObjectId playerId) throws DataAccessException;

    /**
     * Inserts a new order, or writes the refunds of an existing one.
     *
     * @param order the order; a new one has no id yet
     * @return the saved order, with its id; use it instead of the argument afterwards
     * @throws DataAccessException if the database fails
     */
    Order save(Order order) throws DataAccessException;
}
