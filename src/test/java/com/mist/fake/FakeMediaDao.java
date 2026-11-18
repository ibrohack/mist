package com.mist.fake;

import com.mist.dao.MediaDao;
import com.mist.exception.DataAccessException;
import com.mist.model.Media;
import org.bson.types.ObjectId;

import java.util.Optional;

/**
 * {@link MediaDao} on the in-memory database.
 */
public final class FakeMediaDao implements MediaDao {

    /** The shared in-memory database. */
    private final FakeDatabase db;

    /**
     * Creates the DAO.
     *
     * @param db the shared in-memory database
     */
    public FakeMediaDao(FakeDatabase db) {
        this.db = db;
    }

    @Override
    public Optional<Media> findById(ObjectId id) throws DataAccessException {
        db.check();
        return Optional.ofNullable(db.media.get(id));
    }

    @Override
    public Media insert(Media media) throws DataAccessException {
        db.check();
        db.media.put(FakeDatabase.assignId(media), media);
        return media;
    }

    @Override
    public boolean delete(ObjectId id) throws DataAccessException {
        db.check();
        return db.media.remove(id) != null;
    }
}
