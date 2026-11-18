package com.mist.ui.support;

import com.mist.model.Game;
import com.mist.model.SystemRequirements;
import com.mist.service.AchievementForm;
import com.mist.service.GameForm;
import com.mist.service.RequirementsForm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Builds the {@link GameForm} that {@code PublishingService#update} needs from a stored game, so a screen that changes
 * one value (the dashboard's price) sends every other value unchanged, and reads prices typed in either language.
 */
public final class GameForms {

    /** Not instantiable: a holder of form builders. */
    private GameForms() {
    }

    /**
     * Builds the form of a game with another price.
     *
     * @param game  the stored game
     * @param price the new price
     * @return the form with the game's other values unchanged
     */
    public static GameForm withPrice(Game game, BigDecimal price) {
        SystemRequirements requirements = game.getRequirements();
        RequirementsForm requirementsForm = requirements == null ? null : new RequirementsForm(requirements.os(),
                requirements.processor(), requirements.memory(), requirements.graphics(), requirements.storage());
        List<AchievementForm> achievements = game.getAchievements().stream()
                .map(achievement -> new AchievementForm(achievement.name(), achievement.description()))
                .toList();
        return new GameForm(game.getName(), game.getDescription(), price, game.releaseDay(), game.getPegi(),
                requirementsForm, game.getTags(), game.getLanguages(), achievements);
    }

    /**
     * Reads a price typed by the user: {@code 19,99}, {@code 19.99} or {@code 19,99 €}.
     *
     * @param text the typed text
     * @return the price with two decimals, or {@code null} if the text is not a price from 0 to
     *         {@link Game#MAX_PRICE}
     */
    public static BigDecimal parsePrice(String text) {
        if (text == null) {
            return null;
        }
        // Drop the euro sign and every space, including the no-break spaces some locales put before it.
        String digits = text.replace("€", "").replaceAll("[ \t\u00a0\u202f]", "").replace(',', '.')
                .toLowerCase(Locale.ROOT);
        try {
            BigDecimal price = new BigDecimal(digits);
            if (price.signum() < 0 || price.compareTo(Game.MAX_PRICE) > 0 || price.scale() > 2) {
                return null;
            }
            return price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }
}
