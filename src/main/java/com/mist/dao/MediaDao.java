package com.mist.dao;

import com.mist.exception.DataAccessException;
import com.mist.model.Media;
import org.bson.types.ObjectId;

import java.util.Optional;

/**
 * Data access for the {@code media} collection, which holds the images that games and users reference.
 *
 * <p>An image never changes once stored: replacing a cover or an avatar stores a new image and deletes the old one.</p>
 */
public interface MediaDao {

    /**
     * Finds an image by id, with its bytes.
     *
     * @param id the image id
     * @return the image, or empty if there is none
     * @throws DataAccessException if the database fails
     */
    Optional<Media> findById(ObjectId id) throws DataAccessException;

    /**
     * Stores a new image.
     *
     * @param media the image, without an id
     * @return the stored image, with its id
     * @throws DataAccessException if the database fails
     */
    Media insert(Media media) throws DataAccessException;

    /**
     * Deletes an image.
     *
     * @param id the image id
     * @return {@code true} if it existed
     * @throws DataAccessException if the database fails
     */
    boolean delete(ObjectId id) throws DataAccessException;
}
