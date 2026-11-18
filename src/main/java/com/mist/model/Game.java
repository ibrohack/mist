package com.mist.model;

import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A game in the catalog, stored in the {@code games} collection.
 *
 * <p>It embeds what is always shown with it: achievements, minimum requirements and a sales and rating summary
 * ({@link GameStats}). It references its developer in {@code users} and its cover and screenshots in {@code media}.
 * Reviews grow without bound, so they live in their own collection.</p>
 *
 * <p>A game starts as a {@link GameStatus#DRAFT draft}, is {@link #publish() published} once it is complete, and can
 * be {@link #delist() delisted}; owners keep a delisted game in their library.</p>
 */
@Entity
@Table(name = "games")
public class Game {

    /** Longest game name accepted. */
    public static final int MAX_NAME_LENGTH = 120;
    /** Longest description accepted. */
    public static final int MAX_DESCRIPTION_LENGTH = 4000;
    /** Highest price accepted. */
    public static final BigDecimal MAX_PRICE = new BigDecimal("999.99");
    /** Most tags a game can have. */
    public static final int MAX_TAGS = 10;
    /** Longest tag accepted. */
    public static final int MAX_TAG_LENGTH = 24;
    /** Most languages a game can list. */
    public static final int MAX_LANGUAGES = 30;
    /** Most screenshots a game can have. */
    public static final int MAX_SCREENSHOTS = 8;
    /** Most achievements a game can have. */
    public static final int MAX_ACHIEVEMENTS = 50;

    /** The shape of a two-letter language code. */
    private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2}");

    /** The document id. */
    @Id
    @ObjectIdGenerator
    private ObjectId id;

    /** The name in the store. */
    private String name;
    /** The store description. */
    private String description;
    /** The price in euros. */
    private BigDecimal price;
    /** The cover image in the media collection. */
    private ObjectId coverId;
    /** The screenshots in the media collection. */
    private List<ObjectId> screenshotIds;
    /** The release day, at midnight UTC. */
    private Instant releaseDate;
    /** The developer who publishes it. */
    private ObjectId developerId;
    /** The minimum system requirements. */
    private SystemRequirements requirements;

    /** The PEGI age rating. */
    @Enumerated(EnumType.STRING)
    private Pegi pegi;

    /** The genre tags, lower case. */
    private List<String> tags;
    /** The supported languages, as two-letter codes. */
    private List<String> languages;
    /** The achievements shown on the game page. */
    private List<Achievement> achievements;
    /** Units sold, revenue and ratings. */
    private GameStats stats;

    /** Draft, published or delisted. */
    @Enumerated(EnumType.STRING)
    private GameStatus status;

    /** When the draft was created. */
    private Instant createdAt;

    /** Used by Hibernate. */
    protected Game() {
    }

    /**
     * Creates a draft.
     *
     * @param developerId the developer
     * @param name        the name
     * @param price       the price
     * @param createdAt   when the draft was created
     */
    private Game(ObjectId developerId, String name, BigDecimal price, Instant createdAt) {
        this.developerId = developerId;
        this.name = name;
        this.price = price;
        this.createdAt = createdAt;
        this.screenshotIds = List.of();
        this.tags = List.of();
        this.languages = List.of();
        this.achievements = List.of();
        this.stats = GameStats.EMPTY;
        this.status = GameStatus.DRAFT;
    }

    /**
     * Starts a new game as a draft, not yet visible in the store.
     *
     * @param developerId the developer creating it
     * @param name        game name
     * @param price       price, 0.00 to 999.99; rounded half-up to cents
     * @param now         the current time
     * @return the draft, with no sales and no reviews
     * @throws IllegalArgumentException with {@link ModelKeys#GAME_NAME} or {@link ModelKeys#GAME_PRICE}
     */
    public static Game draft(ObjectId developerId, String name, BigDecimal price, Instant now) {
        return new Game(Objects.requireNonNull(developerId, "developerId"), checkedName(name), checkedPrice(price),
                Objects.requireNonNull(now, "now"));
    }

    /**
     * Checks a game name.
     *
     * @param name the name
     * @return the name without surrounding spaces
     */
    private static String checkedName(String name) {
        return Checks.text(name, 1, MAX_NAME_LENGTH, ModelKeys.GAME_NAME);
    }

    /**
     * Checks a price.
     *
     * @param price the price
     * @return the price with two decimals
     */
    private static BigDecimal checkedPrice(BigDecimal price) {
        BigDecimal cents = Checks.money(price, ModelKeys.GAME_PRICE);
        Checks.require(cents.compareTo(MAX_PRICE) <= 0, ModelKeys.GAME_PRICE);
        return cents;
    }

    /**
     * Returns the document id.
     *
     * @return the id, or {@code null} before the game is first saved
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Returns the game name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description shown on the Game view.
     *
     * @return the description, or {@code null} while a draft has none
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the current price.
     *
     * @return the price at scale 2; zero for a free game
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Returns the cover image.
     *
     * @return the id of the image in {@code media}, or {@code null} while a draft has none
     */
    public ObjectId getCoverId() {
        return coverId;
    }

    /**
     * Returns the screenshots.
     *
     * @return an unmodifiable view of the ids of the images in {@code media}, in display order
     */
    public List<ObjectId> getScreenshotIds() {
        return view(screenshotIds);
    }

    /**
     * Returns the release date as stored.
     *
     * @return midnight UTC of the release day, or {@code null} while a draft has none
     */
    public Instant getReleaseDate() {
        return releaseDate;
    }

    /**
     * Returns the release day, for display.
     *
     * @return the release day, or {@code null} while a draft has none
     */
    public LocalDate releaseDay() {
        return releaseDate == null ? null : LocalDate.ofInstant(releaseDate, ZoneOffset.UTC);
    }

    /**
     * Returns the developer who publishes the game.
     *
     * @return the id of the developer in {@code users}
     */
    public ObjectId getDeveloperId() {
        return developerId;
    }

    /**
     * Returns the minimum system requirements.
     *
     * @return the requirements, or {@code null} while a draft has none
     */
    public SystemRequirements getRequirements() {
        return requirements;
    }

    /**
     * Returns the PEGI age rating.
     *
     * @return the rating, or {@code null} while a draft has none
     */
    public Pegi getPegi() {
        return pegi;
    }

    /**
     * Returns the genres, categories and tags, used to filter the store.
     *
     * @return an unmodifiable view of the lowercase tags
     */
    public List<String> getTags() {
        return view(tags);
    }

    /**
     * Returns the languages the game supports.
     *
     * @return an unmodifiable view of the two-letter ISO 639-1 codes
     */
    public List<String> getLanguages() {
        return view(languages);
    }

    /**
     * Returns the achievements the game offers.
     *
     * @return an unmodifiable view of the achievements, in display order
     */
    public List<Achievement> getAchievements() {
        return view(achievements);
    }

    /**
     * Returns the sales and rating summary.
     *
     * @return the statistics
     */
    public GameStats getStats() {
        return stats;
    }

    /**
     * Returns the publication state.
     *
     * @return the status
     */
    public GameStatus getStatus() {
        return status;
    }

    /**
     * Returns when the game was created.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Tells whether the game costs nothing, for the store's "free games" filter.
     *
     * @return {@code true} if the price is zero
     */
    public boolean isFree() {
        return price.signum() == 0;
    }

    /**
     * Tells whether a player can buy the game now.
     *
     * @return {@code true} if the game is published
     */
    public boolean isPurchasable() {
        return status == GameStatus.PUBLISHED;
    }

    /**
     * Renames the game.
     *
     * @param name the new name
     * @throws IllegalArgumentException with {@link ModelKeys#GAME_NAME} if it is blank or too long
     */
    public void rename(String name) {
        this.name = checkedName(name);
    }

    /**
     * Changes the description.
     *
     * @param description the new description; blank removes it
     * @throws IllegalArgumentException with {@link ModelKeys#GAME_DESCRIPTION} if it is too long
     */
    public void changeDescription(String description) {
        this.description = Checks.optionalText(description, MAX_DESCRIPTION_LENGTH, ModelKeys.GAME_DESCRIPTION);
    }

    /**
     * Changes the price. Past orders keep the price they were paid at.
     *
     * @param price the new price, 0.00 to 999.99; rounded half-up to cents
     * @throws IllegalArgumentException with {@link ModelKeys#GAME_PRICE} if it is out of range
     */
    public void changePrice(BigDecimal price) {
        this.price = checkedPrice(price);
    }

    /**
     * Changes the cover image.
     *
     * @param coverId id of the new image in {@code media}, or {@code null} to remove it
     */
    public void changeCover(ObjectId coverId) {
        this.coverId = coverId;
    }

    /**
     * Replaces the screenshots. Repeated ids are kept once.
     *
     * @param screenshotIds ids of the images in {@code media}, in display order
     * @throws IllegalArgumentException with {@link ModelKeys#GAME_SCREENSHOTS} if there are too many
     */
    public void changeScreenshots(List<ObjectId> screenshotIds) {
        List<ObjectId> distinct = List.copyOf(new LinkedHashSet<>(screenshotIds));
        Checks.require(distinct.size() <= MAX_SCREENSHOTS, ModelKeys.GAME_SCREENSHOTS);
        this.screenshotIds = distinct;
    }

    /**
     * Changes the release day.
     *
     * @param releaseDay the release day, stored as midnight UTC; {@code null} removes it
     */
    public void changeReleaseDay(LocalDate releaseDay) {
        this.releaseDate = releaseDay == null ? null : releaseDay.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Changes the minimum system requirements.
     *
     * @param requirements the new requirements, or {@code null} to remove them
     */
    public void changeRequirements(SystemRequirements requirements) {
        this.requirements = requirements;
    }

    /**
     * Changes the PEGI age rating.
     *
     * @param pegi the new rating, or {@code null} to remove it
     */
    public void changePegi(Pegi pegi) {
        this.pegi = pegi;
    }

    /**
     * Replaces the tags. Tags are trimmed and lowercased, and repeated tags are kept once.
     *
     * @param tags the new tags
     * @throws IllegalArgumentException with {@link ModelKeys#GAME_TAGS} if there are too many or one is blank or too
     *                                  long
     */
    public void changeTags(List<String> tags) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            normalized.add(Checks.text(tag, 1, MAX_TAG_LENGTH, ModelKeys.GAME_TAGS).toLowerCase(Locale.ROOT));
        }
        Checks.require(normalized.size() <= MAX_TAGS, ModelKeys.GAME_TAGS);
        this.tags = List.copyOf(normalized);
    }

    /**
     * Replaces the supported languages. Codes are trimmed and lowercased, and repeated codes are kept once.
     *
     * @param languages two-letter ISO 639-1 codes, such as {@code es} and {@code en}
     * @throws IllegalArgumentException with {@link ModelKeys#GAME_LANGUAGES} if there are too many or a code is
     *                                  malformed
     */
    public void changeLanguages(List<String> languages) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String language : languages) {
            String code = language == null ? null : language.strip().toLowerCase(Locale.ROOT);
            normalized.add(Checks.matching(code, LANGUAGE, ModelKeys.GAME_LANGUAGES));
        }
        Checks.require(normalized.size() <= MAX_LANGUAGES, ModelKeys.GAME_LANGUAGES);
        this.languages = List.copyOf(normalized);
    }

    /**
     * Replaces the achievements.
     *
     * @param achievements the new achievements, in display order
     * @throws IllegalArgumentException with {@link ModelKeys#GAME_ACHIEVEMENTS} if there are too many
     */
    public void changeAchievements(List<Achievement> achievements) {
        Checks.require(achievements.size() <= MAX_ACHIEVEMENTS, ModelKeys.GAME_ACHIEVEMENTS);
        this.achievements = List.copyOf(achievements);
    }

    /**
     * Puts the game in the store.
     *
     * @throws IllegalStateException with {@link ModelKeys#GAME_NOT_DRAFT} if the game is not a draft, or
     *                               {@link ModelKeys#GAME_INCOMPLETE} if it lacks a cover, description, release date,
     *                               PEGI rating or requirements
     */
    public void publish() {
        Checks.requireState(status == GameStatus.DRAFT, ModelKeys.GAME_NOT_DRAFT);
        Checks.requireState(coverId != null && description != null && releaseDate != null && pegi != null
                && requirements != null, ModelKeys.GAME_INCOMPLETE);
        status = GameStatus.PUBLISHED;
    }

    /**
     * Takes the game out of the store ("dar de baja"). Players who own it keep it in their library.
     *
     * @throws IllegalStateException with {@link ModelKeys#GAME_NOT_PUBLISHED} if the game is not published
     */
    public void delist() {
        Checks.requireState(status == GameStatus.PUBLISHED, ModelKeys.GAME_NOT_PUBLISHED);
        status = GameStatus.DELISTED;
    }

    /**
     * Counts a sale in the statistics.
     *
     * @param price the price paid
     */
    public void recordSale(BigDecimal price) {
        stats = stats.withSale(price);
    }

    /**
     * Takes a returned copy off the statistics.
     *
     * @param price the price refunded
     */
    public void recordRefund(BigDecimal price) {
        stats = stats.withRefund(price);
    }

    /**
     * Counts a new review's rating in the statistics.
     *
     * @param rating the rating, 1 to 5
     */
    public void recordRating(int rating) {
        stats = stats.withRating(rating);
    }

    /**
     * Returns a read-only view of a list.
     *
     * @param list the list, possibly {@code null}
     * @param <T>  the element type
     * @return an unmodifiable list, empty for {@code null}
     */
    private static <T> List<T> view(List<T> list) {
        return list == null ? List.of() : Collections.unmodifiableList(list);
    }
}
