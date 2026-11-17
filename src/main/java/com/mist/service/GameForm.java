package com.mist.service;

import com.mist.model.Pegi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The editable details of a game, as typed on the developer's game form. Covers and screenshots are uploaded
 * separately.
 *
 * @param name         game name
 * @param description  description, or {@code null}
 * @param price        price in euros, 0 for a free game
 * @param releaseDay   release day, or {@code null}
 * @param pegi         age rating, or {@code null}
 * @param requirements minimum system requirements, or {@code null}
 * @param tags         genres, categories and tags; {@code null} means none
 * @param languages    ISO 639-1 language codes; {@code null} means none
 * @param achievements achievements shown on the game page; {@code null} means none
 */
public record GameForm(String name, String description, BigDecimal price, LocalDate releaseDay, Pegi pegi,
                       RequirementsForm requirements, List<String> tags, List<String> languages,
                       List<AchievementForm> achievements) {
}
