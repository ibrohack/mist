package com.mist.ui;

import com.mist.service.AuthService;
import com.mist.service.LibraryService;
import com.mist.service.LogService;
import com.mist.service.MediaService;
import com.mist.service.PublishingService;
import com.mist.service.ReviewService;
import com.mist.service.StoreService;
import com.mist.service.UserService;

import java.util.Objects;

/**
 * The services the views call, handed to the UI by the {@link Backend} once the database is reachable.
 *
 * @param auth       login, registration and password changes
 * @param users      profiles, preferences, avatars and deactivation
 * @param store      catalog, cart, wishlist and checkout
 * @param library    owned games, orders and returns
 * @param reviews    ratings and reviews
 * @param publishing developers' games and sales
 * @param media      covers, screenshots and avatars
 * @param logs       the database operation log, for admins
 */
public record AppServices(AuthService auth, UserService users, StoreService store, LibraryService library,
                          ReviewService reviews, PublishingService publishing, MediaService media,
                          LogService logs) {

    /**
     * Checks that every service is present.
     *
     * @throws NullPointerException if a service is missing
     */
    public AppServices {
        Objects.requireNonNull(auth, "auth");
        Objects.requireNonNull(users, "users");
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(reviews, "reviews");
        Objects.requireNonNull(publishing, "publishing");
        Objects.requireNonNull(media, "media");
        Objects.requireNonNull(logs, "logs");
    }
}
