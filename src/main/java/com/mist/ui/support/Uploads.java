package com.mist.ui.support;

import com.mist.exception.ValidationException;
import com.mist.model.Media;
import com.mist.model.ModelKeys;
import com.mist.service.ImageUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Turns an image file the user picked into an {@link ImageUpload} for the services. Runs on a worker thread, since it
 * reads the disk.
 *
 * <p>The size is checked before reading, so picking a huge file never loads it into memory; the type is taken from
 * the extension, and the service checks both again.</p>
 */
public final class Uploads {

    /** The content type of each image extension MIST accepts. */
    private static final Map<String, String> TYPES = Map.of(
            "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg", "gif", "image/gif");

    /** Not instantiable: a holder of upload helpers. */
    private Uploads() {
    }

    /**
     * Reads an image file.
     *
     * @param file the file the user picked
     * @return the upload
     * @throws ValidationException if the file is too big ({@code model.media.size}) or not a PNG, JPEG or GIF
     *                             ({@code model.media.type})
     * @throws IOException         if the file cannot be read
     */
    public static ImageUpload image(Path file) throws ValidationException, IOException {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String type = dot < 0 ? null : TYPES.get(name.substring(dot + 1).toLowerCase(Locale.ROOT));
        if (type == null) {
            throw new ValidationException(ModelKeys.MEDIA_TYPE);
        }
        if (Files.size(file) > Media.MAX_BYTES) {
            throw new ValidationException(ModelKeys.MEDIA_SIZE);
        }
        return new ImageUpload(type, Files.readAllBytes(file), name);
    }
}
