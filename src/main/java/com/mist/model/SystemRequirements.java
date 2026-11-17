package com.mist.model;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Struct;

/**
 * Minimum system requirements of a game, shown on the Game view. Embedded in the game's {@code games} document.
 *
 * @param os        operating system, such as {@code Windows 10 64-bit}
 * @param processor processor, such as {@code Intel Core i5-8400}
 * @param memory    memory, such as {@code 8 GB RAM}
 * @param graphics  graphics card, such as {@code GeForce GTX 1060}
 * @param storage   free disk space, such as {@code 40 GB}
 */
@Embeddable
@Struct(name = "SystemRequirements")
public record SystemRequirements(String os, String processor, String memory, String graphics, String storage) {

    /** Longest value accepted for each requirement. */
    public static final int MAX_LENGTH = 80;

    /**
     * Checks and trims every requirement.
     *
     * @throws IllegalArgumentException with {@link ModelKeys#REQUIREMENTS} if one is blank or too long
     */
    public SystemRequirements {
        os = Checks.text(os, 1, MAX_LENGTH, ModelKeys.REQUIREMENTS);
        processor = Checks.text(processor, 1, MAX_LENGTH, ModelKeys.REQUIREMENTS);
        memory = Checks.text(memory, 1, MAX_LENGTH, ModelKeys.REQUIREMENTS);
        graphics = Checks.text(graphics, 1, MAX_LENGTH, ModelKeys.REQUIREMENTS);
        storage = Checks.text(storage, 1, MAX_LENGTH, ModelKeys.REQUIREMENTS);
    }
}
