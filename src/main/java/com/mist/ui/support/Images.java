package com.mist.ui.support;

import com.mist.service.MediaService;
import javafx.scene.image.Image;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loads covers, screenshots and avatars stored in MongoDB, in the background, and keeps the recent ones.
 *
 * <p>Images are decoded at the size they are shown, so a grid of covers stays light; the cache key includes that
 * size. A missing or broken image is logged and leaves the placeholder the view shows by default.</p>
 */
public final class Images {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");
    /** How many decoded images stay in memory. */
    private static final int CACHE_SIZE = 150;

    /**
     * Identifies a decoded image: which media document, at what size.
     *
     * @param id     the media id
     * @param width  the width it was decoded at
     * @param height the height it was decoded at
     */
    private record Key(ObjectId id, double width, double height) {
    }

    /** Runs the loads off the JavaFX thread. */
    private final UiTasks tasks;
    /** The media service, available once connected. */
    private final Supplier<MediaService> media;
    /** The recently decoded images, least recently used first. */
    private final Map<Key, Image> cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Image> eldest) {
            return size() > CACHE_SIZE;
        }
    });

    /**
     * Creates the image loader.
     *
     * @param tasks runs the loads in the background
     * @param media gives the media service, once connected
     */
    public Images(UiTasks tasks, Supplier<MediaService> media) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.media = Objects.requireNonNull(media, "media");
    }

    /**
     * Loads an image at a given size and hands it over on the JavaFX thread.
     *
     * @param id       the media id, or {@code null} for none (nothing happens)
     * @param width    the width to decode at, keeping the ratio
     * @param height   the height to decode at, keeping the ratio
     * @param onLoaded receives the image
     */
    public void load(ObjectId id, double width, double height, Consumer<Image> onLoaded) {
        if (id == null) {
            return;
        }
        Key key = new Key(id, width, height);
        Image cached = cache.get(key);
        if (cached != null) {
            onLoaded.accept(cached);
            return;
        }
        tasks.run(null, () -> new Image(media.get().image(id).openStream(), width, height, true, true), image -> {
            if (image.isError()) {
                LOG.warn("Image {} could not be decoded", id, image.getException());
                return;
            }
            cache.put(key, image);
            onLoaded.accept(image);
        }, error -> LOG.warn("Image {} could not be loaded: {}", id, error.getMessage()));
    }

    /** Forgets every decoded image, after one was replaced. */
    public void clear() {
        cache.clear();
    }
}
