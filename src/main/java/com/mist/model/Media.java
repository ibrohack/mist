package com.mist.model;

import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.types.ObjectId;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * An image stored in MongoDB, in the {@code media} collection: game covers, screenshots and profile pictures.
 *
 * <p>Games and users reference images by id, so loading a game never loads its images. Each image is one document
 * whose {@code data} field is BSON binary, which keeps it far below MongoDB's 16 MiB document limit and makes every
 * image available on any machine that uses the same database. An image never changes after it is stored; a new picture
 * is a new document.</p>
 */
@Entity
@Table(name = "media")
public class Media {

    /** Largest image accepted: 2 MiB. */
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    /** Longest file name accepted. */
    public static final int MAX_FILE_NAME_LENGTH = 255;
    /** Image formats accepted, which are those JavaFX can show. */
    public static final Set<String> CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/gif");

    /** The document id. */
    @Id
    @ObjectIdGenerator
    private ObjectId id;

    /** The image type: PNG, JPEG or GIF. */
    private String contentType;
    /** The image bytes. */
    private byte[] data;
    /** The name of the uploaded file. */
    private String fileName;
    /** The user who uploaded it. */
    private ObjectId ownerId;
    /** When it was uploaded. */
    private Instant createdAt;

    /** Used by Hibernate. */
    protected Media() {
    }

    /**
     * Creates an image document.
     *
     * @param contentType the image type
     * @param data        the bytes
     * @param fileName    the uploaded file name
     * @param ownerId     the uploader
     * @param createdAt   when it was uploaded
     */
    private Media(String contentType, byte[] data, String fileName, ObjectId ownerId, Instant createdAt) {
        this.contentType = contentType;
        this.data = data;
        this.fileName = fileName;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    /**
     * Creates an image to store.
     *
     * @param contentType MIME type: {@code image/png}, {@code image/jpeg} or {@code image/gif}, in any case
     * @param data        the image file's bytes; they are copied
     * @param fileName    the original file name, shown when choosing images
     * @param ownerId     the user who uploaded it
     * @param now         the current time
     * @return the new image
     * @throws IllegalArgumentException with {@link ModelKeys#MEDIA_TYPE}, {@link ModelKeys#MEDIA_SIZE} or
     *                                  {@link ModelKeys#MEDIA_FILE_NAME} if a value is invalid
     */
    public static Media image(String contentType, byte[] data, String fileName, ObjectId ownerId, Instant now) {
        String type = contentType == null ? null : contentType.strip().toLowerCase(Locale.ROOT);
        Checks.require(type != null && CONTENT_TYPES.contains(type), ModelKeys.MEDIA_TYPE);
        Checks.require(data != null && data.length > 0 && data.length <= MAX_BYTES, ModelKeys.MEDIA_SIZE);
        String name = Checks.text(fileName, 1, MAX_FILE_NAME_LENGTH, ModelKeys.MEDIA_FILE_NAME);
        return new Media(type, data.clone(), name, Objects.requireNonNull(ownerId, "ownerId"),
                Objects.requireNonNull(now, "now"));
    }

    /**
     * Returns the document id, which games and users store to reference the image.
     *
     * @return the id, or {@code null} before the image is first saved
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Returns the image format.
     *
     * @return the MIME type, such as {@code image/png}
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the image file's bytes.
     *
     * @return a copy of the bytes
     */
    public byte[] getData() {
        return data.clone();
    }

    /**
     * Opens the image for reading, for example with {@code new javafx.scene.image.Image(media.openStream())}.
     *
     * @return a stream over the image bytes
     */
    public InputStream openStream() {
        return new ByteArrayInputStream(data);
    }

    /**
     * Returns the image size.
     *
     * @return the number of bytes
     */
    public int size() {
        return data.length;
    }

    /**
     * Returns the original file name.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns who uploaded the image.
     *
     * @return the id of the user in {@code users}
     */
    public ObjectId getOwnerId() {
        return ownerId;
    }

    /**
     * Returns when the image was uploaded.
     *
     * @return the upload time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
