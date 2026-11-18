package com.mist.dao.hibernate;

import com.mist.dao.UserDao;
import com.mist.exception.DataAccessException;
import com.mist.model.User;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** {@link UserDao} implemented with Hibernate ORM on MongoDB. */
public class HibernateUserDao extends HibernateDao implements UserDao {

    /**
     * Creates the DAO.
     *
     * @param transactions the transaction runner whose sessions it uses
     */
    public HibernateUserDao(HibernateTransactions transactions) {
        super(transactions, "users");
    }

    @Override
    public Optional<User> findById(ObjectId id) throws DataAccessException {
        Objects.requireNonNull(id, "id");
        return execute("FIND", "_id=" + id, session -> Optional.ofNullable(session.find(User.class, id)));
    }

    @Override
    public Optional<User> findByEmail(String email) throws DataAccessException {
        Objects.requireNonNull(email, "email");
        return execute("FIND", "email=" + email, session -> session
                .createSelectionQuery("from User where email = :email", User.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList().stream().findFirst());
    }

    @Override
    public List<User> findByIds(Collection<ObjectId> ids) throws DataAccessException {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<ObjectId> distinct = ids.stream().distinct().toList();
        return execute("FIND", "_id in " + ids(distinct), session -> session
                .createSelectionQuery("from User where id in :ids", User.class)
                .setParameterList("ids", distinct)
                .getResultList());
    }

    @Override
    public boolean existsByEmail(String email) throws DataAccessException {
        Objects.requireNonNull(email, "email");
        return exists("email=" + email, "select id from User where email = :value", email);
    }

    @Override
    public boolean existsByNickname(String nickname) throws DataAccessException {
        Objects.requireNonNull(nickname, "nickname");
        return exists("nickname=" + nickname, "select id from Player where nickname = :value", nickname);
    }

    @Override
    public boolean existsByStudioName(String studioName) throws DataAccessException {
        Objects.requireNonNull(studioName, "studioName");
        return exists("studioName=" + studioName, "select id from Developer where studioName = :value", studioName);
    }

    /**
     * Tells whether a query finds a user, reading only the {@code _id} of at most one document instead of the whole
     * account.
     *
     * @param details what the operation log shows
     * @param query   an HQL query selecting {@code id}, with one parameter named {@code value}
     * @param value   the value looked for
     * @return {@code true} if a user matches
     * @throws DataAccessException if the database fails
     */
    private boolean exists(String details, String query, String value) throws DataAccessException {
        return execute("EXISTS", details, session -> !session
                .createSelectionQuery(query, ObjectId.class)
                .setParameter("value", value)
                .setMaxResults(1)
                .getResultList().isEmpty());
    }

    @Override
    public <T extends User> T save(T user) throws DataAccessException {
        Objects.requireNonNull(user, "user");
        boolean isNew = user.getId() == null;
        String details = isNew
                ? "role=" + user.role() + ", email=" + user.getEmail()
                : "_id=" + user.getId() + ", role=" + user.role();
        return execute(isNew ? "INSERT" : "UPDATE", details, session -> persistOrMerge(session, user, isNew));
    }
}
