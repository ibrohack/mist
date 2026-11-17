package com.mist.service;

import com.mist.model.Achievement;

/**
 * An achievement shown on a game's page, as typed in a form.
 *
 * @param name        achievement name
 * @param description how to unlock it
 */
public record AchievementForm(String name, String description) {

    /**
     * Builds the model value.
     *
     * @return the achievement
     * @throws IllegalArgumentException with a model key if a value is blank or too long
     */
    Achievement toAchievement() {
        return new Achievement(name, description);
    }
}
