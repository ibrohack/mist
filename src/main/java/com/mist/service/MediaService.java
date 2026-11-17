package com.mist.service;

import com.mist.dao.MediaDao;
import com.mist.exception.MistException;
import com.mist.exception.NotFoundException;
import com.mist.model.Media;
import org.bson.types.ObjectId;

import java.util.Objects;

/**
 * Serves the stored images (game covers, screenshots and profile pictures) to the views, which may not use the DAOs.
 *
 * <p>Games and users only hold image ids, so a list of games never loads its images; a view asks for each image when
 * it shows it.</p>
 */
public class MediaService {

    private final MediaDao media;

    /**
     * Creates the service.
     *
     * @param media the media DAO
     */
    public MediaService(MediaDao media) {
        this.media = Objects.requireNonNull(media, "media");
    }

    /**
     * Loads an image with its bytes.
     *
     * @param mediaId the image id, such as a game's {@code coverId} or a user's {@code avatarId}
     * @return the image
     * @throws NotFoundException {@link ServiceKeys#MEDIA_NOT_FOUND} if there is no such image
     * @throws MistException     if the database fails
     */
    public Media image(ObjectId mediaId) throws MistException {
        if (mediaId == null) {
            throw new NotFoundException(ServiceKeys.MEDIA_NOT_FOUND);
        }
        return media.findById(mediaId).orElseThrow(() -> new NotFoundException(ServiceKeys.MEDIA_NOT_FOUND));
    }
}
