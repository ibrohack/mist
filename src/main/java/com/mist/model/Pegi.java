package com.mist.model;

/**
 * PEGI age rating of a game.
 */
public enum Pegi {

    /** Suitable for all ages. */
    PEGI_3(3),

    /** Suitable from 7 years old. */
    PEGI_7(7),

    /** Suitable from 12 years old. */
    PEGI_12(12),

    /** Suitable from 16 years old. */
    PEGI_16(16),

    /** Adults only. */
    PEGI_18(18);

    private final int minimumAge;

    Pegi(int minimumAge) {
        this.minimumAge = minimumAge;
    }

    /**
     * Returns the age the rating is suitable from.
     *
     * @return the minimum age in years
     */
    public int minimumAge() {
        return minimumAge;
    }
}
