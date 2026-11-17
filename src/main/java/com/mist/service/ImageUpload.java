package com.mist.service;

import com.mist.model.Media;
import org.bson.types.ObjectId;

import java.time.Instant;

/**
 * An image chosen in a file dialog, before it is stored.
 *
 * @param contentType {@code image/png}, {@code image/jpeg} or {@code image/gif}
 * @param data        the file's bytes, up to 2 MiB
 * @param fileName    the original file name
 */
public record ImageUpload(String contentType, byte[] data, String fileName) {

    /**
     * Builds the model value.
     *
     * @param ownerId the user who uploads it
     * @param now     the current time
     * @return the image, not stored yet
     * @throws IllegalArgumentException with a model key if the type, size or name is invalid
     */
    Media toMedia(ObjectId ownerId, Instant now) {
        return Media.image(contentType, data, fileName, ownerId, now);
    }

    /** Leaves the bytes out. */
    @Override
    public String toString() {
        return "ImageUpload[contentType=" + contentType + ", bytes=" + (data == null ? 0 : data.length)
                + ", fileName=" + fileName + "]";
    }
}
