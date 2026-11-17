package com.mist.model;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Struct;

/**
 * An achievement a game offers, shown on the Game view. Embedded in the game's {@code games} document.
 *
 * @param name        short title of the achievement
 * @param description what the player must do to unlock it
 */
@Embeddable
@Struct(name = "Achievement")
public record Achievement(String name, String description) {

    /** Longest achievement name accepted. */
    public static final int MAX_NAME_LENGTH = 80;
    /** Longest achievement description accepted. */
    public static final int MAX_DESCRIPTION_LENGTH = 300;

    /**
     * Checks and trims both texts.
     *
     * @throws IllegalArgumentException with {@link ModelKeys#ACHIEVEMENT} if a text is blank or too long
     */
    public Achievement {
        name = Checks.text(name, 1, MAX_NAME_LENGTH, ModelKeys.ACHIEVEMENT);
        description = Checks.text(description, 1, MAX_DESCRIPTION_LENGTH, ModelKeys.ACHIEVEMENT);
    }
}
