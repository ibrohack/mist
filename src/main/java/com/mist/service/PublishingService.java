package com.mist.service;

import com.mist.dao.GameDao;
import com.mist.dao.MediaDao;
import com.mist.dao.TransactionRunner;
import com.mist.dao.UserDao;
import com.mist.exception.MistException;
import com.mist.exception.NotFoundException;
import com.mist.exception.UnauthorizedException;
import com.mist.exception.ValidationException;
import com.mist.model.Achievement;
import com.mist.model.Developer;
import com.mist.model.Game;
import com.mist.model.Media;
import com.mist.model.ModelKeys;
import com.mist.model.SystemRequirements;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lets developers create, edit, publish and delist their games, and read their sales statistics.
 *
 * <p>A game starts as a draft. It can be published once it has a cover, a description, a release date, a PEGI rating
 * and system requirements; publishing also adds it to the developer's catalog, in the same transaction. A developer can
 * only change their own games.</p>
 */
public class PublishingService {

    /** The games collection. */
    private final GameDao games;
    /** The users collection. */
    private final UserDao users;
    /** The media collection, where images are stored. */
    private final MediaDao media;
    /** Runs the changes that touch several documents. */
    private final TransactionRunner transactions;
    /** The clock changes are dated with. */
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param games        the game DAO
     * @param users        the user DAO
     * @param media        the media DAO, for covers and screenshots
     * @param transactions the transaction runner
     * @param clock        the clock that dates new games and uploads
     */
    public PublishingService(GameDao games, UserDao users, MediaDao media, TransactionRunner transactions,
                             Clock clock) {
        this.games = Objects.requireNonNull(games, "games");
        this.users = Objects.requireNonNull(users, "users");
        this.media = Objects.requireNonNull(media, "media");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Creates a draft game.
     *
     * @param developerId the logged-in developer
     * @param name        game name
     * @param price       price in euros, 0 for a free game
     * @return the new draft, with its id
     * @throws ValidationException if the name or price is invalid
     * @throws MistException       if the account cannot act or the database fails
     */
    public Game createDraft(ObjectId developerId, String name, BigDecimal price) throws MistException {
        Developer developer = Accounts.activeDeveloper(users, developerId);
        return games.save(Rules.check(() -> Game.draft(developer.getId(), name, price, clock.instant())));
    }

    /**
     * Replaces a game's editable details. Nothing changes if any value is invalid.
     *
     * @param developerId the logged-in developer
     * @param gameId      the game
     * @param form        the new details
     * @return the updated game
     * @throws ValidationException   if a value is invalid
     * @throws UnauthorizedException {@link ServiceKeys#GAME_NOT_OWNER} if the game belongs to another developer
     * @throws MistException         if the account cannot act, the game does not exist or the database fails
     */
    public Game update(ObjectId developerId, ObjectId gameId, GameForm form) throws MistException {
        Objects.requireNonNull(form, "form");
        Game game = owned(Accounts.activeDeveloper(users, developerId), gameId);
        SystemRequirements requirements = Rules.check(
                () -> form.requirements() == null ? null : form.requirements().toRequirements());
        List<Achievement> achievements = Rules.check(() -> form.achievements() == null ? List.<Achievement>of()
                : form.achievements().stream().map(AchievementForm::toAchievement).toList());
        Rules.apply(() -> {
            game.rename(form.name());
            game.changeDescription(form.description());
            game.changePrice(form.price());
            game.changeReleaseDay(form.releaseDay());
            game.changePegi(form.pegi());
            game.changeRequirements(requirements);
            game.changeTags(form.tags() == null ? List.of() : form.tags());
            game.changeLanguages(form.languages() == null ? List.of() : form.languages());
            game.changeAchievements(achievements);
        });
        return games.save(game);
    }

    /**
     * Replaces a game's cover. The new image is stored, the game points to it and the old image is deleted, in one
     * transaction.
     *
     * @param developerId the logged-in developer
     * @param gameId      the game
     * @param image       the new cover
     * @return the updated game
     * @throws ValidationException   if the image type, size or file name is invalid
     * @throws UnauthorizedException {@link ServiceKeys#GAME_NOT_OWNER} if the game belongs to another developer
     * @throws MistException         if the account cannot act, the game does not exist or the database fails
     */
    public Game changeCover(ObjectId developerId, ObjectId gameId, ImageUpload image) throws MistException {
        Objects.requireNonNull(image, "image");
        return transactions.call(() -> {
            Developer developer = Accounts.activeDeveloper(users, developerId);
            Game game = owned(developer, gameId);
            Media stored = media.insert(Rules.check(() -> image.toMedia(developer.getId(), clock.instant())));
            ObjectId previous = game.getCoverId();
            game.changeCover(stored.getId());
            Game saved = games.save(game);
            if (previous != null) {
                media.delete(previous);
            }
            return saved;
        });
    }

    /**
     * Adds a screenshot to a game, in one transaction with storing the image.
     *
     * @param developerId the logged-in developer
     * @param gameId      the game
     * @param image       the screenshot
     * @return the updated game
     * @throws ValidationException   if the game already has the most screenshots allowed, or the image is invalid
     * @throws UnauthorizedException {@link ServiceKeys#GAME_NOT_OWNER} if the game belongs to another developer
     * @throws MistException         if the account cannot act, the game does not exist or the database fails
     */
    public Game addScreenshot(ObjectId developerId, ObjectId gameId, ImageUpload image) throws MistException {
        Objects.requireNonNull(image, "image");
        return transactions.call(() -> {
            Developer developer = Accounts.activeDeveloper(users, developerId);
            Game game = owned(developer, gameId);
            if (game.getScreenshotIds().size() >= Game.MAX_SCREENSHOTS) {
                throw new ValidationException(ModelKeys.GAME_SCREENSHOTS);
            }
            Media stored = media.insert(Rules.check(() -> image.toMedia(developer.getId(), clock.instant())));
            List<ObjectId> screenshots = new ArrayList<>(game.getScreenshotIds());
            screenshots.add(stored.getId());
            Rules.apply(() -> game.changeScreenshots(screenshots));
            return games.save(game);
        });
    }

    /**
     * Removes a screenshot from a game and deletes its image, in one transaction.
     *
     * @param developerId  the logged-in developer
     * @param gameId       the game
     * @param screenshotId the screenshot's image id
     * @return the updated game; unchanged if the screenshot was not the game's
     * @throws UnauthorizedException {@link ServiceKeys#GAME_NOT_OWNER} if the game belongs to another developer
     * @throws MistException         if the account cannot act, the game does not exist or the database fails
     */
    public Game removeScreenshot(ObjectId developerId, ObjectId gameId, ObjectId screenshotId) throws MistException {
        return transactions.call(() -> {
            Game game = owned(Accounts.activeDeveloper(users, developerId), gameId);
            if (screenshotId == null || !game.getScreenshotIds().contains(screenshotId)) {
                return game;
            }
            List<ObjectId> screenshots = new ArrayList<>(game.getScreenshotIds());
            screenshots.remove(screenshotId);
            game.changeScreenshots(screenshots);
            Game saved = games.save(game);
            media.delete(screenshotId);
            return saved;
        });
    }

    /**
     * Publishes a draft, which puts it on sale, and adds it to the developer's catalog, in one transaction.
     *
     * @param developerId the logged-in developer
     * @param gameId      the draft
     * @return the published game
     * @throws ValidationException   if the game is not a draft or lacks a cover, description, release date, PEGI rating
     *                               or requirements
     * @throws UnauthorizedException {@link ServiceKeys#GAME_NOT_OWNER} if the game belongs to another developer
     * @throws MistException         if the account cannot act, the game does not exist or the database fails
     */
    public Game publish(ObjectId developerId, ObjectId gameId) throws MistException {
        return transactions.call(() -> {
            Developer developer = Accounts.activeDeveloper(users, developerId);
            Game game = owned(developer, gameId);
            Rules.apply(game::publish);
            Game saved = games.save(game);
            if (developer.addGame(saved.getId())) {
                users.save(developer);
            }
            return saved;
        });
    }

    /**
     * Takes a published game off the store. Players who own it keep it in their library.
     *
     * @param developerId the logged-in developer
     * @param gameId      the game
     * @return the delisted game
     * @throws ValidationException   if the game is not published
     * @throws UnauthorizedException {@link ServiceKeys#GAME_NOT_OWNER} if the game belongs to another developer
     * @throws MistException         if the account cannot act, the game does not exist or the database fails
     */
    public Game delist(ObjectId developerId, ObjectId gameId) throws MistException {
        Game game = owned(Accounts.activeDeveloper(users, developerId), gameId);
        Rules.apply(game::delist);
        return games.save(game);
    }

    /**
     * Lists a developer's games for the dashboard.
     *
     * @param developerId the logged-in developer
     * @return the games, drafts included, newest first
     * @throws MistException if the account cannot act or the database fails
     */
    public List<Game> myGames(ObjectId developerId) throws MistException {
        return games.findByDeveloper(Accounts.activeDeveloper(users, developerId).getId());
    }

    /**
     * Reads a developer's sales figures.
     *
     * @param developerId the logged-in developer
     * @return total revenue, total units sold and every game with its own statistics
     * @throws MistException if the account cannot act or the database fails
     */
    public SalesReport sales(ObjectId developerId) throws MistException {
        Developer developer = Accounts.activeDeveloper(users, developerId);
        List<Game> catalog = games.findByDeveloper(developer.getId());
        int unitsSold = catalog.stream().mapToInt(game -> game.getStats().unitsSold()).sum();
        return new SalesReport(developer.getTotalRevenue(), unitsSold, catalog);
    }

    /**
     * Loads a game and checks that it belongs to the developer.
     *
     * @param developer the acting developer
     * @param gameId    the game
     * @return the game
     * @throws MistException if it does not exist or is another developer's
     */
    private Game owned(Developer developer, ObjectId gameId) throws MistException {
        Game game = gameId == null ? null : games.findById(gameId).orElse(null);
        if (game == null) {
            throw new NotFoundException(ServiceKeys.GAME_NOT_FOUND);
        }
        if (!game.getDeveloperId().equals(developer.getId())) {
            throw new UnauthorizedException(ServiceKeys.GAME_NOT_OWNER);
        }
        return game;
    }
}
