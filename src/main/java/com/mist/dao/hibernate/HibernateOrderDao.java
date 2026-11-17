package com.mist.dao.hibernate;

import com.mist.dao.OrderDao;
import com.mist.exception.DataAccessException;
import com.mist.model.Order;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** {@link OrderDao} implemented with Hibernate ORM on MongoDB. */
public class HibernateOrderDao extends HibernateDao implements OrderDao {

    /**
     * Creates the DAO.
     *
     * @param transactions the transaction runner whose sessions it uses
     */
    public HibernateOrderDao(HibernateTransactions transactions) {
        super(transactions, "orders");
    }

    @Override
    public Optional<Order> findById(ObjectId id) throws DataAccessException {
        Objects.requireNonNull(id, "id");
        return execute("FIND", "_id=" + id, session -> Optional.ofNullable(session.find(Order.class, id)));
    }

    @Override
    public List<Order> findByPlayer(ObjectId playerId) throws DataAccessException {
        Objects.requireNonNull(playerId, "playerId");
        return execute("FIND", "playerId=" + playerId, session -> session
                .createSelectionQuery("from Order where playerId = :playerId order by createdAt desc", Order.class)
                .setParameter("playerId", playerId)
                .getResultList());
    }

    @Override
    public Order save(Order order) throws DataAccessException {
        Objects.requireNonNull(order, "order");
        boolean isNew = order.getId() == null;
        String details = isNew
                ? "playerId=" + order.getPlayerId() + ", items=" + order.getItems().size() + ", total=" + order.getTotal()
                : "_id=" + order.getId() + ", status=" + order.getStatus();
        return execute(isNew ? "INSERT" : "UPDATE", details, session -> persistOrMerge(session, order, isNew));
    }
}
