package com.mist.dao.hibernate;

import com.mist.dao.MediaDao;
import com.mist.exception.DataAccessException;
import com.mist.model.Media;
import org.bson.types.ObjectId;

import java.util.Objects;
import java.util.Optional;

/** {@link MediaDao} implemented with Hibernate ORM on MongoDB. */
public class HibernateMediaDao extends HibernateDao implements MediaDao {

    /**
     * Creates the DAO.
     *
     * @param transactions the transaction runner whose sessions it uses
     */
    public HibernateMediaDao(HibernateTransactions transactions) {
        super(transactions, "media");
    }

    @Override
    public Optional<Media> findById(ObjectId id) throws DataAccessException {
        Objects.requireNonNull(id, "id");
        return execute("FIND", "_id=" + id, session -> Optional.ofNullable(session.find(Media.class, id)));
    }

    @Override
    public Media insert(Media media) throws DataAccessException {
        Objects.requireNonNull(media, "media");
        if (media.getId() != null) {
            throw new IllegalArgumentException("Image already stored: " + media.getId());
        }
        return execute("INSERT", "ownerId=" + media.getOwnerId() + ", type=" + media.getContentType()
                + ", bytes=" + media.size(), session -> persistOrMerge(session, media, true));
    }

    @Override
    public boolean delete(ObjectId id) throws DataAccessException {
        Objects.requireNonNull(id, "id");
        return execute("DELETE", "_id=" + id, session -> {
            Media media = session.find(Media.class, id);
            if (media == null) {
                return false;
            }
            session.remove(media);
            session.flush();
            return true;
        });
    }
}
